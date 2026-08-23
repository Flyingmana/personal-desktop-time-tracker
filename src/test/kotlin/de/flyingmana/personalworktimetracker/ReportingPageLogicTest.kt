package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ReportingPageLogicTest : StringSpec({
    "current month range is inclusive" {
        currentMonthReportingRange(LocalDate.of(2026, 2, 12)) shouldBe Pair(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
        )
    }

    "reporting period counts only completed timers and includes zero-day entries" {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 7)

        val data = TrackerData(entries = listOf(
            TaskTimerEntry(
                id = UUID.randomUUID(),
                text = "First task",
                start = LocalDateTime.of(2026, 8, 1, 9, 0),
                end = LocalDateTime.of(2026, 8, 1, 10, 30),
            ),
            TaskTimerEntry(
                id = UUID.randomUUID(),
                text = "Second task",
                start = LocalDateTime.of(2026, 8, 4, 14, 0),
                end = LocalDateTime.of(2026, 8, 4, 15, 0),
            ),
            TaskTimerEntry(
                id = UUID.randomUUID(),
                text = "Outside period",
                start = LocalDateTime.of(2026, 8, 8, 8, 0),
                end = LocalDateTime.of(2026, 8, 8, 9, 30),
            ),
            TaskTimerEntry(
                id = UUID.randomUUID(),
                text = "Running task",
                start = LocalDateTime.of(2026, 8, 3, 8, 0),
                end = null,
            ),
        ))

        val report = buildReportingPeriodReport(data, start, end)

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

    "report splits completed entries across midnight and rejects reversed periods" {
        val report = buildReportingPeriodReport(
            TrackerData(entries = listOf(
                TaskTimerEntry(
                    UUID.randomUUID(),
                    "Overnight task",
                    LocalDateTime.of(2026, 8, 2, 23, 30),
                    LocalDateTime.of(2026, 8, 3, 1, 30),
                ),
            )),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 3),
        )

        report.dailyTotals.map { it.minutes } shouldBe listOf(30, 90)
        report.totalMinutes shouldBe 120

        shouldThrow<IllegalArgumentException> {
            buildReportingPeriodReport(TrackerData(), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2))
        }
    }

    "report aggregates clipped time for each assigned label" {
        val firstLabel = TimerLabel(UUID.randomUUID(), "Client", "#1976D2")
        val secondLabel = TimerLabel(UUID.randomUUID(), "Admin", "#388E3C")
        val data = TrackerData(
            labels = listOf(firstLabel, secondLabel),
            entries = listOf(
                TaskTimerEntry(
                    UUID.randomUUID(),
                    "Labelled task",
                    LocalDateTime.of(2026, 8, 1, 23, 30),
                    LocalDateTime.of(2026, 8, 2, 1, 30),
                    labelIds = setOf(firstLabel.id, secondLabel.id),
                ),
            ),
        )

        val report = buildReportingPeriodReport(data, LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2))

        report.labelTotals shouldBe listOf(
            LabelTotal(secondLabel.id, "Admin", 90, "#388E3C"),
            LabelTotal(firstLabel.id, "Client", 90, "#1976D2"),
        )
    }
})
