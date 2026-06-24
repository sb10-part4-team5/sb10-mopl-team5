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

# 퍼블릭 접근 차단 (앱이 presigned URL 등으로만 접근)
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
