# Link Clear

Link Clear is an Android app that strips tracking parameters from URLs using
the bundled [ClearURLs](https://github.com/ClearURLs/Rules) ruleset. It works
entirely offline by default: paste or share a link, get back the same link
with tracking noise removed.

## Surfaces

- **Share target** — share a link to Link Clear from any app. You land on a
  confirmation screen showing the cleaned URL with **Copy**, **Share**, and
  **Open** actions. If the "Automatic action" setting is enabled, the chosen
  action runs immediately instead of showing the confirmation screen.
- **Quick Settings tile** — add the "Clean clipboard" tile to your Quick
  Settings panel. Tapping it cleans the current clipboard contents in place.
- **In-app editor** — open the app directly for a **Paste Clean** /
  **Paste Raw** editor with a live before/after diff of what was removed.

## Settings

- **Resolve shortened links** — opt-in, network-based unshortening of
  redirect/shortener links (e.g. `t.co`, `bit.ly`) before cleaning. **Off by
  default.**
- **Automatic action** — when enabled, the share flow skips the confirmation
  screen and automatically performs a chosen action: re-share, copy, or open
  the cleaned link. **Off by default.**
- **Update rules now** — manually trigger a WorkManager job that refreshes
  the bundled ClearURLs ruleset from `https://rules2.clearurls.xyz/` over an
  unmetered network connection. Optional; the app ships with a bundled
  ruleset and works fully offline without ever running this.

## Privacy

Link Clear is offline by default and requires no runtime permissions. The
`INTERNET` permission is declared in the manifest but is only exercised by
two opt-in paths: the shortened-link resolver and the manual rules update.
Neither runs unless you turn on the corresponding setting or explicitly tap
"Update rules now."

## Architecture

The project is split into three Gradle modules:

- **`:core`** — pure Kotlin/JVM cleaning engine (rule loading, rule
  application, URL extraction). No Android dependencies; fully unit-tested.
- **`:unshorten`** — opt-in OkHttp-based resolver for shortened/redirect
  links, used only when "Resolve shortened links" is enabled.
- **`:app`** — the Android application: Jetpack Compose UI, share receiver,
  Quick Settings tile service, settings storage (DataStore), and the
  WorkManager-based rule updater.

Target platform: minSdk 26, compileSdk/targetSdk 35. Kotlin throughout, UI
built with Jetpack Compose and Material 3.

## Install & build

Link Clear targets Android 8.0+ (minSdk 26). Install a released build from
F-Droid, sideload an APK, or build from source (JDK 17 + Android SDK 35, using
the committed Gradle wrapper):

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :core:test :unshorten:test :app:testDebugUnitTest   # unit tests
```

Full instructions — prerequisites, release builds, instrumented tests, and
troubleshooting — are in [`docs/INSTALL.md`](docs/INSTALL.md).

## Contributing

Contributions are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the
workflow, quality gates, and project conventions.

## Design decisions

The [`docs/adr/`](docs/adr/) directory records the architectural decisions
behind Link Clear — why there's no background clipboard monitor, why it reuses
the ClearURLs ruleset, why unshortening is opt-in, and more.

## License & credits

Link Clear is licensed under the **GNU General Public License v3.0** — see
[`LICENSE`](LICENSE).

URL-cleaning rules are sourced from the
[ClearURLs project](https://github.com/ClearURLs/Rules), licensed under
LGPL-3.0. A copy of the ruleset is bundled in `:core` so the app works
offline out of the box; it can optionally be refreshed from
`https://rules2.clearurls.xyz/` via the "Update rules now" setting.

Downloaded rulesets are validated before use — the response must be valid
JSON, HTTPS-only, under a size cap, and contain a minimum number of
providers — but they are **not cryptographically signed**, since ClearURLs
does not publish signed releases. This is a known limitation; see
`RuleUpdateWorker` for details.
