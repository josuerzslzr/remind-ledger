# Architectural decisions

Architectural decision records (ADRs) capture why significant technical choices were made. **Accepted** describes the state of the decision, not whether every part of it has already been implemented; see the root [current status](../../../README.md#current-status) for implementation progress.

| ADR | Status | Decision |
|-----|--------|----------|
| [ADR-001](ADR-001-backend-framework.md) | Accepted | Use Spring Boot 3 with Java 21, Maven, and springdoc-openapi for the backend API. |
| [ADR-002](ADR-002-frontend-framework.md) | Accepted | Build the planned SPA with React, Vite, TypeScript, React Router, and Tailwind CSS. |
| [ADR-003](ADR-003-database.md) | Accepted | Use Amazon RDS for PostgreSQL with Flyway-managed schema migrations. |
| [ADR-004](ADR-004-iac-tool.md) | Accepted | Manage AWS infrastructure with Terraform and remote state. |
| [ADR-005](ADR-005-deployment-platform.md) | Accepted | Support selectable ECS Fargate or EKS runtimes behind shared backend infrastructure. |
| [ADR-006](ADR-006-authentication.md) | Accepted | Use Amazon Cognito as the OAuth 2.0/OIDC identity provider with a custom frontend login. |
| [ADR-007](ADR-007-repository-structure.md) | Accepted | Keep backend, frontend, infrastructure, and documentation in one repository. |
| [ADR-008](ADR-008-cicd-platform.md) | Accepted | Use GitHub Actions with OIDC federation for AWS delivery workflows. |

Each ADR remains the authoritative source for its context, alternatives, and consequences.
