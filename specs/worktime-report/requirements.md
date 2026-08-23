# Worktime Report

## User Story

As a user, I want a worktime report on the reporting page, so that I can see how much time I
actually worked during the selected time frame, broken down in a way that's useful for tracking
against my expected work hours.

## Acceptance Criteria

- **Given** a selected time frame on the reporting page, **when** the worktime report is displayed,
  **then** it shows the total worked time for that time frame.
- **Given** a selected time frame, **when** the worktime report is displayed, **then** it shows a
  per-day breakdown of worked time within that time frame.
- **Given** the attendance timer is used, **when** the worktime report is calculated, **then** it
  distinguishes attendance time from task-timer time (exact presentation to be decided in design),
  consistent with the attendance timer story.
- **Given** a day within the reported time frame has no recorded time, **when** the report is
  displayed, **then** that day is shown with zero worked time rather than being silently omitted.
- **Given** the worktime report, **when** displayed, **then** it also shows an average worked time
  per day (excluding days with zero worked time, or as decided in design) for the selected time
  frame.

## Out of scope

- Comparison against a target/contracted work schedule
- Overtime calculation
- Exporting the report (e.g. to PDF/CSV)
