variable "project" {
  type        = string
  description = "Project name used to name and tag ECS resources."
}

variable "aws_region" {
  type        = string
  description = "AWS region used by the ECS task logging configuration."
}

variable "vpc_id" {
  type        = string
  description = "ID of the VPC in which the ECS service runs."
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "IDs of the public subnets used by the ECS Fargate tasks."
}

variable "alb_security_group_id" {
  type        = string
  description = "ID of the ALB security group allowed to send application traffic to ECS tasks."
}

variable "rds_security_group_id" {
  type        = string
  description = "ID of the RDS security group that permits database traffic from ECS tasks."
}

variable "target_group_arn" {
  type        = string
  description = "ARN of the shared ALB target group in which the ECS service registers tasks."
}

variable "ecr_repository_url" {
  type        = string
  description = "ECR repository URL containing the backend application image."
}

variable "container_image_tag" {
  type        = string
  description = "Tag of the backend application image deployed by the ECS task definition."
}

variable "cognito_issuer_uri" {
  type        = string
  description = "Cognito issuer URI used by the backend to validate JWTs."
}

variable "spring_profile" {
  type        = string
  description = "Spring Boot profile activated in the backend container."
}

variable "db_host" {
  type        = string
  description = "Hostname of the PostgreSQL database used by the backend."
}

variable "db_port" {
  type        = number
  description = "Port of the PostgreSQL database used by the backend."
}

variable "db_name" {
  type        = string
  description = "Name of the PostgreSQL database used by the backend."
}

variable "db_secret_arn" {
  type        = string
  description = "ARN of the Secrets Manager secret containing the database username and password."
}
