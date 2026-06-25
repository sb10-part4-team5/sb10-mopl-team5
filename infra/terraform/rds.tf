# RDS를 배치할 서브넷 그룹 (DB 전용 프라이빗 서브넷, 2개 AZ)
resource "aws_db_subnet_group" "mopl" {
  name_prefix = "mopl-db-private-" # create_before_destroy와 호환 (고유 접미사 자동 부여)
  subnet_ids  = aws_subnet.private[*].id

  tags = { Name = "mopl-db-private-subnet" }

  # 새 그룹 먼저 생성 → RDS 이전 → 기존 그룹 삭제 (사용 중 삭제 충돌 방지)
  lifecycle {
    create_before_destroy = true
  }
}

# RDS 방화벽 — VPC 내부에서만 5432 접근 허용 (나중에 앱 SG로 더 좁힐 수 있음)
resource "aws_security_group" "rds" {
  name        = "mopl-rds-sg"
  description = "RDS PostgreSQL access"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description = "PostgreSQL from within VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-rds-sg" }
}

# PostgreSQL 인스턴스
resource "aws_db_instance" "mopl" {
  identifier     = "mopl-db"
  engine         = "postgres"
  engine_version = "17.10"
  instance_class = "db.t3.micro" # 프리티어 대상

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true # 저장소 암호화 (KMS 기본 키)

  db_name  = "mopl"
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.mopl.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false # 외부 직접 접속 차단 (VPC 내부 앱만)
  skip_final_snapshot = true  # 학습용: 삭제 시 최종 스냅샷 생략
  deletion_protection = false # 학습용: 삭제 보호 끔

  tags = { Name = "mopl-db" }
}
