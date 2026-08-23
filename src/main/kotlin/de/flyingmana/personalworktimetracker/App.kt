package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.delay

private enum class AppTab {
    Timers,
    Reporting,
}

fun elapsedMinutes(start: LocalDateTime, currentTime: LocalDateTime): Long =
    ChronoUnit.MINUTES.between(start, currentTime).coerceAtLeast(0)

@Composable
fun App(clock: () -> LocalDateTime = LocalDateTime::now) {
    var selectedTab by remember { mutableStateOf(AppTab.Timers) }
    var trackerData by remember { mutableStateOf(TrackerData()) }

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
                modifier = Modifier.testTag("reportingTab")
            ) {
                Text("Reporting")
            }
        }

        when (selectedTab) {
            AppTab.Timers -> TimerListScreen(
                data = trackerData,
                onDataChanged = { trackerData = it },
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            currentTime = clock()
        }
    }

    Column {
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
                modifier = Modifier.testTag("startTimerButton")
            ) {
                Text("Start")
            }
        }

        if (taskTimers.isEmpty()) {
            Text(text = "No timers yet", modifier = Modifier.testTag("timerListEmpty"))
        } else {
            LazyColumn {
                items(taskTimers, key = { it.id }) { timer ->
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
    Column(modifier = Modifier.padding(top = 8.dp).testTag("timerRow")) {
        Text("Timer text")
        TextField(
            value = timer.text,
            onValueChange = onTextChanged,
            modifier = Modifier.testTag("timerLabelInput")
        )
        if (timer.end == null) {
            Text("Start: ${timer.start.format(timerDateTimeFormatter)}")
            Text(
                "Elapsed: ${elapsedMinutes(timer.start, currentTime)} min",
                modifier = Modifier.testTag("runningTimerElapsed")
            )
            Button(onClick = onStop, modifier = Modifier.testTag("stopTimerButton")) {
                Text("Stop")
            }
        } else {
            Text(
                "Start: ${timer.start.format(timerDateTimeFormatter)}",
                modifier = Modifier.testTag("finishedTimerStart")
            )
            Text(
                "End: ${timer.end.format(timerDateTimeFormatter)}",
                modifier = Modifier.testTag("finishedTimerEnd")
            )
            Button(onClick = onContinue, modifier = Modifier.testTag("continueTimerButton")) {
                Text("Continue")
            }
        }
    }
}

private val timerDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
