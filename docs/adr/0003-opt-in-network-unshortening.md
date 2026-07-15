# 0003. Opt-in, isolated network unshortening

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

Some tracking hides behind link shorteners and redirectors (`t.co`, `bit.ly`,
and similar). To clean the *real* destination, the app would have to follow the
redirect first — which means making a network request to a third party and
revealing that the link is being processed.

This is a direct tension with the app's privacy-first, offline-by-default
stance ([ADR 0002](0002-no-background-clipboard-monitor.md)). Resolving a short
link necessarily leaks a request to the shortener's servers.

Many shortened links also encode their target directly in a query parameter
(`?url=…`), which the cleaning engine can unwrap offline with no network at
all. Network resolution is only needed for opaque redirects.

## Decision

Make network unshortening **opt-in and OFF by default**, and isolate it behind
a module boundary.

- The resolver lives in its own Gradle module, **`:unshorten`**
  (`RemoteResolver` / `DirectHeadResolver` behind a `Resolver` interface), so
  the network capability is a distinct, auditable unit rather than tangled into
  the cleaning engine.
- It runs **only** when the user enables the "Resolve shortened links" setting.
  When disabled, no network call is made — the code path is skipped entirely.
- Offline redirect unwrapping (`?url=…`-style) is handled by `:core` and always
  works without this feature.
- Resolution uses short timeouts and falls back to cleaning the original short
  URL if it fails, so a network hiccup never blocks the result.

## Consequences

### Positive

- The default experience is fully offline and leaks nothing.
- The one path that touches the network is a single, isolated, testable module
  the user consciously turns on.
- The `INTERNET` permission's purpose is honest and narrow: opt-in
  unshortening, plus the opt-in rules refresh
  ([ADR 0001](0001-bundle-and-reuse-clearurls-ruleset.md)).

### Negative / trade-offs

- With the setting off, opaque shortened links are cleaned only as far as their
  short form allows. We surface this rather than silently resolving.
- Enabling the feature reveals processed links to the shortener's servers. The
  setting is documented as such, so the choice is informed.
