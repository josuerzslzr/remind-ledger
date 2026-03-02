# Docs

Requirements, design, and architecture documentation for RemindLedger.

## PlantUML diagrams

Diagrams are written in **PlantUML** (`.puml`). When you push or update any `.puml` file under `docs/`, a **GitHub Action** runs and generates a PNG image next to each changed source file. You don’t need to commit the PNGs yourself—they are produced and committed by the workflow.

- **Workflow:** [../.github/workflows/plantuml.yml](../.github/workflows/plantuml.yml)
- **Design diagrams:** [2-design/](2-design/) (C4, data model, etc.)
