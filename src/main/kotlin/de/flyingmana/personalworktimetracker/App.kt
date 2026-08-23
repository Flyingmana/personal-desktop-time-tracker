package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.delay

private enum class AppTab {
    Timers,
    Reporting,
    Labels,
}

fun elapsedMinutes(start: LocalDateTime, currentTime: LocalDateTime): Long =
    ChronoUnit.MINUTES.between(start, currentTime).coerceAtLeast(0)

fun attendanceMinutesOn(
    data: TrackerData,
    date: LocalDate,
    currentTime: LocalDateTime,
): Long {
    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()

    return data.entries.filterIsInstance<AttendanceEntry>().sumOf { entry ->
        val intervalEnd = entry.end ?: currentTime
        val overlapStart = maxOf(entry.start, dayStart)
        val overlapEnd = minOf(intervalEnd, dayEnd)
        if (overlapEnd.isAfter(overlapStart)) {
            ChronoUnit.MINUTES.between(overlapStart, overlapEnd)
        } else {
            0
        }
    }
}

data class TaskTimerDayGroup(
    val date: LocalDate,
    val totalMinutes: Long,
    val timers: List<TaskTimerEntry>,
)

fun taskTimerDayGroups(
    timers: List<TaskTimerEntry>,
    currentTime: LocalDateTime,
): List<TaskTimerDayGroup> = timers.groupBy { it.start.toLocalDate() }
    .map { (date, dayTimers) ->
        val sortedTimers = dayTimers.sortedWith(
            compareBy<TaskTimerEntry> { it.end != null }.thenByDescending { it.start }
        )
        TaskTimerDayGroup(
            date = date,
            totalMinutes = sortedTimers.sumOf { timer ->
                elapsedMinutes(timer.start, timer.end ?: currentTime)
            },
            timers = sortedTimers,
        )
    }
    .sortedWith(
        compareBy<TaskTimerDayGroup> { group -> group.timers.none { it.end == null } }
            .thenByDescending { it.date }
    )

@Composable
fun App(
    initialData: TrackerData = TrackerData(),
    onDataChanged: (TrackerData) -> Unit = {},
    clock: () -> LocalDateTime = LocalDateTime::now,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Timers) }
    var trackerData by remember { mutableStateOf(initialData) }
    val updateTrackerData: (TrackerData) -> Unit = { updatedData ->
        trackerData = updatedData
        onDataChanged(updatedData)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.testTag("appTabs")) {
            Button(
                onClick = { selectedTab = AppTab.Timers },
                modifier = Modifier.testTag("timersTab")
            ) {
                Text("Timers")
            }
            Button(
                onClick = { selectedTab = AppTab.Reporting },
                modifier = Modifier.padding(start = 8.dp).testTag("reportingTab")
            ) {
                Text("Reporting")
            }
            Button(
                onClick = { selectedTab = AppTab.Labels },
                modifier = Modifier.padding(start = 8.dp).testTag("labelsTab")
            ) {
                Text("Labels")
            }
        }

        when (selectedTab) {
            AppTab.Timers -> TimerListScreen(
                data = trackerData,
                onDataChanged = updateTrackerData,
                clock = clock,
            )
            AppTab.Reporting -> ReportingPageScreen(data = trackerData)
            AppTab.Labels -> LabelManagementScreen(data = trackerData, onDataChanged = updateTrackerData)
        }
    }
}

@Composable
private fun TimerListScreen(
    data: TrackerData,
    onDataChanged: (TrackerData) -> Unit,
    clock: () -> LocalDateTime,
) {
    var timerText by remember { mutableStateOf("") }
    var currentTime by remember { mutableStateOf(clock()) }
    var newTimerLabelIds by remember { mutableStateOf(emptySet<UUID>()) }
    var isNewTimerLabelPickerOpen by remember { mutableStateOf(false) }
    val taskTimers = data.entries.filterIsInstance<TaskTimerEntry>()
    val timerGroups = taskTimerDayGroups(taskTimers, currentTime)

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            currentTime = clock()
        }
    }

    Column {
        AttendanceTimerSection(
            data = data,
            currentTime = currentTime,
            onStartAttendance = {
                (startAttendance(data, UUID.randomUUID(), clock()) as? TrackerDataResult.Success)
                    ?.let { onDataChanged(it.data) }
            },
            onStopAttendance = { attendanceId ->
                (stopEntry(data, attendanceId, clock()) as? TrackerDataResult.Success)
                    ?.let { onDataChanged(it.data) }
            },
        )

        Row {
            TextField(
                value = timerText,
                onValueChange = { timerText = it },
                label = { Text("Timer text") },
                modifier = Modifier.testTag("timerTextInput")
            )
            Button(
                onClick = {
                    (startTaskTimer(data, UUID.randomUUID(), timerText, clock(), newTimerLabelIds)
                        as? TrackerDataResult.Success)
                        ?.let { result ->
                            onDataChanged(result.data)
                            timerText = ""
                            newTimerLabelIds = emptySet()
                        }
                },
                modifier = Modifier.padding(start = 8.dp).testTag("startTimerButton")
            ) {
                Text("Start")
            }
            OutlinedButton(
                onClick = { isNewTimerLabelPickerOpen = true },
                modifier = Modifier.padding(start = 8.dp).testTag("newTimerLabelsButton")
            ) {
                Text("Labels (${newTimerLabelIds.size})")
            }
        }

        if (isNewTimerLabelPickerOpen) {
            LabelPickerDialog(
                labels = data.labels,
                initialLabelIds = newTimerLabelIds,
                onDismiss = { isNewTimerLabelPickerOpen = false },
                onApply = { labelIds ->
                    newTimerLabelIds = labelIds
                    isNewTimerLabelPickerOpen = false
                },
            )
        }

        if (taskTimers.isEmpty()) {
            Text(text = "No timers yet", modifier = Modifier.testTag("timerListEmpty"))
        } else {
            TimerListColumnHeaders()
            val timerListState = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = timerListState, modifier = Modifier.fillMaxWidth()) {
                items(timerGroups, key = { it.date }) { group ->
                    Column(modifier = Modifier.testTag("timerDayGroup")) {
                        Row(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                            Spacer(modifier = Modifier.weight(1f))
                            Column(modifier = Modifier.width(timerTimeColumnWidth)) {
                                Text(
                                    group.date.format(timerDayFormatter),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("timerDayHeading")
                                )
                                Text(
                                    formatMinutes(group.totalMinutes),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("timerDayTotal")
                                )
                            }
                            Spacer(modifier = Modifier.width(timerActionColumnWidth))
                        }
                        group.timers.forEachIndexed { index, timer ->
                            TimerRow(
                                timer = timer,
                                labels = data.labels,
                                currentTime = currentTime,
                                onStop = {
                                    (stopEntry(data, timer.id, clock()) as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                                onContinue = {
                                    (startTaskTimer(data, UUID.randomUUID(), timer.text, clock(), timer.labelIds)
                                        as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                                onTextChanged = { text ->
                                    (updateTaskTimerText(data, timer.id, text) as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                                onLabelsChanged = { labelIds ->
                                    (assignLabels(data, timer.id, labelIds) as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                            )
                            if (index < group.timers.lastIndex) {
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                            }
                        }
                    }
                }
            }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(timerListState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun LabelManagementScreen(data: TrackerData, onDataChanged: (TrackerData) -> Unit) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<UUID?>(null) }
    var colorCode by remember { mutableStateOf<String?>(null) }
    var parentMenuExpanded by remember { mutableStateOf(false) }
    var labelPendingDeletion by remember { mutableStateOf<TimerLabel?>(null) }
    var labelPendingColorEdit by remember { mutableStateOf<TimerLabel?>(null) }
    val labelScrollState = rememberScrollState()
    val parentOptions = data.labels.filter { labelDepth(it, data.labels) < MAX_LABEL_HIERARCHY_DEPTH }

    Box(modifier = Modifier.fillMaxHeight().testTag("labelManager")) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(labelScrollState)) {
        Text("Labels", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Label name") },
            modifier = Modifier.testTag("labelNameInput")
        )
        Button(
            onClick = { parentMenuExpanded = true },
            modifier = Modifier.padding(top = 8.dp).testTag("labelParentSelector")
        ) {
            Text(parentId?.let { labelPath(data.labels.first { label -> label.id == it }, data.labels) } ?: "No parent")
        }
        DropdownMenu(expanded = parentMenuExpanded, onDismissRequest = { parentMenuExpanded = false }) {
            DropdownMenuItem(onClick = {
                parentId = null
                parentMenuExpanded = false
            }) {
                Text("No parent")
            }
            parentOptions.forEach { parent ->
                DropdownMenuItem(onClick = {
                    parentId = parent.id
                    parentMenuExpanded = false
                }, modifier = Modifier.testTag("labelParentOption")) {
                    Text(labelPath(parent, data.labels))
                }
            }
        }
        Text("Color", modifier = Modifier.padding(top = 8.dp))
        LabelColorPalette(
            selectedColor = colorCode,
            onSelect = { colorCode = it },
            testTagPrefix = "labelColorSwatch",
            modifier = Modifier.testTag("labelColorSelector")
        )
        Button(
            onClick = {
                (createLabel(data, UUID.randomUUID(), name, colorCode, parentId) as? TrackerDataResult.Success)
                    ?.let {
                        onDataChanged(it.data)
                        name = ""
                        parentId = null
                        colorCode = null
                    }
            },
            modifier = Modifier.padding(top = 8.dp).testTag("createLabelButton")
        ) {
            Text("Create label")
        }

        Column(modifier = Modifier.padding(top = 16.dp, end = 12.dp).testTag("labelTree")) {
            labelsInDisplayOrder(data.labels).forEach { label ->
                val deletionBlockReason = labelDeletionBlockReason(label, data)
                val indentation = ((labelDepth(label, data.labels) - 1) * 16).dp
                Row(modifier = Modifier.padding(start = indentation, top = 4.dp, bottom = 4.dp)) {
                    LabelChip(label = label, labels = data.labels)
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { labelPendingColorEdit = label },
                        modifier = Modifier.testTag("editLabelColorButton")
                    ) {
                        Text("Edit color")
                    }
                    Button(
                        onClick = { labelPendingDeletion = label },
                        enabled = deletionBlockReason == null,
                        modifier = Modifier.padding(start = 8.dp).testTag("labelDeleteButton")
                    ) {
                        Text("Delete")
                    }
                    deletionBlockReason?.let { Text(it, modifier = Modifier.padding(start = 8.dp)) }
                }
            }
        }
    }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(labelScrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }

    labelPendingColorEdit?.let { label ->
        LabelColorDialog(
            label = label,
            onDismiss = { labelPendingColorEdit = null },
            onApply = { updatedColorCode ->
                (updateLabelColor(data, label.id, updatedColorCode) as? TrackerDataResult.Success)
                    ?.let { onDataChanged(it.data) }
                labelPendingColorEdit = null
            },
        )
    }

    labelPendingDeletion?.let { label ->
        AlertDialog(
            onDismissRequest = { labelPendingDeletion = null },
            title = { Text("Delete ${label.name}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    (deleteLabel(data, label.id) as? TrackerDataResult.Success)?.let { onDataChanged(it.data) }
                    labelPendingDeletion = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { labelPendingDeletion = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LabelColorDialog(
    label: TimerLabel,
    onDismiss: () -> Unit,
    onApply: (String?) -> Unit,
) {
    var colorCode by remember(label.id, label.colorCode) { mutableStateOf(label.colorCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${label.name} color") },
        text = {
            LabelColorPalette(
                selectedColor = colorCode,
                onSelect = { colorCode = it },
                testTagPrefix = "labelEditColorSwatch",
            )
        },
        confirmButton = {
            TextButton(onClick = { onApply(colorCode) }, modifier = Modifier.testTag("saveLabelColorButton")) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LabelPickerDialog(
    labels: List<TimerLabel>,
    initialLabelIds: Set<UUID>,
    onDismiss: () -> Unit,
    onApply: (Set<UUID>) -> Unit,
) {
    var selectedLabelIds by remember(initialLabelIds) { mutableStateOf(initialLabelIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign labels") },
        text = {
            Column(modifier = Modifier.testTag("labelPicker")) {
                if (labels.isEmpty()) {
                    Text("No labels available")
                } else {
                    labelsInDisplayOrder(labels).forEach { label ->
                        Row {
                            Checkbox(
                                checked = label.id in selectedLabelIds,
                                onCheckedChange = { isSelected ->
                                    selectedLabelIds = if (isSelected) selectedLabelIds + label.id else selectedLabelIds - label.id
                                },
                                modifier = Modifier.testTag("labelPickerOption"),
                            )
                            Text(labelPath(label, labels))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedLabelIds) }, modifier = Modifier.testTag("applyLabelsButton")) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimerListColumnHeaders() {
    Row(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text("Task", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text("Time", modifier = Modifier.width(timerTimeColumnWidth), fontWeight = FontWeight.SemiBold)
        Text("Action", modifier = Modifier.width(timerActionColumnWidth), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AttendanceTimerSection(
    data: TrackerData,
    currentTime: LocalDateTime,
    onStartAttendance: () -> Unit,
    onStopAttendance: (UUID) -> Unit,
) {
    val runningAttendance = runningAttendanceEntry(data)
    val latestFinishedAttendance = data.entries.filterIsInstance<AttendanceEntry>()
        .filter { it.end != null }
        .maxByOrNull { it.start }

    Column(modifier = Modifier.padding(bottom = 16.dp).testTag("attendanceTimerSection")) {
        Row {
            Text("Attendance: ")
            Text(
                "Today: ${attendanceMinutesOn(data, currentTime.toLocalDate(), currentTime)} min",
                modifier = Modifier.testTag("attendanceTodayElapsed")
            )
        }

        if (runningAttendance != null) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Start: ${runningAttendance.start.format(timerDateTimeFormatter)}",
                        modifier = Modifier.testTag("attendanceStart")
                    )
                    Text(
                        "Elapsed: ${elapsedMinutes(runningAttendance.start, currentTime)} min",
                        modifier = Modifier.testTag("runningAttendanceElapsed")
                    )
                }
                Button(
                    onClick = { onStopAttendance(runningAttendance.id) },
                    modifier = Modifier.padding(start = 8.dp).testTag("stopAttendanceButton")
                ) {
                    Text("Stop attendance")
                }
            }
        } else {
            Row {
                latestFinishedAttendance?.let { attendance ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Start: ${attendance.start.format(timerDateTimeFormatter)}",
                            modifier = Modifier.testTag("attendanceStart")
                        )
                        Text(
                            "End: ${requireNotNull(attendance.end).format(timerDateTimeFormatter)}",
                            modifier = Modifier.testTag("attendanceEnd")
                        )
                    }
                }
                Button(
                    onClick = onStartAttendance,
                    modifier = Modifier.padding(start = 8.dp).testTag(
                        if (latestFinishedAttendance == null) "startAttendanceButton" else "continueAttendanceButton"
                    )
                ) {
                    Text(if (latestFinishedAttendance == null) "Start work day" else "Continue attendance")
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: TaskTimerEntry,
    labels: List<TimerLabel>,
    currentTime: LocalDateTime,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onTextChanged: (String) -> Unit,
    onLabelsChanged: (Set<UUID>) -> Unit,
) {
    var isLabelPickerOpen by remember { mutableStateOf(false) }
    Row(modifier = Modifier.padding(vertical = 6.dp).testTag("timerRow")) {
        Column(modifier = Modifier.weight(1f)) {
            TextField(
                value = timer.text,
                onValueChange = onTextChanged,
                modifier = Modifier.fillMaxWidth().testTag("timerLabelInput")
            )
            Row {
                OutlinedButton(onClick = { isLabelPickerOpen = true }, modifier = Modifier.testTag("timerLabelsButton")) {
                    Text("Labels (${timer.labelIds.size})")
                }
                labelsFor(timer, TrackerData(labels = labels)).forEach { label ->
                    LabelChip(label = label, labels = labels, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        Column(modifier = Modifier.width(timerTimeColumnWidth)) {
            if (timer.end == null) {
                Text("Running", color = runningTimerColor, fontWeight = FontWeight.SemiBold)
                Text("Start: ${timer.start.format(timerTimeFormatter)}")
                Text(
                    "Elapsed: ${elapsedMinutes(timer.start, currentTime)} min",
                    modifier = Modifier.testTag("runningTimerElapsed")
                )
            } else {
                Text("Finished", color = finishedTimerColor)
                Text(
                    "Start: ${timer.start.format(timerTimeFormatter)}",
                    modifier = Modifier.testTag("finishedTimerStart")
                )
                Text(
                    "End: ${timer.end.format(timerTimeFormatter)}",
                    modifier = Modifier.testTag("finishedTimerEnd")
                )
            }
        }
        if (timer.end == null) {
            Button(onClick = onStop, modifier = Modifier.width(timerActionColumnWidth).testTag("stopTimerButton")) {
                Text("Stop")
            }
        } else {
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.width(timerActionColumnWidth).testTag("continueTimerButton")
            ) {
                Text("Continue")
            }
        }
    }

    if (isLabelPickerOpen) {
        LabelPickerDialog(
            labels = labels,
            initialLabelIds = timer.labelIds,
            onDismiss = { isLabelPickerOpen = false },
            onApply = { labelIds ->
                onLabelsChanged(labelIds)
                isLabelPickerOpen = false
            },
        )
    }
}

@Composable
private fun LabelChip(label: TimerLabel, labels: List<TimerLabel>, modifier: Modifier = Modifier) {
    val color = label.colorCode?.let(::labelColor) ?: MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    Surface(color = color.copy(alpha = 0.2f), modifier = modifier.testTag("timerLabelChip")) {
        Text(
            label.name,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatMinutes(totalMinutes: Long): String =
    "${totalMinutes / 60}h ${totalMinutes % 60}m"

private val timerTimeColumnWidth = 160.dp
private val timerActionColumnWidth = 108.dp
private val runningTimerColor = Color(0xFF2E7D32)
private val finishedTimerColor = Color(0xFF666666)
private val timerDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd")
private val timerTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timerDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private const val MAX_LABEL_HIERARCHY_DEPTH = 3
private const val LABEL_COLOR_SWATCHES_PER_ROW = 8

private val labelColorOptions = listOf(
    "#D32F2F", "#C2185B", "#7B1FA2", "#512DA8", "#303F9F", "#1976D2", "#0288D1", "#0097A7",
    "#00796B", "#388E3C", "#689F38", "#AFB42B", "#FBC02D", "#FFA000", "#F57C00", "#E64A19",
    "#5D4037", "#616161", "#455A64", "#EF5350", "#EC407A", "#AB47BC", "#7E57C2", "#5C6BC0",
    "#42A5F5", "#29B6F6", "#26C6DA", "#26A69A", "#66BB6A", "#9CCC65", "#D4E157", "#FFCA28",
)

@Composable
private fun LabelColorPalette(
    selectedColor: String?,
    onSelect: (String?) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { onSelect(null) },
            border = if (selectedColor == null) selectedSwatchBorder else null,
        ) {
            Text("No color")
        }
        labelColorOptions.chunked(LABEL_COLOR_SWATCHES_PER_ROW).forEach { rowColors ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                rowColors.forEach { color ->
                    Button(
                        onClick = { onSelect(color) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = labelColor(color)),
                        border = if (selectedColor == color) selectedSwatchBorder else null,
                        modifier = Modifier.padding(start = 4.dp).testTag("$testTagPrefix-$color")
                    ) {
                        Text(" ")
                    }
                }
            }
        }
    }
}

private val selectedSwatchBorder = BorderStroke(3.dp, Color.Black)

private fun labelColor(colorCode: String): Color = Color(0xFF000000 or colorCode.removePrefix("#").toLong(16))

private fun labelDepth(label: TimerLabel, labels: List<TimerLabel>): Int {
    val labelsById = labels.associateBy { it.id }
    var depth = 1
    var current = label
    while (current.parentId != null) {
        current = labelsById[current.parentId] ?: return depth
        depth += 1
    }
    return depth
}

private fun labelPath(label: TimerLabel, labels: List<TimerLabel>): String {
    val labelsById = labels.associateBy { it.id }
    val names = mutableListOf(label.name)
    var parentId = label.parentId
    while (parentId != null) {
        val parent = labelsById[parentId] ?: break
        names += parent.name
        parentId = parent.parentId
    }
    return names.asReversed().joinToString(" > ")
}

private fun labelsInDisplayOrder(labels: List<TimerLabel>): List<TimerLabel> {
    val labelsByParent = labels.groupBy { it.parentId }
    fun descendants(parentId: UUID?): List<TimerLabel> = labelsByParent[parentId].orEmpty().flatMap { label ->
        listOf(label) + descendants(label.id)
    }
    return descendants(null)
}

private fun labelDeletionBlockReason(label: TimerLabel, data: TrackerData): String? = when {
    data.labels.any { it.parentId == label.id } -> "Has child labels"
    data.entries.filterIsInstance<TaskTimerEntry>().any { label.id in it.labelIds } -> "Assigned to a timer"
    else -> null
}
