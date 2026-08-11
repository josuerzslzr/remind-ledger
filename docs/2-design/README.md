# Design

Target architecture and data models for RemindLedger. These documents describe the intended system; consult the root [current status](../../README.md#current-status) to distinguish implemented and planned components.

## Contents

| Area | Content |
|------|---------|
| [C4 model](c4/) | System context and container views, with PlantUML sources and rendered PNGs. |
| [Data design](data/README.md) | Logical entities, relationships, and schedule representations. |
| [Architectural decisions](decisions/README.md) | Technology and architecture choices, alternatives, and consequences. |

Design is driven by the [user stories](../1-requirements/user-stories.md). An accepted architectural decision records an agreed direction; it does not by itself mean that the decision has been implemented.

## Working with diagrams

- **GitHub:** pushing a changed `.puml` file triggers the [PlantUML workflow](../../.github/workflows/plantuml.yml), which updates PNGs alongside their sources.
- **VS Code:** use a PlantUML extension to preview or export a diagram.
- **CLI:** install [PlantUML](https://plantuml.com/) and run, for example, `plantuml docs/2-design/c4/context.puml` from the repository root.

The C4 sources use [C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML).
