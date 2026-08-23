# Design: Daily Timer Overview

## Approach

- Keep attendance separate in its existing dedicated section. This story groups the regular
  `TaskTimerEntry` list; attendance already exposes its own daily accumulated figure and a future
  daily-overview story can define any combined aggregation.
- Add a pure `taskTimerDayGroups` function that accepts task timers and a supplied current time.
  It groups entries by `start.toLocalDate()`, orders days descending, and calculates each group
  total from completed entries or the current elapsed duration of running entries.
- Replace the flat task `LazyColumn` with a grouped list. Each group emits a day heading that
  includes its total, followed by that day's existing editable task rows.
- Reuse the existing `currentTime` Compose state, refreshed every 15 seconds, so a running entry
  updates both its row and the owning day total in the same recomposition.

## Data Shape

```kotlin
data class TaskTimerDayGroup(
    val date: LocalDate,
    val totalMinutes: Long,
    val timers: List<TaskTimerEntry>,
)
```

- `taskTimerDayGroups(timers, currentTime)` returns an empty list when there are no task timers.
- A finished timer uses `end`; a running timer uses `currentTime`.
- Timers are grouped by their start date even if they continue into a later day, matching the
  story requirement. A timer's full duration belongs to its start-date group.

## UI Structure

- The Attendance section remains first.
- The task input remains below it.
- When no task timers exist, keep the existing `No timers yet` state.
- Otherwise, show one `timerDayGroup` per date with a `timerDayHeading` such as
  `2026-08-23: 1h 30m`, then render the group's task timer rows.

## Test Tags

- `timerDayGroup`
- `timerDayHeading`
- `timerDayTotal`
- `timerRow`

## Edge Cases

- Empty calendar days are absent because groups originate only from task entries.
- Group ordering uses the date, not insertion order.
- A clock earlier than a running timer's start contributes zero minutes, consistent with
  `elapsedMinutes`.
- Attendance entries are intentionally not inserted into task-day groups; they remain visible in
  the Attendance section and are out of scope for the task-timer aggregate.

## Out of Scope

- Weekly or monthly summaries and reporting
- Editing a timer's start day
- Combining attendance and task time into a single daily total