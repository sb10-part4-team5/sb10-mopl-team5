# Hosted Zone — mopl-dev.site
resource "aws_route53_zone" "mopl" {
  name = "mopl-dev.site"
}

# ACM 인증서 (도메인 + 와일드카드)
resource "aws_acm_certificate" "mopl" {
  domain_name               = "mopl-dev.site"
  subject_alternative_names = ["*.mopl-dev.site"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

# ACM DNS 검증 레코드
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.mopl.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.mopl.zone_id
}

# 인증서 검증 완료 대기
resource "aws_acm_certificate_validation" "mopl" {
  certificate_arn         = aws_acm_certificate.mopl.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# A 레코드 — mopl-dev.site → ALB (Alias)
resource "aws_route53_record" "mopl" {
  zone_id = aws_route53_zone.mopl.zone_id
  name    = "mopl-dev.site"
  type    = "A"

  alias {
    name                   = aws_lb.mopl.dns_name
    zone_id                = aws_lb.mopl.zone_id
    evaluate_target_health = true
  }
}
