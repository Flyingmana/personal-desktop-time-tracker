# Reporting Page

## User Story

As a user, I want a dedicated reporting page that lists statistics for a selectable time frame, so
that I can review and analyze my tracked time outside of the day-by-day timer list.

## Acceptance Criteria

- **Given** the app is open, **when** I navigate to the reporting page, **then** I see statistics
  calculated from my recorded timer entries.
- **Given** the reporting page, **when** no time frame has been explicitly chosen yet, **then** the
  default time frame is the current calendar month.
- **Given** a reporting time frame that includes a week, **when** the report is calculated, **then**
  the week is defined as Monday through Sunday.
- **Given** a timer is still running, **when** the report is calculated, **then** it is excluded until
  the timer has been stopped and has an end timestamp recorded.
- **Given** the reporting page, **when** I select a custom start and end date, **then** the
  displayed statistics are recalculated for that time frame.
- **Given** a selected time frame, **when** statistics are displayed, **then** they include at least
  the total worked time for the time frame (further breakdowns, e.g. per label or per day, may be
  added by other stories).
- **Given** a time frame contains no timer entries, **when** viewing the reporting page, **then** the
  displayed totals and day-level values are zero rather than being hidden or shown as misleading
  non-zero values.
- **Given** a time frame spans multiple days, **when** the report is displayed, **then** every day in
  the selected time frame is included in the day breakdown, even if its worked time is zero.

## Out of scope

- Predefined time frame shortcuts (see separate story)
- Specific worktime report content/format (see separate story)
- Exporting or printing reports
- General app navigation layout or tab structure (see separate story)
