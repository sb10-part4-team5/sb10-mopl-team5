# =========================================================
# CI/CD용 — GitHub Actions (OIDC, 액세스 키 없음)
# 런타임 앱의 S3 접근은 ECS task-role(ecs-iam.tf)이 담당한다.
# =========================================================

# GitHub OIDC Provider는 계정당 1개만 존재 가능.
# 이미 있는 계정에서는 create_github_oidc_provider=false 로 두고 기존 것을 참조한다.
resource "aws_iam_openid_connect_provider" "github" {
  count           = var.create_github_oidc_provider ? 1 : 0
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_iam_openid_connect_provider" "github" {
  count = var.create_github_oidc_provider ? 0 : 1
  url   = "https://token.actions.githubusercontent.com"
}

locals {
  github_oidc_arn = var.create_github_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
}

resource "aws_iam_role" "github_actions" {
  name = "mopl-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = local.github_oidc_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          # 해당 레포의 main 브랜치 워크플로만 이 Role 사용 가능
          # (PR/fork/다른 브랜치는 차단 — 배포는 main에 머지된 것만)
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_org}/${var.github_repo}:ref:refs/heads/main"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_actions" {
  name = "mopl-github-actions-policy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # ECR 로그인 토큰은 리소스 한정이 불가능 (AWS 사양상 *)
      {
        Sid      = "EcrAuth"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      # ECR 특정 리포지토리에만 push/pull
      {
        Sid    = "EcrRepo"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer"
        ]
        Resource = "arn:aws:ecr:${var.aws_region}:${var.account_id}:repository/${var.ecr_repository}"
      },
      # ECS 특정 서비스에만 배포
      {
        Sid      = "Ecs"
        Effect   = "Allow"
        Action   = ["ecs:UpdateService", "ecs:DescribeServices"]
        Resource = "arn:aws:ecs:${var.aws_region}:${var.account_id}:service/${var.ecs_cluster}/${var.ecs_service}"
      },
      # 특정 task role 에만 PassRole
      {
        Sid    = "PassRole"
        Effect = "Allow"
        Action = ["iam:PassRole"]
        Resource = [
          "arn:aws:iam::${var.account_id}:role/mopl-task-execution-role",
          "arn:aws:iam::${var.account_id}:role/mopl-task-role"
        ]
      }
    ]
  })
}
