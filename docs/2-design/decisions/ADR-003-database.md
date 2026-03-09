# ADR-003: Database and Schema Migrations

## Status

Accepted

## Context

RemindLedger needs persistent storage for users, reminders, and notification records.
The data is structured and relational: a User has many Reminders; a Reminder generates
many Notifications. Strong referential integrity is desirable (a notification must always
belong to a valid reminder). Query patterns are flexible and not fully known upfront
(e.g. filter reminders by user, list pending notifications by due time, aggregate by status).

User authentication is handled by AWS Cognito (ADR-006); the database stores only
the Cognito subject ID (`sub`) as a foreign reference — no credentials or
password hashes.

A schema migration tool is needed from day one to manage DDL changes in a
repeatable, version-controlled way.

## Decision

### Database
**Amazon RDS for PostgreSQL** (`db.t3.micro`, Single-AZ, in Phase 1).

### Schema migrations
**Flyway.**

Flyway is the most common migration tool in the Spring Boot ecosystem. It uses
plain SQL files with a simple versioning convention. Spring Boot auto-configures
Flyway — migrations run automatically on application startup.

Naming convention: `V{NNN}__{description}.sql` with zero-padded three-digit
version numbers (e.g. `V001__create_users.sql`, `V002__create_reminders.sql`).
This ensures correct lexicographic ordering in file explorers and version control.

## Alternatives considered

### Database

| Option | Reason rejected |
|--------|----------------|
| Amazon DynamoDB | Access patterns must be defined upfront; relational model maps poorly to key-value/document; adds design complexity without scale benefit at this stage. |
| Amazon Aurora PostgreSQL | No free tier; ~$53/month minimum; overkill for current scale. Migration path from RDS PostgreSQL is straightforward if scale demands it later. |
| MySQL on RDS | No meaningful advantage over PostgreSQL for this use case; PostgreSQL has stronger JSON support, stricter SQL compliance, and better community momentum for new projects in 2026. |
| Self-managed PostgreSQL on EC2 | Too much operational overhead; RDS provides backups, patching, and failover management. |

### Schema migrations

| Option | Reason rejected |
|--------|----------------|
| Liquibase | More feature-rich (declarative changelogs, auto-rollback, database diffing, multi-format support). Not selected because the additional capabilities are not needed for a single-database project with a schema designed from scratch. Flyway's SQL-first, imperative approach is simpler and more transparent. |
| Manual DDL scripts (no tool) | No version tracking, no repeatable execution, no validation that the schema matches the expected state. Not viable for any project beyond a prototype. |

## Consequences

- Migrations are stored in `src/main/resources/db/migration/` following the Flyway's naming convention but with with zero-padded three-digit `V{NNN}__{description}.sql` (e.g. `V001__create_users.sql`).
- Flyway tracks applied migrations in a `flyway_schema_history` table in the database.
- Migrations run automatically on Spring Boot startup; no separate CLI step required.
- All migrations are forward-only (no automatic rollback). Schema fixes are applied as new versioned migrations.
- Single-AZ in Phase 1 means no automatic failover; accepted given the current scale requirements.
- RDS instance should be stopped (not deleted) when not actively developing to minimise cost.
- AWS free tier covers 750 hours/month on `db.t3.micro` for 12 months (legacy free tier) or via credits on the new model.
- If scale demands it, migration path to Aurora PostgreSQL is straightforward (engine-compatible).

