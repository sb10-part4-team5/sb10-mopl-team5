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

# 공개 폴더(profiles/, thumbnails/)만 정책 기반 read 허용
resource "aws_s3_bucket_public_access_block" "mopl" {
  bucket                  = aws_s3_bucket.mopl.id
  block_public_acls       = true  # ACL은 계속 차단 (정책 방식만 사용)
  block_public_policy     = false # 정책 기반 public 허용
  ignore_public_acls      = true
  restrict_public_buckets = false # public 정책 버킷 허용
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

# 버킷 정책: TLS 강제 + 공개 폴더(profiles/, thumbnails/) read 허용
resource "aws_s3_bucket_policy" "mopl" {
  bucket = aws_s3_bucket.mopl.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # 전송 구간 TLS 강제 — HTTP(비암호화) 접근은 거부
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
        # 프로필/썸네일 이미지 공개 read (그 외 폴더는 비공개 유지)
        Sid       = "PublicReadImages"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource = [
          "${aws_s3_bucket.mopl.arn}/profiles/*",
          "${aws_s3_bucket.mopl.arn}/thumbnails/*"
        ]
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.mopl]
}
