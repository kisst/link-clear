# Open-Source Audit & Hardening — Link Clear (`app.linkclear`)

**Date:** 2026-07-17

**Repo:** `github.com/kisst/link-clear` — **already PUBLIC** (verified via
`gh repo view kisst/link-clear --json visibility`).

**Method:** Orchestrator + six read-only specialist sub-agents (`secrets-historian`
ran first to completion; the rest in parallel). Remediations were applied only
after per-item approval, on branch `audit/pre-release-hardening`, not pushed.

> **Correction:** an earlier draft asserted the repo was still private, inherited
> from the task framing and never verified. It is public. The secrets scan is
> clean, so nothing sensitive is currently exposed — but the "fix before anyone
> sees it" margin does not exist; anything pushed is already visible.

## Status: SAFE — public is fine to keep

Zero CRITICAL findings, no currently-exposed secrets. History clean, signing key
uncompromised, license valid FOSS, no trackers / GMS / blobs, release build sound.
Both HIGH items are now closed. The prior JDK gap was resolved by running the
transitive CVE scan via the project's Nix toolchain: 39 CVEs, **all build/test
tooling, zero reaching the shipped APK**.

## Release blockers (CRITICAL / HIGH)

No CRITICAL findings. Both HIGH items are fixed.

| # | Area | Finding | Status |
|---|------|---------|--------|
| B1 | supply-chain | Wrapper had no `distributionSha256Sum`. | FIXED — pinned + verified vs gradle.org; wrapper.jar == upstream 8.14.4 |
| B2 | tooling | Transitive CVE tree unverified (no JDK). | RESOLVED — scanned via Nix + osv-scanner; 39 tooling-only CVEs, 0 shipped |

## Should-fix (MEDIUM)

| # | Area | Finding | Status |
|---|------|---------|--------|
| M1 | security | `allowBackup=true` with a fragile exclude-denylist. | FIXED — fail-safe allowlist |
| M2 | release | Static `versionCode=1`; F-Droid needs monotonic. | FIXED — derived from git tag |
| M3 | supply-chain | Unused `androidx.test:core`, `espresso-core`. | FIXED — removed |
| M4 | tooling | trufflehog not run (gitleaks clean). | OPEN — low priority |

Plus: 39 transitive CVEs (AGP Unified Test Platform + ktlint logback) — ACCEPTED
as non-shipping; a ktlint/detekt bump clears none (verified). osv-scanner added to
CI (warn-only) for future drift.

## Per-area results

### 1. Secrets & history — CLEAN

- gitleaks over all 9 commits: only hit is the public release-cert fingerprint
  (false positive; matches the recorded fingerprint).
- No keystore / `google-services.json` / `local.properties` / `.env` / PKCS12
  blob ever committed. CI uses `${{ secrets.* }}` only.
- Signing key not compromised. No history rewrite, no key rotation.

### 2. F-Droid compliance — 6/6 gates PASS

| Gate | Result |
|------|--------|
| License (FOSS + consistent) | PASS — GPL-3.0 |
| No-GMS / Firebase | PASS |
| No-trackers | PASS |
| No build-time blobs | PASS |
| Fastlane metadata | PASS |
| Reproducible (Path 1) | PASS-ready |

No anti-features expected. The optional rules-refresh endpoint is public,
self-hostable, and the app works fully offline.

### 3. Android security — minimal surface, no privileged capabilities

- No Device-Owner, no VpnService. Only `INTERNET`.
- 3 exported components, all sound (`CleanTileService` guarded by
  `BIND_QUICK_SETTINGS_TILE`; `MainActivity`/`ShareReceiverActivity` benign).
- No cleartext, no trust-all TLS, no hardcoded keys, no persisted link history.
- No logging of URLs/config. `RuleUpdateWorker` download well-hardened.
- M1 (`allowBackup`) fixed as a fail-safe allowlist.

### 4. Supply chain — clean, FOSS, transitive tree VERIFIED

- All deps Apache-2.0 / MIT / EPL — compatible with GPL-3.0. Nothing phones home.
- Only `google()` + `mavenCentral()` + `gradlePluginPortal()`; no dynamic versions.
- Wrapper.jar verified == upstream 8.14.4; distribution SHA pinned (B1).
- Transitive scan (B2): 324 artifacts, 39 CVEs, **0 in release classpath**.
- CVE sources: netty/grpc/protobuf/commons-io → AGP Unified Test Platform;
  logback 1.3.5 → ktlint-gradle. Bumping ktlint/detekt clears none (verified).
- okhttp 4.12.0 (shipping HTTP client) patched for CVE-2023-3635, no OSV advisory.

### 5. Release hygiene — sound

- R8 minify + resource shrink on for release; proguard keep-rules correct.
- No `BuildConfig.DEBUG` leaks, backdoors, mock code, or security TODOs.
- No tracked build artifacts.
- Signing from env vars. M2 (versionCode) fixed.

### 6. Docs & licensing — drafts delivered (uncommitted)

- GPL-3.0-only; inbound=outbound per CONTRIBUTING. Icons original. ClearURLs data
  (LGPL-3.0) credited in README.
- Draft proposals created for review (see below).

## Draft deliverables (UNCOMMITTED — for review)

| File | Purpose |
|------|---------|
| `SECURITY.md` | Responsible-disclosure policy (contact + scope + safe-harbor). |
| `docs/PRIVACY.md` | Network/privacy disclosure derived from actual code. |
| `docs/README_BUILD_SECTION.draft.md` | Proposed README build-from-source section. |
| `docs/ATTRIBUTION.draft.md` | Attribution/NOTICE assessment. |

## Remediation status

All fixes are atomic commits on `audit/pre-release-hardening`, each verified with
the Nix toolchain before commit. Not pushed — you push.

**Done (committed):**

1. B1 — pinned `distributionSha256Sum` (verified vs gradle.org + wrapper.jar).
2. M1 — backup configs converted to a fail-safe allowlist.
3. M2 — version derived from git tag (monotonic `versionCode`).
4. M3 — removed unused test deps.
5. B2 / CVEs — transitive scan run; osv-scanner added to CI (warn-only).

**Open (your decision, low priority):**

- M4 — install trufflehog; run verified-secrets pass (gitleaks already clean).
- Add `keystore.properties`, `.env`, `*.p12` to `.gitignore`.
- Confirm SPDX intent (`GPL-3.0-only` recommended), consistently.
- Finalize the 4 doc drafts; confirm GitHub private vuln reporting is enabled.

**Skipped — and why:**

- ktlint/detekt bump "to clear CVEs" — verified it clears none; a version change
  with a false rationale.

## History-rewriting / key-rotation

None required — history is clean and the key is not compromised. Note: since the
repo is already public, a found secret could not be fixed by history rewrite
alone; it would have to be treated as exposed and rotated. Moot here.

## Definition of done

- All six sub-agents ran; the JDK gap was closed via Nix rather than skipped.
- No secret printed in full; no history mutated; nothing pushed.
- Repo confirmed public via `gh` (correcting the earlier unverified claim).
- This report reflects verified findings and current remediation status.
- Draft `SECURITY.md` / `docs/PRIVACY.md` / README / attribution remain uncommitted.
