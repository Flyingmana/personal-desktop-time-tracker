# Add Timers

## User Story

As a user, I want to start and stop named timers, so that I can track how much time I spend on a
task without manually writing down start and end times.

## Acceptance Criteria

- **Given** the app is open, **when** I enter text into the timer's text input and click "Start",
  **then** a new running timer is created with the current date and time recorded as its start
  time.
- **Given** a timer is running, **when** time passes, **then** its elapsed runtime in minutes is
  displayed and refreshed at least every 15 seconds without requiring user interaction.
- **Given** a timer is running, **when** I click "Stop", **then** the current date and time is
  recorded as its end time, and the timer is stored with its text, start (date)time, and end
  (date)time.
- **Given** a stopped (finished) timer entry, **when** I look at it, **then** it displays its text,
  start (date)time, and end (date)time (no longer a live-updating runtime).
- **Given** a finished timer entry, **when** I click its "Continue" button, **then** a new timer is
  started with the same text and with its start time set to the current date/time.
- **Given** the text input is empty, **when** I try to start a timer, **then** starting is prevented
  or a validation message is shown (exact behavior to be decided in design).

## Out of scope

- Editing a stopped timer's text/start/end time after the fact
- Deleting timer entries
- Labels/tags on timers (see separate story)
- Grouping/listing timers by day (see separate story)
