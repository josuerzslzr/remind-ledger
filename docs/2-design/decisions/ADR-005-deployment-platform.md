# ADR-005: Deployment Platform

## Status

Accepted

## Context

RemindLedger's backend (Spring Boot) and frontend (React SPA) need to be hosted
on AWS. The backend requires containerised deployment with HTTPS, health checks,
and the ability to scale horizontally. The frontend is a static SPA build (see
ADR-002) that requires only a file host with HTTPS and CDN caching.

Reminders must trigger notifications at their configured time. Rather than
running a dedicated worker container that polls for due reminders, the scheduling
responsibility is delegated to AWS EventBridge Scheduler — a managed service
purpose-built for time-based event delivery.

## Decision

### Backend API — Amazon ECS Fargate + Application Load Balancer (ALB)

- ALB provides HTTPS termination; TLS certificate provisioned via AWS ACM (free).
- Spring Boot API: one ECS service, `desired_count = 1` in Phase 1.
- Container images stored in Amazon ECR.

### Reminder scheduling — Amazon EventBridge Scheduler + Amazon SQS

- When a reminder is created, updated, or deleted, the API creates, updates, or
  deletes a corresponding EventBridge Schedule (cron or rate expression).
- Each schedule carries the reminder ID as payload and targets an SQS queue.
- The Spring Boot API consumes the SQS queue via a listener, loads the reminder
  from the database, and dispatches the notification.
- No dedicated worker container is required; the API handles notification
  dispatch as part of its existing runtime.

### Frontend — Amazon S3 + CloudFront

- Vite build output (`dist/`) uploaded to a private S3 bucket.
- CloudFront distribution serves the bucket over HTTPS with edge caching.
- TLS certificate provisioned via AWS ACM (free, must be in `us-east-1` for CloudFront).
- CloudFront custom error response: 404 → `/index.html` with 200 status, enabling client-side SPA routing.

## Alternatives considered

### Backend API

| Option | Reason rejected |
|--------|----------------|
| EC2 (self-managed) | Too much operational overhead; patching, scaling, and load balancing all manual. |
| AWS Elastic Beanstalk | Legacy PaaS; EC2-based, not container-native; not the direction of AWS investment. |
| AWS App Runner | Simpler than ECS but less control; more expensive per vCPU-hour when active; smaller ecosystem and community vs ECS. |
| Kubernetes (EKS) | Significant operational complexity; overkill for this scale. |
| AWS Lambda (API) | JVM cold start (~3–8s) is prohibitive for a user-facing REST API without GraalVM native compilation. |
| API Gateway + Lambda | Same cold start issue; API Gateway unnecessary when ALB handles routing and HTTPS termination for ECS. |

### Reminder scheduling

| Option | Reason rejected |
|--------|----------------|
| Long-running ECS worker (polling) | Runs continuously even when no reminders are due; requires custom scheduling/polling logic; additional ECS service cost (~$9/month at 24/7). |
| EventBridge Scheduler + ECS RunTask | Viable — fires a Fargate task per schedule. Not selected because the API is already running and can consume SQS messages directly, avoiding the overhead of spinning up a separate container for each notification. |
| EventBridge Scheduler + Lambda | JVM cold start makes Java Lambdas slow to start. Would require a separate runtime (Node.js/Python) or GraalVM native image, introducing a second language or build complexity. |
| Spring-internal `@Scheduled` / cron | Runs inside the API container; does not survive restarts; cannot distribute across multiple API instances; tightly couples scheduling state to application lifecycle. |

### Frontend

| Option | Reason rejected |
|--------|----------------|
| ECS Fargate (nginx or Node.js container) | Running a container to serve static files is operationally heavier and more expensive than S3 + CloudFront. |
| S3 static website hosting (without CloudFront) | No HTTPS on custom domains; no edge caching; not suitable for a production app. |
| Vercel / Netlify | Not AWS; cannot be managed by Terraform; breaks the cloud-native AWS-hosted requirement. |

## Consequences

- ALB has no free tier; costs ~$16–18/month when running. At ~3h/day active usage, cost is ~$2/month. Covered by AWS free credits.
- ECS Fargate has no free tier; costs ~$9/month per task at 24/7. At ~3h/day, ~$1/month per task.
- EventBridge Scheduler is free for the first 14 million invocations/month; no cost impact at this scale.
- SQS is free for the first 1 million requests/month; no cost impact at this scale.
- S3 + CloudFront costs are negligible at this scale (S3 storage: pennies; CloudFront free tier: 1 TB/month transfer).
- ECS tasks and RDS should be stopped when not actively developing to minimise cost. CloudFront + S3 can remain active with no meaningful cost.
- Frontend deployment is `aws s3 sync` + CloudFront cache invalidation — no Docker build or container registry involved.
- The API is responsible for managing EventBridge Schedules as a side effect of reminder CRUD operations. Schedule lifecycle must stay in sync with the reminder lifecycle.
- SQS provides durability and retry semantics: if the API is temporarily unavailable, messages remain in the queue and are redelivered.
- ECS Auto Scaling (target tracking by CPU) is deferred to Phase 2; not required at initial scale.
- Spring Boot must be stateless (JWT auth, no in-memory session) for horizontal scaling to work correctly.
- If multiple API tasks run simultaneously, SQS messages are delivered to only one consumer (standard queue behaviour), avoiding duplicate notifications.

