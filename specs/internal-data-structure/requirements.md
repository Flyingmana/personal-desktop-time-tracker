# Internal Data Structure

## User Story

As a user, I want the app to maintain my tracked time and labels in a coherent internal data
model, so that timers, attendance, daily views, and reports can use the same accurate data without
being coupled to how it is stored on disk.

## Acceptance Criteria

- **Given** I create a regular task timer or an attendance timer, **when** its internal entry is
  created, **then** it has a stable unique identity and is distinguishable as either a regular
  timer or attendance time.
- **Given** a regular timer is started, **when** it has not yet been stopped, **then** the model
  records its text and start timestamp and represents it as running without an end timestamp.
- **Given** a running timer is stopped, **when** its entry is updated, **then** the model records
  its end timestamp and represents it as finished while retaining its original identity, text, and
  start timestamp.
- **Given** several regular timers exist, **when** more than one is running, **then** the model
  retains each running timer independently and does not replace or stop another running timer.
- **Given** attendance time is started, stopped, and later resumed, **when** its intervals are
  represented, **then** each attendance interval retains its own start and end timestamps, and the
  model allows at most one attendance interval to be running at a time.
- **Given** labels are associated with a timer, **when** the timer is retrieved, **then** the model
  preserves zero, one, or multiple label associations by stable label identity without embedding or
  duplicating label definitions in the timer entry.
- **Given** a label has a parent label, **when** the label hierarchy is represented, **then** the
  parent relationship is preserved by label identity and can represent the maximum three levels
  required by the timer-labels story.
- **Given** the timer list or daily overview needs entries for a day, **when** it queries the
  model, **then** entries can be selected and grouped by their start date while retaining their
  creation order or another deterministic order for presentation.
- **Given** reporting needs finished entries in a selected date range, **when** it queries the
  model, **then** it can distinguish finished from running entries and select the relevant finished
  entries without changing the underlying records.
- **Given** the application state is changed by starting, stopping, continuing, or labeling a
  timer, **when** another app feature reads the model afterwards, **then** it observes the updated
  consistent state rather than a separate copy of the same domain data.

## Out of Scope

- Files, databases, serialization formats, write scheduling, recovery, and weekly storage
  partitioning (see the data-persistence story)
- Backups and restore workflows (see the data-backups story)
- User interface layout, navigation, and timer interaction controls
- Report calculations, totals, and date-range boundary rules beyond supplying the required records
- Editing or deletion rules for entries and labels unless defined by their respective stories