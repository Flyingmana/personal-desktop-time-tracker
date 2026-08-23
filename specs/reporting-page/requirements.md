# Reporting Page

## User Story

As a user, I want a dedicated reporting page that lists statistics for a selectable time frame, so
that I can review and analyze my tracked time outside of the day-by-day timer list.

## Acceptance Criteria

- **Given** the app is open, **when** I navigate to the reporting page, **then** I see statistics
  calculated from my recorded timer entries.
- **Given** the reporting page, **when** no time frame has been explicitly chosen yet, **then** a
  sensible default time frame is selected (exact default to be decided in design).
- **Given** the reporting page, **when** I select a custom start and end date, **then** the
  displayed statistics are recalculated for that time frame.
- **Given** a selected time frame, **when** statistics are displayed, **then** they include at least
  the total worked time for the time frame (further breakdowns, e.g. per label or per day, may be
  added by other stories).
- **Given** a time frame with no timer entries, **when** viewing the reporting page, **then** it
  clearly indicates that there is no data instead of showing misleading empty/zero statistics.

## Out of scope

- Predefined time frame shortcuts (see separate story)
- Specific worktime report content/format (see separate story)
- Exporting or printing reports
