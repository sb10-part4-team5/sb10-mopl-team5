# Task Definition (레시피) — 컨테이너 실행 설계도
resource "aws_ecs_task_definition" "mopl" {
  family                   = "mopl"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = "512" # 0.5 vCPU
  memory                   = "768" # MB (t3.micro 1GB 중 시스템 여유)
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "mopl"
      image     = "${aws_ecr_repository.mopl.repository_url}:${var.app_image_tag}"
      essential = true

      portMappings = [{
        containerPort = 8080
        hostPort      = 0
        protocol      = "tcp"
      }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.mopl.address}:5432/mopl" },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "S3_BUCKET", value = var.s3_bucket },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "CDN_BASE_URL", value = "https://${var.cdn_domain}" },
        { name = "MAIL_HOST", value = var.mail_host },
        { name = "MAIL_PORT", value = var.mail_port },
        { name = "MAIL_USERNAME", value = var.mail_username },
        { name = "MAIL_PASSWORD", value = var.mail_password },
        { name = "JWT_ACCESS_SECRET_KEY", value = var.jwt_access_secret_key },
        { name = "JWT_REFRESH_SECRET_KEY", value = var.jwt_refresh_secret_key },
        { name = "ADMIN_EMAIL", value = var.admin_email },
        { name = "ADMIN_PASSWORD", value = var.admin_password },
        { name = "ADMIN_NAME", value = var.admin_name },
        { name = "TMDB_ACCESS_TOKEN", value = var.tmdb_access_token },
        { name = "TMDB_API_KEY", value = var.tmdb_api_key },
        { name = "SPORTS_DB_API_KEY", value = var.sports_db_api_key },
        { name = "GOOGLE_CLIENT_ID", value = var.google_client_id },
        { name = "GOOGLE_CLIENT_SECRET", value = var.google_client_secret },
        { name = "KAKAO_CLIENT_ID", value = var.kakao_client_id },
        { name = "KAKAO_CLIENT_SECRET", value = var.kakao_client_secret },
        { name = "JDK_JAVA_OPTIONS", value = "-Xms256m -Xmx350m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC" },
        { name = "COOKIE_SIGNATURE_SECRET_KEY", value = var.cookie_signature_secret_key }
      ]
    },
    {
      name      = "alloy"
      image     = "grafana/alloy:v1.6.1"
      essential = false

      memoryReservation = 64
      memory            = 128

      environment = [
        { name = "ALLOY_CONFIG_B64", value = local.alloy_config_b64 }
      ]

      secrets = [
        { name = "GRAFANA_TOKEN", valueFrom = aws_ssm_parameter.grafana_token.arn }
      ]

      links      = ["mopl"]
      entryPoint = ["sh", "-c"]
      # HOST_IP는 호스트가 부팅 시 써둔 파일에서 읽음 (컨테이너에서 IMDS 직접 호출 안 함)
      command = ["HOST_IP=$(cat /host-ip) && echo $ALLOY_CONFIG_B64 | base64 -d | sed \"s/__HOST_IP__/$HOST_IP/g\" > /tmp/config.alloy && exec /bin/alloy run /tmp/config.alloy"]

      mountPoints = [
        {
          sourceVolume  = "docker-sock"
          containerPath = "/var/run/docker.sock"
          readOnly      = true
        },
        {
          sourceVolume  = "host-ip"
          containerPath = "/host-ip"
          readOnly      = true
        }
      ]

      logConfiguration = {
        logDriver = "json-file"
        options = {
          max-size = "10m"
          max-file = "3"
        }
      }
    }
  ])

  volume {
    name      = "docker-sock"
    host_path = "/var/run/docker.sock"
  }

  volume {
    name      = "host-ip"
    host_path = "/etc/mopl-host-ip"
  }
}

# Service — Task를 desired_count 만큼 실행/유지 + ALB 연결
resource "aws_ecs_service" "mopl" {
  name            = var.ecs_service
  cluster         = aws_ecs_cluster.mopl.id
  task_definition = aws_ecs_task_definition.mopl.arn
  desired_count                      = 1 # 나중에 2로 확장 가능
  health_check_grace_period_seconds  = 180

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

  depends_on = [aws_lb_listener.https]

  # CD가 매 배포마다 새 task def revision(:sha)으로 갱신하므로 terraform은 무시
  lifecycle {
    ignore_changes = [task_definition]
  }
}
