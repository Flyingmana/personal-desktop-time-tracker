package de.flyingmana.personalworktimetracker

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.file.Path

fun main() {
    val storage = SqlDelightTrackerStorage(
        Path.of(System.getProperty("user.home"), ".personal-worktime-tracker")
    )
    val initialData = storage.load().data

    application {
    Window(onCloseRequest = ::exitApplication, title = "Personal Worktime Tracker") {
            App(initialData = initialData, onDataChanged = storage::save)
        }
    }
}
