mock_provider "aws" {
  override_data {
    target = data.aws_availability_zones.available
    values = {
      names = ["us-east-1a", "us-east-1b"]
    }
  }

  override_data {
    target = data.aws_caller_identity.current
    values = {
      account_id = "123456789012"
      arn        = "arn:aws:iam::123456789012:user/terraform-test"
      user_id    = "AIDATEST"
    }
  }

  override_data {
    target = data.aws_iam_session_context.current[0]
    values = {
      issuer_arn = "arn:aws:iam::123456789012:user/terraform-test"
    }
  }

  override_data {
    target = data.aws_ec2_managed_prefix_list.cloudfront
    values = {
      id   = "pl-12345678"
      name = "com.amazonaws.global.cloudfront.origin-facing"
    }
  }

  override_data {
    target = module.ecs[0].data.aws_iam_policy_document.task_assume
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.ecs[0].data.aws_iam_policy_document.execution_secrets
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.cluster_assume
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.eks_admin_assume
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.eks_admin_permissions
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.node_assume
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.pod_identity_assume
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.load_balancer_controller
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.external_secrets
    values = {
      json = "{}"
    }
  }

  override_data {
    target = module.eks[0].data.aws_iam_policy_document.fluent_bit
    values = {
      json = "{}"
    }
  }

  override_resource {
    target          = module.eks[0].aws_iam_role.eks_admin
    override_during = plan
    values = {
      arn  = "arn:aws:iam::123456789012:role/remind-ledger-eks-admin"
      id   = "remind-ledger-eks-admin"
      name = "remind-ledger-eks-admin"
    }
  }
}

mock_provider "random" {}

variables {
  aws_region              = "us-east-1"
  ecr_repository_url      = "123456789012.dkr.ecr.us-east-1.amazonaws.com/remind-ledger"
  cognito_issuer_uri      = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test"
  container_image_tag     = "test"
  eks_public_access_cidrs = ["203.0.113.10/32"]
}

run "ecs_target_selects_only_ecs_module" {
  command = plan

  variables {
    deployment_target = "ecs"
  }

  assert {
    condition     = length(module.ecs) == 1 && length(module.eks) == 0
    error_message = "The ECS target must enable only the ECS runtime module."
  }

  assert {
    condition     = output.eks_admin_role_arn == null
    error_message = "The ECS target must not expose an EKS administrator role."
  }
}

run "eks_target_selects_only_eks_module" {
  command = plan

  variables {
    deployment_target = "eks"
  }

  assert {
    condition     = length(module.ecs) == 0 && length(module.eks) == 1
    error_message = "The EKS target must enable only the EKS runtime module."
  }

  assert {
    condition     = data.aws_iam_session_context.current[0].issuer_arn == "arn:aws:iam::123456789012:user/terraform-test"
    error_message = "A direct IAM user must remain the trusted EKS administrator principal."
  }

  assert {
    condition     = output.eks_admin_role_arn == "arn:aws:iam::123456789012:role/remind-ledger-eks-admin"
    error_message = "The EKS target must expose its generated administrator role ARN."
  }
}

run "assumed_role_resolves_to_permanent_iam_role" {
  command = plan

  override_data {
    target = data.aws_caller_identity.current
    values = {
      account_id = "123456789012"
      arn        = "arn:aws:sts::123456789012:assumed-role/terraform-operator/test-session"
      user_id    = "AROATEST:test-session"
    }
  }

  override_data {
    target = data.aws_iam_session_context.current[0]
    values = {
      issuer_arn = "arn:aws:iam::123456789012:role/terraform-operator"
    }
  }

  variables {
    deployment_target = "eks"
  }

  assert {
    condition     = data.aws_iam_session_context.current[0].issuer_arn == "arn:aws:iam::123456789012:role/terraform-operator"
    error_message = "An STS session must resolve to its permanent source IAM role."
  }
}

run "root_principal_is_rejected" {
  command = plan

  override_data {
    target = data.aws_iam_session_context.current[0]
    values = {
      issuer_arn = "arn:aws:iam::123456789012:root"
    }
  }

  variables {
    deployment_target = "eks"
  }

  expect_failures = [data.aws_iam_session_context.current[0]]
}

run "service_linked_role_is_rejected" {
  command = plan

  override_data {
    target = data.aws_iam_session_context.current[0]
    values = {
      issuer_arn = "arn:aws:iam::123456789012:role/aws-service-role/eks.amazonaws.com/AWSServiceRoleForAmazonEKS"
    }
  }

  variables {
    deployment_target = "eks"
  }

  expect_failures = [data.aws_iam_session_context.current[0]]
}

run "invalid_eks_node_size_order_is_rejected" {
  command = plan

  variables {
    deployment_target     = "eks"
    eks_node_min_size     = 2
    eks_node_desired_size = 1
    eks_node_max_size     = 3
  }

  expect_failures = [var.eks_node_max_size]
}
