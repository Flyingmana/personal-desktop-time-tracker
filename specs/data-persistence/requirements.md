# Data Persistence

## User Story

As a user, I want my timers, labels, and attendance data to be persisted locally in a way that
survives app restarts, so that I don't have to manually save my work and don't lose data if the app
closes unexpectedly.

## Acceptance Criteria

- **Given** I create, start, stop, or edit a timer (or label), **when** the change happens,
  **then** it is written to local storage immediately, without requiring an explicit "Save" action
  by the user.
- **Given** many small changes happen in quick succession (e.g. a running timer's periodic runtime
  update, or several timers started/stopped back-to-back), **when** they are persisted, **then**
  the storage format and write strategy remain resilient and performant under frequent small writes
  (e.g. append-friendly writes rather than rewriting the entire dataset on every change).
- **Given** the app is closed (normally or unexpectedly, e.g. crash or power loss) after a change
  was persisted, **when** the app is reopened, **then** all previously persisted timers, labels,
  and attendance data are restored.
- **Given** the persisted data on disk, **when** it needs to be queried (e.g. for the daily overview
  or reporting page), **then** it can be filtered efficiently (e.g. by date range or label) without
  needing to load and parse the entire dataset into memory first.
- **Given** the local storage format, **when** inspected, **then** it is a well-defined, documented
  format (exact choice, e.g. append-only log, embedded database, or similar, to be decided in
  design) rather than ad-hoc file writes.
- **Given** a corrupted or partially-written storage file (e.g. from an interrupted write),
  **when** the app starts, **then** it recovers as much valid data as possible instead of failing
  to start entirely.
- **Given** a time entry needs a small correction (e.g. fixing a start/end time, label, or other
  detail) after the fact, **when** I edit it, **then** the storage format supports updating
  existing entries flexibly, without requiring a full rewrite of unrelated data.
- **Given** time entries accumulate over time, **when** they are persisted, **then** they are
  partitioned by week (e.g. one storage unit per calendar week), so that a write failure or
  corruption in one partition has minimal impact on data from other weeks.

## Out of scope

- Cloud sync or multi-device persistence
- Data export/import
- Backups (see separate "Data Backups" story)
- Migrating/versioning the storage format across app updates (may be a future story)
