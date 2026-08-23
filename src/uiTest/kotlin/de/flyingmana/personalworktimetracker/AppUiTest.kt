package de.flyingmana.personalworktimetracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
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
import java.time.format.DateTimeFormatter
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
    fun labelsTab_createsLabelsWithAnOptionalColor() {
        var persistedData: TrackerData? = null
        composeRule.setContent { App(onDataChanged = { persistedData = it }) }

        composeRule.onNodeWithTag("labelsTab").performClick()
        composeRule.onNodeWithTag("labelManager").assertExists()
        composeRule.onNodeWithTag("labelNameInput").performTextInput("Client")
        composeRule.onNodeWithTag("labelColorSwatch-#1976D2").performClick()
        composeRule.onNodeWithTag("createLabelButton").performClick()

        composeRule.runOnIdle {
            val label = requireNotNull(persistedData).labels.single()
            check(label.name == "Client")
            check(label.colorCode == "#1976D2")
        }
    }

    @Test
    fun labelsTab_updatesAnExistingLabelColor() {
        val label = TimerLabel(UUID.randomUUID(), "Client")
        var persistedData: TrackerData? = null
        composeRule.setContent {
            App(initialData = TrackerData(labels = listOf(label)), onDataChanged = { persistedData = it })
        }

        composeRule.onNodeWithTag("labelsTab").performClick()
        composeRule.onNodeWithTag("editLabelColorButton").performClick()
        composeRule.onNodeWithTag("labelEditColorSwatch-#388E3C").performClick()
        composeRule.onNodeWithTag("saveLabelColorButton").performClick()

        composeRule.runOnIdle {
            check(requireNotNull(persistedData).labels.single().colorCode == "#388E3C")
        }
    }

    @Test
    fun timerLabels_canBeAssignedWhenStartingAndDisplayAsChips() {
        val label = TimerLabel(UUID.randomUUID(), "Client", "#1976D2")
        composeRule.setContent { App(initialData = TrackerData(labels = listOf(label))) }

        composeRule.onNodeWithTag("newTimerLabelsButton").performClick()
        composeRule.onNodeWithTag("labelPickerOption").performClick()
        composeRule.onNodeWithTag("applyLabelsButton").performClick()
        composeRule.onNodeWithTag("startTimerButton").performClick()

        composeRule.onNodeWithTag("timerLabelChip").assertExists()
    }

    @Test
    fun timerLabels_canBeAssignedToAnExistingTimer() {
        val label = TimerLabel(UUID.randomUUID(), "Client")
        val timer = TaskTimerEntry(
            UUID.randomUUID(),
            "Existing task",
            LocalDateTime.of(2026, 8, 23, 9, 0),
        )
        composeRule.setContent { App(initialData = TrackerData(entries = listOf(timer), labels = listOf(label))) }

        composeRule.onNodeWithTag("timerLabelsButton").performClick()
        composeRule.onNodeWithTag("labelPickerOption").performClick()
        composeRule.onNodeWithTag("applyLabelsButton").performClick()

        composeRule.onNodeWithTag("timerLabelChip").assertExists()
    }

    @Test
    fun continuingALabelledTimer_carriesOverItsLabels() {
        val label = TimerLabel(UUID.randomUUID(), "Client")
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        val finishedTimer = TaskTimerEntry(
            UUID.randomUUID(),
            "Finished task",
            start,
            start.plusMinutes(30),
            labelIds = setOf(label.id),
        )
        composeRule.setContent {
            App(initialData = TrackerData(entries = listOf(finishedTimer), labels = listOf(label)))
        }

        composeRule.onNodeWithTag("continueTimerButton").performClick()

        composeRule.onAllNodesWithTag("timerLabelChip").assertCountEquals(2)
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
    fun successfulTimerChange_isPersistedImmediately() {
        val start = LocalDateTime.of(2026, 8, 23, 9, 0)
        var persistedData: TrackerData? = null
        composeRule.setContent {
            App(onDataChanged = { persistedData = it }) { start }
        }

        composeRule.onNodeWithTag("startTimerButton").performClick()

        composeRule.runOnIdle {
            val persistedTimer = requireNotNull(persistedData)
                .entries.filterIsInstance<TaskTimerEntry>().single()
            check(persistedTimer.start == start)
        }
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
        val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd")

        composeRule.onAllNodesWithTag("timerDayHeading")[0]
            .assertTextEquals(LocalDate.of(2026, 8, 23).format(weekdayFormatter))
        composeRule.onAllNodesWithTag("timerDayHeading")[1]
            .assertTextEquals(LocalDate.of(2026, 8, 22).format(weekdayFormatter))
        composeRule.onAllNodesWithTag("timerDayTotal")[0].assertTextEquals("0h 30m")
        composeRule.onAllNodesWithTag("timerDayTotal")[1].assertTextEquals("0h 30m")
    }

    @Test
    fun timerList_showsRunningTimerGroupsBeforeNewerCompletedGroups() {
        val currentTime = LocalDateTime.of(2026, 8, 23, 10, 30)
        val olderRunningStart = LocalDateTime.of(2026, 8, 22, 9, 0)
        val newerFinishedStart = LocalDateTime.of(2026, 8, 23, 9, 0)
        val data = TrackerData(entries = listOf(
            TaskTimerEntry(UUID.randomUUID(), "Older running", olderRunningStart),
            TaskTimerEntry(
                UUID.randomUUID(),
                "Newer finished",
                newerFinishedStart,
                newerFinishedStart.plusMinutes(30),
            ),
        ))
        composeRule.setContent { App(clock = { currentTime }, initialData = data) }
        val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd")

        composeRule.onAllNodesWithTag("timerDayHeading")[0]
            .assertTextEquals(olderRunningStart.toLocalDate().format(weekdayFormatter))
        composeRule.onAllNodesWithTag("timerDayHeading")[1]
            .assertTextEquals(newerFinishedStart.toLocalDate().format(weekdayFormatter))
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
