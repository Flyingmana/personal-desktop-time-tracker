# Design: Reporting Page

## Approach

- Keep reporting calculations in pure Kotlin functions so they can be tested without Compose.
- Represent a selected period as an inclusive `LocalDate` start and end pair.
- Build a `ReportingPeriodReport` containing the selected bounds, the total worked minutes, and one
  `DailyTotal` for every calendar day in the period, plus time totals grouped by label.
- Let `ReportingPageScreen` own the editable start and end dates with Compose state and recompute the
  report from the current `TrackerData` whenever either date changes.
- Use the existing reporting-page navigation supplied by the reporting-page UI structure story;
  this story owns the page content and calculations, not the app's tab structure.

## Data Model And Calculation

```kotlin
data class DailyTotal(
    val date: LocalDate,
    val minutes: Int,
)

data class ReportingPeriodReport(
    val start: LocalDate,
    val end: LocalDate,
    val totalMinutes: Int,
    val dailyTotals: List<DailyTotal>,
    val labelTotals: List<LabelTotal>,
)

  data class LabelTotal(
    val labelId: UUID,
    val labelName: String,
    val minutes: Int,
  )
```

- `currentMonthReportingRange(referenceDate)` returns the first and last day of the reference
  month, inclusive. The page uses the current calendar month when no dates were chosen.
- `buildReportingPeriodReport(data, start, end)` creates a zero-valued day entry for every date from
  `start` through `end`, inclusive.
- Only entries with a recorded end timestamp are included. Running entries (`end == null`) are
  ignored entirely.
- A completed entry contributes only the overlap between its interval and the selected period.
  Entries crossing midnight are split across the affected days, and the total is the sum of the
  daily values.
- Label totals use the same clipped completed-entry duration. An entry assigned to multiple labels
  contributes its full duration to each assigned label, so label totals are category totals and may
  exceed the overall total. Unlabelled task and attendance entries are omitted from the label chart.
- Label totals are sorted from most to least time, with label name as a deterministic tie-breaker.
- A label bar uses its stored `TimerLabel.colorCode`; labels without a color, or invalid stored
  colors, use the standard blue fallback.
- Date calculations use `LocalDate`; when a weekly breakdown is introduced, weeks use ISO/Monday
  through Sunday semantics. This story does not add weekly summary rows.
- An invalid period where `end` precedes `start` should be rejected by the UI or domain boundary;
  the implementation must not silently display a misleading report.

## UI Flow

1. The user opens the reporting page and sees the current month selected by default.
2. The page shows editable ISO local-date fields for the start and end dates.
3. A valid date edit updates the selected period and recalculates the report immediately.
4. The page displays the selected period, total worked time, and a scrollable day breakdown.
5. The page shows a horizontal bar diagram for labelled task time, followed by every day in the
  selected period, including days with zero minutes.

Invalid date text remains unchanged until it can be parsed as a valid `LocalDate`; malformed input
must not trigger a partial or stale date range update. Date-range validation should prevent a report
with the end before the start.

## Layout And Test Tags

- Page title: `reportingPageTitle`
- Start date label/input: `reportingStartDateLabel`, `reportingStartDateInput`
- End date label/input: `reportingEndDateLabel`, `reportingEndDateInput`
- Selected period: `reportingPeriodText`
- Total: `reportingTotalText`
- Label chart and rows: `reportingLabelChart`, `reportingLabelRow`, `reportingLabelBar`
- Each daily row: `reportingDayRow`

The day list may use a lazy scroll container with a scrollbar, while the total and date controls stay
visible above it.

## Tests

- Add Kotest coverage for current-month defaults, inclusive ranges, zero-filled days, completed and
  running entries, cross-midnight clipping, Monday-based week boundaries, and label aggregation.
- Add Compose UI coverage for the default period, displayed total/day rows, and recalculation after
  changing both date fields, including proportional label bars.
- Include an empty-data case asserting a zero total and a row for every selected day.

## Out Of Scope

- Predefined period shortcuts
- Detailed worktime-report presentation, averages, attendance/task distinctions, or label hierarchy
  visualization
- Exporting or printing
- Navigation/tab structure and persistence of the selected period