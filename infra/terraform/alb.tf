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

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-alb-sg" }
}

# 앱(ECS task) 방화벽 — ALB에서만 8080 허용
resource "aws_security_group" "app" {
  name        = "mopl-app-sg"
  description = "App from ALB"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description     = "App port from ALB"
    from_port       = 8080
    to_port         = 8080
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

  tags = { Name = "mopl-alb" }
}

# Target Group — 앱 8080, health check는 actuator
resource "aws_lb_target_group" "mopl" {
  name        = "mopl-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.mopl.id
  target_type = "ip" # awsvpc 네트워크 모드 (task별 IP)

  health_check {
    path                = "/actuator/health"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }

  tags = { Name = "mopl-tg" }
}

# Listener — 80으로 들어온 요청을 Target Group으로
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.mopl.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.mopl.arn
  }
}
