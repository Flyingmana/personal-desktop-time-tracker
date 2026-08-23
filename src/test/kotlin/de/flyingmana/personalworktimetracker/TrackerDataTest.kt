package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TrackerDataTest : StringSpec({
    "task timers retain their identity and can run concurrently" {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val first = startTaskTimer(TrackerData(), firstId, "First task", start).successData()
        val data = startTaskTimer(first, secondId, "Second task", start.plusMinutes(5)).successData()

        data.entries shouldBe listOf(
            TaskTimerEntry(firstId, "First task", start),
            TaskTimerEntry(secondId, "Second task", start.plusMinutes(5)),
        )
        runningEntries(data).map { it.id } shouldBe listOf(firstId, secondId)
    }

    "starting a task timer allows blank text but rejects duplicate identities" {
        val id = UUID.randomUUID()
        val blankTextId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val data = startTaskTimer(TrackerData(), id, "Task", start).successData()

        startTaskTimer(data, blankTextId, "  ", start).successData().entries.last() shouldBe
            TaskTimerEntry(blankTextId, "", start)
        startTaskTimer(data, id, "Another task", start) shouldBe
            TrackerDataResult.Failure(TrackerDataError.DuplicateId)
    }

    "task timer text can change while running or finished without changing timestamps" {
        val runningId = UUID.randomUUID()
        val finishedId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val runningData = startTaskTimer(TrackerData(), runningId, "Draft", start).successData()
        val data = startTaskTimer(runningData, finishedId, "Review", start.plusHours(1)).successData()
        val finishedData = stopEntry(data, finishedId, start.plusHours(2)).successData()

        val renamedRunning = updateTaskTimerText(finishedData, runningId, "Plan").successData()
        val renamedFinished = updateTaskTimerText(renamedRunning, finishedId, "Review notes").successData()

        renamedFinished.entries shouldBe listOf(
            TaskTimerEntry(runningId, "Plan", start),
            TaskTimerEntry(finishedId, "Review notes", start.plusHours(1), start.plusHours(2)),
        )
    }

    "stopping an entry retains its immutable fields and validates transitions" {
        val id = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val data = startTaskTimer(TrackerData(), id, "Task", start).successData()
        val stopped = stopEntry(data, id, start.plusMinutes(45)).successData()

        stopped.entries.single() shouldBe TaskTimerEntry(id, "Task", start, start.plusMinutes(45))
        stopEntry(data, UUID.randomUUID(), start.plusMinutes(45)) shouldBe
            TrackerDataResult.Failure(TrackerDataError.EntryNotFound)
        stopEntry(data, id, start) shouldBe TrackerDataResult.Failure(TrackerDataError.InvalidEndTimestamp)
        stopEntry(stopped, id, start.plusHours(1)) shouldBe
            TrackerDataResult.Failure(TrackerDataError.EntryAlreadyFinished)
    }

    "attendance permits only one running interval and can resume after stopping" {
        val firstId = UUID.randomUUID()
        val secondId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val running = startAttendance(TrackerData(), firstId, start).successData()

        startAttendance(running, secondId, start.plusMinutes(5)) shouldBe
            TrackerDataResult.Failure(TrackerDataError.AttendanceAlreadyRunning)

        val stopped = stopEntry(running, firstId, start.plusHours(1)).successData()
        val resumed = startAttendance(stopped, secondId, start.plusHours(2)).successData()

        resumed.entries shouldBe listOf(
            AttendanceEntry(firstId, start, start.plusHours(1)),
            AttendanceEntry(secondId, start.plusHours(2)),
        )
        runningAttendanceEntry(resumed)?.id shouldBe secondId
    }

    "labels are assigned by identity and support a hierarchy of three levels" {
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val grandchildId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val root = createLabel(TrackerData(), rootId, "Client", "#112233").successData()
        val child = createLabel(root, childId, "Project", parentId = rootId).successData()
        val labels = createLabel(child, grandchildId, "Task", parentId = childId).successData()
        val taskData = startTaskTimer(labels, taskId, "Work", start).successData()
        val assigned = assignLabels(taskData, taskId, setOf(rootId, grandchildId)).successData()
        val task = assigned.entries.single() as TaskTimerEntry

        task.labelIds shouldBe setOf(rootId, grandchildId)
        labelsFor(task, assigned).map { it.id } shouldBe listOf(rootId, grandchildId)
        createLabel(assigned, UUID.randomUUID(), "Too deep", parentId = grandchildId) shouldBe
            TrackerDataResult.Failure(TrackerDataError.LabelHierarchyTooDeep)
    }

    "task timers can start with multiple existing labels" {
        val firstLabelId = UUID.randomUUID()
        val secondLabelId = UUID.randomUUID()
        val timerId = UUID.randomUUID()
        val data = createLabel(TrackerData(), firstLabelId, "Client").successData()
        val labels = createLabel(data, secondLabelId, "Project", "#12ab34").successData()

        startTaskTimer(
            labels,
            timerId,
            "Work",
            LocalDateTime.of(2026, 8, 23, 9, 0),
            setOf(firstLabelId, secondLabelId),
        ).successData().entries.single() shouldBe TaskTimerEntry(
            timerId,
            "Work",
            LocalDateTime.of(2026, 8, 23, 9, 0),
            labelIds = setOf(firstLabelId, secondLabelId),
        )
        startTaskTimer(labels, UUID.randomUUID(), "Invalid", LocalDateTime.now(), setOf(UUID.randomUUID())) shouldBe
            TrackerDataResult.Failure(TrackerDataError.LabelNotFound)
    }

    "labels validate colors and can only be deleted when unreferenced leaves" {
        val parentId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val timerId = UUID.randomUUID()
        val root = createLabel(TrackerData(), parentId, "Client", "#123ABC").successData()
        val nested = createLabel(root, childId, "Project", parentId = parentId).successData()
        val assigned = startTaskTimer(
            nested,
            timerId,
            "Work",
            LocalDateTime.of(2026, 8, 23, 9, 0),
            setOf(childId),
        ).successData()

        createLabel(TrackerData(), UUID.randomUUID(), "Invalid", "blue") shouldBe
            TrackerDataResult.Failure(TrackerDataError.InvalidLabelColorCode)
        deleteLabel(nested, parentId) shouldBe TrackerDataResult.Failure(TrackerDataError.LabelHasChildren)
        deleteLabel(assigned, childId) shouldBe TrackerDataResult.Failure(TrackerDataError.LabelIsAssigned)
        deleteLabel(root, parentId).successData().labels shouldBe emptyList()
    }

    "label colors can be changed or cleared without changing their hierarchy" {
        val labelId = UUID.randomUUID()
        val label = TimerLabel(labelId, "Client", "#123456")

        updateLabelColor(TrackerData(labels = listOf(label)), labelId, "#abcdef").successData().labels.single() shouldBe
            label.copy(colorCode = "#abcdef")
        updateLabelColor(TrackerData(labels = listOf(label)), labelId, null).successData().labels.single() shouldBe
            label.copy(colorCode = null)
        updateLabelColor(TrackerData(labels = listOf(label)), labelId, "invalid") shouldBe
            TrackerDataResult.Failure(TrackerDataError.InvalidLabelColorCode)
    }

    "label mutations reject invalid input without changing the model" {
        val parentId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val taskData = startTaskTimer(
            TrackerData(),
            taskId,
            "Work",
            LocalDateTime.of(2026, 8, 23, 9, 0),
        ).successData()

        createLabel(taskData, UUID.randomUUID(), " ") shouldBe
            TrackerDataResult.Failure(TrackerDataError.BlankLabelName)
        createLabel(taskData, UUID.randomUUID(), "Child", parentId = parentId) shouldBe
            TrackerDataResult.Failure(TrackerDataError.ParentLabelNotFound)
        assignLabels(taskData, taskId, setOf(UUID.randomUUID())) shouldBe
            TrackerDataResult.Failure(TrackerDataError.LabelNotFound)
        assignLabels(taskData, UUID.randomUUID(), emptySet()) shouldBe
            TrackerDataResult.Failure(TrackerDataError.TaskEntryNotFound)
    }

    "queries select entries by start day and finished interval overlap" {
        val august23 = LocalDateTime.of(2026, 8, 23, 23, 30)
        val crossingId = UUID.randomUUID()
        val runningId = UUID.randomUUID()
        val attendanceId = UUID.randomUUID()
        val data = TrackerData(
            entries = listOf(
                TaskTimerEntry(crossingId, "Late task", august23, august23.plusHours(2)),
                TaskTimerEntry(runningId, "Running task", august23.plusHours(1)),
                AttendanceEntry(attendanceId, august23.minusHours(1), august23.plusMinutes(15)),
            ),
        )

        entriesStartedOn(data, LocalDate.of(2026, 8, 23)).map { it.id } shouldBe
            listOf(crossingId, attendanceId)
        finishedEntriesOverlapping(data, LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 24)).map { it.id } shouldBe
            listOf(crossingId)
        finishedEntriesOverlapping(data, LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23)).map { it.id } shouldBe
            listOf(crossingId, attendanceId)
    }
})

private fun TrackerDataResult.successData(): TrackerData =
    (this as? TrackerDataResult.Success)?.data ?: error("Expected a successful tracker data operation")