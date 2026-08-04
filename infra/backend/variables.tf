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

variable "deployment_target" {
  type        = string
  description = "Application runtime to provision. Valid values are ecs and eks."
  default     = "eks"

  validation {
    condition     = contains(["ecs", "eks"], var.deployment_target)
    error_message = "deployment_target must be either \"ecs\" or \"eks\"."
  }
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
  description = "Image tag pushed to ECR before the selected runtime is deployed."
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

variable "eks_version" {
  type        = string
  description = "Kubernetes minor version for the EKS control plane."
  default     = "1.35"
}

variable "eks_instance_types" {
  type        = list(string)
  description = "EC2 instance types available to the EKS managed node group."
  default     = ["t3.medium"]

  validation {
    condition     = length(var.eks_instance_types) > 0
    error_message = "eks_instance_types must contain at least one instance type."
  }
}

variable "eks_node_min_size" {
  type        = number
  description = "Minimum EKS managed node group size."
  default     = 1

  validation {
    condition     = var.eks_node_min_size >= 0
    error_message = "eks_node_min_size must be zero or greater."
  }
}

variable "eks_node_desired_size" {
  type        = number
  description = "Desired EKS managed node group size."
  default     = 1

  validation {
    condition     = var.eks_node_desired_size >= 0
    error_message = "eks_node_desired_size must be zero or greater."
  }
}

variable "eks_node_max_size" {
  type        = number
  description = "Maximum EKS managed node group size."
  default     = 2

  validation {
    condition     = var.eks_node_max_size >= 1
    error_message = "eks_node_max_size must be at least one."
  }
}

variable "eks_public_access_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to reach the public EKS API endpoint. Restrict this to trusted administrator networks."
  default     = ["0.0.0.0/0"]

  validation {
    condition     = length(var.eks_public_access_cidrs) > 0 && alltrue([for cidr in var.eks_public_access_cidrs : can(cidrnetmask(cidr))])
    error_message = "eks_public_access_cidrs must contain valid IPv4 or IPv6 CIDR blocks."
  }
}

variable "eks_admin_principal_arn" {
  type        = string
  description = "IAM principal granted cluster-admin access. Defaults to the principal running Terraform."
  default     = null
  nullable    = true
}
