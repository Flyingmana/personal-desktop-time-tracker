# Tasks: Data Persistence

- [x] Add the SQLDelight Gradle plugin and SQLite JDBC driver dependency.
- [x] Define the SQLDelight schema and indexed query files for catalog and ISO-week partitions.
- [x] Persist changed entries and labels via transactional UPSERTs.
- [x] Load valid persisted data while skipping corrupt weekly partitions.
- [x] Add date-range and label query APIs scoped to weekly partitions.
- [x] Load saved data on application startup and save every model transition.
- [x] Add focused storage round-trip, update, query, partition, and recovery tests.
- [x] Run unit, UI, and full Gradle validation.