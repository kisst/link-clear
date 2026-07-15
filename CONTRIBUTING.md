# Contributing to Link Clear

Thanks for your interest in contributing! Link Clear is a privacy-first Android
URL cleaner, and contributions of all kinds — bug reports, fixes, features,
docs — are welcome.

## Before you start

**Read the design decisions.** The [ADRs](docs/adr/) explain *why* the app is
built the way it is. A few are load-bearing for any contribution:

- [No background clipboard monitoring](docs/adr/0002-no-background-clipboard-monitor.md)
  — the app only acts on explicit foreground user actions, and requests **no
  runtime permissions**. Please don't propose always-on clipboard watching,
  accessibility services, or foreground monitors.
- [Opt-in network unshortening](docs/adr/0003-opt-in-network-unshortening.md)
  — network access is off by default and isolated in `:unshorten`.
- [Bundle and reuse the ClearURLs ruleset](docs/adr/0001-bundle-and-reuse-clearurls-ruleset.md)
  — we consume community rules rather than writing our own.

Then:

- **Check existing issues** before filing a new one or starting large work.
- For anything non-trivial, **open an issue first** to discuss the approach — it
  saves everyone time.

## Development setup

See [docs/INSTALL.md](docs/INSTALL.md#build-from-source) for prerequisites
(JDK 17, Android SDK 35) and build commands. The Gradle wrapper is committed, so
you don't need a local Gradle install.

## Project layout

Three Gradle modules (see
[ADR 0004](docs/adr/0004-three-module-split.md)):

- **`:core`** — pure Kotlin/JVM cleaning engine. No Android dependencies. This
  is where URL extraction and rule application live, and where most tests go.
- **`:unshorten`** — opt-in OkHttp resolver for shortened links.
- **`:app`** — the Android app: Compose UI, share receiver, tile service,
  settings, and the rule-update worker.

Keep Android types out of `:core`. If your change is about *how links are
cleaned*, it almost certainly belongs in `:core` with a JVM unit test.

## Making a change

1. **Branch** off `main`.
2. **Write tests.** New cleaning behavior needs a `:core` unit test; new
   resolver behavior needs a `:unshorten` test (use MockWebServer). We favor a
   test-first workflow.
3. **Keep commits atomic** — one logical change per commit, each building and
   passing tests on its own. A fix, a refactor, and a lint cleanup are separate
   commits. Write a clear one-line subject describing *why*.
4. **Run the quality gates locally** before pushing (see below).
5. **Open a PR** against `main` with a description of what and why. Link the
   issue if there is one.

## Quality gates

CI runs the same command on every PR (see
[ADR 0006](docs/adr/0006-tooling.md)). Run it locally first:

```bash
./gradlew ktlintCheck detekt test lint
```

- **ktlint** — formatting. Auto-fix most issues with `./gradlew ktlintFormat`.
- **detekt** — static analysis (config in `config/detekt/detekt.yml`).
- **test** — unit tests across all modules.
- **lint** — Android lint.

Please fix warnings you encounter, even pre-existing ones in code you're
touching — don't leave the build noisier than you found it.

### Markdown

Documentation is linted with [`mdl`](https://github.com/markdownlint/markdownlint)
(config in `.mdlrc` / `.mdl_style.rb`). It is **not** enforced in CI, but please
run it locally when you touch docs:

```bash
gem install mdl        # once
mdl .                  # lint all Markdown from the repo root
```

## Dependencies

All dependency and plugin versions live in the version catalog
(`gradle/libs.versions.toml`). Add or bump dependencies there, not as inline
version strings in build files.

## Cleaning rules

Link Clear does not maintain its own ruleset — it bundles and reuses
[ClearURLs](https://github.com/ClearURLs/Rules). If a specific site isn't being
cleaned correctly because of a missing or wrong rule, the fix usually belongs
**upstream in ClearURLs**. If it's a parsing or application bug on our side,
that's a `:core` fix with a regression test.

## License

Link Clear is licensed under **GPL-3.0** (see [LICENSE](LICENSE)). By
contributing, you agree that your contributions are licensed under the same
terms.
