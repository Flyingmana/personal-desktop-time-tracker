# Design: Data Persistence

## Storage Format

- Use SQLDelight with its SQLite JDBC driver. SQLDelight compiles the documented SQL schema and
  queries into Kotlin APIs, while SQLite provides transactional individual-row updates and indexes.
- Store data below `~/.personal-worktime-tracker/` by default. Tests supply a temporary directory.
- `labels.db` stores the global label catalog in a `labels` table.
- Each ISO calendar week has a separate `weeks/YYYY-Www.db` database. Each contains:
  - `entries`: ID, entry type, optional task text, start timestamp, optional end timestamp
  - `entry_labels`: task-entry/label associations
  - indexes on start timestamp and label ID
- An entry belongs to the partition for its start timestamp's ISO week. Updating that entry writes
  only its owning weekly database. Labels are upserted only in `labels.db`.

## SQLDelight Schema and Write Strategy

- A `TrackerDatabase` SQLDelight schema defines `labels`, `entries`, and `entry_labels`, including
  indexes for entry start time and label association. SQLDelight generates `TrackerDatabase` and
  its typed query APIs at build time.
- `SqlDelightTrackerStorage.save(data)` keeps the last successfully stored `TrackerData` snapshot and
  detects changed entries and labels by ID/value equality.
- A save calls generated UPSERT queries only for changed entities, in short SQLDelight
  transactions. It never rewrites a
  whole dataset or unrelated weekly partitions.
- For an updated task entry, its label links are replaced transactionally within its own weekly
  database; unrelated entries remain untouched.
- Every SQLDelight JDBC driver connection enables WAL mode and full synchronous commits. SQLite
  transactions leave either the previous valid state or the complete new state after interruption.

## Loading and Queries

- `load()` reads labels and valid weekly partitions, then restores a `TrackerData` value at app
  startup.
- A malformed, unreadable, or corrupted partition is skipped independently; valid partitions and
  labels still load. The adapter exposes skipped database paths to callers through a recovery
  result for future user-facing diagnostics.
- `loadEntries(start, end)` identifies only ISO-week files overlapping the requested range and
  calls SQLDelight's indexed start-time query, avoiding a full in-memory or full-file scan.
- `loadEntriesForLabel(labelId, start, end)` calls SQLDelight's generated join query to return only
  matching entries from those partitions.

## Application Integration

- `Main.kt` creates the storage adapter, loads persisted data before opening the Compose window,
  and passes both the initial data and a persistence callback to `App()`.
- `App()` persists every successful `TrackerData` transition immediately after updating Compose
  state. The existing 15-second visual refresh does not mutate data and therefore does not write.
- The current app has no label-management UI yet; when it is added, its existing domain operation
  will flow through the same callback and be persisted immediately.

## Recovery and Limits

- A corrupt weekly database may lose only that week; all other weeks remain loadable.
- No schema migration mechanism is included, matching the story's out-of-scope scope.
- Deletion is not currently a domain operation, so the adapter does not remove persisted rows.

## Out of Scope

- Cloud synchronization, export/import, backups, and schema migration
- A user-visible recovery screen or manual database repair controls