# Installing Link Clear

Link Clear is an Android app (minSdk 26 / Android 8.0+). You can install a
released build or build it yourself from source.

## Install a released build

### Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) installs apps straight from
their source — here, the GitHub Releases of this repo — and keeps them updated
automatically. It's the recommended way to install today, and unlike a manual
sideload it handles update notifications for you.

1. Install Obtainium (itself available from
   [F-Droid](https://f-droid.org/packages/dev.imranr.obtainium.fdroid/) or its
   own [GitHub Releases](https://github.com/ImranR98/Obtainium/releases)).
2. In Obtainium, tap **Add App** and paste this repo's URL:
   `https://github.com/kisst/link-clear`
3. Obtainium picks up the signed APK from the latest release. Install it, and
   it will offer updates whenever a new release is published.

Because every release APK is signed with the same key
(see [ADR 0007](adr/0007-release-and-packaging.md)), Obtainium's update checks
verify the signature stays consistent.

### F-Droid

F-Droid is the intended long-term distribution channel (see
[ADR 0005](adr/0005-gpl3-and-fdroid-first.md)). It is **not yet published**
there. Once it is, you'll be able to install it from any F-Droid client;
F-Droid builds reproducibly from this repo and signs with its own key. The
submission plan is documented in
[docs/fdroid-submission.md](fdroid-submission.md).

### Manual sideload

If you'd rather install the APK by hand, grab it from the
[latest release](https://github.com/kisst/link-clear/releases/latest):

1. On your device, enable **Install unknown apps** for your file manager or
   browser (Settings → Apps → Special access).
2. Open the downloaded `.apk` and confirm the install.

Verify the APK's signature against the release notes before installing — the
signing certificate SHA-256 is fixed across all releases.

## Build from source

### Prerequisites

- **JDK 17** (Temurin or any standard distribution).
- **Android SDK 35** — `compileSdk`/`targetSdk` are 35.
- The **Gradle wrapper** is committed, so you do **not** need a local Gradle
  install. The wrapper pins Gradle 8.14.4.

Set `ANDROID_HOME` (or `sdk.dir` in a `local.properties` file at the repo root)
to your Android SDK location. `local.properties` is gitignored.

The project was developed against a Nix-provided toolchain, but any standard
JDK 17 + Android SDK 35 setup works.

### Build the debug APK

```bash
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Install it on a connected
device with:

```bash
./gradlew :app:installDebug
```

### Build a release APK

```bash
./gradlew :app:assembleRelease
```

Release builds are minified and resource-shrunk (see
[ADR 0007](adr/0007-release-and-packaging.md)). Signing is not configured in
the repository — supply your own signing config to produce an installable
release build. No keystore or signing secret is committed.

### Run the tests

Fast JVM unit tests (no device or emulator needed):

```bash
./gradlew :core:test :unshorten:test :app:testDebugUnitTest
```

Instrumented UI tests under `:app` require a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Troubleshooting

- **"SDK location not found"** — set `ANDROID_HOME` or create
  `local.properties` with `sdk.dir=/path/to/Android/sdk`.
- **Wrong Java version** — confirm `java -version` reports 17. Gradle uses the
  JDK on your `PATH` / `JAVA_HOME`.
- **First build is slow** — Gradle downloads the pinned distribution and
  dependencies on the first run; subsequent builds are cached.
