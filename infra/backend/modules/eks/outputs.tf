output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "admin_role_arn" {
  value = aws_iam_role.eks_admin.arn
}

output "app_log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}
