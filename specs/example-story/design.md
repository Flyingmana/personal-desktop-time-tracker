# Design: Log an hour of work

> This is a template/example. Copy this folder's structure for real stories and delete this note.

## Approach

- A single `@Composable fun App()` holds a `remember { mutableStateOf(0) }` counter.
- A `Button` labeled "Log an hour" calls a pure function `incrementCount(current: Int): Int` and
  assigns the result back to the state, keeping the increment logic unit-testable independent of
  Compose.
- A `Text` node tagged `countText` displays `"Hours logged: $count"` for UI-test assertions.

## Out of scope

- Persistence across app restarts
- Editing/removing logged hours
