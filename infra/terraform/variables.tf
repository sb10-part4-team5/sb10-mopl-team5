variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "aws_profile" {
  description = "AWS CLI 프로파일명 (자격증명)"
  type        = string
  default     = "mopl"
}

variable "account_id" {
  description = "AWS 계정 ID"
  type        = string
}

variable "github_org" {
  description = "GitHub 조직(또는 사용자)명 — OIDC sub 조건에 사용"
  type        = string
}

variable "github_repo" {
  description = "GitHub 레포지토리명 — OIDC sub 조건에 사용"
  type        = string
  default     = "sb10-mopl-team5"
}

variable "app_image_tag" {
  description = "task def 부트스트랩 이미지 태그. 실배포는 CD가 커밋 sha로 교체(service lifecycle ignore_changes)"
  type        = string
  default     = "latest"
}

variable "create_github_oidc_provider" {
  description = "GitHub OIDC Provider 생성 여부 (신규 계정은 true, 이미 있으면 false 로 기존 것 참조). 실수 방지를 위해 기본값 없이 명시 입력"
  type        = bool
}

variable "s3_bucket" {
  description = "이미지 업로드용 S3 버킷명 (전역 유일, terraform.tfvars 에 작성)"
  type        = string
}

variable "ecr_repository" {
  description = "ECR 리포지토리명"
  type        = string
  default     = "mopl"
}

variable "ecs_cluster" {
  description = "ECS 클러스터명"
  type        = string
  default     = "mopl-cluster"
}

variable "ecs_service" {
  description = "ECS 서비스명"
  type        = string
  default     = "mopl-service"
}

variable "db_username" {
  description = "RDS 마스터 사용자명"
  type        = string
  default     = "mopl"
}

variable "db_password" {
  description = "RDS 마스터 비밀번호 (terraform.tfvars 에 작성)"
  type        = string
  sensitive   = true
}

variable "team_members" {
  description = "SSM 접근 권한을 줄 팀원 IAM 사용자명 목록 (사용자는 코드로 생성, 액세스 키는 각자 콘솔에서 발급)"
  type        = list(string)
  default     = []
}

variable "cdn_domain" {
  description = "CloudFront CDN 커스텀 도메인"
  type        = string
  default     = "cdn.mopl-dev.site"
}

variable "mail_host" {
  description = "SMTP 호스트"
  type        = string
  default     = "smtp.gmail.com"
}

variable "mail_port" {
  description = "SMTP 포트"
  type        = string
  default     = "587"
}

variable "mail_username" {
  description = "SMTP 계정 (terraform.tfvars 에 작성)"
  type        = string
}

variable "mail_password" {
  description = "SMTP 앱 비밀번호 (terraform.tfvars 에 작성)"
  type        = string
  sensitive   = true
}

variable "jwt_access_secret_key" {
  description = "JWT 액세스 토큰 서명 키"
  type        = string
  sensitive   = true
}

variable "jwt_refresh_secret_key" {
  description = "JWT 리프레시 토큰 서명 키"
  type        = string
  sensitive   = true
}

variable "admin_email" {
  description = "초기 Admin 계정 이메일"
  type        = string
}

variable "admin_password" {
  description = "초기 Admin 계정 비밀번호"
  type        = string
  sensitive   = true
}

variable "admin_name" {
  description = "초기 Admin 계정 이름"
  type        = string
  default     = "Admin"
}

variable "tmdb_access_token" {
  description = "TMDB API Bearer 토큰"
  type        = string
  sensitive   = true
}

variable "tmdb_api_key" {
  description = "TMDB API 키"
  type        = string
  sensitive   = true
}

variable "sports_db_api_key" {
  description = "SportsDB API 키"
  type        = string
  default     = "123"
}

variable "google_client_id" {
  description = "Google OAuth 클라이언트 ID"
  type        = string
}

variable "google_client_secret" {
  description = "Google OAuth 클라이언트 시크릿"
  type        = string
  sensitive   = true
}

variable "kakao_client_id" {
  description = "Kakao OAuth 클라이언트 ID"
  type        = string
}

variable "kakao_client_secret" {
  description = "Kakao OAuth 클라이언트 시크릿"
  type        = string
  sensitive   = true
}

variable "grafana_prometheus_url" {
  description = "Grafana Cloud Prometheus remote_write URL (terraform.tfvars 에 작성)"
  type        = string
}

variable "grafana_prometheus_username" {
  description = "Grafana Cloud Prometheus org ID (username, terraform.tfvars 에 작성)"
  type        = string
}

variable "grafana_loki_url" {
  description = "Grafana Cloud Loki push URL (terraform.tfvars 에 작성)"
  type        = string
}

variable "grafana_loki_username" {
  description = "Grafana Cloud Loki org ID (username, terraform.tfvars 에 작성)"
  type        = string
}

variable "grafana_alloy_token" {
  description = "Grafana Cloud Alloy API 토큰"
  type        = string
  sensitive   = true
}

variable "grafana_cloudwatch_aws_account_id" {
  description = "Grafana Cloud가 CloudWatch 조회 시 assume할 role의 신뢰 주체 AWS 계정 ID"
  type        = string
  default     = "008923505280"
}

variable "grafana_cloudwatch_external_id" {
  description = "Grafana Cloud CloudWatch assume role의 sts:ExternalId"
  type        = string
  default     = "1715822"
}

variable "cookie_signature_secret_key" {
  description = "OAuth2 인가 요청 쿠키 HMAC 서명 키"
  type        = string
  sensitive   = true
}
