terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 팀 협업용 원격 state.
  # 연습 중에는 주석 처리(로컬 state). S3 버킷을 만든 뒤 주석을 해제하면 원격 state로 전환된다.
  # terraform 1.10+ 는 use_lockfile 로 S3 자체 잠금을 지원하므로 DynamoDB 테이블은 불필요하다.
  # backend "s3" {
  #   bucket       = "mopl-terraform-state"
  #   key          = "iam/terraform.tfstate"
  #   region       = "ap-northeast-2"
  #   encrypt      = true
  #   use_lockfile = true
  # }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile # 지정한 CLI 프로파일(mopl)의 자격증명 사용
}
