# ── SSM bastion for RDS port-forwarding (dev/debug) ─────────────

data "aws_ssm_parameter" "al2023_ami" {
  count = var.enable_bastion ? 1 : 0
  name  = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# ── Security group ──────────────────────────────────────────────

resource "aws_security_group" "bastion" {
  count       = var.enable_bastion ? 1 : 0
  name        = "${var.project}-bastion"
  description = "SSM bastion - outbound only"
  vpc_id      = aws_vpc.main.id

  egress {
    description = "HTTPS to SSM endpoints"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description     = "Postgres to RDS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.rds.id]
  }

  tags = {
    Name = "${var.project}-bastion-sg"
  }
}

# ── IAM role + instance profile ─────────────────────────────────

data "aws_iam_policy_document" "bastion_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "bastion" {
  count              = var.enable_bastion ? 1 : 0
  name               = "${var.project}-bastion"
  assume_role_policy = data.aws_iam_policy_document.bastion_assume.json

  tags = {
    Name = "${var.project}-bastion-role"
  }
}

resource "aws_iam_role_policy_attachment" "bastion_ssm" {
  count      = var.enable_bastion ? 1 : 0
  role       = aws_iam_role.bastion[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "bastion" {
  count = var.enable_bastion ? 1 : 0
  name  = "${var.project}-bastion"
  role  = aws_iam_role.bastion[0].name
}

# ── EC2 instance ────────────────────────────────────────────────

resource "aws_instance" "bastion" {
  count                  = var.enable_bastion ? 1 : 0
  ami                    = data.aws_ssm_parameter.al2023_ami[0].value
  instance_type          = "t3.nano"
  subnet_id              = aws_subnet.public[0].id
  iam_instance_profile   = aws_iam_instance_profile.bastion[0].name
  vpc_security_group_ids = [aws_security_group.bastion[0].id]

  metadata_options {
    http_tokens   = "required"
    http_endpoint = "enabled"
  }

  tags = {
    Name = "${var.project}-bastion"
  }
}

# ── Developer IAM policy (attach to users/roles for access) ────

resource "aws_iam_policy" "bastion_developer_access" {
  count       = var.enable_bastion ? 1 : 0
  name        = "${var.project}-bastion-developer-access"
  description = "Allow SSM port-forwarding through the bastion to RDS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "StartSSMSession"
        Effect = "Allow"
        Action = "ssm:StartSession"
        Resource = [
          aws_instance.bastion[0].arn,
          "arn:aws:ssm:${var.aws_region}::document/AWS-StartPortForwardingSessionToRemoteHost"
        ]
      },
      {
        Sid    = "ManageOwnSession"
        Effect = "Allow"
        Action = [
          "ssm:TerminateSession",
          "ssm:ResumeSession",
          "ssmmessages:OpenDataChannel"
        ]
        Resource = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:session/$${aws:userid}-*"
      }
    ]
  })
}
