package de.flyingmana.personalworktimetracker

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TrackerData(
    val entries: List<TimeEntry> = emptyList(),
    val labels: List<TimerLabel> = emptyList(),
)

sealed interface TimeEntry {
    val id: UUID
    val start: LocalDateTime
    val end: LocalDateTime?
}

data class TaskTimerEntry(
    override val id: UUID,
    val text: String,
    override val start: LocalDateTime,
    override val end: LocalDateTime? = null,
    val labelIds: Set<UUID> = emptySet(),
) : TimeEntry

data class AttendanceEntry(
    override val id: UUID,
    override val start: LocalDateTime,
    override val end: LocalDateTime? = null,
) : TimeEntry

data class TimerLabel(
    val id: UUID,
    val name: String,
    val colorCode: String? = null,
    val parentId: UUID? = null,
)

sealed interface TrackerDataResult {
    data class Success(val data: TrackerData) : TrackerDataResult
    data class Failure(val reason: TrackerDataError) : TrackerDataResult
}

sealed interface TrackerDataError {
    data object BlankTaskText : TrackerDataError
    data object BlankLabelName : TrackerDataError
    data object DuplicateId : TrackerDataError
    data object AttendanceAlreadyRunning : TrackerDataError
    data object EntryNotFound : TrackerDataError
    data object EntryAlreadyFinished : TrackerDataError
    data object InvalidEndTimestamp : TrackerDataError
    data object TaskEntryNotFound : TrackerDataError
    data object LabelNotFound : TrackerDataError
    data object ParentLabelNotFound : TrackerDataError
    data object LabelHierarchyCycle : TrackerDataError
    data object LabelHierarchyTooDeep : TrackerDataError
}

fun startTaskTimer(
    data: TrackerData,
    id: UUID,
    text: String,
    startedAt: LocalDateTime,
): TrackerDataResult {
    if (text.isBlank()) return TrackerDataResult.Failure(TrackerDataError.BlankTaskText)
    if (containsId(data, id)) return TrackerDataResult.Failure(TrackerDataError.DuplicateId)

    return TrackerDataResult.Success(
        data.copy(entries = data.entries + TaskTimerEntry(id, text, startedAt))
    )
}

fun startAttendance(
    data: TrackerData,
    id: UUID,
    startedAt: LocalDateTime,
): TrackerDataResult {
    if (containsId(data, id)) return TrackerDataResult.Failure(TrackerDataError.DuplicateId)
    if (runningAttendanceEntry(data) != null) {
        return TrackerDataResult.Failure(TrackerDataError.AttendanceAlreadyRunning)
    }

    return TrackerDataResult.Success(
        data.copy(entries = data.entries + AttendanceEntry(id, startedAt))
    )
}

fun stopEntry(
    data: TrackerData,
    id: UUID,
    endedAt: LocalDateTime,
): TrackerDataResult {
    val entry = data.entries.firstOrNull { it.id == id }
        ?: return TrackerDataResult.Failure(TrackerDataError.EntryNotFound)
    if (entry.end != null) return TrackerDataResult.Failure(TrackerDataError.EntryAlreadyFinished)
    if (!endedAt.isAfter(entry.start)) {
        return TrackerDataResult.Failure(TrackerDataError.InvalidEndTimestamp)
    }

    val stoppedEntry = when (entry) {
        is TaskTimerEntry -> entry.copy(end = endedAt)
        is AttendanceEntry -> entry.copy(end = endedAt)
    }
    return TrackerDataResult.Success(
        data.copy(entries = data.entries.map { current -> if (current.id == id) stoppedEntry else current })
    )
}

fun createLabel(
    data: TrackerData,
    id: UUID,
    name: String,
    colorCode: String? = null,
    parentId: UUID? = null,
): TrackerDataResult {
    if (name.isBlank()) return TrackerDataResult.Failure(TrackerDataError.BlankLabelName)
    if (containsId(data, id)) return TrackerDataResult.Failure(TrackerDataError.DuplicateId)
    if (parentId != null && data.labels.none { it.id == parentId }) {
        return TrackerDataResult.Failure(TrackerDataError.ParentLabelNotFound)
    }
    if (parentId != null && parentChainHasCycle(data.labels, parentId)) {
        return TrackerDataResult.Failure(TrackerDataError.LabelHierarchyCycle)
    }
    if (parentId != null && labelDepth(data.labels, parentId) >= MAX_LABEL_DEPTH) {
        return TrackerDataResult.Failure(TrackerDataError.LabelHierarchyTooDeep)
    }

    return TrackerDataResult.Success(
        data.copy(labels = data.labels + TimerLabel(id, name, colorCode, parentId))
    )
}

fun assignLabels(
    data: TrackerData,
    taskEntryId: UUID,
    labelIds: Set<UUID>,
): TrackerDataResult {
    val entry = data.entries.firstOrNull { it.id == taskEntryId }
        ?: return TrackerDataResult.Failure(TrackerDataError.TaskEntryNotFound)
    if (entry !is TaskTimerEntry) return TrackerDataResult.Failure(TrackerDataError.TaskEntryNotFound)
    if (labelIds.any { labelId -> data.labels.none { it.id == labelId } }) {
        return TrackerDataResult.Failure(TrackerDataError.LabelNotFound)
    }

    val updatedEntry = entry.copy(labelIds = labelIds)
    return TrackerDataResult.Success(
        data.copy(entries = data.entries.map { current -> if (current.id == taskEntryId) updatedEntry else current })
    )
}

fun runningEntries(data: TrackerData): List<TimeEntry> = data.entries.filter { it.end == null }

fun runningAttendanceEntry(data: TrackerData): AttendanceEntry? =
    data.entries.filterIsInstance<AttendanceEntry>().firstOrNull { it.end == null }

fun entriesStartedOn(data: TrackerData, date: LocalDate): List<TimeEntry> =
    data.entries.filter { it.start.toLocalDate() == date }

fun finishedEntriesOverlapping(
    data: TrackerData,
    startDate: LocalDate,
    endDate: LocalDate,
): List<TimeEntry> = data.entries.filter { entry ->
    val entryEnd = entry.end ?: return@filter false
    !entry.start.toLocalDate().isAfter(endDate) && !entryEnd.toLocalDate().isBefore(startDate)
}

fun labelsFor(entry: TaskTimerEntry, data: TrackerData): List<TimerLabel> =
    data.labels.filter { it.id in entry.labelIds }

private const val MAX_LABEL_DEPTH = 3

private fun containsId(data: TrackerData, id: UUID): Boolean =
    data.entries.any { it.id == id } || data.labels.any { it.id == id }

private fun parentChainHasCycle(labels: List<TimerLabel>, parentId: UUID): Boolean {
    val labelsById = labels.associateBy { it.id }
    val visited = mutableSetOf<UUID>()
    var currentId: UUID? = parentId

    while (currentId != null) {
        if (!visited.add(currentId)) return true
        currentId = labelsById[currentId]?.parentId
    }
    return false
}

private fun labelDepth(labels: List<TimerLabel>, labelId: UUID): Int {
    val labelsById = labels.associateBy { it.id }
    var depth = 0
    var currentId: UUID? = labelId

    while (currentId != null) {
        depth += 1
        currentId = labelsById[currentId]?.parentId
    }
    return depth
}