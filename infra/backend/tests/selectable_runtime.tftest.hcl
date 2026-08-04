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
}

mock_provider "random" {}

variables {
  aws_region          = "us-east-1"
  ecr_repository_url  = "123456789012.dkr.ecr.us-east-1.amazonaws.com/remind-ledger"
  cognito_issuer_uri  = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test"
  container_image_tag = "test"
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
}
