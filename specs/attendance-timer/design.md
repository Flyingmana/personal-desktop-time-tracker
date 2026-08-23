# Design: Attendance Timer

## Approach

- Reuse the existing `AttendanceEntry`, `startAttendance`, `stopEntry`, and
  `runningAttendanceEntry` domain APIs. `startAttendance` already guarantees that exactly one
  attendance interval can run at a time.
- Render a dedicated `AttendanceTimerSection` above the regular task-timer controls so the
  attendance state is visibly distinct.
- When no attendance interval is running, show `Start work day` if no previous interval exists,
  or `Continue attendance` when the latest interval has finished. Both actions create a new
  `AttendanceEntry` at the current time.
- When attendance is running, show its start time, current live elapsed minutes, and a `Stop
  attendance` action. Stopping uses the existing `stopEntry` operation and retains the finished
  interval in `TrackerData`.
- Calculate today's accumulated attendance with a pure function that sums the portion of every
  attendance interval overlapping the current calendar day, treating an active interval as ending
  at the supplied current time. The section refreshes its supplied current time every 15 seconds
  through the existing timer-list coroutine.

## UI Flow

1. A user starts their work day from the Attendance section.
2. The section replaces the start action with the running interval's start time, live runtime, and
   stop action.
3. Stopping attendance records the end time and exposes a continue action.
4. Continuing creates another interval; today's accumulated runtime includes all completed and
   currently running intervals for that calendar day.

## State and Pure Logic

- `App()` continues to own the single `TrackerData` value and supplies updates and the clock to
  `TimerListScreen`.
- `AttendanceTimerSection` is stateless. It receives data, current time, and action callbacks.
- `attendanceMinutesOn(data, date, currentTime)` is a pure helper that returns non-negative whole
  minutes, including only the part of each attendance interval within the requested day.
- UUID generation and clock reads stay at the Compose boundary, preserving deterministic domain
  operations and pure calculation tests.

## Test Tags

- `attendanceTimerSection`
- `startAttendanceButton`
- `runningAttendanceElapsed`
- `attendanceTodayElapsed`
- `stopAttendanceButton`
- `continueAttendanceButton`
- `attendanceStart`
- `attendanceEnd`

## Edge Cases

- Starting attendance while an interval is already running is prevented by the domain operation;
  the UI renders only a stop action in that state.
- Stopping at the same instant as the start is rejected by the shared transition validation and
  leaves attendance running.
- Completed intervals from previous days are retained but do not contribute to today's displayed
  accumulated runtime.
- The daily-overview story may display the same attendance total separately from task time. This
  story does not define whether a broader total combines the two categories.

## Out of Scope

- Automatic break detection and stop reminders
- Persistence across app restarts
- The daily-overview screen and its aggregation policy