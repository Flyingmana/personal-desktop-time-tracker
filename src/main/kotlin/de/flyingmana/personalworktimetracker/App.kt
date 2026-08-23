package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

private enum class AppTab {
    Timers,
    Reporting,
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
        }

        when (selectedTab) {
            AppTab.Timers -> TimerListScreen(
                data = trackerData,
                onDataChanged = updateTrackerData,
                clock = clock,
            )
            AppTab.Reporting -> ReportingPageScreen(data = trackerData)
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
                    (startTaskTimer(data, UUID.randomUUID(), timerText, clock()) as? TrackerDataResult.Success)
                        ?.let { result ->
                            onDataChanged(result.data)
                            timerText = ""
                        }
                },
                modifier = Modifier.padding(start = 8.dp).testTag("startTimerButton")
            ) {
                Text("Start")
            }
        }

        if (taskTimers.isEmpty()) {
            Text(text = "No timers yet", modifier = Modifier.testTag("timerListEmpty"))
        } else {
            TimerListColumnHeaders()
            LazyColumn {
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
                                currentTime = currentTime,
                                onStop = {
                                    (stopEntry(data, timer.id, clock()) as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                                onContinue = {
                                    (startTaskTimer(data, UUID.randomUUID(), timer.text, clock())
                                        as? TrackerDataResult.Success)
                                        ?.let { onDataChanged(it.data) }
                                },
                                onTextChanged = { text ->
                                    (updateTaskTimerText(data, timer.id, text) as? TrackerDataResult.Success)
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
        }
    }
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
    currentTime: LocalDateTime,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onTextChanged: (String) -> Unit,
) {
    Row(modifier = Modifier.padding(vertical = 6.dp).testTag("timerRow")) {
        TextField(
            value = timer.text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f).testTag("timerLabelInput")
        )
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
