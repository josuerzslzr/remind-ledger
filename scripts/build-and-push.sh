#!/usr/bin/env bash
#
# Build the Spring Boot Docker image and push it to ECR.
# Works identically from a developer laptop or CI.
#
# Required inputs (env vars or auto-detected):
#   ECR_REPO_URL  — ECR repository URL
#                   CI: GitHub Actions variable
#                   Local: read from terraform -chdir=infra/foundation output
#   AWS_REGION    — AWS region
#                   CI: GitHub Actions variable
#                   Local: from AWS CLI config
#   IMAGE_TAG     — optional positional arg $1, defaults to git short SHA
#
# Usage:
#   ./scripts/build-and-push.sh              # tag = git short SHA
#   ./scripts/build-and-push.sh v1.2.3       # tag = v1.2.3

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"

IMAGE_TAG="${1:-$(git -C "$REPO_ROOT" rev-parse --short HEAD)}"

# ---------------------------------------------------------------------------
# Resolve infrastructure coordinates.
# In CI these come from env vars; locally they're read from Terraform outputs.
# ---------------------------------------------------------------------------
if [[ -z "${ECR_REPO_URL:-}" ]]; then
  TF_DIR="$REPO_ROOT/infra/foundation"
  echo "==> Reading Terraform outputs from $TF_DIR"
  ECR_REPO_URL=$(terraform -chdir="$TF_DIR" output -raw ecr_repository_url)
fi

AWS_REGION="${AWS_REGION:-$(aws configure get region || echo "us-east-1")}"
ECR_REGISTRY="${ECR_REPO_URL%%/*}"

echo "==> Config"
echo "    ECR_REPO_URL  = $ECR_REPO_URL"
echo "    IMAGE_TAG     = $IMAGE_TAG"
echo "    AWS_REGION    = $AWS_REGION"

# ---------------------------------------------------------------------------
# ECR login, build, push
# ---------------------------------------------------------------------------
echo "==> Authenticating Docker with ECR ($ECR_REGISTRY)"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "==> Building image from $BACKEND_DIR"
docker build -t "$ECR_REPO_URL:$IMAGE_TAG" "$BACKEND_DIR"
docker tag "$ECR_REPO_URL:$IMAGE_TAG" "$ECR_REPO_URL:latest"

echo "==> Pushing $ECR_REPO_URL:$IMAGE_TAG and :latest"
docker push "$ECR_REPO_URL:$IMAGE_TAG"
docker push "$ECR_REPO_URL:latest"

echo "==> Done. Image: $ECR_REPO_URL:$IMAGE_TAG"
