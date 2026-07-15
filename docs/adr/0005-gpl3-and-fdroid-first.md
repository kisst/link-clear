# 0005. GPL-3.0 license and F-Droid-first distribution

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

Link Clear bundles the ClearURLs ruleset, which is licensed **LGPL-3.0**
([ADR 0001](0001-bundle-and-reuse-clearurls-ruleset.md)). The app's own license
must be compatible with that, and the distribution channel should fit a
privacy-first FOSS tool without policy friction.

Two questions had to be answered together: what license, and where to publish.

## Decision

**License the app under GPL-3.0**, and treat **F-Droid as the primary
distribution channel**.

- **GPL-3.0** is compatible with the LGPL-3.0 ruleset we bundle, keeps the whole
  project free/open, and matches the ethos of a privacy tool built on community
  data.
- **F-Droid-first** because it is the natural home for GPL software, has no
  Play-Store-style policy friction around clipboard/share behavior, and its
  audience expects and values FOSS. It also enforces reproducible,
  source-built packages, which reinforces the app's trust story.
- The Google Play Store is **not excluded** — it remains an optional later
  channel — but it is explicitly not the design target.

## Consequences

### Positive

- License compatibility with the bundled ruleset is guaranteed.
- No conflict between the app's foreground-only clipboard model and store
  review policies (the design already avoids the risky patterns — see
  [ADR 0002](0002-no-background-clipboard-monitor.md)).
- Reproducible F-Droid builds align with the privacy claims: users can verify
  the published APK matches the source.

### Negative / trade-offs

- GPL-3.0 constrains downstream reuse (copyleft). This is intentional for this
  project.
- F-Droid's build and inclusion process is stricter and slower than uploading
  to Play. Accepted in exchange for the trust and compatibility benefits.
