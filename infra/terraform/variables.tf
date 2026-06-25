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
