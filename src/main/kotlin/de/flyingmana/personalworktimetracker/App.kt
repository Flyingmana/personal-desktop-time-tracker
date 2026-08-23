package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private enum class AppTab {
    Timers,
    Reporting,
}

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(AppTab.Timers) }
    var count by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.testTag("appTabs")) {
            Button(
                onClick = { selectedTab = AppTab.Timers },
                modifier = Modifier.testTag("timersTab")
            ) {
                Text("Timers")
            }
            Button(
                onClick = { selectedTab = AppTab.Reporting },
                modifier = Modifier.testTag("reportingTab")
            ) {
                Text("Reporting")
            }
        }

        when (selectedTab) {
            AppTab.Timers -> TimerListScreen(count = count, onIncrement = { count = incrementCount(count) })
            AppTab.Reporting -> ReportingPageScreen()
        }
    }
}

@Composable
private fun TimerListScreen(count: Int, onIncrement: () -> Unit) {
    Column {
        Text(text = "Hours logged: $count", modifier = Modifier.testTag("countText"))
        Button(onClick = onIncrement, modifier = Modifier.testTag("incrementButton")) {
            Text("Log an hour")
        }
    }
}

fun incrementCount(current: Int): Int = current + 1
