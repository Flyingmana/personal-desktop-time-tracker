package de.flyingmana.personalworktimetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

    @Test
    fun startingTimer_showsItsLiveElapsedTime() {
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        composeRule.setContent { App { start } }

        composeRule.onNodeWithTag("timerTextInput").performTextInput("Write design")
        composeRule.onNodeWithTag("startTimerButton").performClick()

        composeRule.onNodeWithTag("runningTimerElapsed").assertExists()
        composeRule.onNodeWithTag("stopTimerButton").assertExists()
    }

    @Test
    fun attendanceTimer_startsStopsAndContinuesWithAccumulatedTime() {
        var currentTime = LocalDateTime.of(2026, 8, 23, 9, 0)
        composeRule.setContent { App { currentTime } }

        composeRule.onNodeWithTag("attendanceTimerSection").assertExists()
        composeRule.onNodeWithTag("startAttendanceButton").performClick()
        composeRule.onNodeWithTag("runningAttendanceElapsed").assertExists()
        currentTime = currentTime.plusMinutes(30)
        composeRule.onNodeWithTag("stopAttendanceButton").performClick()

        composeRule.onNodeWithTag("attendanceStart").assertExists()
        composeRule.onNodeWithTag("attendanceEnd").assertExists()
        composeRule.onNodeWithTag("attendanceTodayElapsed").assertTextEquals("Today: 30 min")
        composeRule.onNodeWithTag("continueAttendanceButton").performClick()
        composeRule.onNodeWithTag("runningAttendanceElapsed").assertExists()
    }

    @Test
    fun timerList_groupsTaskTimersByNewestStartDateWithDailyTotals() {
        val currentTime = LocalDateTime.of(2026, 8, 23, 10, 30)
        val data = TrackerData(entries = listOf(
            TaskTimerEntry(
                UUID.randomUUID(),
                "Earlier task",
                LocalDateTime.of(2026, 8, 22, 9, 0),
                LocalDateTime.of(2026, 8, 22, 9, 30),
            ),
            TaskTimerEntry(UUID.randomUUID(), "Live task", LocalDateTime.of(2026, 8, 23, 10, 0)),
        ))
        composeRule.setContent { App(clock = { currentTime }, initialData = data) }

        composeRule.onAllNodesWithTag("timerDayHeading")[0]
            .assertTextEquals("${LocalDate.of(2026, 8, 23)}: 0h 30m")
        composeRule.onAllNodesWithTag("timerDayHeading")[1]
            .assertTextEquals("${LocalDate.of(2026, 8, 22)}: 0h 30m")
    }

    @Test
    fun startingWithEmptyText_createsUnnamedTimer() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("startTimerButton").performClick()

        composeRule.onNodeWithTag("runningTimerElapsed").assertExists()
        composeRule.onNodeWithTag("timerLabelInput").assertTextEquals("")
    }

    @Test
    fun runningTimerText_canBeChanged() {
        composeRule.setContent { App() }

        composeRule.onNodeWithTag("startTimerButton").performClick()
        composeRule.onNodeWithTag("timerLabelInput").performTextReplacement("Plan release")

        composeRule.onNodeWithTag("timerLabelInput").assertTextEquals("Plan release")
    }

    @Test
    fun stoppingAndContinuingTimer_showsFinishedEntryAndNewRunningTimer() {
        var currentTime = LocalDateTime.of(2026, 8, 23, 9, 0)
        composeRule.setContent { App { currentTime } }

        composeRule.onNodeWithTag("timerTextInput").performTextInput("Write design")
        composeRule.onNodeWithTag("startTimerButton").performClick()
        currentTime = currentTime.plusMinutes(30)
        composeRule.onNodeWithTag("stopTimerButton").performClick()

        composeRule.onNodeWithTag("finishedTimerStart").assertExists()
        composeRule.onNodeWithTag("finishedTimerEnd").assertExists()
        composeRule.onNodeWithTag("timerLabelInput").performTextReplacement("Document design")
        composeRule.onNodeWithTag("timerLabelInput").assertTextEquals("Document design")
        composeRule.onNodeWithTag("continueTimerButton").performClick()
        composeRule.onNodeWithTag("runningTimerElapsed").assertExists()
    }
}
