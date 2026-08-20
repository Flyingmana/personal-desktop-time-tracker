package de.flyingmana.personalworktimetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingLogAnHour_incrementsCount() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("countText").assertTextEquals("Hours logged: 0")
        composeRule.onNodeWithTag("incrementButton").performClick()
        composeRule.onNodeWithTag("countText").assertTextEquals("Hours logged: 1")
    }
}
