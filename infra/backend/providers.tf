provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}

data "aws_iam_session_context" "current" {
  count = var.deployment_target == "eks" ? 1 : 0

  arn = data.aws_caller_identity.current.arn

  lifecycle {
    postcondition {
      condition = (
        can(regex("^arn:(aws|aws-us-gov|aws-cn|aws-iso|aws-iso-b|aws-iso-e|aws-iso-f):iam::[0-9]{12}:(user|role)/[A-Za-z0-9+=,.@_/-]+$", self.issuer_arn)) &&
        !can(regex(":role/aws-service-role/", self.issuer_arn))
      )
      error_message = "The automatically resolved EKS administrator principal must be a permanent IAM user or non-service-linked IAM role ARN."
    }
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)
}
