# Tasks: Internal Data Structure

- [ ] Add `TrackerData.kt` with immutable `TrackerData`, `TimeEntry`, `TaskTimerEntry`,
  `AttendanceEntry`, and `TimerLabel` domain types using UUID identities.
- [ ] Add sealed domain result and error types for successful and rejected model operations.
- [ ] Implement `startTaskTimer` with blank-text and duplicate-ID validation.
- [ ] Implement `startAttendance` with duplicate-ID validation and enforcement of one running
  attendance interval.
- [ ] Implement `stopEntry` so it updates only a running entry, retains its identity and immutable
  fields, and rejects missing, finished, and non-increasing end timestamps.
- [ ] Implement `createLabel` with ID, name, parent existence, cycle, and maximum-depth validation.
- [ ] Implement `assignLabels` for task timer entries, validating the task and every referenced
  label before applying associations atomically.
- [ ] Implement read-only queries for running entries, the running attendance entry, entries
  started on a date, finished entries overlapping an inclusive date range, and resolved labels.
- [ ] Add `TrackerDataTest` Kotest coverage for timer and attendance creation/stopping, including
  multiple concurrent task timers and rejection cases.
- [ ] Add `TrackerDataTest` coverage for label associations and valid/invalid label hierarchies,
  including the three-level depth limit.
- [ ] Add `TrackerDataTest` coverage for day lookup, finished-range lookup, running-entry
  exclusion, and intervals crossing midnight.
- [ ] Replace `WorkEntry` in `ReportingPage.kt` with the new time-entry model and
  `finishedEntriesOverlapping` query while retaining the existing reporting calculations.
- [ ] Update `ReportingPageLogicTest` to build explicit finished/running task entries and verify
  reporting behavior remains unchanged.
- [ ] Replace the temporary count state in `App()` with one remembered `TrackerData` value for
  future timer, attendance, label, and persistence features to share.
- [ ] Run `./gradlew test` and `./gradlew uiTest`.