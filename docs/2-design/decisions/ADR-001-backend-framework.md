# ADR-001: Backend Framework

## Status
Accepted

## Context
RemindLedger needs a backend to expose REST APIs for reminder management, handle
scheduled notification dispatch, and integrate with AWS services (RDS, SQS, ECS).
The backend must support a conventional layered architecture (Controller → Service → Repository),
transactional data access, and straightforward horizontal scaling on ECS.

## Decision
**Spring Boot 3.x + Java 21 + Spring MVC (imperative, thread-per-request).**

Virtual threads (Project Loom, enabled via Spring Boot 3.2+) are used to close
the concurrency gap with reactive programming without changing the imperative
programming model.

## Alternatives considered

| Option | Reason rejected |
|--------|----------------|
| Spring Boot + Kotlin | Kotlin on Spring is a valid and growing combination. Deferred — Java is sufficient for this project and avoids introducing an additional language alongside modern Spring patterns. |
| Spring WebFlux (reactive) | Steeper learning curve; requires R2DBC instead of JPA; stack traces are harder to debug; RemindLedger's scale does not justify a non-blocking reactive model. Virtual threads on Spring MVC close the concurrency gap without the complexity. Kept as a Phase 2 module. |
| NestJS + TypeScript | Strong ecosystem and enterprise patterns. Not selected because the project is intentionally Spring Boot / Java to leverage the JVM stack end-to-end. |
| FastAPI (Python) | Python is not part of the chosen stack for this project. |
| Go | Excellent for high-throughput services; less mature ORM ecosystem; not the chosen stack. |

## Consequences

- Spring Boot 3.x requires Java 17 minimum; Java 21 chosen for virtual thread support and LTS status.
- All `javax.*` imports are now `jakarta.*` (Jakarta EE namespace migration in Spring Boot 3.x).
- Spring Security 6 configuration model changed (no `WebSecurityConfigurerAdapter`; use `SecurityFilterChain` beans instead).
- Spring WebFlux is deferred to Phase 2 as a deliberate exploration (e.g. a Server-Sent Events endpoint).
