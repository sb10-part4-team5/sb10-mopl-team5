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

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.app.id]
  }

  # IMDSv2 강제 (토큰 필수) — SSRF 등으로 메타데이터/자격증명 탈취 방지
  # hop_limit 1: 브리지 네트워크의 컨테이너(mopl 앱 포함)에서는 IMDS 접근 자체를 차단
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  # 호스트(0홉)에서만 부팅 시 프라이빗 IP를 조회해 파일로 저장 — alloy가 이 파일을
  # 읽기 전용으로 마운트해서 사용 (컨테이너에서 직접 IMDS를 호출하지 않도록 함)
  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -euo pipefail

    echo "ECS_CLUSTER=${var.ecs_cluster}" >> /etc/ecs/ecs.config

    TOKEN=$(curl -sf -X PUT -H "X-aws-ec2-metadata-token-ttl-seconds: 60" http://169.254.169.254/latest/api/token)
    HOST_IP=$(curl -sf -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)
    test -n "$HOST_IP"
    printf '%s' "$HOST_IP" > /etc/mopl-host-ip.tmp
    mv /etc/mopl-host-ip.tmp /etc/mopl-host-ip
    chmod 0644 /etc/mopl-host-ip
  EOF
  )

  tag_specifications {
    resource_type = "instance"
    tags          = { Name = "mopl-ecs-instance" }
  }
}

# EC2 오토스케일링 그룹 (롤링 배포 가능하도록 2대)
resource "aws_autoscaling_group" "ecs" {
  name                = "mopl-ecs-asg"
  vpc_zone_identifier = aws_subnet.public[*].id
  desired_capacity    = 2
  min_size            = 1
  max_size            = 2

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = "true"
    propagate_at_launch = true
  }

  # desired_capacity는 초기값만 지정 — 이후 capacity_provider의 managed_scaling이 조정하므로
  # terraform이 계속 2로 되돌리며 드리프트나지 않도록 무시
  lifecycle {
    ignore_changes = [desired_capacity]
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
