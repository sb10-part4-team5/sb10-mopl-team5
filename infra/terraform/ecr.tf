# Docker 이미지 저장소
resource "aws_ecr_repository" "mopl" {
  name                 = var.ecr_repository
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true # push 시 취약점 스캔
  }
}

# 오래된 이미지 자동 정리 (최근 10개만 보관)
resource "aws_ecr_lifecycle_policy" "mopl" {
  repository = aws_ecr_repository.mopl.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "최근 10개 이미지만 보관"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
