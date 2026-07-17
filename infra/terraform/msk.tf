# MSK 방화벽
resource "aws_security_group" "msk" {
  name        = "mopl-msk-sg"
  description = "MSK Kafka access"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description = "Kafka PLAINTEXT from within VPC"
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  ingress {
    description = "Kafka TLS from within VPC"
    from_port   = 9094
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-msk-sg" }
}

resource "aws_msk_cluster" "mopl" {
  cluster_name           = "mopl-kafka"
  kafka_version          = "3.9.x.kraft"
  number_of_broker_nodes = 2

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = aws_subnet.private[*].id
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = 10
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
      in_cluster    = false
    }
  }

  tags = { Name = "mopl-kafka" }
}

output "kafka_bootstrap_brokers_tls" {
  value = aws_msk_cluster.mopl.bootstrap_brokers_tls
}
