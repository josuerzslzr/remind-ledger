#!/usr/bin/env bash
#
# Deploy an already-pushed ECR image to ECS by registering a new task
# definition revision and updating the service.
# Works identically from a developer laptop or CI.
#
# Required inputs (env vars or auto-detected):
#   ECR_REPO_URL  — ECR repository URL
#   ECS_CLUSTER   — ECS cluster name
#   ECS_SERVICE   — ECS service name
#   AWS_REGION    — AWS region
#                   CI: all four from GitHub Actions variables
#                   Local: read from terraform -chdir=infra/backend output
#   IMAGE_TAG     — optional positional arg $1, defaults to git short SHA
#
# Usage:
#   ./scripts/deploy-to-ecs.sh              # tag = git short SHA
#   ./scripts/deploy-to-ecs.sh v1.2.3       # tag = v1.2.3

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

IMAGE_TAG="${1:-$(git -C "$REPO_ROOT" rev-parse --short HEAD)}"

# ---------------------------------------------------------------------------
# Resolve infrastructure coordinates.
# In CI these come from env vars; locally they're read from Terraform outputs.
# ---------------------------------------------------------------------------
if [[ -z "${ECR_REPO_URL:-}" ]]; then
  echo "==> Reading ECR_REPO_URL from infra/foundation Terraform state"
  ECR_REPO_URL=$(terraform -chdir="$REPO_ROOT/infra/foundation" output -raw ecr_repository_url)
fi

if [[ -z "${ECS_CLUSTER:-}" || -z "${ECS_SERVICE:-}" ]]; then
  TF_DIR="$REPO_ROOT/infra/backend"
  echo "==> Reading ECS coordinates from $TF_DIR"
  ECS_CLUSTER="${ECS_CLUSTER:-$(terraform -chdir="$TF_DIR" output -raw ecs_cluster_name)}"
  ECS_SERVICE="${ECS_SERVICE:-$(terraform -chdir="$TF_DIR" output -raw ecs_service_name)}"
fi

AWS_REGION="${AWS_REGION:-$(aws configure get region 2>/dev/null)}"

# Fail fast if any required variable is missing.
MISSING=()
[[ -z "${ECR_REPO_URL:-}" ]] && MISSING+=("ECR_REPO_URL")
[[ -z "${ECS_CLUSTER:-}" ]] && MISSING+=("ECS_CLUSTER")
[[ -z "${ECS_SERVICE:-}" ]] && MISSING+=("ECS_SERVICE")
[[ -z "${AWS_REGION:-}" ]]  && MISSING+=("AWS_REGION")
if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "ERROR: Missing required variables: ${MISSING[*]}" >&2
  echo "       Set them as env vars or ensure Terraform state is available." >&2
  exit 1
fi

ECS_TASK_FAMILY="${ECS_CLUSTER}"
IMAGE_URI="$ECR_REPO_URL:$IMAGE_TAG"

echo "==> Config"
echo "    ECR_REPO_URL  = $ECR_REPO_URL"
echo "    ECS_CLUSTER   = $ECS_CLUSTER"
echo "    ECS_SERVICE   = $ECS_SERVICE"
echo "    IMAGE_TAG     = $IMAGE_TAG"

# ---------------------------------------------------------------------------
# Register a new task definition revision with the exact image URI,
# then point the ECS service at it.
# ---------------------------------------------------------------------------
echo "==> Fetching current task definition ($ECS_TASK_FAMILY)"
aws ecs describe-task-definition \
  --task-definition "$ECS_TASK_FAMILY" \
  --query 'taskDefinition' \
  --output json \
  | jq 'del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
  > /tmp/task-def.json

echo "==> Patching image to $IMAGE_URI"
jq --arg IMG "$IMAGE_URI" \
   '.containerDefinitions[0].image = $IMG' \
   /tmp/task-def.json > /tmp/task-def-updated.json

echo "==> Registering new task definition revision"
NEW_ARN=$(aws ecs register-task-definition \
  --cli-input-json file:///tmp/task-def-updated.json \
  --query 'taskDefinition.taskDefinitionArn' \
  --output text)
echo "    New revision: $NEW_ARN"

echo "==> Updating ECS service to $NEW_ARN"
aws ecs update-service \
  --cluster "$ECS_CLUSTER" \
  --service "$ECS_SERVICE" \
  --task-definition "$NEW_ARN" \
  --no-cli-pager > /dev/null

echo "==> Deploy triggered. Image: $IMAGE_URI"
echo "    Monitor: aws ecs wait services-stable --cluster $ECS_CLUSTER --services $ECS_SERVICE"
