# Privacy

Link Clear is a link cleaner: it strips tracking parameters from URLs and can
optionally expand shortened links. This document states exactly what the app
does and does **not** do with your data and the network. It reflects the app's
actual code, not a marketing summary.

## Summary

- **No analytics, no telemetry, no ads, no crash reporting, no trackers.** The
  app contains no analytics or advertising SDKs.
- **No account, no sign-in, no user identifiers.**
- **Nothing is ever sent to the developer.** There is no Link Clear backend.
- **Offline by default.** Out of the box the app performs **zero** network
  requests. The URLs you clean are processed entirely on your device using a
  ruleset bundled inside the app.
- **The only network access is opt-in** and limited to the specific paths
  described below. The `INTERNET` permission is declared in the manifest solely
  to enable those opt-in paths.

## What runs on-device (always, offline)

- URL cleaning is done by the bundled [ClearURLs](https://github.com/ClearURLs/Rules)
  ruleset shipped inside the app (`:core` module). Pasting, sharing, or cleaning
  the clipboard with the Quick Settings tile does not touch the network.
- Cleaning happens in response to explicit foreground actions only. The app runs
  **no background clipboard monitor** and requests **no runtime permissions**.

## Network access (all opt-in, off by default)

There are exactly three paths that can make an outbound request. None runs
unless you enable the corresponding setting or explicitly tap a button.

### 1. Shortened-link resolver — "Resolve shortened links" (off by default)

When enabled, the resolver expands shortener/redirect links before cleaning.
There are two modes (setting `resolver_mode`, default `OFF`):

- **DIRECT (on-device):** your phone sends a `HEAD` request directly to the
  shortener host and reads the `Location` redirect header. Redirects are **not**
  auto-followed and non-`http(s)` `Location` values are rejected. Only hosts on
  a fixed shortlist are contacted: `bit.ly`, `t.co`, `tinyurl.com`, `goo.gl`,
  `ow.ly`, `buff.ly`, `is.gd` (see `DirectHeadResolver`). In this mode the
  shortener sees your device's IP address, exactly as it would if you had opened
  the link in a browser.
- **CUSTOM (off-device):** the link is sent to an **HTTPS resolver endpoint that
  you configure yourself** (`custom_resolver_url`), which fetches the target on
  your behalf and returns the final URL as JSON. HTTPS is enforced; non-HTTPS
  endpoints are refused. The app ships **no default resolver** — the field is
  empty until you fill it in. The settings screen offers preset *starting
  points* (`unshorten.me`, `unshorten.it`, or your own self-hosted resolver),
  but selecting a preset only prefills the editable URL field; nothing is
  contacted until you have configured a URL and turned the feature on. Some
  presets require your own API key.

  > When you use a third-party resolver you configure, the link you are
  > resolving is sent to that third party under **their** privacy policy, not
  > this app's. For maximum privacy, self-host a resolver.

When the resolver is off (the default), the `:unshorten` code path is a no-op
that performs zero network I/O.

### 2. Manual ruleset update — "Update rules now" (never automatic)

The app ships with a bundled ClearURLs ruleset and works fully offline. If you
tap **Update rules now**, a WorkManager job downloads a fresh ruleset over an
unmetered connection from a single fixed host:

- `https://rules2.clearurls.xyz/data.minify.json`

The download is HTTPS-only, size-capped (5 MB), time-bounded, and structurally
validated before use. This request sends only a standard HTTP GET to the
ClearURLs distribution host; it does **not** include any of your links or
personal data. It never runs unless you explicitly trigger it.

## What is stored on your device

The app stores a small settings file locally (an Android DataStore preferences
file, app-private storage). It contains only your settings:

- resolver mode, your custom resolver URL, and the automatic-action preferences.

**Link Clear keeps no history of the links you clean.** URLs are processed in
memory for the current action and are not written to a database, log, or history
list by the app.

### Backup handling

Because a custom resolver URL may embed a personal API key, the settings
DataStore is **excluded** from Android cloud backup and device-to-device
transfer (see `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml`).
The rest of the app is eligible for the normal Android backup mechanism, but no
link data is stored there because none is persisted.

## Permissions

- `android.permission.INTERNET` — required only for the opt-in resolver and the
  manual rules update described above. No other permissions are requested.

## Third parties

Link Clear has no servers of its own. Data leaves your device only to hosts you
opt into:

- **ClearURLs distribution host** (`rules2.clearurls.xyz`) — only when you tap
  "Update rules now"; receives a plain ruleset download request, no user data.
- **A shortener host** — only in DIRECT resolver mode; receives the shortened
  link you are expanding (and thus your IP), same as opening it in a browser.
- **A resolver endpoint you configure** — only in CUSTOM resolver mode; receives
  the link being resolved, under that provider's terms.

## Contact

Privacy questions or concerns: <tamas@56k.cloud>, or open an issue at
<https://github.com/kisst/link-clear>. Please report suspected privacy/security
**vulnerabilities** privately per [SECURITY.md](../SECURITY.md).
