# 0004. Three-module split (`:core` / `:unshorten` / `:app`)

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

The cleaning logic — parsing the ruleset, extracting URLs from arbitrary text,
applying rules to produce a `CleanResult` — is the heart of the app and the
part most in need of thorough, fast testing. If that logic lived inside the
Android application module, testing it would require Robolectric or an emulator,
making the primary safety net slow and awkward.

Separately, the network unshortening capability
([ADR 0003](0003-opt-in-network-unshortening.md)) needs to be isolable so its
network access is a clear boundary rather than diffused through the app.

## Decision

Split the project into three Gradle modules with strict dependency direction:

```text
:core        pure Kotlin/JVM, no Android dependencies
:unshorten   Android library, OkHttp resolver (depends on nothing app-specific)
:app         Android application (depends on :core and :unshorten)
```

- **`:core`** — `CleaningEngine`, `RuleLoader`, `RuleModels`, `UrlExtractor`,
  `CleanResult`, `BundledRules`. Zero Android APIs. Runs under plain JUnit 5.
- **`:unshorten`** — the opt-in `Resolver` implementations. Depends only on
  OkHttp; tested with MockWebServer.
- **`:app`** — Jetpack Compose UI, `ShareReceiverActivity`, `CleanTileService`,
  `SettingsStore` (DataStore), `RuleUpdateWorker`. A thin shell over `:core`.

Every UI surface is a thin adapter that calls into `CleaningEngine`.

## Consequences

### Positive

- The entire cleaning engine is unit-tested with fast JVM tests — no emulator,
  no Robolectric — which makes the core safety net cheap to run in CI and
  locally.
- Clear seams: network lives in `:unshorten`, cleaning lives in `:core`, Android
  glue lives in `:app`. Each module is understandable in isolation.
- Contributors can work on cleaning rules or the engine without any Android SDK
  knowledge.

### Negative / trade-offs

- Slightly more Gradle boilerplate (three build files, a version catalog).
- Discipline required to keep Android types out of `:core`. This is enforced
  simply by `:core` not depending on the Android Gradle plugin.
