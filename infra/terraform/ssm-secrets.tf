# ECS 컨테이너 시크릿 — 평문 environment 대신 SSM Parameter Store(SecureString)로 주입

resource "aws_ssm_parameter" "db_password" {
  name  = "/mopl/app/db-password"
  type  = "SecureString"
  value = var.db_password
}

resource "aws_ssm_parameter" "mail_password" {
  name  = "/mopl/app/mail-password"
  type  = "SecureString"
  value = var.mail_password
}

resource "aws_ssm_parameter" "jwt_access_secret_key" {
  name  = "/mopl/app/jwt-access-secret-key"
  type  = "SecureString"
  value = var.jwt_access_secret_key
}

resource "aws_ssm_parameter" "jwt_refresh_secret_key" {
  name  = "/mopl/app/jwt-refresh-secret-key"
  type  = "SecureString"
  value = var.jwt_refresh_secret_key
}

resource "aws_ssm_parameter" "admin_password" {
  name  = "/mopl/app/admin-password"
  type  = "SecureString"
  value = var.admin_password
}

resource "aws_ssm_parameter" "google_client_secret" {
  name  = "/mopl/app/google-client-secret"
  type  = "SecureString"
  value = var.google_client_secret
}

resource "aws_ssm_parameter" "kakao_client_secret" {
  name  = "/mopl/app/kakao-client-secret"
  type  = "SecureString"
  value = var.kakao_client_secret
}

resource "aws_ssm_parameter" "cookie_signature_secret_key" {
  name  = "/mopl/app/cookie-signature-secret-key"
  type  = "SecureString"
  value = var.cookie_signature_secret_key
}

resource "aws_ssm_parameter" "tmdb_access_token" {
  name  = "/mopl/app/tmdb-access-token"
  type  = "SecureString"
  value = var.tmdb_access_token
}

resource "aws_ssm_parameter" "tmdb_api_key" {
  name  = "/mopl/app/tmdb-api-key"
  type  = "SecureString"
  value = var.tmdb_api_key
}

resource "aws_ssm_parameter" "sports_db_api_key" {
  name  = "/mopl/app/sports-db-api-key"
  type  = "SecureString"
  value = var.sports_db_api_key
}

resource "aws_ssm_parameter" "redis_auth_token" {
  name  = "/mopl/app/redis-auth-token"
  type  = "SecureString"
  value = var.redis_auth_token
}

resource "aws_ssm_parameter" "opensearch_master_password" {
  name  = "/mopl/app/opensearch-master-password"
  type  = "SecureString"
  value = var.opensearch_master_password
}
