output "alb_dns_name" {
  description = "Public ALB DNS name (HTTP on 80, or HTTPS on 443 when certificate is set)."
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
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
