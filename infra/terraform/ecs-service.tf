# Task Definition (레시피) — 컨테이너 실행 설계도
resource "aws_ecs_task_definition" "mopl" {
  family                   = "mopl"
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]
  cpu                      = "1024" # 1 vCPU (t3.micro 2048 unit 중 50%)
  memory                   = "768"  # MB (t3.micro 1GB 중 시스템 여유)
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "mopl"
      image     = "${aws_ecr_repository.mopl.repository_url}:${var.app_image_tag}"
      essential = true

      memory = 640

      portMappings = [{
        containerPort = 8080
        hostPort      = 0
        protocol      = "tcp"
      }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.mopl.address}:5432/mopl" },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "S3_BUCKET", value = var.s3_bucket },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "CDN_BASE_URL", value = "https://${var.cdn_domain}" },
        { name = "MAIL_HOST", value = var.mail_host },
        { name = "MAIL_PORT", value = var.mail_port },
        { name = "MAIL_USERNAME", value = var.mail_username },
        { name = "ADMIN_EMAIL", value = var.admin_email },
        { name = "ADMIN_NAME", value = var.admin_name },
        { name = "GOOGLE_CLIENT_ID", value = var.google_client_id },
        { name = "KAKAO_CLIENT_ID", value = var.kakao_client_id },
        { name = "JDK_JAVA_OPTIONS", value = "-XX:MaxRAMPercentage=35.0 -XX:InitialRAMPercentage=20.0 -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=64m -Xss512k -XX:+UseG1GC" },
        { name = "REDIS_HOST", value = aws_elasticache_replication_group.mopl.primary_endpoint_address },
        { name = "REDIS_PORT", value = tostring(aws_elasticache_replication_group.mopl.port) },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = aws_msk_cluster.mopl.bootstrap_brokers_tls },
        { name = "OPENSEARCH_URIS", value = "https://${aws_opensearch_domain.mopl.endpoint}" },
        { name = "OPENSEARCH_USERNAME", value = var.opensearch_master_username }
      ]

      secrets = [
        { name = "DB_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn },
        { name = "MAIL_PASSWORD", valueFrom = aws_ssm_parameter.mail_password.arn },
        { name = "JWT_ACCESS_SECRET_KEY", valueFrom = aws_ssm_parameter.jwt_access_secret_key.arn },
        { name = "JWT_REFRESH_SECRET_KEY", valueFrom = aws_ssm_parameter.jwt_refresh_secret_key.arn },
        { name = "ADMIN_PASSWORD", valueFrom = aws_ssm_parameter.admin_password.arn },
        { name = "GOOGLE_CLIENT_SECRET", valueFrom = aws_ssm_parameter.google_client_secret.arn },
        { name = "KAKAO_CLIENT_SECRET", valueFrom = aws_ssm_parameter.kakao_client_secret.arn },
        { name = "COOKIE_SIGNATURE_SECRET_KEY", valueFrom = aws_ssm_parameter.cookie_signature_secret_key.arn },
        { name = "TMDB_ACCESS_TOKEN", valueFrom = aws_ssm_parameter.tmdb_access_token.arn },
        { name = "TMDB_API_KEY", valueFrom = aws_ssm_parameter.tmdb_api_key.arn },
        { name = "SPORTS_DB_API_KEY", valueFrom = aws_ssm_parameter.sports_db_api_key.arn },
        { name = "REDIS_PASSWORD", valueFrom = aws_ssm_parameter.redis_auth_token.arn },
        { name = "OPENSEARCH_PASSWORD", valueFrom = aws_ssm_parameter.opensearch_master_password.arn }
      ]

      # 로테이션 설정 없으면 json-file 로그가 무제한으로 커져 디스크/메모리 압박 유발
      logConfiguration = {
        logDriver = "json-file"
        options = {
          max-size = "10m"
          max-file = "3"
        }
      }
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
        },
        {
          sourceVolume  = "docker-containers"
          containerPath = "/var/lib/docker/containers"
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

  volume {
    name      = "docker-containers"
    host_path = "/var/lib/docker/containers"
  }
}

# Service — Task를 desired_count 만큼 실행/유지 + ALB 연결
resource "aws_ecs_service" "mopl" {
  name                              = var.ecs_service
  cluster                           = aws_ecs_cluster.mopl.id
  task_definition                   = aws_ecs_task_definition.mopl.arn
  desired_count                     = 2 # 다중 인스턴스 (EC2 2대에 태스크 1개씩)
  health_check_grace_period_seconds = 180

  # 인스턴스 2대 상한 + distinctInstance: 배포 중 old 1대를 내려 빈 자리에 new를 올리도록
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 100

  load_balancer {
    target_group_arn = aws_lb_target_group.mopl.arn
    container_name   = "mopl"
    container_port   = 8080
  }

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.mopl.name
    weight            = 100
  }

  # 롤링 배포 시 신구 태스크가 같은 EC2에 몰리지 않도록 강제 분산
  placement_constraints {
    type = "distinctInstance"
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
