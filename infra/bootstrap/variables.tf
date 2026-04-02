variable "aws_region" {
  type        = string
  description = "AWS region for the state bucket and lock table."
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique S3 bucket name for Terraform state."

  validation {
    condition     = length(var.state_bucket_name) >= 3 && length(var.state_bucket_name) <= 63
    error_message = "Bucket name must be 3-63 characters."
  }
}

variable "lock_table_name" {
  type        = string
  description = "DynamoDB table name for Terraform state locking."
}
