# Backend

Spring Boot REST API for user-scoped reminder management. It provides CRUD endpoints, validates schedule data, persists to PostgreSQL, and exposes an OpenAPI definition.

## Prerequisites

- Java 21
- Docker with Compose support
- Network access on the first run so the Maven wrapper and container images can be downloaded

## Run locally

From the repository root:

```bash
export DB_HOST=localhost DB_PORT=5432 DB_NAME=remindledger
export DB_USERNAME=remindledger DB_PASSWORD=remindledger
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Spring Boot starts the PostgreSQL service from the root `docker-compose.yml` and applies Flyway migrations. The `local` profile supplies a development user and token, so Cognito is not required for local requests.

| Endpoint | Purpose |
|----------|---------|
| <http://localhost:8080/swagger-ui.html> | Explore and call the API interactively. |
| <http://localhost:8080/v3/api-docs> | Generated OpenAPI document. |
| <http://localhost:8080/actuator/health> | Application and dependency health. |
| `http://localhost:8080/api/reminders` | Reminder collection API. |

Stop the application with `Ctrl+C`. The database container uses a named volume and remains available for the next run.

## Tests

```bash
cd backend
./mvnw test
```

The repository tests use Testcontainers for PostgreSQL, so the Docker daemon must be running and able to obtain the `postgres:16` image.

For data shapes and schedule rules, see the [data design](../docs/2-design/data/README.md). Backend technology choices are explained in [ADR-001](../docs/2-design/decisions/ADR-001-backend-framework.md) and [ADR-003](../docs/2-design/decisions/ADR-003-database.md).
