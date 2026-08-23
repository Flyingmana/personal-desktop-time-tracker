package de.flyingmana.personalworktimetracker

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import de.flyingmana.personalworktimetracker.persistence.Entries
import de.flyingmana.personalworktimetracker.persistence.TrackerDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.UUID

data class StorageLoadResult(
    val data: TrackerData,
    val skippedPartitions: List<Path> = emptyList(),
)

class SqlDelightTrackerStorage(private val root: Path) {
    private var lastSavedData: TrackerData? = null

    fun load(): StorageLoadResult {
        Files.createDirectories(weeksDirectory)
        val skippedPartitions = mutableListOf<Path>()
        val labels = runCatching(::loadLabels).getOrElse {
            skippedPartitions.add(labelsPath)
            emptyList()
        }
        val entries = Files.list(weeksDirectory).use { paths ->
            paths.filter { WEEK_DATABASE_REGEX.matches(it.fileName.toString()) }
                .toList()
                .sorted()
                .flatMap { path ->
                    runCatching { readEntries(path) }.getOrElse {
                        skippedPartitions.add(path)
                        emptyList()
                    }
                }
        }
        val data = TrackerData(entries = entries, labels = labels)
        lastSavedData = data
        return StorageLoadResult(data, skippedPartitions)
    }

    fun save(data: TrackerData) {
        val previous = lastSavedData ?: TrackerData()
        val previousLabels = previous.labels.associateBy { it.id }
        val previousEntries = previous.entries.associateBy { it.id }

        val changedLabels = data.labels.filter { previousLabels[it.id] != it }
        if (changedLabels.isNotEmpty()) {
            withDatabase(labelsPath) { database ->
                database.transaction {
                    changedLabels.forEach { label ->
                        database.trackerQueries.upsertLabel(
                            label.id.toString(),
                            label.name,
                            label.colorCode,
                            label.parentId?.toString(),
                        )
                    }
                }
            }
        }

        data.entries.filter { previousEntries[it.id] != it }
            .groupBy { weekPath(it.start.toLocalDate()) }
            .forEach { (path, entries) ->
                withDatabase(path) { database ->
                    database.transaction {
                        entries.forEach { entry -> writeEntry(database, entry) }
                    }
                }
            }
        lastSavedData = data
    }

    fun loadEntries(start: LocalDate, end: LocalDate): List<TimeEntry> =
        selectedWeekPaths(start, end).flatMap { path ->
            if (Files.exists(path)) readEntries(path, start, end) else emptyList()
        }

    fun loadEntriesForLabel(labelId: UUID, start: LocalDate, end: LocalDate): List<TaskTimerEntry> =
        selectedWeekPaths(start, end).flatMap { path ->
            if (Files.exists(path)) readEntries(path, start, end, labelId).filterIsInstance<TaskTimerEntry>() else emptyList()
        }

    private fun loadLabels(): List<TimerLabel> = withDatabase(labelsPath) { database ->
        database.trackerQueries.selectAllLabels().executeAsList().map { row ->
            TimerLabel(
                id = UUID.fromString(row.id),
                name = row.name,
                colorCode = row.color_code,
                parentId = row.parent_id?.let(UUID::fromString),
            )
        }
    }

    private fun writeEntry(database: TrackerDatabase, entry: TimeEntry) {
        database.trackerQueries.upsertEntry(
            id = entry.id.toString(),
            entry_type = if (entry is TaskTimerEntry) TASK_ENTRY_TYPE else ATTENDANCE_ENTRY_TYPE,
            text = (entry as? TaskTimerEntry)?.text,
            start_at = entry.start.toString(),
            end_at = entry.end?.toString(),
        )
        if (entry is TaskTimerEntry) {
            database.trackerQueries.deleteEntryLabels(entry.id.toString())
            entry.labelIds.forEach { labelId ->
                database.trackerQueries.insertEntryLabel(entry.id.toString(), labelId.toString())
            }
        }
    }

    private fun readEntries(
        path: Path,
        start: LocalDate? = null,
        end: LocalDate? = null,
        labelId: UUID? = null,
    ): List<TimeEntry> = withDatabase(path) { database ->
        val rows = when {
            labelId != null && start != null && end != null -> database.trackerQueries
                .selectEntriesForLabelInRange(labelId.toString(), rangeStart(start), rangeEnd(end))
                .executeAsList()
            start != null && end != null -> database.trackerQueries
                .selectEntriesInRange(rangeStart(start), rangeEnd(end))
                .executeAsList()
            else -> database.trackerQueries.selectAllEntries().executeAsList()
        }
        rows.map { row -> toTimeEntry(database, row) }
    }

    private fun toTimeEntry(database: TrackerDatabase, row: Entries): TimeEntry {
        val id = UUID.fromString(row.id)
        val start = LocalDateTime.parse(row.start_at)
        val end = row.end_at?.let(LocalDateTime::parse)
        return when (row.entry_type) {
            TASK_ENTRY_TYPE -> TaskTimerEntry(
                id = id,
                text = row.text ?: "",
                start = start,
                end = end,
                labelIds = database.trackerQueries.selectLabelIdsForEntry(row.id).executeAsList()
                    .map(UUID::fromString).toSet(),
            )
            ATTENDANCE_ENTRY_TYPE -> AttendanceEntry(id, start, end)
            else -> error("Unknown persisted entry type: ${row.entry_type}")
        }
    }

    private fun selectedWeekPaths(start: LocalDate, end: LocalDate): List<Path> =
        generateSequence(start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(end) }
            .map(::weekPath)
            .toList()

    private fun rangeStart(date: LocalDate): String = date.atStartOfDay().toString()
    private fun rangeEnd(date: LocalDate): String = date.plusDays(1).atStartOfDay().toString()

    private fun weekPath(date: LocalDate): Path {
        val fields = WeekFields.ISO
        return weeksDirectory.resolve(
            "week-%04d-%02d.db".format(date.get(fields.weekBasedYear()), date.get(fields.weekOfWeekBasedYear()))
        )
    }

    private fun <T> withDatabase(path: Path, block: (TrackerDatabase) -> T): T {
        Files.createDirectories(path.parent)
        val isNew = Files.notExists(path)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${path.toAbsolutePath()}")
        driver.execute(null, "PRAGMA journal_mode=WAL", 0)
        driver.execute(null, "PRAGMA synchronous=FULL", 0)
        if (isNew) TrackerDatabase.Schema.create(driver)
        val database = TrackerDatabase(driver)
        return try {
            block(database)
        } finally {
            driver.close()
        }
    }

    private val labelsPath: Path get() = root.resolve("labels.db")
    private val weeksDirectory: Path get() = root.resolve("weeks")

    private companion object {
        const val TASK_ENTRY_TYPE = "task"
        const val ATTENDANCE_ENTRY_TYPE = "attendance"
        val WEEK_DATABASE_REGEX = Regex("week-\\d{4}-\\d{2}\\.db")
    }
}