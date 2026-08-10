variable "project" { type = string }
variable "aws_region" { type = string }
variable "vpc_id" { type = string }
variable "public_subnet_ids" { type = list(string) }
variable "private_subnet_ids" { type = list(string) }
variable "alb_security_group_id" { type = string }
variable "rds_security_group_id" { type = string }
variable "target_group_arn" { type = string }
variable "db_secret_arn" { type = string }
variable "kubernetes_version" { type = string }
variable "instance_types" { type = list(string) }
variable "node_min_size" { type = number }
variable "node_desired_size" { type = number }
variable "node_max_size" { type = number }
variable "public_access_cidrs" { type = list(string) }
variable "admin_trusted_principal_arn" {
  type        = string
  description = "Permanent IAM user or role ARN allowed to assume the generated EKS administrator role."
}
