# 0006. Build tooling and quality gates

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

An open project that invites contributions needs a build that is reproducible
on any machine and quality gates that catch style and correctness issues
automatically, so review can focus on substance rather than formatting.

## Decision

Standardize on the mainstream Android/Kotlin toolchain, pinned centrally, with
lint/static-analysis/test gates enforced in CI.

### Language & UI

- **Kotlin** throughout (`1.9.24`).
- **Jetpack Compose** with **Material 3** for all UI; no XML layouts.
- `minSdk 26` (Android 8), `compileSdk`/`targetSdk 35`.

### Build

- **Gradle** with the **Kotlin DSL** (`*.gradle.kts`) and the **Gradle
  wrapper** committed, so builds are reproducible without a local Gradle
  install.
- **Android Gradle Plugin** `8.6.1`.
- A **version catalog** (`gradle/libs.versions.toml`) is the single source of
  truth for every dependency and plugin version. New dependencies go through
  the catalog, not inline strings.
- **JDK 17** is the required toolchain (source/target compatibility and
  `jvmTarget`).

### Quality gates

All of these run in CI on every push to `main` and every pull request:

- **ktlint** (`org.jlleitschuh.gradle.ktlint`) — formatting.
- **detekt** (config in `config/detekt/detekt.yml`) — static analysis.
- **Android lint** (`lint` task).
- **JUnit 5** unit tests for `:core`; **MockWebServer** for `:unshorten`;
  Robolectric available where an Android context is unavoidable.

CI runs a single gate: `./gradlew ktlintCheck detekt test lint`.

## Consequences

### Positive

- Any contributor with JDK 17 + Android SDK 35 and the wrapper can build and
  test — no bespoke setup.
- Formatting and static-analysis disputes are settled by tools, not reviewers.
- Centralized versions make upgrades a one-file change.

### Negative / trade-offs

- Contributors must run the same gates locally to avoid CI churn (documented in
  `CONTRIBUTING.md`).
- Pinned versions require periodic maintenance to stay current.
