package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

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
})