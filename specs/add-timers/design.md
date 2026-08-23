# Design: Add Timers

## Approach

- Keep `TrackerData` as the single app-level state value. The existing `startTaskTimer` and
  `stopEntry` domain operations remain responsible for creating and completing task entries.
- `App()` supplies the current `LocalDateTime` and generated UUIDs at the UI boundary. The domain
  model remains deterministic and does not read the clock.
- Add a focused `TimerListScreen` with an optional text input and `Start` button. Empty input
  starts an unnamed timer.
- Render each `TaskTimerEntry` in its own list row. A running row displays text, start time,
  live elapsed whole minutes, and a `Stop` button. A finished row displays text, start time, end
  time, and a `Continue` button.
- `Continue` invokes `startTaskTimer` with the finished entry's text, creating a new running
  entry rather than modifying the original.
- Add a pure `updateTaskTimerText` domain operation. It updates only a task timer's text and is
  valid for both running and finished timers.
- Hold a current-time Compose state in the timer list and update it every 15 seconds with a
  coroutine. The elapsed-duration calculation itself is a pure function, allowing unit tests to
  cover its rounding behavior without Compose.

## UI Flow

1. The user optionally enters task text and selects `Start`.
2. The screen starts a `TaskTimerEntry` at the current time, clears the input, and renders its
   running row.
3. While the entry runs, the refresh state causes its elapsed minutes to update every 15 seconds.
4. Selecting `Stop` completes that entry at the current time and swaps its live runtime and stop
   control for its fixed end time and continue control.
5. Selecting `Continue` creates another running entry with the same text and a new current start
   time.
6. Changing a timer row's text field immediately updates that timer, whether it is running or
  finished.

## State and Pure Logic

- `App()` owns `TrackerData` and passes an update callback to `TimerListScreen`.
- `TimerListScreen` owns only transient input text and the periodic current-time state.
- `elapsedMinutes(start, currentTime)` returns the non-negative number of complete minutes
  between timestamps. Finished entries do not call this function for display.
- The text-update operation preserves a timer's ID, timestamps, and assigned category labels.

## Test Tags

- `timerTextInput`
- `startTimerButton`
- `timerListEmpty`
- `timerRow`
- `timerLabelInput`
- `runningTimerElapsed`
- `stopTimerButton`
- `finishedTimerStart`
- `finishedTimerEnd`
- `continueTimerButton`

## Edge Cases

- Whitespace-only input creates an unnamed/blank-text timer and is allowed in timer-row editing.
- Multiple running task timers are supported because the domain model imposes no single-task
  restriction.
- A timer stopped immediately may be rejected by the existing model's strictly-later end-time
  rule; real UI clock values normally satisfy this, and the UI keeps the running entry otherwise.
- The story does not persist entries; closing the app discards the in-memory tracker state.

## Out of Scope

- Deleting entries or editing their start/end times
- Labels, grouping entries by day, and persistence
- Attendance timer controls