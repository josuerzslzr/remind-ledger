output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "service_name" {
  value = aws_ecs_service.app.name
}
