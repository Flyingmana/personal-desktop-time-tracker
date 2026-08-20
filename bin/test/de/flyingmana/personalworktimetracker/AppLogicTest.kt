package de.flyingmana.personalworktimetracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AppLogicTest : StringSpec({
    "incrementCount increases the value by one" {
        incrementCount(0) shouldBe 1
        incrementCount(41) shouldBe 42
    }
})
