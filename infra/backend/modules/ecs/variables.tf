variable "project" { type = string }
variable "aws_region" { type = string }
variable "vpc_id" { type = string }
variable "public_subnet_ids" { type = list(string) }
variable "alb_security_group_id" { type = string }
variable "rds_security_group_id" { type = string }
variable "alb_listener_arn" { type = string }
variable "origin_verify_value" {
  type      = string
  sensitive = true
}
variable "ecr_repository_url" { type = string }
variable "container_image_tag" { type = string }
variable "cognito_issuer_uri" { type = string }
variable "spring_profile" { type = string }
variable "db_host" { type = string }
variable "db_port" { type = number }
variable "db_name" { type = string }
variable "db_secret_arn" { type = string }
