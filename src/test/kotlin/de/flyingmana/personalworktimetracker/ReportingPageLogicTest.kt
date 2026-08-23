package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class ReportingPageLogicTest : StringSpec({
    "reporting period counts only completed timers and includes zero-day entries" {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 7)

        val entries = listOf(
            WorkEntry(
                start = LocalDateTime.of(2026, 8, 1, 9, 0),
                end = LocalDateTime.of(2026, 8, 1, 10, 30),
            ),
            WorkEntry(
                start = LocalDateTime.of(2026, 8, 4, 14, 0),
                end = LocalDateTime.of(2026, 8, 4, 15, 0),
            ),
            WorkEntry(
                start = LocalDateTime.of(2026, 8, 8, 8, 0),
                end = LocalDateTime.of(2026, 8, 8, 9, 30),
            ),
            WorkEntry(
                start = LocalDateTime.of(2026, 8, 3, 8, 0),
                end = null,
            ),
        )

        val report = buildReportingPeriodReport(entries, start, end)

        report.totalMinutes shouldBe 150
        report.dailyTotals.map { it.minutes } shouldBe listOf(90, 0, 0, 60, 0, 0, 0)
        report.dailyTotals.map { it.date } shouldBe listOf(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 4),
            LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 8, 6),
            LocalDate.of(2026, 8, 7),
        )
    }
})
