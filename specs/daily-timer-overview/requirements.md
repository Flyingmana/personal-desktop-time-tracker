# Daily Timer Overview

## User Story

As a user, I want my timers listed grouped by day with a total time worked per day, so that I can
quickly see how much time I worked on any given day.

## Acceptance Criteria

- **Given** I have timer entries spanning multiple days, **when** I view the timer list, **then**
  entries are grouped under their respective day (based on the timer's start date).
- **Given** a day's group of timers, **when** it is displayed, **then** a sum of the total worked
  time for that day is shown alongside the day heading.
- **Given** a timer is still running, **when** the daily sum is calculated, **then** its
  currently-elapsed time is included in the running total and updates as the timer's runtime
  updates.
- **Given** a day with no timer entries, **when** viewing the list, **then** no group is shown for
  that day (empty days are not displayed).
- **Given** multiple days of entries, **when** viewing the list, **then** days are ordered with the
  most recent day first.

## Out of scope

- Weekly/monthly summaries or reports
- Editing which day a timer belongs to
