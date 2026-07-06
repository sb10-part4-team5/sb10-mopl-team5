# 이미지 업로드용 S3 버킷
resource "aws_s3_bucket" "mopl" {
  bucket = var.s3_bucket
}

# 버전 관리 (실수로 덮어써도 복구)
resource "aws_s3_bucket_versioning" "mopl" {
  bucket = aws_s3_bucket.mopl.id
  versioning_configuration {
    status = "Enabled"
  }
}

# 완전 비공개 — CloudFront OAC 경유로만 접근
resource "aws_s3_bucket_public_access_block" "mopl" {
  bucket                  = aws_s3_bucket.mopl.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 기본 암호화
resource "aws_s3_bucket_server_side_encryption_configuration" "mopl" {
  bucket = aws_s3_bucket.mopl.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 버킷 정책: TLS 강제 + CloudFront OAC만 GetObject 허용
resource "aws_s3_bucket_policy" "mopl" {
  bucket = aws_s3_bucket.mopl.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyInsecureTransport"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.mopl.arn,
          "${aws_s3_bucket.mopl.arn}/*"
        ]
        Condition = {
          Bool = { "aws:SecureTransport" = "false" }
        }
      },
      {
        Sid    = "AllowCloudFrontOAC"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.mopl.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.mopl.arn
          }
        }
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.mopl]
}
