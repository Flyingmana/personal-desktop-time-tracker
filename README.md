# Personal Worktime Tracker

A Kotlin desktop application built with [Compose Multiplatform for Desktop](https://www.jetbrains.com/lp/compose-multiplatform/),
targeting Windows and Ubuntu.

## AI Disclosure

Notable parts of this code were created by AI over Copilot in vscode.
Only Free Tiers were used, no money got payed into AI.


## Requirements

- JDK 21 (Gradle's toolchain support will auto-provision it if not already installed)

## Common tasks

```shell
# Compile everything
./gradlew build

# Run the app
./gradlew run

# Run unit tests (Kotest)
./gradlew test

# Run UI tests (Compose UI Test)
./gradlew uiTest

# Build a native installer for the current OS
./gradlew packageMsi   # Windows
./gradlew packageDeb   # Ubuntu/Debian
```

Note: `jpackage`-based installers can only be built on their target OS, so `packageMsi` must be
run on Windows and `packageDeb` on Ubuntu/Debian (e.g. via separate CI runners).

## Project structure

- `src/main/kotlin` — application source code
- `src/test/kotlin` — unit tests (Kotest)
- `src/uiTest/kotlin` — UI tests (Compose UI Test, JUnit4-based)
- `specs/` — user stories as Requirements/Design/Tasks, one folder per story (see [specs/README.md](specs/README.md))
