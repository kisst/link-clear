# Security Policy

Link Clear is a privacy tool. If you find a vulnerability — especially one that
could leak the URLs a user cleans, expose a stored resolver API key, or cause
the app to make network requests the user did not opt into — we want to hear
about it privately before it is disclosed publicly.

## Reporting a vulnerability

Please report security issues **privately**. Do **not** open a public GitHub
issue, pull request, or discussion for a suspected vulnerability, and do not
post details on social media until a fix has shipped.

Preferred channels, in order:

1. **GitHub private vulnerability reporting** — on
   <https://github.com/kisst/link-clear>, go to the **Security** tab and choose
   **Report a vulnerability**. This keeps the report and its discussion private.
2. **Email** — <tamas@56k.cloud>. Please put `link-clear security` in the
   subject line. If you want to send encrypted mail, ask for a key in an
   initial (non-sensitive) message.

Please include:

- the affected version (release tag or commit) and the device/Android version,
- a description of the issue and its impact,
- clear reproduction steps or a proof of concept, and
- any suggested remediation, if you have one.

## Supported versions

Link Clear is a single actively-developed app distributed as rolling releases.
Security fixes are applied to the **latest release** and shipped in a new
release; there are no separate maintenance branches for older versions.

| Version                    | Supported          |
| -------------------------- | ------------------ |
| Latest published release   | :white_check_mark: |
| Any older release          | :x:                |

If you are running an older build (sideloaded APK, an old F-Droid/Obtainium
version), please update to the latest release before reporting, in case the
issue is already fixed.

## Scope

In scope — issues in **this repository's** code, for example:

- the URL-cleaning engine (`:core`) mishandling input in a way that leaks or
  corrupts data,
- the opt-in resolver (`:unshorten`) sending requests to unintended hosts,
  following hostile redirects into non-`http(s)` schemes, or leaking the
  stored resolver API key,
- the rule-update worker (`app/.../update/RuleUpdateWorker.kt`) accepting a
  malicious ruleset, exceeding its size/host guards, or downgrading from HTTPS,
- the settings store persisting sensitive data (e.g. a resolver API key) where
  it can be backed up or transferred off-device unexpectedly,
- any code path that performs network I/O without the corresponding opt-in
  setting being enabled.

Out of scope:

- Vulnerabilities in **third-party services** the user explicitly configures
  (e.g. `unshorten.me`, `unshorten.it`, a self-hosted resolver) — report those
  to the respective provider.
- Vulnerabilities in the upstream **ClearURLs** ruleset or its distribution
  host (`rules2.clearurls.xyz`) — report those to the
  [ClearURLs project](https://github.com/ClearURLs/Rules).
- Issues requiring a rooted device, a physical-access attacker who can already
  read app-private storage, or a malicious app already granted equivalent OS
  privileges.
- Reports generated solely by automated scanners with no demonstrated impact.

## What to expect

This is a small, volunteer-maintained project, so timelines are best-effort:

- **Acknowledgement** of your report within about **5 business days**.
- An initial **assessment** (accepted / needs-more-info / out-of-scope) within
  about **10 business days**.
- For accepted issues, we will work with you on a fix and coordinate a
  disclosure timeline — typically aiming to release a fix within **90 days**,
  sooner for actively-exploited or high-severity issues.

We are happy to credit you in the release notes for the fix unless you prefer to
remain anonymous.

## Safe harbor

We will not pursue or support legal action against anyone who, in good faith,
discovers and reports a security issue in Link Clear, provided you:

- make a genuine effort to avoid privacy violations, data destruction, and
  service disruption during your research,
- only interact with accounts and data you own or have explicit permission to
  test (do not access other users' data),
- give us a reasonable opportunity to fix the issue before disclosing it
  publicly, and
- do not exploit the issue beyond the minimum needed to demonstrate it.

Activity conducted consistent with this policy is considered authorized, and we
will not treat it as a violation of our terms. If in doubt about whether a
specific action is acceptable, ask us first at the contacts above.
