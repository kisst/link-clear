# 0002. No background clipboard monitoring

- **Status:** Accepted
- **Date:** 2026-07-14

## Context

The obvious "magic" version of a link cleaner is an always-on service that
watches the clipboard and silently rewrites any URL you copy. It is tempting
because it needs zero user interaction.

It is also the wrong design on modern Android, for several compounding reasons:

- **Android 10 (API 29) blocks background clipboard reads entirely.** An app
  can only read the clipboard when it holds focus (a foreground activity) or is
  the default input method. A background monitor simply cannot see the
  clipboard.
- **Android 12 (API 31) shows a system toast** every time an app reads
  clipboard data it did not put there — so even a partial workaround would spam
  the user with "Link Clear pasted from your clipboard" notifications.
- **Getting anywhere close would require invasive permissions** — an
  accessibility service or a persistent foreground service with an ongoing
  notification — to keep a process alive and observing. That is a large,
  scary permission surface for a privacy tool, and precisely the kind of app
  that violates a user's trust and gets flagged.
- **Store and F-Droid friction.** Accessibility-service and always-running
  designs draw heightened review scrutiny and rejection risk.

A privacy-first tool that demands sweeping permissions to watch everything you
copy is self-contradictory.

## Decision

Clean links **only in response to explicit, foreground user actions.** There is
no background service, no clipboard monitor, and no accessibility service.

The three entry points are all foreground-triggered:

1. **Share target** (`ShareReceiverActivity`) — the user shares a link to the
   app.
2. **Quick Settings tile** (`CleanTileService`) — the tile tap gives a
   foreground context, so reading the clipboard in place is permitted.
3. **In-app editor** (`EditorScreen`) — the user pastes into the app.

## Consequences

### Positive

- **Zero runtime permissions.** The only manifest permission is `INTERNET`,
  and even that is exercised only by opt-in paths
  (see [ADR 0003](0003-opt-in-network-unshortening.md)).
- No persistent process, no ongoing notification, no battery cost.
- No system toasts, because every clipboard read happens with foreground focus.
- Trivially explainable privacy story: the app can only act when you ask it to.

### Negative / trade-offs

- Not fully automatic — the user must take one action (share, tap the tile, or
  paste). We consider this a feature, not a bug: it keeps the tool honest and
  predictable.
- The Quick Settings tile is the closest thing to one-tap cleaning; it is the
  recommended fast path.
