# Concurrent Timers

## User Story

As a user, I want to run multiple timers at the same time, so that I can track overlapping
activities (e.g. a general attendance timer alongside a specific task timer) without one timer
blocking another.

## Acceptance Criteria

- **Given** one timer is already running, **when** I start another timer, **then** both timers run
  and update independently at the same time.
- **Given** multiple timers are running, **when** I view the timer list, **then** each running
  timer's own elapsed runtime is displayed and updated independently (at least every 15 seconds).
- **Given** multiple timers are running, **when** I stop one of them, **then** the other running
  timers are unaffected and continue running.
- **Given** multiple running timers, **when** they are included in a day's total worked time,
  **then** their overlapping runtimes are each counted (no automatic deduplication of overlapping
  time), unless stated otherwise by a future reporting story.

## Out of scope

- Warnings or restrictions when timers overlap
- Merging or deduplicating overlapping time in totals
