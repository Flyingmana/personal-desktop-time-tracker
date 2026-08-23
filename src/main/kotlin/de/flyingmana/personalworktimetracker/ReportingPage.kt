package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val MINUTES_PER_HOUR = 60

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

data class WorkEntry(
    val start: LocalDateTime,
    val end: LocalDateTime? = null,
)

data class DailyTotal(
    val date: LocalDate,
    val minutes: Int,
)

data class ReportingPeriodReport(
    val start: LocalDate,
    val end: LocalDate,
    val totalMinutes: Int,
    val dailyTotals: List<DailyTotal>,
)

fun currentMonthReportingRange(referenceDate: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
    val start = referenceDate.withDayOfMonth(1)
    val end = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
    return start to end
}

fun buildReportingPeriodReport(
    entries: List<WorkEntry>,
    start: LocalDate,
    end: LocalDate,
): ReportingPeriodReport {
    val allDates = generateSequence(start) { current ->
        if (current.isBefore(end)) current.plusDays(1) else null
    }.toList()

    val totalsByDate = allDates.associateWith { 0 }.toMutableMap()

    entries.filter { it.end != null }.forEach { entry ->
        val actualEnd = entry.end ?: return@forEach
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

    return ReportingPeriodReport(
        start = start,
        end = end,
        totalMinutes = totalMinutes,
        dailyTotals = dailyTotals,
    )
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return "${hours}h ${minutes}m"
}

fun currentMonthSampleEntries(): List<WorkEntry> = listOf(
    WorkEntry(LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 10, 30)),
    WorkEntry(LocalDateTime.of(2026, 8, 4, 14, 0), LocalDateTime.of(2026, 8, 4, 15, 0)),
    WorkEntry(LocalDateTime.of(2026, 8, 10, 8, 30), LocalDateTime.of(2026, 8, 10, 11, 30)),
)

@Composable
fun ReportingPageScreen(
    entries: List<WorkEntry> = currentMonthSampleEntries(),
    initialStart: LocalDate = currentMonthReportingRange().first,
    initialEnd: LocalDate = currentMonthReportingRange().second,
) {
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }

    val report = buildReportingPeriodReport(entries, startDate, endDate)

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

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(report.dailyTotals) { day ->
                Row(modifier = Modifier.padding(vertical = 4.dp).testTag("reportingDayRow")) {
                    Text(day.date.format(dateFormatter), modifier = Modifier.padding(end = 8.dp))
                    Text(formatMinutes(day.minutes))
                }
            }
        }
    }
}
