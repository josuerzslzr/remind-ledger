# ADR-005: Selectable Deployment Platform

## Status

Accepted

## Context

RemindLedger's Spring Boot backend and React frontend need to be hosted on AWS.
The backend requires containerised deployment, HTTPS, health checks, access to
shared AWS services, and support for horizontal scaling.

ECS Fargate and EKS provide different, useful operating models. ECS offers a
smaller operational surface for running containers, while EKS provides a
standard Kubernetes environment and its broader tooling. Selecting only one
would remove the ability to use the other model without redesigning the shared
infrastructure.

The frontend is a static SPA build (see ADR-002). Reminder scheduling is
delegated to EventBridge Scheduler rather than an always-running worker.

## Decision

### Backend API — selectable ECS or EKS runtime

Retain **both ECS Fargate and EKS as supported deployment targets**. Terraform
exposes `deployment_target`, accepting `ecs` or `eks` and defaulting to `eks`.
Exactly one runtime is provisioned by an `infra/backend` apply; the targets are
mutually exclusive rather than parallel environments.

Runtime-specific resources are isolated in local Terraform modules under
`infra/backend/modules/ecs` and `infra/backend/modules/eks`. Both modules use
the same backend state and integrate with the shared target group owned by the
root module. They do not define independent Terraform backends.

The deployment targets provide complementary benefits:

| Target | Benefits retained by this decision |
|--------|------------------------------------|
| ECS Fargate | Runs containers without managing worker nodes; has fewer platform components; integrates directly with AWS networking, IAM, logging, and load balancing. |
| EKS | Uses standard Kubernetes APIs and tooling; supports Helm and the Kubernetes ecosystem; keeps workload definitions portable at the orchestration layer. |

The ECS target consists of a Fargate cluster, task definition, service, IAM
roles, security rules, and logging. ECS registers its tasks directly with the
shared target group.

The EKS target consists of an EKS cluster with a managed EC2 node group,
managed Kubernetes add-ons, Pod Identity roles, private node networking,
logging, and security rules. The application is packaged as a Helm chart.
Kubernetes controllers integrate the workload with AWS:

- AWS Load Balancer Controller registers pod IPs in the existing target group
  through `TargetGroupBinding`; it does not create another load balancer.
- External Secrets Operator projects the database credentials from AWS Secrets
  Manager into a Kubernetes Secret.
- Fluent Bit sends container logs to CloudWatch Logs.

All resources required by the selected backend environment are Terraform-owned
and can be removed with `terraform destroy` from `infra/backend`. The foundation
stack (ECR and Cognito) and bootstrap stack (Terraform state storage and
locking) have separate state and remain intact.

Application deployment remains an explicit manual choice. After selecting and
applying the matching Terraform target, run either `scripts/deploy-to-ecs.sh`
or `scripts/deploy-to-eks.sh`. No wrapper automatically selects a deployment
script.

### Shared backend infrastructure

The following resources remain independent of the runtime selection:

- The VPC, RDS database, Secrets Manager secret, ALB, application target group,
  listener rule, and CloudFront distribution are managed by the `infra/backend`
  root module.
- A shared ECR repository stores the same application image for either target.
- CloudFront provides HTTPS and connects to the Terraform-managed ALB.
- The ALB security group accepts traffic only from the AWS-managed CloudFront
  origin-facing prefix list.
- The ALB default action returns 403. One priority-1 forwarding rule requires
  the secret `X-Origin-Verify` header injected by CloudFront and routes to the
  shared target group.

This boundary allows either runtime to host the same stateless application
without duplicating the database, public entry point, container image, or
authentication infrastructure.

### Reminder scheduling — Amazon EventBridge Scheduler + Amazon SQS

- Reminder changes create, update, or delete corresponding EventBridge
  schedules.
- Each schedule targets an SQS queue with the reminder ID as payload.
- The Spring Boot API consumes the queue and dispatches notifications; no
  dedicated worker container is required.

### Frontend — Amazon S3 + CloudFront

- Vite build output is uploaded to a private S3 bucket.
- CloudFront serves the SPA over HTTPS with edge caching.
- A custom 404 response returns `/index.html` with status 200 for client-side
  routes.

## Alternatives considered

### Backend API

| Option | Outcome |
|--------|---------|
| ECS and EKS simultaneously | Not selected. Running both would unnecessarily duplicate compute capacity, cost, complicate routing, and introduce data and deployment-coordination challenges. |
| EKS with managed nodes | Selected for the EKS target because it supports standard node, DaemonSet, add-on, and scheduling behavior while AWS manages node-group integration. |
| EKS on Fargate | Not selected because it changes pod capacity and scheduling behavior and does not support requirements such as DaemonSets. |
| EKS Auto Mode | Not selected because managed nodes keep the underlying Kubernetes and node-management concepts explicit. |
| EC2 application host | Rejected due to host patching, deployment, scaling, and load-balancing overhead. |
| AWS App Runner | Rejected because it offers less infrastructure control and does not provide a Kubernetes deployment interface. |
| AWS Lambda | Rejected because JVM and Spring cold starts are undesirable for the user-facing API without native compilation. |

### Reminder scheduling

| Option | Reason rejected |
|--------|-----------------|
| Long-running worker | Runs continuously and duplicates scheduling logic already provided by EventBridge Scheduler. |
| EventBridge Scheduler + one-off container | Adds container startup overhead when the API can consume durable SQS messages. |
| EventBridge Scheduler + Lambda | Introduces a second runtime or JVM cold-start concerns. |
| Spring-internal scheduling | Does not survive restarts reliably and complicates horizontal scaling. |

### Frontend

| Option | Reason rejected |
|--------|-----------------|
| Container-hosted static files | Operationally heavier than S3 and CloudFront for static assets. |
| S3 website hosting without CloudFront | Does not provide the selected HTTPS and CDN behavior. |
| Vercel / Netlify | Falls outside the AWS and Terraform deployment requirement. |

## Consequences

### Shared consequences

- Both deployment paths must be maintained, documented, and tested when shared
  application or infrastructure contracts change.
- Only the selected runtime exists. Changing `deployment_target` replaces the
  runtime-specific resources; it is not a live migration between ECS and EKS.
- The selected Terraform target and manual deployment script must match.
- The application image and configuration contract must remain compatible with
  both runtimes.
- Spring Boot must remain stateless so tasks or pods can be replaced and scaled
  horizontally.
- Destroying `infra/backend` removes its shared infrastructure as well as the
  selected runtime. ECR, Cognito, and Terraform state infrastructure remain
  because they belong to separate stacks.
- CloudFront supplies HTTPS without requiring a custom domain. A custom domain
  and ACM certificate can be added later without changing the runtime design.

### ECS target consequences

- Fargate removes worker-node provisioning, patching, and capacity management
  from the project.
- The smaller set of platform components makes ECS the more direct deployment
  and troubleshooting path.
- Task definitions, ECS services, and their deployment behavior are AWS-specific
  and do not provide Kubernetes APIs or ecosystem tooling.
- ECS registers tasks with the ALB target group without in-cluster controllers.
- ECS service auto scaling is deferred; the initial service runs a fixed desired
  task count.

### EKS target consequences

- Kubernetes provides familiar `kubectl`, Helm, Deployment, Service, and
  controller interfaces, and supports extending the platform with Kubernetes
  ecosystem components.
- The EKS runtime has more moving parts than ECS: the control plane, worker
  nodes, managed add-ons, controllers, RBAC, Pod Identity, and Kubernetes
  upgrades all require maintenance and troubleshooting.
- Managed nodes still require compute capacity and networking. The current
  small node group and single-NAT topology favour a minimal footprint over
  runtime high availability.
- Workload readiness depends on the AWS Load Balancer Controller and External
  Secrets Operator in addition to the application pod.
- The complete EKS environment is intentionally destroyable and must be
  recreated, bootstrapped, and redeployed when it is needed again.

### Scheduling and frontend consequences

- The API must keep each EventBridge Schedule in sync with its reminder's
  lifecycle.
- SQS provides durability and retry behavior while the API is unavailable. If
  multiple API instances run, a message is delivered to one consumer under
  standard queue semantics.
- Frontend deployment remains independent of the backend runtime and consists
  of uploading the SPA build and invalidating the CloudFront cache.
