#!/usr/bin/env bash
#
# Deploy an already-pushed ECR image to the EKS runtime. The script bootstraps
# the cluster controllers idempotently, installs/upgrades the application Helm
# release, and waits for the pod and ALB target to become healthy.
#
# Usage:
#   ./scripts/deploy-to-eks.sh              # tag = git short SHA
#   ./scripts/deploy-to-eks.sh v1.2.3       # tag = v1.2.3

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TF_DIR="$REPO_ROOT/infra/backend"
APP_CHART="$TF_DIR/helm/remind-ledger"
FLUENT_BIT_VALUES="$TF_DIR/helm/controllers/fluent-bit-values.yaml"
IMAGE_TAG="${1:-$(git -C "$REPO_ROOT" rev-parse --short HEAD)}"

for command in aws terraform kubectl helm; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "ERROR: Required command not found: $command" >&2
    exit 1
  fi
done

tf_output() {
  terraform -chdir="$TF_DIR" output -raw "$1"
}

DEPLOYMENT_TARGET="${DEPLOYMENT_TARGET:-$(tf_output deployment_target)}"
if [[ "$DEPLOYMENT_TARGET" != "eks" ]]; then
  echo "ERROR: Terraform deployment_target is '$DEPLOYMENT_TARGET', not 'eks'." >&2
  exit 1
fi

AWS_REGION="${AWS_REGION:-$(tf_output aws_region)}"
ECR_REPO_URL="${ECR_REPO_URL:-$(tf_output ecr_repository_url)}"
EKS_CLUSTER="${EKS_CLUSTER:-$(tf_output eks_cluster_name)}"
VPC_ID="${VPC_ID:-$(tf_output vpc_id)}"
TARGET_GROUP_ARN="${TARGET_GROUP_ARN:-$(tf_output target_group_arn)}"
DB_HOST="${DB_HOST:-$(tf_output rds_endpoint)}"
DB_PORT="${DB_PORT:-$(tf_output db_port)}"
DB_NAME="${DB_NAME:-$(tf_output db_name)}"
DB_SECRET_ARN="${DB_SECRET_ARN:-$(tf_output db_secret_arn)}"
COGNITO_ISSUER_URI="${COGNITO_ISSUER_URI:-$(tf_output cognito_issuer_uri)}"
SPRING_PROFILE="${SPRING_PROFILE:-$(tf_output spring_profile)}"
CLOUDWATCH_LOG_GROUP="${CLOUDWATCH_LOG_GROUP:-$(tf_output eks_app_log_group_name)}"

echo "==> Configuring kubectl for EKS cluster $EKS_CLUSTER"
aws eks update-kubeconfig \
  --name "$EKS_CLUSTER" \
  --region "$AWS_REGION"

echo "==> Waiting for the managed node group"
kubectl wait --for=condition=Ready nodes --all --timeout=10m

echo "==> Updating Helm repositories"
helm repo add eks https://aws.github.io/eks-charts --force-update
helm repo add external-secrets https://charts.external-secrets.io --force-update
helm repo add fluent https://fluent.github.io/helm-charts --force-update
helm repo update

echo "==> Installing AWS Load Balancer Controller"
helm show crds eks/aws-load-balancer-controller | kubectl apply --server-side -f -
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --namespace kube-system \
  --set clusterName="$EKS_CLUSTER" \
  --set region="$AWS_REGION" \
  --set vpcId="$VPC_ID" \
  --set serviceAccount.create=true \
  --set serviceAccount.name=aws-load-balancer-controller \
  --wait \
  --timeout 10m

echo "==> Installing External Secrets Operator"
helm upgrade --install external-secrets external-secrets/external-secrets \
  --namespace external-secrets \
  --create-namespace \
  --set installCRDs=true \
  --set serviceAccount.create=true \
  --set serviceAccount.name=external-secrets \
  --wait \
  --timeout 10m

echo "==> Installing Fluent Bit"
helm upgrade --install fluent-bit fluent/fluent-bit \
  --namespace logging \
  --create-namespace \
  --values "$FLUENT_BIT_VALUES" \
  --set serviceAccount.create=true \
  --set serviceAccount.name=fluent-bit \
  --set "env[0].name=AWS_REGION" \
  --set-string "env[0].value=$AWS_REGION" \
  --set "env[1].name=CLOUDWATCH_LOG_GROUP" \
  --set-string "env[1].value=$CLOUDWATCH_LOG_GROUP" \
  --wait \
  --timeout 10m

echo "==> Deploying $ECR_REPO_URL:$IMAGE_TAG"
helm upgrade --install remind-ledger "$APP_CHART" \
  --namespace kube-system \
  --set-string image.repository="$ECR_REPO_URL" \
  --set-string image.tag="$IMAGE_TAG" \
  --set-string awsRegion="$AWS_REGION" \
  --set-string springProfile="$SPRING_PROFILE" \
  --set-string cognitoIssuerUri="$COGNITO_ISSUER_URI" \
  --set-string targetGroupArn="$TARGET_GROUP_ARN" \
  --set-string database.host="$DB_HOST" \
  --set-string database.port="$DB_PORT" \
  --set-string database.name="$DB_NAME" \
  --set-string database.secretArn="$DB_SECRET_ARN" \
  --wait \
  --timeout 15m

echo "==> Verifying secret synchronization and application rollout"
kubectl wait \
  --namespace remind-ledger \
  --for=condition=Ready \
  externalsecret/remind-ledger-db \
  --timeout=5m
kubectl rollout status \
  --namespace remind-ledger \
  deployment/remind-ledger \
  --timeout=10m

echo "==> Waiting for the application target to become healthy"
aws elbv2 wait target-in-service \
  --target-group-arn "$TARGET_GROUP_ARN" \
  --region "$AWS_REGION"

echo "==> Deployment complete: $ECR_REPO_URL:$IMAGE_TAG"
