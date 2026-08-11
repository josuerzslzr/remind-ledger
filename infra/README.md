# Infrastructure

Terraform configuration for the AWS resources that support RemindLedger. The infrastructure is split into independently managed stacks and can provision either ECS Fargate or EKS as the backend runtime.

> AWS resources created here can incur charges, particularly RDS, the load balancer, and EKS worker nodes. Review every Terraform plan and the selected runtime before applying it.

## Prerequisites

- An AWS account and locally available AWS credentials
- Terraform 1.9 or newer
- AWS CLI and Docker for building and publishing the backend image
- `jq` for ECS deployments
- `kubectl` and Helm for EKS deployments

## Stack order

| Stack | State | Responsibility |
|-------|-------|----------------|
| [`bootstrap/`](bootstrap/) | Local | Creates the versioned S3 state bucket and DynamoDB lock table. |
| [`foundation/`](foundation/) | Remote | Creates resources shared across environments, including ECR and Cognito. |
| [`backend/`](backend/) | Remote | Creates networking, RDS, CloudFront/ALB, and exactly one application runtime. |

Apply the stacks in that order. The foundation and backend stacks use separate S3 state keys; create ignored `foundation.hcl` and `backend.hcl` backend configuration files from the bootstrap outputs. Supply stack inputs through ignored `terraform.tfvars` files or explicit `-var` arguments.

A typical command sequence for each stack is:

```bash
terraform -chdir=infra/bootstrap init
terraform -chdir=infra/bootstrap plan
terraform -chdir=infra/bootstrap apply

terraform -chdir=infra/foundation init -backend-config=foundation.hcl
terraform -chdir=infra/foundation plan
terraform -chdir=infra/foundation apply

terraform -chdir=infra/backend init -backend-config=backend.hcl
terraform -chdir=infra/backend plan
terraform -chdir=infra/backend apply
```

Review the variables and outputs in each stack before running these commands. The backend stack consumes the foundation stack's ECR repository URL and Cognito issuer URI.

## Runtime selection

Set the backend variable `deployment_target` to `ecs` or `eks`; it defaults to `eks`. Only the selected runtime is provisioned, while the database, network, load balancer, and CloudFront entry point remain shared.

The EKS option also requires trusted administrator CIDR ranges and installs the application through the Helm chart under [`backend/helm/remind-ledger/`](backend/helm/remind-ledger/). See [ADR-005](../docs/2-design/decisions/ADR-005-deployment-platform.md) for the design and tradeoffs.

## Build and deployment scripts

Run deployment scripts from the repository root after applying the matching Terraform stack.

| Script | Purpose |
|--------|---------|
| [`build-and-push.sh`](../scripts/build-and-push.sh) | Builds the backend image and pushes both a versioned tag and `latest` to ECR. |
| [`deploy-to-ecs.sh`](../scripts/deploy-to-ecs.sh) | Registers an ECS task definition revision and updates the ECS service. |
| [`deploy-to-eks.sh`](../scripts/deploy-to-eks.sh) | Configures the EKS dependencies and deploys the current `latest` image with Helm. |

The scripts read Terraform outputs when matching environment variables are not supplied. They intentionally keep application deployment separate from infrastructure provisioning.
