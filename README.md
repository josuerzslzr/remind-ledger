# RemindLedger

RemindLedger is a web-first service for creating and managing recurring reminders. The project explores a cloud-native architecture on AWS while keeping the application straightforward to run and evolve.

## Architecture

The diagram below shows the intended end state. Some components are still planned, as described in [Current status](#current-status).

![RemindLedger target container architecture](docs/2-design/c4/RemindLedger%20-%20Containers.png)

See the [design documentation](docs/2-design/README.md) for the diagram sources and supporting models.

## Current status

**Now**

- Authenticated reminder CRUD API built with Spring Boot.
- PostgreSQL persistence managed with Flyway migrations.
- Cognito JWT validation and a self-contained local authentication profile.
- Terraform infrastructure with selectable ECS Fargate or EKS runtimes and manual deployment scripts.

**Next** — planned, without an implied delivery order

- React web application for managing reminders.
- EventBridge Scheduler and SQS-based notification delivery.
- GitHub Actions workflows for application and infrastructure delivery.

## Technology

| Area | Selection |
|------|-----------|
| Backend | Java 21, Spring Boot, Maven, OpenAPI |
| Data | PostgreSQL, Flyway |
| Authentication | Amazon Cognito, OAuth 2.0/OIDC |
| Infrastructure | Terraform, AWS, Docker, ECS Fargate or EKS |
| Frontend | React, Vite, TypeScript *(planned)* |
| Reminder scheduling | EventBridge Scheduler, SQS *(planned)* |
| Notification delivery | Channels and providers to be defined *(planned)* |
| Mobile | To be defined *(future)* |

The rationale behind these choices is recorded in the [architectural decisions](docs/2-design/decisions/README.md).

## Repository guide

| Path | Purpose |
|------|---------|
| [backend/](backend/README.md) | Reminder API, database migrations, and backend tests. |
| `frontend/` *(planned)* | React single-page application. |
| [infra/](infra/README.md) | Terraform stacks, AWS runtime selection, and deployment guidance. |
| [docs/](docs/README.md) | Requirements, architecture, data design, and decisions. |
| [scripts/](scripts/) | Image build and runtime-specific deployment scripts. |

Start with the [documentation index](docs/README.md), or go directly to the [backend](backend/README.md) and [infrastructure](infra/README.md) guides for operational details.
