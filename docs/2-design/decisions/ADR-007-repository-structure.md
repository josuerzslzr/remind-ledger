# ADR-007: Repository Structure (Monorepo)

## Status

Accepted

## Context

RemindLedger consists of three distinct codebases with different languages,
build tools, and runtimes:

| Component      | Language / Tool   | Build output               |
| -------------- | ----------------- | -------------------------- |
| Backend API    | Java 21 / Maven   | Executable JAR / container |
| Frontend SPA   | TypeScript / Vite  | Static files (`dist/`)     |
| Infrastructure | HCL / Terraform    | Terraform state            |

The repository structure decision affects developer workflow, CI/CD pipeline
design, cross-component coordination (e.g. a backend API change that requires a
matching frontend update), and the overhead of managing version control across
components.

This decision should be made before the first line of application code is
committed, because it determines directory layout, CI configuration, and how
components reference each other.

## Decision

**Monorepo — all three components live in a single Git repository.**

Top-level directory layout:

```
/
├── backend/          # Spring Boot (Maven)
├── frontend/         # React SPA (Vite)
├── infra/            # Terraform
├── docs/             # Architecture docs, ADRs
└── .github/          # CI/CD workflows
```

Each component retains its own build tool and dependency management
(`pom.xml`, `package.json`, `*.tf`). There is no monorepo orchestration tool
(Nx, Turborepo, Bazel) — the project is small enough that CI workflows
coordinate builds directly.

## Alternatives considered

| Option | Reason rejected |
|--------|----------------|
| Polyrepo (one repo per component) | Atomic cross-component changes (e.g. new API endpoint + frontend page + Terraform resource) require coordinated commits across multiple repos. Pull requests cannot span repos, so changes that must be deployed together are harder to review and merge atomically. Adds Git overhead (multiple clones, multiple remotes) for a single-developer project with no organisational boundary justifying the split. |
| Monorepo with orchestration tool (Nx, Turborepo) | These tools shine in large monorepos with many packages and complex dependency graphs. RemindLedger has exactly three components with no shared libraries between them (Java and TypeScript do not share code). The orchestration layer adds configuration and learning overhead without meaningful benefit at this scale. Reconsidered if shared TypeScript packages or additional services are introduced. |
| Git submodules | Combines some disadvantages of both approaches: repos are technically separate (independent histories, separate cloning) but appear nested. Submodule workflows are error-prone (detached HEAD, out-of-sync pointers) and widely considered a last resort when repo separation is mandatory. |

## Consequences

- All code, docs, and infrastructure live under one `git clone`. A single branch can contain a backend change, its matching frontend change, and the Terraform resource — reviewable in one pull request.
- CI/CD workflows use path filters (e.g. `paths: [backend/**]`) to run only the relevant pipeline when a component changes, avoiding unnecessary builds.
- Each component's build is independent: `mvn` in `backend/`, `npm` in `frontend/`, `terraform` in `infra/`. No cross-component build dependency exists at the tool level.
- Repository size grows over time with all three components. At this project's scale (single developer, no binary assets) this is not a concern; Git handles repositories of this size without issue.
- If the project grows to multiple services or teams with independent release cadences, polyrepo can be reconsidered. Extracting a component into its own repo is straightforward with `git filter-branch` or `git subtree split`.
- No monorepo tooling means CI workflow files must be maintained manually. Accepted because three workflows (backend, frontend, infra) are manageable without automation.
