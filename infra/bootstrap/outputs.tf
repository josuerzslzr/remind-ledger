output "state_bucket_name" {
  value       = aws_s3_bucket.terraform_state.bucket
  description = "Use this as backend \"s3\" bucket in infra/backend."
}

output "lock_table_name" {
  value       = aws_dynamodb_table.terraform_locks.name
  description = "Use this as backend \"s3\" dynamodb_table in infra/backend."
}

output "aws_region" {
  value       = var.aws_region
  description = "Region where bootstrap resources were created."
}
