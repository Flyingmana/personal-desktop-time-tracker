package de.flyingmana.personalworktimetracker

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Personal Worktime Tracker") {
        App()
    }
}
