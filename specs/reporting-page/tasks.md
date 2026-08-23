# Tasks: Reporting Page

- [x] Add pure reporting data classes for daily totals and period reports.
- [x] Implement current-calendar-month range selection with inclusive start and end dates.
- [x] Implement period aggregation for completed entries, clipping intervals at day boundaries.
- [x] Exclude running entries until they have an end timestamp.
- [x] Generate zero-valued daily totals for every day in the selected inclusive period.
- [x] Aggregate completed task time by assigned label for the selected period.
- [x] Render a proportional horizontal bar diagram for label totals.
- [x] Add the reporting page date controls with current-month defaults.
- [x] Recalculate and display the total and daily breakdown after valid custom date changes.
- [x] Validate or safely handle an end date before the start date.
- [x] Add stable Compose test tags for date controls, total, period, and daily rows.
- [x] Add Kotest coverage for date ranges, week boundaries, running entries, empty periods, and
  cross-midnight entries.
- [x] Add Compose UI coverage for default rendering and custom-period recalculation.
- [x] Add unit and Compose coverage for label aggregation and bar rendering.
- [x] Run focused unit and UI tests.