# ACM 인증서 — CloudFront는 us-east-1 필수
resource "aws_acm_certificate" "cdn" {
  provider          = aws.us_east_1
  domain_name       = "cdn.mopl-dev.site"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

# ACM DNS 검증 레코드
resource "aws_route53_record" "cdn_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.cdn.domain_validation_options : dvo.domain_name => {
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
resource "aws_acm_certificate_validation" "cdn" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.cdn.arn
  validation_record_fqdns = [for record in aws_route53_record.cdn_cert_validation : record.fqdn]
}

# OAC — CloudFront가 S3에 서명된 요청으로 접근
resource "aws_cloudfront_origin_access_control" "mopl" {
  name                              = "mopl-s3-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# CloudFront 배포
resource "aws_cloudfront_distribution" "mopl" {
  enabled             = true
  aliases             = ["cdn.mopl-dev.site"]
  default_root_object = ""
  price_class         = "PriceClass_200" # 아시아 포함

  origin {
    domain_name              = aws_s3_bucket.mopl.bucket_regional_domain_name
    origin_id                = "s3-mopl"
    origin_access_control_id = aws_cloudfront_origin_access_control.mopl.id
  }

  default_cache_behavior {
    target_origin_id       = "s3-mopl"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6" # CachingOptimized (AWS 관리형)
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.cdn.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  tags = { Name = "mopl-cdn" }
}

# cdn.mopl-dev.site → CloudFront (Alias)
resource "aws_route53_record" "cdn" {
  zone_id = aws_route53_zone.mopl.zone_id
  name    = "cdn.mopl-dev.site"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.mopl.domain_name
    zone_id                = aws_cloudfront_distribution.mopl.hosted_zone_id
    evaluate_target_health = false
  }
}
