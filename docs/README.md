# Documentation

The project documentation moves from product intent to implementation decisions without repeating operational guidance from the component READMEs.

| Area | What it covers |
|------|----------------|
| [Requirements](1-requirements/README.md) | User stories, acceptance criteria, and product intent. |
| [Design](2-design/README.md) | Target architecture and C4 diagrams. |
| [Data design](2-design/data/README.md) | Logical data model and reminder schedule representation. |
| [Architectural decisions](2-design/decisions/README.md) | Accepted technology and architecture choices, with their rationale. |
| [Backend guide](../backend/README.md) | Local startup, API exploration, and tests. |
| [Infrastructure guide](../infra/README.md) | Terraform stack boundaries and deployment workflow. |

## Diagrams

Diagram sources are written in PlantUML (`.puml`). When a source changes under `docs/`, the [PlantUML workflow](../.github/workflows/plantuml.yml) generates and commits the corresponding PNG alongside it.
