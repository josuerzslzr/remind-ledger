output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "app_log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}
