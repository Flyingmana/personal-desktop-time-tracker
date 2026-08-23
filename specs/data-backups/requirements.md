# Data Backups

## User Story

As a user, I want the app to create backups of my persisted data, so that I can recover my timers,
labels, and attendance data if the primary storage is lost, corrupted, or I make a mistake I want
to undo.

## Acceptance Criteria

- **Given** the app is running, **when** persisted data changes, **then** the app periodically
  creates a backup of the data without requiring manual action by the user.
- **Given** backups accumulate over time, **when** new backups are created, **then** old backups
  are retained up to a reasonable limit (exact retention policy to be decided in design), so disk
  usage doesn't grow unbounded.
- **Given** the primary storage is corrupted or missing at startup, **when** the app detects this,
  **then** it offers to restore from the most recent valid backup instead of starting with empty
  data.
- **Given** a backup exists, **when** inspected, **then** it is stored in a well-defined location
  and format (to be decided in design) separate from the primary storage.

## Out of scope

- Cloud or remote backup destinations
- Manual/on-demand backup or restore triggered by the user
- Data export/import in user-facing formats
