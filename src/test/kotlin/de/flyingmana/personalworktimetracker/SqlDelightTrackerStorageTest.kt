package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SqlDelightTrackerStorageTest : StringSpec({
    "storage restores entries and labels across instances" {
        val root = Files.createTempDirectory("tracker-storage")
        val label = TimerLabel(UUID.randomUUID(), "Client", "#123456")
        val task = TaskTimerEntry(
            UUID.randomUUID(), "Implement storage", LocalDateTime.of(2026, 8, 23, 9, 0),
            LocalDateTime.of(2026, 8, 23, 10, 0), setOf(label.id),
        )
        val attendance = AttendanceEntry(
            UUID.randomUUID(), LocalDateTime.of(2026, 8, 23, 8, 0), LocalDateTime.of(2026, 8, 23, 17, 0),
        )
        val data = TrackerData(listOf(task, attendance), listOf(label))

        SqlDelightTrackerStorage(root).save(data)

        SqlDelightTrackerStorage(root).load().data shouldBe data
    }

    "storage updates entries and queries requested weeks and labels" {
        val root = Files.createTempDirectory("tracker-storage")
        val labelId = UUID.randomUUID()
        val first = TaskTimerEntry(
            UUID.randomUUID(), "Earlier", LocalDateTime.of(2026, 8, 16, 9, 0), LocalDateTime.of(2026, 8, 16, 10, 0),
            setOf(labelId),
        )
        val second = TaskTimerEntry(
            UUID.randomUUID(), "Later", LocalDateTime.of(2026, 8, 23, 9, 0), LocalDateTime.of(2026, 8, 23, 10, 0),
        )
        val storage = SqlDelightTrackerStorage(root)
        storage.save(TrackerData(listOf(first, second), listOf(TimerLabel(labelId, "Client"))))
        storage.save(TrackerData(listOf(first.copy(text = "Updated"), second), listOf(TimerLabel(labelId, "Client"))))

        storage.loadEntries(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23)) shouldBe listOf(second)
        storage.loadEntriesForLabel(labelId, LocalDate.of(2026, 8, 16), LocalDate.of(2026, 8, 16)) shouldBe
            listOf(first.copy(text = "Updated"))
    }

    "storage skips corrupt weekly partitions and restores valid weeks" {
        val root = Files.createTempDirectory("tracker-storage")
        val valid = TaskTimerEntry(
            UUID.randomUUID(), "Valid", LocalDateTime.of(2026, 8, 23, 9, 0), LocalDateTime.of(2026, 8, 23, 10, 0),
        )
        SqlDelightTrackerStorage(root).save(TrackerData(entries = listOf(valid)))
        val corrupt = root.resolve("weeks").resolve("week-2026-35.db")
        Files.writeString(corrupt, "not a sqlite database")

        val result = SqlDelightTrackerStorage(root).load()

        result.data.entries shouldBe listOf(valid)
        result.skippedPartitions shouldBe listOf(corrupt)
    }
})