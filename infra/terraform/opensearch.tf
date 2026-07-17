# OpenSearch 서비스 연결 역할 (VPC 도메인 생성 전 필요)
resource "aws_iam_service_linked_role" "opensearch" {
  aws_service_name = "opensearchservice.amazonaws.com"
}

# OpenSearch 방화벽
resource "aws_security_group" "opensearch" {
  name        = "mopl-opensearch-sg"
  description = "OpenSearch domain access"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description = "HTTPS from within VPC"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-opensearch-sg" }
}

resource "aws_opensearch_domain" "mopl" {
  domain_name    = "mopl-search"
  engine_version = "OpenSearch_3.5"

  cluster_config {
    instance_type            = "t3.small.search"
    instance_count           = 1
    dedicated_master_enabled = false
    zone_awareness_enabled   = false
  }

  ebs_options {
    ebs_enabled = true
    volume_type = "gp3"
    volume_size = 10
  }

  vpc_options {
    subnet_ids         = [aws_subnet.private[0].id]
    security_group_ids = [aws_security_group.opensearch.id]
  }

  # FGAC(advanced_security_options)는 저장 데이터 암호화 + 노드 간 암호화가 선행 조건
  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https = true
  }

  advanced_security_options {
    enabled                        = true
    internal_user_database_enabled = true

    master_user_options {
      master_user_name     = var.opensearch_master_username
      master_user_password = var.opensearch_master_password
    }
  }

  access_policies = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { AWS = "*" }
      Action    = "es:*"
      Resource  = "arn:aws:es:${var.aws_region}:${var.account_id}:domain/mopl-search/*"
    }]
  })

  tags = { Name = "mopl-opensearch" }

  depends_on = [aws_iam_service_linked_role.opensearch]
}

# nori(한국어 형태소 분석) 플러그인 연결 — OpenSearch_3.5용 AWS 제공 패키지
resource "aws_opensearch_package_association" "nori" {
  package_id  = "G259293935"
  domain_name = aws_opensearch_domain.mopl.domain_name
}

output "opensearch_endpoint" {
  value = aws_opensearch_domain.mopl.endpoint
}
