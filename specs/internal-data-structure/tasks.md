# Tasks: Internal Data Structure

- [x] Add `TrackerData.kt` with immutable `TrackerData`, `TimeEntry`, `TaskTimerEntry`,
  `AttendanceEntry`, and `TimerLabel` domain types using UUID identities.
- [x] Add sealed domain result and error types for successful and rejected model operations.
- [x] Implement `startTaskTimer` with blank-text and duplicate-ID validation.
- [x] Implement `startAttendance` with duplicate-ID validation and enforcement of one running
  attendance interval.
- [x] Implement `stopEntry` so it updates only a running entry, retains its identity and immutable
  fields, and rejects missing, finished, and non-increasing end timestamps.
- [x] Implement `createLabel` with ID, name, parent existence, cycle, and maximum-depth validation.
- [x] Implement `assignLabels` for task timer entries, validating the task and every referenced
  label before applying associations atomically.
- [x] Implement read-only queries for running entries, the running attendance entry, entries
  started on a date, finished entries overlapping an inclusive date range, and resolved labels.
- [x] Add `TrackerDataTest` Kotest coverage for timer and attendance creation/stopping, including
  multiple concurrent task timers and rejection cases.
- [x] Add `TrackerDataTest` coverage for label associations and valid/invalid label hierarchies,
  including the three-level depth limit.
- [x] Add `TrackerDataTest` coverage for day lookup, finished-range lookup, running-entry
  exclusion, and intervals crossing midnight.
- [x] Replace `WorkEntry` in `ReportingPage.kt` with the new time-entry model and
  `finishedEntriesOverlapping` query while retaining the existing reporting calculations.
- [x] Update `ReportingPageLogicTest` to build explicit finished/running task entries and verify
  reporting behavior remains unchanged.
- [x] Replace the temporary count state in `App()` with one remembered `TrackerData` value for
  future timer, attendance, label, and persistence features to share.
- [x] Run `./gradlew test` and `./gradlew uiTest`.