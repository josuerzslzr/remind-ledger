# 2. Design

Architecture and behavior diagrams for RemindLedger. All diagrams are in **PlantUML** (`.puml`).

## Layout

| Folder | Content |
|--------|--------|
| [c4/](c4/) | C4 model: system context, containers, components (use [C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML)). |
| [data/](data/) | Logical and physical data model (entities, relationships, schema). |
| [sequence/](sequence/) | Sequence diagrams for key flows (e.g. create reminder, notify user). |

One `.puml` file per diagram. Add `img/` subfolders and export PNG/SVG here if you want rendered images in the repo.

## How to generate

- **VS Code:** Install “PlantUML” extension (e.g. jebbs.plantuml), then preview with `Alt+D` or export from command palette.
- **CLI:** Install [PlantUML](https://plantuml.com/) (Java required), then e.g. `plantuml docs/2-design/c4/context.puml`.
- **C4:** Include C4-PlantUML in your diagram (see `c4/context.puml` or [C4-PlantUML samples](https://github.com/plantuml-stdlib/C4-PlantUML)).

## Requirements

Design is driven by [../1-requirements/user-stories.md](../1-requirements/user-stories.md). C4 describes structure; sequence diagrams describe main flows.
