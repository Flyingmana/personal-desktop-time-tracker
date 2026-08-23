# Timer Labels

## User Story

As a user, I want to assign hierarchical, optionally colored labels to my timers, so that I can
categorize and later filter or report on my tracked time (e.g. by project, client, or task type).

## Acceptance Criteria

- **Given** I am creating or editing a timer, **when** I assign labels, **then** I can assign zero,
  one, or multiple labels to a single timer.
- **Given** the label management UI, **when** I create a label, **then** I can optionally nest it
  under a parent label, up to a maximum hierarchy depth of 3 levels (e.g. `Client > Project >
  Task`).
- **Given** a label, **when** I create or edit it, **then** I can optionally assign it a color code,
  which is used to visually distinguish it wherever the label is displayed.
- **Given** a label has no color assigned, **when** it is displayed, **then** a sensible default
  appearance is used (exact behavior to be decided in design).
- **Given** a label with child labels, **when** I attempt to delete it, **then** the app either
  prevents the deletion or clarifies what happens to its children (exact behavior to be decided in
  design).
- **Given** a timer with assigned labels, **when** it is displayed in a list, **then** its labels
  (and their colors) are shown alongside it.

## Out of scope

- Filtering/reporting timers by label (may be a future story)
- Renaming/reorganizing the label hierarchy after creation (may be a future story)
