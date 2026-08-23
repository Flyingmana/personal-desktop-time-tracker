package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AppLogicTest : StringSpec({
    "elapsed minutes include only completed minutes" {
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)

        elapsedMinutes(start, start.plusSeconds(59)) shouldBe 0
        elapsedMinutes(start, start.plusMinutes(42).plusSeconds(59)) shouldBe 42
    }

    "elapsed minutes never becomes negative" {
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)

        elapsedMinutes(start, start.minusMinutes(1)) shouldBe 0
    }

    "attendance minutes include completed, overnight, and running intervals for the day" {
        val date = LocalDate.of(2026, 8, 23)
        val currentTime = LocalDateTime.of(2026, 8, 23, 17, 30)
        val data = TrackerData(entries = listOf(
            AttendanceEntry(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 22, 23, 0),
                LocalDateTime.of(2026, 8, 23, 1, 0),
            ),
            AttendanceEntry(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 23, 8, 0),
                LocalDateTime.of(2026, 8, 23, 9, 15),
            ),
            AttendanceEntry(UUID.randomUUID(), LocalDateTime.of(2026, 8, 23, 16, 0)),
        ))

        attendanceMinutesOn(data, date, currentTime) shouldBe 225
    }

    "task timer day groups order newest first and include live running time" {
        val august22 = LocalDateTime.of(2026, 8, 22, 14, 0)
        val august23 = LocalDateTime.of(2026, 8, 23, 9, 0)
        val currentTime = LocalDateTime.of(2026, 8, 23, 10, 45)
        val timers = listOf(
            TaskTimerEntry(UUID.randomUUID(), "Earlier", august22, august22.plusMinutes(30)),
            TaskTimerEntry(UUID.randomUUID(), "Finished", august23, august23.plusMinutes(45)),
            TaskTimerEntry(UUID.randomUUID(), "Running", august23.plusHours(1)),
        )

        taskTimerDayGroups(timers, currentTime) shouldBe listOf(
            TaskTimerDayGroup(LocalDate.of(2026, 8, 23), 90, timers.drop(1)),
            TaskTimerDayGroup(LocalDate.of(2026, 8, 22), 30, timers.take(1)),
        )
    }
})