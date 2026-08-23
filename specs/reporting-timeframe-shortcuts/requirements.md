# Reporting Time Frame Shortcuts

## User Story

As a user, I want predefined time frame shortcuts on the reporting page, so that I can quickly
switch between common reporting periods without manually picking start and end dates every time.

## Acceptance Criteria

- **Given** the reporting page, **when** I look for time frame options, **then** I see shortcut
  buttons/options for: current week, last week, current month, last month, current quarter, last
  quarter, and current year.
- **Given** I click a time frame shortcut, **when** it is applied, **then** the reporting page's
  statistics update to reflect exactly that time frame (e.g. "current week" spans Monday through
  Sunday of the current week).
- **Given** a shortcut is active, **when** the reporting page is displayed, **then** the currently
  selected shortcut is visually indicated as active/selected.
- **Given** a shortcut is active, **when** I manually change the start or end date, **then** the
  active shortcut is deselected (since the time frame no longer matches a predefined shortcut).
- **Given** the "last quarter"/"current quarter" shortcuts, **when** applied, **then** quarters are
  calendar quarters (Q1: Jan–Mar, Q2: Apr–Jun, Q3: Jul–Sep, Q4: Oct–Dec).

## Out of scope

- Custom user-defined/saved shortcuts
- Shortcuts for ranges other than the ones listed above (e.g. "last 7 days", "year to date")
