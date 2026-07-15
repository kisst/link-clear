# 0001. Bundle and reuse the ClearURLs ruleset

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

Stripping tracking parameters from URLs requires knowing which parameters are
tracking noise and which are functional, per site. Getting this right across
the whole web is a large, never-finished, adversarial maintenance job:
providers change parameter names, add new trackers, and encode redirects in
countless ways.

[ClearURLs](https://github.com/ClearURLs/Rules) already maintains exactly this:
a community-curated ruleset (`data.min.json`) of provider patterns, parameter
rules, raw rules, and redirection rules, kept current by many contributors and
consumed by the popular ClearURLs browser extension.

Building and maintaining our own equivalent ruleset would duplicate that effort
poorly — we would ship worse coverage and fall behind immediately.

## Decision

Reuse the ClearURLs ruleset rather than authoring our own.

- **Bundle** a copy of `data.min.json` in `:core`
  (`core/src/main/resources/clearurls/data.min.json`). The app cleans links
  fully offline, out of the box, with broad coverage from day one.
- **Parse** the ruleset into typed rules (`RuleLoader` → `RuleModels`) rather
  than treating it as opaque data, so the engine can apply provider patterns,
  param rules, raw rules, and redirections deterministically.
- **Optionally refresh** the bundled copy over the network from
  `https://rules2.clearurls.xyz/` via `RuleUpdateWorker` (WorkManager,
  unmetered-only, user-triggered). This is a convenience, not a requirement —
  the app never needs to fetch rules to function.

## Consequences

### Positive

- Broad, community-maintained coverage without us maintaining a ruleset.
- Fully offline by default; the bundled ruleset is the source of truth.
- Clear licensing path: bundling an LGPL-3.0 ruleset is compatible with our
  GPL-3.0 app (see [ADR 0005](0005-gpl3-and-fdroid-first.md)).

### Negative / trade-offs

- We inherit the ClearURLs data model and its quirks; our parser must track its
  schema.
- The bundled ruleset ages between refreshes. We accept this because the update
  path exists and coverage degrades gracefully (unknown params are left alone).
- Remote refresh is validated (HTTPS-only, valid JSON, size cap, minimum
  provider count) but **not cryptographically signed** — ClearURLs does not
  publish signed releases. This is a known limitation documented in
  `RuleUpdateWorker`. It is acceptable because the refresh is opt-in and the
  bundled ruleset remains the trusted fallback.

## Attribution

ClearURLs rules are LGPL-3.0. Credit is surfaced in the app's Settings/About
and in the README.
