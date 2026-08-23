# Copilot Instructions — Personal Worktime Tracker

## Project overview

Kotlin desktop app built with Compose Multiplatform for Desktop (JVM-only, no Android/iOS
targets), targeting Windows (MSI) and Ubuntu/Debian (DEB). Single-module Gradle project.

- Package root: `de.flyingmana.personalworktimetracker`
- Entry point: [src/main/kotlin/de/flyingmana/personalworktimetracker/Main.kt](../src/main/kotlin/de/flyingmana/personalworktimetracker/Main.kt)
- JVM target 23, JDK toolchain 25 (Gradle auto-provisions if missing)

## Build & test commands

```shell
./gradlew build       # compile + run all checks
./gradlew run         # run the app
./gradlew test        # unit tests (Kotest)
./gradlew uiTest      # UI tests (Compose UI Test)
./gradlew packageMsi  # Windows installer (Windows only)
./gradlew packageDeb  # Debian/Ubuntu installer (Linux only)
```

`packageMsi`/`packageDeb` must run on their respective target OS.

## Architecture conventions

- Keep business/UI logic as **pure functions** separate from Compose state whenever possible
  (e.g. `incrementCount(current: Int): Int`), so logic is unit-testable without Compose.
- Composables hold state via `remember { mutableStateOf(...) }` and delegate calculations to
  those pure functions.
- Tag UI elements that tests need to find/assert with `Modifier.testTag("...")`, using
  descriptive camelCase tags (e.g. `countText`, `incrementButton`).

## Testing conventions

- Unit tests live in `src/test/kotlin`, written with **Kotest** `StringSpec` and `shouldBe`
  matchers. One test class per source file under test (e.g. `AppLogicTest` for `App.kt` logic).
- UI tests live in `src/uiTest/kotlin`, written with **Compose UI Testing** + JUnit4
  (`createComposeRule`, `onNodeWithTag`, `performClick`, `assertTextEquals`).
- When adding a feature, add both a unit test for the pure logic and a UI test for the
  user-visible behavior, matching the acceptance criteria in the story's `requirements.md`.

## Spec-driven workflow (important)

Work is driven by user stories under `specs/<story-slug>/` (kebab-case folder names), each with
three files, written and used **in this order**:

1. `requirements.md` — user story + acceptance criteria (Given/When/Then). Agree on this first.
2. `design.md` — technical approach: architecture, data model, UI flow, edge cases, out-of-scope
   items. Based on the agreed requirements.
3. `tasks.md` — checklist of concrete implementation tasks derived from the design. Implement
   task by task, checking items off (`- [x]`) as they land.

See [specs/README.md](../specs/README.md) and [specs/example-story/](../specs/example-story/)
for the canonical template.

When asked to implement a story:
- If `requirements.md` doesn't exist yet for the feature, draft it first and pause for
  confirmation before writing `design.md`.
- If `requirements.md` exists but `design.md` doesn't, propose a design before writing code.
- If `tasks.md` exists, implement the next unchecked task(s) and check them off as completed.
- Do not silently change requirements/design while implementing tasks — flag discrepancies
  instead of reinterpreting scope.

## General style

- Prefer idiomatic, minimal Kotlin; avoid introducing new architectural layers or dependencies
  not called for by the current story's design.
- Don't add persistence, navigation, or other infrastructure speculatively — check `design.md`
  for what's in/out of scope.
