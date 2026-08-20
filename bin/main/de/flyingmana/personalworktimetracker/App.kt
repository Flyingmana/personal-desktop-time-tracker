package de.flyingmana.personalworktimetracker

import androidx.compose.foundation.layout.Column
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

/** Placeholder starting screen; replace once specs/example-story is implemented. */
@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Hours logged: $count", modifier = Modifier.testTag("countText"))
        Button(onClick = { count = incrementCount(count) }, modifier = Modifier.testTag("incrementButton")) {
            Text("Log an hour")
        }
    }
}

fun incrementCount(current: Int): Int = current + 1
