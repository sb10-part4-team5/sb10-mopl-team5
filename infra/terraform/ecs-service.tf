# Task Definition (레시피) — 컨테이너 실행 설계도
resource "aws_ecs_task_definition" "mopl" {
  family                   = "mopl"
  network_mode             = "awsvpc"
  requires_compatibilities = ["EC2"]
  cpu                      = "512" # 0.5 vCPU
  memory                   = "768" # MB (t3.micro 1GB 중 시스템 여유)
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name      = "mopl"
    image     = "${aws_ecr_repository.mopl.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.mopl.address}:5432/mopl" },
      { name = "DB_USERNAME", value = var.db_username },
      { name = "DB_PASSWORD", value = var.db_password },
      { name = "S3_BUCKET", value = var.s3_bucket },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "JAVA_TOOL_OPTIONS", value = "-Xmx512m" }
    ]

    # logConfiguration 생략 → ECS EC2 기본 json-file (EC2 로컬, CloudWatch 미사용)
    # 추후 Loki 도입 시 awsfirelens(Fluent Bit)로 전환
  }])
}

# Service — Task를 desired_count 만큼 실행/유지 + ALB 연결
resource "aws_ecs_service" "mopl" {
  name            = var.ecs_service
  cluster         = aws_ecs_cluster.mopl.id
  task_definition = aws_ecs_task_definition.mopl.arn
  desired_count   = 1 # 나중에 2로 확장 가능

  network_configuration {
    subnets         = aws_subnet.public[*].id
    security_groups = [aws_security_group.app.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.mopl.arn
    container_name   = "mopl"
    container_port   = 8080
  }

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.mopl.name
    weight            = 100
  }

  # 배포 실패(새 task가 안정화 안 됨) 시 이전 버전으로 자동 롤백
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.http]

  # CD가 매 배포마다 새 task def revision(:sha)으로 갱신하므로 terraform은 무시
  lifecycle {
    ignore_changes = [task_definition]
  }
}
