# 0007. Release and packaging

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

Given F-Droid-first distribution ([ADR 0005](0005-gpl3-and-fdroid-first.md)),
the way the app is packaged and released has to support F-Droid's model:
builds from source that anyone can reproduce, no proprietary components, and a
verifiable link between a released APK and its source commit. The packaging
choices also have to keep the shipped app small and free of tracking.

## Decision

Package the app as a single, minified, source-buildable APK with no proprietary
dependencies.

- **Single application module** (`:app`) produces one APK; the `:core` and
  `:unshorten` libraries are linked in. `applicationId = app.linkclear`,
  `versionCode`/`versionName` bumped per release.
- **Release builds are minified and shrunk** (`isMinifyEnabled = true`,
  `isShrinkResources = true`) with R8/ProGuard, keeping the APK lean. Keep
  rules live in `app/proguard-rules.pro`.
- **No proprietary SDKs, analytics, or crash reporters** are included — a
  requirement both for F-Droid inclusion and for the privacy story. All
  dependencies are FOSS (Compose, DataStore, WorkManager, OkHttp).
- **Reproducible from source**: the committed Gradle wrapper and pinned version
  catalog ([ADR 0006](0006-tooling.md)) mean a given commit builds to a
  deterministic package, which is what F-Droid rebuilds and verifies.
- **Signing** is handled outside the repository. No keystore or signing secret
  is committed; F-Droid signs its own reproducible builds, and any
  developer/GitHub-release APK is signed with a local key that never enters
  version control (`*.apk`/`*.aab` are gitignored).

## Consequences

### Positive

- Meets F-Droid's reproducible-build and no-proprietary-code requirements.
- Small, tracker-free APK consistent with the app's privacy claims.
- No secrets in the repo; signing keys stay local/CI-secret.

### Negative / trade-offs

- Minification requires keep rules to be maintained (notably for reflection —
  e.g. kotlinx.serialization models) to avoid stripping needed classes.
- Reproducibility imposes discipline: no timestamp-varying or
  environment-dependent build steps.
