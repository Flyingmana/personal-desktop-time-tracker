# Specs

This directory holds the user stories that drive implementation. Each user story gets its own
folder under `specs/<story-slug>/` (kebab-case) containing three files:

- `requirements.md` — the user story and its acceptance criteria (what the feature must do)
- `design.md` — the technical approach for implementing the story (architecture, data model, UI
  flow, edge cases)
- `tasks.md` — a checklist of concrete implementation tasks derived from the design

## Workflow

1. Write `requirements.md` first and get it agreed upon before starting design
2. Write `design.md` based on the agreed requirements
3. Break the design down into `tasks.md` and implement task by task, checking items off as they land

See `specs/example-story/` for a filled-out template.
