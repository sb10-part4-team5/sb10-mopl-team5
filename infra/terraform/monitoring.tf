locals {
  alloy_config_b64 = base64encode(templatefile("${path.module}/alloy-config.alloy.tpl", {
    prometheus_url      = var.grafana_prometheus_url
    prometheus_username = var.grafana_prometheus_username
    loki_url            = var.grafana_loki_url
    loki_username       = var.grafana_loki_username
  }))
}

resource "aws_ssm_parameter" "grafana_token" {
  name  = "/mopl/grafana/alloy-token"
  type  = "SecureString"
  value = var.grafana_alloy_token
}

# Grafana Cloud가 CloudWatch 조회를 위해 assume할 IAM 역할
resource "aws_iam_role" "grafana_cloudwatch" {
  name = "mopl-grafana-cloudwatch"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        AWS = "arn:aws:iam::008923505280:root"
      }
      Action = "sts:AssumeRole"
      Condition = {
        StringEquals = {
          "sts:ExternalId" = "1715822"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "grafana_cloudwatch" {
  role       = aws_iam_role.grafana_cloudwatch.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
}

output "grafana_cloudwatch_role_arn" {
  value = aws_iam_role.grafana_cloudwatch.arn
}
