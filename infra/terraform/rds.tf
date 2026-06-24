# RDS를 배치할 서브넷 그룹 (2개 AZ의 서브넷)
resource "aws_db_subnet_group" "mopl" {
  name       = "mopl-db-subnet"
  subnet_ids = aws_subnet.public[*].id

  tags = { Name = "mopl-db-subnet" }
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
