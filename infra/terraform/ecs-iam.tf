# =========================================================
# 1) EC2 인스턴스 Role — ECS 에이전트가 클러스터 등록 + ECR pull
# =========================================================
resource "aws_iam_role" "ecs_instance" {
  name = "mopl-ecs-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_instance" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

# Session Manager로 EC2 터미널 접속 + RDS 포트포워딩을 받을 수 있게
# (인스턴스가 SSM에 등록됨 — SSH 키/22 포트 없이 접속 가능)
resource "aws_iam_role_policy_attachment" "ecs_instance_ssm" {
  role       = aws_iam_role.ecs_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ecs_instance" {
  name = "mopl-ecs-instance-profile"
  role = aws_iam_role.ecs_instance.name
}

# =========================================================
# 2) Task Execution Role — task 시작 시 이미지 pull + 로그 전송
# =========================================================
resource "aws_iam_role" "task_execution" {
  name = "mopl-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# =========================================================
# 3) Task Role — 컨테이너 앱이 AWS(S3) 접근
# =========================================================
resource "aws_iam_role" "task" {
  name = "mopl-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# 앱이 S3 이미지 업로드 (특정 버킷/prefix만)
resource "aws_iam_role_policy" "task_s3" {
  name = "mopl-task-s3"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
      Resource = [
        "arn:aws:s3:::${var.s3_bucket}/thumbnails/*",
        "arn:aws:s3:::${var.s3_bucket}/profiles/*"
      ]
    }]
  })
}
