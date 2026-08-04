locals {
  eks_admin_principal_arn = coalesce(var.eks_admin_principal_arn, data.aws_caller_identity.current.arn)
}

module "ecs" {
  count  = var.deployment_target == "ecs" ? 1 : 0
  source = "./modules/ecs"

  project               = var.project
  aws_region            = var.aws_region
  vpc_id                = aws_vpc.main.id
  public_subnet_ids     = aws_subnet.public[*].id
  alb_security_group_id = aws_security_group.alb.id
  rds_security_group_id = aws_security_group.rds.id
  alb_listener_arn      = aws_lb_listener.http.arn
  origin_verify_value   = random_uuid.origin_verify.result
  ecr_repository_url    = var.ecr_repository_url
  container_image_tag   = var.container_image_tag
  cognito_issuer_uri    = var.cognito_issuer_uri
  spring_profile        = var.spring_profile
  db_host               = aws_db_instance.main.address
  db_port               = aws_db_instance.main.port
  db_name               = aws_db_instance.main.db_name
  db_secret_arn         = aws_secretsmanager_secret.db.arn
}

module "eks" {
  count  = var.deployment_target == "eks" ? 1 : 0
  source = "./modules/eks"

  project                 = var.project
  aws_region              = var.aws_region
  vpc_id                  = aws_vpc.main.id
  public_subnet_ids       = aws_subnet.public[*].id
  private_subnet_ids      = aws_subnet.private[*].id
  alb_security_group_id   = aws_security_group.alb.id
  rds_security_group_id   = aws_security_group.rds.id
  alb_listener_arn        = aws_lb_listener.http.arn
  origin_verify_value     = random_uuid.origin_verify.result
  db_secret_arn           = aws_secretsmanager_secret.db.arn
  kubernetes_version      = var.eks_version
  instance_types          = var.eks_instance_types
  node_min_size           = var.eks_node_min_size
  node_desired_size       = var.eks_node_desired_size
  node_max_size           = var.eks_node_max_size
  public_access_cidrs     = var.eks_public_access_cidrs
  cluster_admin_principal = local.eks_admin_principal_arn

  depends_on = [aws_internet_gateway.main]
}
