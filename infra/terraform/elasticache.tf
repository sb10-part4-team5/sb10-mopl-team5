resource "aws_elasticache_subnet_group" "mopl" {
  name       = "mopl-redis-private"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "mopl-redis-private-subnet" }
}

# Redis 방화벽
resource "aws_security_group" "redis" {
  name        = "mopl-redis-sg"
  description = "ElastiCache Redis access"
  vpc_id      = aws_vpc.mopl.id

  ingress {
    description = "Redis from within VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.mopl.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "mopl-redis-sg" }
}

resource "aws_elasticache_cluster" "mopl" {
  cluster_id           = "mopl-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  port                 = 6379
  parameter_group_name = "default.redis7"

  subnet_group_name  = aws_elasticache_subnet_group.mopl.name
  security_group_ids = [aws_security_group.redis.id]

  apply_immediately = true

  tags = { Name = "mopl-redis" }
}

output "redis_host" {
  value = aws_elasticache_cluster.mopl.cache_nodes[0].address
}

output "redis_port" {
  value = aws_elasticache_cluster.mopl.cache_nodes[0].port
}
