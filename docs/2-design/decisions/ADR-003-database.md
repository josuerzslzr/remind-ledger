# ADR-003: Database

## Status

Accepted

## Context

RemindLedger needs persistent storage for users, reminders, and notification records.
The data is structured and relational: a User has many Reminders; a Reminder generates
many Notifications. Strong referential integrity is desirable (a notification must always
belong to a valid reminder). Query patterns are flexible and not fully known upfront
(e.g. filter reminders by user, list pending notifications by due time, aggregate by status).

## Decision

**Amazon RDS for PostgreSQL** (`db.t3.micro`, Single-AZ, in Phase 1).

## Alternatives considered


| Option                         | Reason rejected                                                                                                                                                                                                                          |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Amazon DynamoDB                | Access patterns must be defined upfront; relational model maps poorly to key-value/document; adds design complexity without scale benefit at this stage                                                                                  |
| Amazon Aurora PostgreSQL       | No free tier; ~$53/month minimum; overkill for current scale. Migration path from RDS PostgreSQL is straightforward if scale demands it later.                                                                                           |
| MySQL on RDS                   | No meaningful advantage over PostgreSQL for this use case; PostgreSQL has stronger JSON support, stricter SQL compliance, and better community momentum for new projects in 2026. Oracle ownership of MySQL is a concern for some teams. |
| Self-managed PostgreSQL on EC2 | Too much operational overhead; RDS provides backups, patching, and failover management.                                                                                                                                                  |


## Consequences

- Schema changes require migrations; Flyway or Liquibase to be decided during implementation.
- Single-AZ in Phase 1 means no automatic failover; accepted for a personal/portfolio project.
- RDS instance should be stopped (not deleted) when not actively developing to minimise cost.
- AWS free tier covers 750 hours/month on `db.t3.micro` for 12 months (legacy free tier) or via credits on the new model.
- If scale demands it, migration path to Aurora PostgreSQL is straightforward (engine-compatible).

