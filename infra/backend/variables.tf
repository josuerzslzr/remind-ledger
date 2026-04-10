variable "aws_region" {
  type        = string
  description = "AWS region for all resources."
}

variable "ecr_repository_url" {
  type        = string
  description = "ECR repository URL from the foundation stack output (ecr_repository_url)."
}

variable "cognito_issuer_uri" {
  type        = string
  description = "Cognito JWT issuer URI from the foundation stack output (cognito_issuer_uri)."
}

variable "project" {
  type        = string
  description = "Short name prefix for resources."
  default     = "remind-ledger"
}

variable "db_name" {
  type        = string
  description = "PostgreSQL database name."
  default     = "remindledger"
}

variable "db_username" {
  type        = string
  description = "PostgreSQL master username."
  default     = "remindledger"
}

variable "container_image_tag" {
  type        = string
  description = "Image tag pushed to ECR (build and push before ECS can become healthy)."
  default     = "latest"
}

variable "skip_final_snapshot" {
  type        = bool
  description = "Set false for production-style RDS teardown protection."
  default     = true
}

variable "spring_profile" {
  type        = string
  description = "Spring Boot profile to activate (e.g. dev, prod)."
  default     = "prod"
}

variable "enable_bastion" {
  type        = bool
  description = "Deploy an SSM bastion for RDS port-forwarding (dev/debug only)."
  default     = false
}
