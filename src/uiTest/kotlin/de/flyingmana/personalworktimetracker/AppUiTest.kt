package de.flyingmana.personalworktimetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialTimerList_showsEmptyState() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("timerListEmpty").assertExists()
    }

    @Test
    fun switchingTabs_changesVisibleView() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("timersTab").assertExists()
        composeRule.onNodeWithTag("reportingTab").assertExists()
        composeRule.onNodeWithTag("reportingTab").performClick()
        composeRule.onNodeWithTag("reportingPageTitle").assertExists()
        composeRule.onNodeWithTag("reportingStartDateInput").assertExists()
        composeRule.onNodeWithTag("reportingEndDateInput").assertExists()
        composeRule.onNodeWithTag("timersTab").performClick()
        composeRule.onNodeWithTag("timerListEmpty").assertExists()
    }
}
