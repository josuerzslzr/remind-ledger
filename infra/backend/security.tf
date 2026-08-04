data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "alb" {
  name        = "${var.project}-alb"
  description = "ALB ingress - CloudFront only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "HTTP from CloudFront edge"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project}-alb-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.project}-rds"
  description = "PostgreSQL from the selected application runtime"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project}-rds-sg"
  }
}

resource "aws_security_group_rule" "rds_from_bastion" {
  count                    = var.enable_bastion ? 1 : 0
  type                     = "ingress"
  description              = "Postgres from bastion"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = aws_security_group.bastion[0].id
}
