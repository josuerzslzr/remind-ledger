# ADR-001: Backend Framework and Build Tool

## Status
Accepted

## Context
RemindLedger needs a backend to expose REST APIs for reminder management, handle
scheduled notification dispatch, and integrate with AWS services (RDS, SQS, ECS).
The backend must support a conventional layered architecture (Controller → Service → Repository),
transactional data access, and straightforward horizontal scaling on ECS.

A build tool is needed to manage dependencies, compile, test, and package the
application. The choice is made alongside the framework because it determines
project structure, CI scripts, and the day-to-day developer workflow.

## Decision

### Framework
**Spring Boot 3.x + Java 21 + Spring MVC (imperative, thread-per-request).**

Virtual threads (Project Loom, enabled via Spring Boot 3.2+) are used to close
the concurrency gap with reactive programming without changing the imperative
programming model.

### Build tool
**Apache Maven.**

Maven is the dominant build tool in the Spring Boot ecosystem. Its declarative,
convention-over-configuration model (`pom.xml`) keeps the build predictable and
readable without requiring a separate scripting language.

### API documentation
**springdoc-openapi.**

The `springdoc-openapi` library is used to auto-generate an OpenAPI 3.0
specification from Spring MVC controllers and to serve Swagger UI for
interactive exploration and testing. Controllers are annotated with
standard Jakarta/OpenAPI annotations (e.g. `@Operation`, `@Parameter`) as
needed; the spec is derived at runtime without maintaining a separate spec file.

## Alternatives considered

### Framework

| Option | Reason rejected |
|--------|----------------|
| Spring Boot + Kotlin | Kotlin on Spring is a valid and growing combination. Deferred — Java is sufficient for this project and avoids introducing an additional language alongside modern Spring patterns. |
| Spring WebFlux (reactive) | Steeper learning curve; requires R2DBC instead of JPA; stack traces are harder to debug; RemindLedger's scale does not justify a non-blocking reactive model. Virtual threads on Spring MVC close the concurrency gap without the complexity. Kept as a Phase 2 module. |
| NestJS + TypeScript | Strong ecosystem and enterprise patterns. Not selected because the project is intentionally Spring Boot / Java to leverage the JVM stack end-to-end. |
| FastAPI (Python) | Python is not part of the chosen stack for this project. |
| Go | Excellent for high-throughput services; less mature ORM ecosystem; not the chosen stack. |

### API documentation

| Option | Reason rejected |
|--------|----------------|
| springfox (Swagger 2) | Deprecated; no Spring Boot 3.x support; relies on Swagger 2.0 (OpenAPI 2) spec format. |
| Manual OpenAPI YAML/JSON | Single source of truth possible, but duplicates controller logic and is error-prone to keep in sync with code. |
| No formal API docs | Not viable for external consumers or client generation. |

### Build tool

| Option | Reason rejected |
|--------|----------------|
| Gradle (Kotlin DSL) | Faster incremental builds and more concise build files. Not selected because it introduces Kotlin as a build-script language in a Java project — a minor contradiction with deferring Kotlin in the framework decision — and adds a steeper learning curve for a conventional enterprise stack. |
| Gradle (Groovy DSL) | Legacy Gradle format being phased out in favour of Kotlin DSL. No advantage over Maven for a new project. |

## Consequences

- Spring Boot 3.x requires Java 17 minimum; Java 21 chosen for virtual thread support and LTS status.
- All `javax.*` imports are now `jakarta.*` (Jakarta EE namespace migration in Spring Boot 3.x).
- Spring Security 6 configuration model changed (no `WebSecurityConfigurerAdapter`; use `SecurityFilterChain` beans instead).
- Spring WebFlux is deferred to Phase 2 (e.g. a Server-Sent Events endpoint); imperative model is sufficient for Phase 1 requirements.
- Project structure follows the Maven standard layout (`src/main/java`, `src/main/resources`, `src/test/java`).
- Spring Boot Maven Plugin (`spring-boot-maven-plugin`) used for building the executable JAR and container image (via `spring-boot:build-image` or a Dockerfile with a multi-stage build — decided during implementation).
- Maven Wrapper (`mvnw`) committed to the repo so CI and contributors do not need a global Maven install.
- springdoc-openapi exposes `/v3/api-docs` (JSON) and `/swagger-ui.html` (Swagger UI) by default; configuration (e.g. disabling in production, custom paths) is done via `application.properties`/`application.yml`.
