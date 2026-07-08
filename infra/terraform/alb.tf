# ALB 방화벽 — 외부에서 80(HTTP) 허용
resource "aws_security_group" "alb" {
  name        = "mopl-alb-sg"
  description = "ALB inbound"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS는 ACM 인증서 붙일 때 사용 (지금은 열어만 둠)
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # bridge dynamic port mapping (Linux ephemeral 범위: 32768-65535)
  egress {
    description = "to app dynamic ports within VPC"
    from_port   = 32768
    to_port     = 65535
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  tags = { Name = "mopl-alb-sg" }
}

# 앱(ECS task) 방화벽 — ALB에서만 허용 (bridge 동적 포트 대응)
resource "aws_security_group" "app" {
  name        = "mopl-app-sg"
  description = "App from ALB"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description     = "App dynamic ports from ALB"
    from_port       = 32768
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-app-sg" }
}

# Application Load Balancer (퍼블릭 서브넷)
resource "aws_lb" "mopl" {
  name               = "mopl-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  drop_invalid_header_fields = true # 비정상 헤더 차단

  tags = { Name = "mopl-alb" }
}

# Target Group — 앱 8080, health check는 actuator
resource "aws_lb_target_group" "mopl" {
  name_prefix = "mopl-"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.mopl.id
  target_type = "instance" # bridge 네트워크 모드 (EC2 인스턴스 단위 등록)

  health_check {
    path                = "/actuator/health"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }

  tags = { Name = "mopl-tg" }

  lifecycle {
    create_before_destroy = true
  }
}

# Listener — 80 → 443 리다이렉트
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.mopl.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# Listener — 443 HTTPS → Target Group
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.mopl.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.mopl.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.mopl.arn
  }
}

# 잘 알려진 스캐너/봇 프로브 경로 차단 — WordPress/PHP류 취약점 스캐너가 흔히 찌르는 경로.
# 우리 스택(Spring Boot)엔 존재하지 않지만 요청 자체를 앱까지 안 보내고 ALB에서 끊는다.
resource "aws_lb_listener_rule" "block_known_scan_paths_1" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10

  action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }

  condition {
    path_pattern {
      values = ["/wp-admin*", "/wp-login*", "/xmlrpc.php", "/.env*", "/.git*"]
    }
  }
}

resource "aws_lb_listener_rule" "block_known_scan_paths_2" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 11

  action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }

  condition {
    path_pattern {
      values = ["/administrator*", "/phpmyadmin*", "/.aws*", "/vendor/*", "/config.php"]
    }
  }
}

# API 문서는 인터넷에 노출하지 않는다. 앱에는 여전히 활성화돼 있어 SSM 포트포워딩으로만 확인 가능.
resource "aws_lb_listener_rule" "block_swagger" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 12

  action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }

  condition {
    path_pattern {
      values = ["/swagger-ui*", "/v3/api-docs*"]
    }
  }
}
