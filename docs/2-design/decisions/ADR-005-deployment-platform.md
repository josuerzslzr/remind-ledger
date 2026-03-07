# ADR-005: Deployment Platform

## Status

Accepted

## Context

RemindLedger's backend (Spring Boot) and frontend (Next.js) need to be hosted on AWS.
The deployment model must support containerised workloads, HTTPS, horizontal scaling,
and health checks without requiring manual EC2 instance management.

## Decision

**Amazon ECS Fargate + Application Load Balancer (ALB).**

- One shared ALB with host-based or path-based routing rules serving both the API and frontend ECS services.
- HTTPS termination at the ALB; TLS certificate provisioned via AWS ACM (free).
- Spring Boot API: one ECS service, `desired_count = 1` in Phase 1.
- Next.js frontend: one ECS service, `desired_count = 1` in Phase 1.
- Container images stored in Amazon ECR.

## Alternatives considered


| Option                    | Reason rejected                                                                                                                            |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| EC2 (self-managed)        | Too much operational overhead; patching, scaling, and load balancing all manual.                                                           |
| AWS Elastic Beanstalk     | Legacy PaaS; EC2-based, not container-native; not the direction of AWS investment; weaker portfolio signal.                                |
| AWS App Runner            | Simpler than ECS but less control; 58% more expensive per vCPU-hour when active; smaller ecosystem and community vs ECS.                   |
| Kubernetes (EKS)          | Significant operational complexity; overkill for this scale; EKS deserves a dedicated project to learn properly.                           |
| Vercel (Next.js frontend) | Not AWS; cannot be managed by Terraform; breaks the cloud-native AWS-hosted requirement.                                                   |
| AWS Lambda (API)          | JVM cold start (~3–8s) is prohibitive for a user-facing REST API without GraalVM native compilation. Considered for the worker in Phase 2. |
| API Gateway + Lambda      | Same cold start issue; API Gateway unnecessary when ALB handles routing and HTTPS termination for ECS.                                     |


## Consequences

- ALB has no free tier; costs ~$16–18/month when running. At ~3h/day active usage, cost is ~$2/month. Covered by AWS free credits.
- ECS Fargate has no free tier; costs ~$9/month per task at 24/7. At ~3h/day, ~$1/month per task.
- ECS tasks and RDS should be stopped when not actively developing to minimise cost.
- ECS Auto Scaling (target tracking by CPU) is deferred to Phase 2 as a deliberate learning exercise.
- Spring Boot must be stateless (JWT auth, no in-memory session) for horizontal scaling to work correctly.
- WebSocket sticky sessions or a message broker (SQS/Redis Pub/Sub) required if multiple API tasks run simultaneously — addressed in Phase 2.

