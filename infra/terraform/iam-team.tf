# =========================================================
# 팀원 개발자용 — SSM Session Manager 접근 권한
#   용도: EC2 터미널 접속 + RDS 포트포워딩(DataGrip 등)
#   사용자는 코드로 생성, 액세스 키는 각자 콘솔에서 발급한다.
#   (액세스 키를 코드로 만들면 secret이 tfstate에 평문 저장되므로 제외)
# =========================================================

# 개발자 그룹 — SSM 권한을 그룹에 한 번만 부여
resource "aws_iam_group" "developers" {
  name = "mopl-developers"
}

# 팀원 IAM 사용자 (team_members 변수로 관리, 코드에서 빼면 삭제됨)
resource "aws_iam_user" "developers" {
  for_each = toset(var.team_members)
  name     = each.value
}

# 사용자를 개발자 그룹에 소속
resource "aws_iam_user_group_membership" "developers" {
  for_each = toset(var.team_members)
  user     = aws_iam_user.developers[each.value].name
  groups   = [aws_iam_group.developers.name]
}

# SSM 세션 + 포트포워딩 권한 (최소 권한)
resource "aws_iam_group_policy" "developers_ssm" {
  name  = "mopl-developers-ssm"
  group = aws_iam_group.developers.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # 세션 시작: mopl ECS 인스턴스(AmazonECSManaged 태그)에만 허용
      {
        Sid      = "StartSessionOnEcsInstances"
        Effect   = "Allow"
        Action   = ["ssm:StartSession"]
        Resource = ["arn:aws:ec2:${var.aws_region}:${var.account_id}:instance/*"]
        Condition = {
          StringEquals = { "ssm:resourceTag/AmazonECSManaged" = "true" }
        }
      },
      # 포트포워딩/셸 문서 사용
      {
        Sid    = "StartSessionDocuments"
        Effect = "Allow"
        Action = ["ssm:StartSession"]
        Resource = [
          "arn:aws:ssm:${var.aws_region}::document/AWS-StartPortForwardingSessionToRemoteHost",
          "arn:aws:ssm:${var.aws_region}:${var.account_id}:document/SSM-SessionManagerRunShell"
        ]
      },
      # 본인이 시작한 세션만 종료/재개
      {
        Sid      = "ManageOwnSession"
        Effect   = "Allow"
        Action   = ["ssm:TerminateSession", "ssm:ResumeSession"]
        Resource = ["arn:aws:ssm:${var.aws_region}:${var.account_id}:session/$${aws:username}-*"]
      },
      # 접속 대상 인스턴스/세션 조회
      {
        Sid    = "Describe"
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ssm:DescribeInstanceInformation",
          "ssm:DescribeSessions"
        ]
        Resource = "*"
      }
    ]
  })
}
