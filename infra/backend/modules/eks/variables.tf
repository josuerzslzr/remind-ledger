variable "project" {
  type        = string
  description = "Project name used to name and tag EKS resources."
}

variable "aws_region" {
  type        = string
  description = "AWS region containing the EKS cluster and its supporting resources."
}

variable "vpc_id" {
  type        = string
  description = "ID of the VPC in which the EKS cluster runs."
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "IDs of the public subnets available for the EKS NAT gateway."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "IDs of the private subnets used by the EKS control plane and managed nodes."
}

variable "alb_security_group_id" {
  type        = string
  description = "ID of the ALB security group allowed to send application traffic to EKS workloads."
}

variable "rds_security_group_id" {
  type        = string
  description = "ID of the RDS security group that permits database traffic from EKS workloads."
}

variable "target_group_arn" {
  type        = string
  description = "ARN of the shared ALB target group managed by the AWS Load Balancer Controller."
}

variable "db_secret_arn" {
  type        = string
  description = "ARN of the Secrets Manager secret read by External Secrets Operator."
}

variable "kubernetes_version" {
  type        = string
  description = "Kubernetes minor version used by the EKS control plane."
}

variable "instance_types" {
  type        = list(string)
  description = "EC2 instance types available to the EKS managed node group."
}

variable "node_min_size" {
  type        = number
  description = "Minimum number of nodes in the EKS managed node group."
}

variable "node_desired_size" {
  type        = number
  description = "Desired number of nodes in the EKS managed node group."
}

variable "node_max_size" {
  type        = number
  description = "Maximum number of nodes in the EKS managed node group."
}

variable "public_access_cidrs" {
  type        = list(string)
  description = "Administrative IPv4 CIDR blocks allowed to reach the public EKS API endpoint."
}

variable "admin_trusted_principal_arn" {
  type        = string
  description = "Permanent IAM user or role ARN allowed to assume the generated EKS administrator role."
}
