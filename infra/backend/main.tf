module "ecs" {
  count  = var.deployment_target == "ecs" ? 1 : 0
  source = "./modules/ecs"

  project               = var.project
  aws_region            = var.aws_region
  vpc_id                = aws_vpc.main.id
  public_subnet_ids     = aws_subnet.public[*].id
  alb_security_group_id = aws_security_group.alb.id
  rds_security_group_id = aws_security_group.rds.id
  target_group_arn      = aws_lb_target_group.app.arn
  ecr_repository_url    = var.ecr_repository_url
  container_image_tag   = var.container_image_tag
  cognito_issuer_uri    = var.cognito_issuer_uri
  spring_profile        = var.spring_profile
  db_host               = aws_db_instance.main.address
  db_port               = aws_db_instance.main.port
  db_name               = aws_db_instance.main.db_name
  db_secret_arn         = aws_secretsmanager_secret.db.arn

  depends_on = [aws_lb_listener_rule.verified_origin]
}

module "eks" {
  count  = var.deployment_target == "eks" ? 1 : 0
  source = "./modules/eks"

  project                     = var.project
  aws_region                  = var.aws_region
  vpc_id                      = aws_vpc.main.id
  public_subnet_ids           = aws_subnet.public[*].id
  private_subnet_ids          = aws_subnet.private[*].id
  alb_security_group_id       = aws_security_group.alb.id
  rds_security_group_id       = aws_security_group.rds.id
  target_group_arn            = aws_lb_target_group.app.arn
  db_secret_arn               = aws_secretsmanager_secret.db.arn
  kubernetes_version          = var.eks_version
  instance_types              = var.eks_instance_types
  node_min_size               = var.eks_node_min_size
  node_desired_size           = var.eks_node_desired_size
  node_max_size               = var.eks_node_max_size
  public_access_cidrs         = var.eks_public_access_cidrs
  admin_trusted_principal_arn = data.aws_iam_session_context.current[0].issuer_arn

  depends_on = [aws_internet_gateway.main]
}
