# ECS 클러스터
resource "aws_ecs_cluster" "mopl" {
  name = var.ecs_cluster

  # 운영 가시성(컨테이너 CPU/메모리/네트워크 메트릭)
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# ECS 최적화 AMI (최신, SSM 파라미터로 자동 조회)
data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2/recommended/image_id"
}

# EC2 시작 설정 (ECS 에이전트가 클러스터에 등록되도록 user_data 설정)
resource "aws_launch_template" "ecs" {
  name_prefix   = "mopl-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = "t3.micro" # 프리티어 (메모리 1GB — 학습용)

  iam_instance_profile {
    name = aws_iam_instance_profile.ecs_instance.name
  }

  vpc_security_group_ids = [aws_security_group.app.id]

  # IMDSv2 강제 (토큰 필수) — SSRF 등으로 메타데이터/자격증명 탈취 방지
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2 # 컨테이너(awsvpc)에서 메타데이터 접근 허용
  }

  user_data = base64encode(<<-EOF
    #!/bin/bash
    echo "ECS_CLUSTER=${var.ecs_cluster}" >> /etc/ecs/ecs.config
  EOF
  )

  tag_specifications {
    resource_type = "instance"
    tags          = { Name = "mopl-ecs-instance" }
  }
}

# EC2 오토스케일링 그룹 (1대 시작, 최대 2대까지)
resource "aws_autoscaling_group" "ecs" {
  name                = "mopl-ecs-asg"
  vpc_zone_identifier = aws_subnet.public[*].id
  desired_capacity    = 1
  min_size            = 1
  max_size            = 2

  # TODO: 부하 기반 자동 확장(scaling policy) 미설정 — 현재는 1대 고정.
  #   트래픽 증가에 대응하려면 아래 두 가지를 추가해야 한다.
  #   1) ECS Service 오토스케일링(target tracking, CPU/메모리 기준)으로 task 수 자동 조정
  #   2) Capacity Provider managed_scaling 이 task 수요에 맞춰 EC2를 max_size까지 확장

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = "true"
    propagate_at_launch = true
  }
}

# Capacity Provider (ASG를 ECS에 연결)
resource "aws_ecs_capacity_provider" "mopl" {
  name = "mopl-cp"

  auto_scaling_group_provider {
    auto_scaling_group_arn = aws_autoscaling_group.ecs.arn

    managed_scaling {
      status          = "ENABLED"
      target_capacity = 100
    }
  }
}

resource "aws_ecs_cluster_capacity_providers" "mopl" {
  cluster_name       = aws_ecs_cluster.mopl.name
  capacity_providers = [aws_ecs_capacity_provider.mopl.name]
}
