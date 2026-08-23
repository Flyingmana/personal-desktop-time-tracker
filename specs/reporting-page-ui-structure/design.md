# Design: Reporting Page UI Structure

## Approach

- Keep the app-level screen flow intentionally simple: a single `App()` composable owns the active tab state.
- Model navigation as an enum, e.g. `AppTab = Timers | Reporting`, stored in Compose state via `remember { mutableStateOf(AppTab.Timers) }`.
- Render a tab row at the top of the main screen, with exactly two entries: one for the timer list and one for the reporting page.
- Use a `when (selectedTab)` branch to swap between the two page views while preserving the underlying timer data in the same app state tree.
- The current story only covers the selection and display of the page; it does not define reporting calculations or data aggregation.

## UI structure

- Main root: `Column` with a top navigation row and a content area below it.
- Tab row responsibilities:
  - show both tabs clearly and visibly
  - mark the active tab as selected
  - allow switching without reloading the app state
- Content area responsibilities:
  - render the timer list screen when `Timers` is selected
  - render the reporting page shell when `Reporting` is selected

## Suggested state shape

```kotlin
private enum class AppTab {
    Timers,
    Reporting,
}
```

```kotlin
@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(AppTab.Timers) }

    Column {
        TabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        when (selectedTab) {
            AppTab.Timers -> TimerListScreen()
            AppTab.Reporting -> ReportingPageScreen()
        }
    }
}
```

## Styling and interaction details

- The active tab should be visually distinct via selected state styling, such as a highlighted indicator or text color change.
- The tab selection should update immediately on click and remain consistent until the user switches again.
- The reporting page should not appear as a modal or overlay; it should replace the main content area while preserving the app shell.

## Test tags

- `timersTab`
- `reportingTab`
- `appTabs`

These tags will let UI tests assert that the tab exists, that the selected tab is visually active, and that switching tabs changes the content area.

## Edge cases

- The app should still work if the user changes tabs before any timer data exists.
- A selected reporting tab should show its page shell without crashing even if there are no timer entries yet.
- The active-tab state should be reset only when the app is closed or restarted, not as part of regular tab switching.

## Out of scope

- The detailed calculation logic for reported totals
- Any filter, search, or date-range behavior on the reporting page itself
- Persistence of the selected tab across app restarts
- Advanced navigation patterns such as nested screens or routing
