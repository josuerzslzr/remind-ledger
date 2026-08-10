output "cloudfront_domain_name" {
  description = "CloudFront HTTPS URL for the API (default *.cloudfront.net domain)."
  value       = aws_cloudfront_distribution.api.domain_name
}

output "alb_dns_name" {
  description = "ALB DNS name (HTTP only, restricted to CloudFront via SG + origin header)."
  value       = aws_lb.main.dns_name
}

output "vpc_id" {
  description = "VPC used by the selected application runtime."
  value       = aws_vpc.main.id
}

output "deployment_target" {
  description = "Application runtime selected for this backend stack."
  value       = var.deployment_target
}

output "target_group_arn" {
  description = "Shared target group used by the selected runtime."
  value       = aws_lb_target_group.app.arn
}

output "ecs_cluster_name" {
  description = "ECS cluster name when deployment_target is ecs."
  value       = var.deployment_target == "ecs" ? module.ecs[0].cluster_name : null
}

output "ecs_service_name" {
  description = "ECS service name when deployment_target is ecs."
  value       = var.deployment_target == "ecs" ? module.ecs[0].service_name : null
}

output "eks_cluster_name" {
  description = "EKS cluster name when deployment_target is eks."
  value       = var.deployment_target == "eks" ? module.eks[0].cluster_name : null
}

output "eks_admin_role_arn" {
  description = "Generated Kubernetes administrator role ARN when deployment_target is eks."
  value       = var.deployment_target == "eks" ? module.eks[0].admin_role_arn : null
}

output "eks_app_log_group_name" {
  description = "CloudWatch application log group when deployment_target is eks."
  value       = var.deployment_target == "eks" ? module.eks[0].app_log_group_name : null
}

output "rds_endpoint" {
  description = "RDS hostname (also passed to the task as DB_HOST)."
  value       = aws_db_instance.main.address
  sensitive   = true
}

output "db_secret_arn" {
  description = "Secrets Manager ARN holding DB username/password (JSON keys: username, password)."
  value       = aws_secretsmanager_secret.db.arn
  sensitive   = true
}

output "db_name" {
  description = "Database name passed to the application."
  value       = aws_db_instance.main.db_name
}

output "db_port" {
  description = "Database port passed to the application."
  value       = aws_db_instance.main.port
}

output "aws_region" {
  value = var.aws_region
}

output "ecr_repository_url" {
  value = var.ecr_repository_url
}

output "cognito_issuer_uri" {
  value = var.cognito_issuer_uri
}

output "spring_profile" {
  value = var.spring_profile
}

output "bastion_instance_id" {
  description = "Bastion EC2 instance ID (use with aws ssm start-session)."
  value       = var.enable_bastion ? aws_instance.bastion[0].id : null
}

output "bastion_ssm_port_forward_command" {
  description = "Run this to open a local port-forward to RDS Postgres."
  value = var.enable_bastion ? join(" ", [
    "aws ssm start-session",
    "--target ${aws_instance.bastion[0].id}",
    "--document-name AWS-StartPortForwardingSessionToRemoteHost",
    "--parameters '{\"host\":[\"${aws_db_instance.main.address}\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"5432\"]}'",
  ]) : null
}

output "bastion_developer_policy_arn" {
  description = "IAM policy ARN to attach to developer users/roles for SSM bastion access."
  value       = var.enable_bastion ? aws_iam_policy.bastion_developer_access[0].arn : null
}
