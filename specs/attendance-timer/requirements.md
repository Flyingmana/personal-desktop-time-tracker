# Attendance Timer

## User Story

As a user, I want a separate, special "attendance" timer that represents my overall presence at
work, so that I only stop it for breaks or when I finish the work day, independent of the specific
task timers I start and stop throughout the day.

## Acceptance Criteria

- **Given** the app is open, **when** I look for the attendance timer, **then** it is clearly
  distinguished from regular task timers (e.g. by a dedicated section, icon, or label) and there is
  exactly one attendance timer active at a time.
- **Given** the attendance timer is not running, **when** I start my work day, **then** I can start
  it and its start (date)time is recorded.
- **Given** the attendance timer is running, **when** I take a break, **then** I can stop it,
  recording an end (date)time, and later resume it (e.g. via a "continue"-style action) to record a
  new start (date)time.
- **Given** the attendance timer is running, **when** I finish my work day, **then** I can stop it
  to record the final end (date)time for the day.
- **Given** the attendance timer, **when** it is displayed, **then** its accumulated runtime for the
  day follows the same live-updating behavior (elapsed minutes, refreshed at least every 15
  seconds) as regular timers.
- **Given** the attendance timer, **when** shown in the daily overview, **then** its worked time is
  distinguishable from (but may also contribute to) the day's total worked time (exact aggregation
  behavior to be decided in design).

## Out of scope

- Automatic break detection
- Reminders/notifications to stop the attendance timer
