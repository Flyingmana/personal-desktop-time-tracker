package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

private const val MINUTES_PER_HOUR = 60

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

data class DailyTotal(
    val date: LocalDate,
    val minutes: Int,
)

data class LabelTotal(
    val labelId: UUID,
    val labelName: String,
    val minutes: Int,
    val colorCode: String?,
)

data class ReportingPeriodReport(
    val start: LocalDate,
    val end: LocalDate,
    val totalMinutes: Int,
    val dailyTotals: List<DailyTotal>,
    val labelTotals: List<LabelTotal>,
)

fun currentMonthReportingRange(referenceDate: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
    val start = referenceDate.withDayOfMonth(1)
    val end = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
    return start to end
}

fun buildReportingPeriodReport(
    data: TrackerData,
    start: LocalDate,
    end: LocalDate,
): ReportingPeriodReport {
    require(!end.isBefore(start)) { "Reporting period end must not precede its start" }

    val allDates = generateSequence(start) { current ->
        if (current.isBefore(end)) current.plusDays(1) else null
    }.toList()

    val totalsByDate = allDates.associateWith { 0 }.toMutableMap()

    finishedEntriesOverlapping(data, start, end).forEach { entry ->
        val actualEnd = requireNotNull(entry.end)
        val selectedRangeStart = maxOf(start, entry.start.toLocalDate())
        val selectedRangeEnd = minOf(end, actualEnd.toLocalDate())

        val impactedDates = generateSequence(selectedRangeStart) { current ->
            if (current.isBefore(selectedRangeEnd)) current.plusDays(1) else null
        }.toList()

        impactedDates.forEach { date ->
            val dayStart = date.atStartOfDay()
            val dayEnd = date.plusDays(1).atStartOfDay()
            val overlapStart = maxOf(entry.start, dayStart)
            val overlapEnd = minOf(actualEnd, dayEnd)
            if (overlapEnd.isAfter(overlapStart)) {
                val minutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd).toInt()
                totalsByDate[date] = (totalsByDate[date] ?: 0) + minutes
            }
        }
    }

    val dailyTotals = allDates.map { date -> DailyTotal(date, totalsByDate[date] ?: 0) }
    val totalMinutes = dailyTotals.sumOf { it.minutes }
    val labelTotals = data.entries
        .filterIsInstance<TaskTimerEntry>()
        .filter { it.end != null && it.labelIds.isNotEmpty() }
        .mapNotNull { entry ->
            val actualEnd = requireNotNull(entry.end)
            val overlapStart = maxOf(entry.start, start.atStartOfDay())
            val overlapEnd = minOf(actualEnd, end.plusDays(1).atStartOfDay())
            if (!overlapEnd.isAfter(overlapStart)) return@mapNotNull null
            val minutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd).toInt()
            entry.labelIds.map { labelId -> labelId to minutes }
        }
        .flatten()
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (labelId, minutes) ->
            data.labels.firstOrNull { it.id == labelId }?.let { label ->
                LabelTotal(label.id, label.name, minutes.sum(), label.colorCode)
            }
        }
        .sortedWith(compareByDescending<LabelTotal> { it.minutes }.thenBy { it.labelName })

    return ReportingPeriodReport(
        start = start,
        end = end,
        totalMinutes = totalMinutes,
        dailyTotals = dailyTotals,
        labelTotals = labelTotals,
    )
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return "${hours}h ${minutes}m"
}

private fun labelBarColor(colorCode: String?): Color {
    val digits = colorCode?.removePrefix("#")
    val colorValue = when (digits?.length) {
        6 -> runCatching { ("FF$digits").toLong(16) }.getOrNull()
        8 -> runCatching { digits.toLong(16) }.getOrNull()
        else -> null
    }
    return colorValue?.let { Color(it) } ?: Color(0xFF1976D2)
}

fun currentMonthSampleData(): TrackerData = TrackerData(
    entries = listOf(
        TaskTimerEntry(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Sample task",
            LocalDateTime.of(2026, 8, 1, 9, 0),
            LocalDateTime.of(2026, 8, 1, 10, 30),
        ),
        TaskTimerEntry(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "Sample task",
            LocalDateTime.of(2026, 8, 4, 14, 0),
            LocalDateTime.of(2026, 8, 4, 15, 0),
        ),
        TaskTimerEntry(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "Sample task",
            LocalDateTime.of(2026, 8, 10, 8, 30),
            LocalDateTime.of(2026, 8, 10, 11, 30),
        ),
    ),
)

@Composable
fun ReportingPageScreen(
    data: TrackerData = currentMonthSampleData(),
    initialStart: LocalDate = currentMonthReportingRange().first,
    initialEnd: LocalDate = currentMonthReportingRange().second,
) {
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }

    val report = buildReportingPeriodReport(data, startDate, endDate)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Reporting", modifier = Modifier.testTag("reportingPageTitle"))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start date", modifier = Modifier.testTag("reportingStartDateLabel"))
                TextField(
                    value = startDate.format(dateFormatter),
                    onValueChange = { value ->
                        runCatching { LocalDate.parse(value, dateFormatter) }
                            .onSuccess { startDate = it }
                    },
                    label = { Text("Start date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reportingStartDateInput")
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("End date", modifier = Modifier.testTag("reportingEndDateLabel"))
                TextField(
                    value = endDate.format(dateFormatter),
                    onValueChange = { value ->
                        runCatching { LocalDate.parse(value, dateFormatter) }
                            .onSuccess { endDate = it }
                    },
                    label = { Text("End date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reportingEndDateInput")
                )
            }
        }

        Text(
            "Total worked time: ${formatMinutes(report.totalMinutes)}",
            modifier = Modifier.testTag("reportingTotalText")
        )
        Text(
            "${startDate.format(dateFormatter)} to ${endDate.format(dateFormatter)}",
            modifier = Modifier.testTag("reportingPeriodText")
        )

        if (report.labelTotals.isNotEmpty()) {
            val maximumLabelMinutes = report.labelTotals.maxOf { it.minutes }
            Column(modifier = Modifier.testTag("reportingLabelChart").padding(top = 8.dp)) {
                Text("Time by label")
                report.labelTotals.forEach { label ->
                    Row(modifier = Modifier.fillMaxWidth().testTag("reportingLabelRow")) {
                        Text(label.labelName, modifier = Modifier.width(120.dp))
                        Box(modifier = Modifier.weight(1f).height(20.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(label.minutes.toFloat() / maximumLabelMinutes)
                                    .height(20.dp)
                                    .background(labelBarColor(label.colorCode))
                                    .testTag("reportingLabelBar")
                            )
                        }
                        Text(formatMinutes(label.minutes), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        val reportListState = rememberLazyListState()
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp), state = reportListState) {
                items(report.dailyTotals) { day ->
                    Row(modifier = Modifier.padding(vertical = 4.dp).testTag("reportingDayRow")) {
                        Text(day.date.format(dateFormatter), modifier = Modifier.padding(end = 8.dp))
                        Text(formatMinutes(day.minutes))
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(reportListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}
