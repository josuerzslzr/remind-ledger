variable "aws_region" {
  type        = string
  description = "AWS region for all resources."
}

variable "project" {
  type        = string
  description = "Short name prefix for resources."
  default     = "remind-ledger"
}
