# Submitting Link Clear to F-Droid

This is an actionable, repo-specific guide for getting **Link Clear**
(`app.linkclear`) into the F-Droid main repository. It is written against
F-Droid's primary documentation and the `fdroiddata` metadata format, and it
maps every requirement onto files that actually exist in this repo.

Sources are linked inline. The two that matter most:

- Inclusion How-To: <https://f-droid.org/docs/Inclusion_How-To/>
- Submitting Quick Start Guide:
  <https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/>
- Build Metadata Reference:
  <https://f-droid.org/docs/Build_Metadata_Reference/>
- Reproducible Builds: <https://f-droid.org/docs/Reproducible_Builds/>

## The two inclusion paths

F-Droid can distribute an app in one of two ways
([Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)):

1. **F-Droid builds from source and signs with its own key** (the default).
   F-Droid's build server compiles the app from a tagged commit in an isolated
   VM with a 100% FLOSS toolchain, then signs the resulting APK with the
   F-Droid signing key. Users trust F-Droid's signature. This is the simplest
   path and the one F-Droid recommends for new apps.

2. **Reproducible builds — F-Droid verifies a developer-signed APK.** F-Droid
   still builds from source, but instead of signing itself it checks that its
   unsigned build is byte-for-byte identical to a developer-published APK, then
   copies the developer's signature onto it and publishes that. This lets a user
   who installed the developer's APK (e.g. from GitHub Releases via Obtainium)
   receive F-Droid updates without an uninstall/reinstall, because the signing
   key never changes.

### Which path to pick for Link Clear

**Go with Path 2 (reproducible builds — F-Droid verifies a developer-signed
APK).** Reasoning:

- The goal is that Obtainium users (who install the GitHub-Releases APK) and
  F-Droid users **share one signing key**, so updates flow between the two
  channels without an uninstall/reinstall. Only Path 2 delivers this; under
  Path 1 F-Droid signs with its own key and the two installs diverge.
- The signing key already exists and is used by the release workflow
  (`.github/workflows/release.yml`), which builds and uploads a signed
  `link-clear-v%v.apk` to each GitHub Release. Path 2 points F-Droid at that
  published APK via `Binaries` and pins the certificate via
  `AllowedAPKSigningKeys`.
- The cost is that our GitHub-Actions build must be **byte-for-byte
  reproducible** against F-Droid's from-source build. R8/`isMinifyEnabled` +
  `isShrinkResources` (build.gradle.kts) plus the v2 APK signature (which covers
  *all* bytes) make exact reproducibility unlikely on the first try
  ([Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)). We
  accept iterating on this in the F-Droid CI / MR thread rather than falling back
  to Path 1.

Recommended sequence: publish the signed APK to a GitHub Release, verify its
signing-cert SHA-256 against `AllowedAPKSigningKeys`, then open the Path 2 MR and
work through reproducibility with F-Droid CI.

## What is already satisfied vs. what must be added

Checklist against F-Droid's compliance requirements
([Inclusion How-To](https://f-droid.org/docs/Inclusion_How-To/),
[Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/)):

| Requirement | Status | Evidence in this repo |
| --- | --- | --- |
| Public source repo | ✅ | <https://github.com/kisst/link-clear> |
| FOSS license file | ✅ | `LICENSE` (GPL-3.0), see ADR 0005 |
| Only FOSS deps, no GMS/Firebase | ✅ | Compose, DataStore, WorkManager, coroutines; OkHttp in `:unshorten`. No Play Services. |
| No non-free bundled assets | ✅ | Only bundled data is ClearURLs `data.min.json` (LGPL-3.0) in `:core` (ADR 0001) |
| No unconsented binary downloads | ✅ | Rules refresh is opt-in, user-triggered, fetches *data* not executables (ADR 0001, 0003) |
| Buildable unsigned without secrets | ✅ | `hasSigning` guard leaves release unsigned when env vars absent (build.gradle.kts 26-54) |
| Release git tag matching the version | ✅ | Tag `v1.0.0` exists (commit `e040b88`) |
| Author consent for inclusion | ⚠️ action | You are the author — implied, but the MR should state it |
| Fastlane store-listing metadata | ❌ add | No `fastlane/` dir exists yet — see below |
| `metadata/app.linkclear.yml` in fdroiddata | ❌ add | Worked example below |

Two real gaps: **Fastlane metadata** (for the store listing) and the
**fdroiddata metadata file** (for the build recipe). Neither lives in this repo
except Fastlane, which F-Droid reads from upstream.

### Note on the release tag vs. versionName

`versionName` is `"1.0"` (build.gradle.kts line 13) but the git tag is
`v1.0.0`. F-Droid's `UpdateCheckMode: Tags` reads the versionName from the
built APK, and `AutoUpdateMode: Version v%v` would look for a tag literally
matching the versionName. Because the tag (`v1.0.0`) does not equal `v1.0`, the
worked example below pins the first build by commit and uses an explicit tag
pattern. Going forward, the cleanest fix is to make tags and `versionName`
agree (e.g. set `versionName = "1.0.0"` for the next release, or tag future
releases `v1.0`). This is an upstream choice, not required for the first build.

## Worked example: `metadata/app.linkclear.yml`

This is the file that goes into **fdroiddata** (not into this repo). It is
filled in for `versionCode 1` / `versionName 1.0` at tag `v1.0.0`. Assumptions
are called out in comments.

```yaml
Categories:
  - Internet
  - Security
License: GPL-3.0-only
AuthorName: KissT
WebSite: https://github.com/kisst/link-clear
SourceCode: https://github.com/kisst/link-clear
IssueTracker: https://github.com/kisst/link-clear/issues
Changelog: https://github.com/kisst/link-clear/releases

AutoName: Link Clear

RepoType: git
Repo: https://github.com/kisst/link-clear.git

Builds:
  - versionName: "1.0"
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v.0
UpdateCheckMode: Tags
CurrentVersion: "1.0"
CurrentVersionCode: 1
```

Field notes, each tied to its source:

- **`License: GPL-3.0-only`** — a single SPDX identifier for the binary as a
  whole ([Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)).
  The bundled LGPL-3.0 ruleset is compatible and does not change the app's own
  license (ADR 0005). Use `GPL-3.0-only` or `GPL-3.0-or-later` to match exactly
  what the `LICENSE` header states.
- **`Categories`** — a required list; values come from F-Droid's category set.
  `Internet` and `Security` fit a tracking-parameter stripper. Adjust to the
  live category list if a linter objects.
- **`RepoType: git` / `Repo`** — must be an HTTPS URL with no authentication
  ([Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)).
- **`Builds:`** — one entry per published version. `commit: v1.0.0` pins the
  build to the existing tag; `subdir: app` points at the `:app` module (the
  root `settings.gradle.kts` includes `:core`, `:unshorten`, `:app`, and
  Gradle resolves the module deps from the root); `gradle: [yes]` builds the
  default variant with no product flavors (Link Clear defines none — see
  build.gradle.kts). F-Droid gets an **unsigned** APK because none of the
  `LINKCLEAR_*` env vars are set on its build server, and then signs it.
- **`AutoUpdateMode: Version v%v.0`** — `%v` expands to the versionName; the
  trailing `.0` reconstructs the `v1.0.0` tag shape from versionName `1.0`.
  If you align tags and versionName in future (see note above), simplify this to
  `Version v%v`. `%c` is available for versionCode if ever needed
  ([Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)).
- **`UpdateCheckMode: Tags`** — F-Droid watches the repo's git tags for new
  releases ([Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/)).
  Since this repo tags each release (`v1.0.0`), Tags is the right mode. Ensure
  tags are pushed and that the versionCode in `build.gradle.kts` is bumped for
  every new tag, or AutoUpdate will skip it.
- **`CurrentVersion` / `CurrentVersionCode`** — the version F-Droid recommends;
  set to the latest published release (`1.0` / `1`).

### Reproducible-builds add-on (Path 2, later)

Once Path 1 builds succeed, to have F-Droid publish *your* signature, add to
the same file
([Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)):

```yaml
Binaries: https://github.com/kisst/link-clear/releases/download/v%v.0/link-clear-v%v.0.apk
AllowedAPKSigningKeys: <sha256-of-your-signing-cert>
```

`Binaries` points at the developer-published APK (the release workflow names it
`link-clear-${GITHUB_REF_NAME}.apk`, i.e. `link-clear-v1.0.0.apk` — matched by
the pattern above). Get the fingerprint from a signed APK:

```sh
apksigner verify --print-certs link-clear-v1.0.0.apk   # SHA-256 of certificate
# or, from the keystore:
keytool -list -v -keystore <keystore> -alias <alias>
```

Path 2 only publishes the developer APK when F-Droid's from-source rebuild is
byte-identical to it before signing. R8 minification and v2/v3 signing make this
finicky; treat it as a follow-up, not a launch blocker.

## Anti-features that might apply

F-Droid anti-features are warning labels, not automatic disqualifiers
([Anti-Features](https://f-droid.org/docs/Anti-Features/)). Assessment for Link
Clear:

- **Tracking** — does **not** apply. The only network calls are the opt-in,
  user-triggered rules refresh and opt-in unshortening (ADR 0001, ADR 0003);
  nothing phones home or reports usage.
- **NonFreeNet / NonFreeDep / NonFreeAssets / NonFreeAdd** — do **not** apply.
  All dependencies are FOSS, the ClearURLs ruleset is LGPL-3.0 and
  redistributable, and there is no proprietary backend.
- **Ads / UpstreamNonFree / NonFreeComp** — do **not** apply.
- **Tethered network service** — worth a quick look but **should not apply**.
  The rules refresh defaults to `https://rules2.clearurls.xyz/`, but (a) it is
  entirely optional — the app works fully offline with the bundled copy — and
  (b) it fetches a public, self-hostable community data file, not a proprietary
  service the app is locked to. F-Droid excludes this anti-feature when the app
  functions without the service, which Link Clear does. If a reviewer disagrees,
  the mitigation is to expose the rules URL as a user-configurable setting.

**Expected outcome: no anti-feature flags.** State this explicitly in the MR so
reviewers can confirm quickly.

## Fastlane metadata layout (upstream, in this repo)

F-Droid reads the store listing from a Fastlane/Triple-T structure in the
**upstream** repo ([Inclusion How-To](https://f-droid.org/docs/Inclusion_How-To/)).
Create these files in link-clear (this is the one set of files to add to *this*
repo — do so in a separate change, not as part of this doc):

```text
fastlane/metadata/android/en-US/
├── title.txt                 # "Link Clear"
├── short_description.txt      # <= 80 chars, one line
├── full_description.txt       # long description, Markdown-ish plain text
├── images/
│   ├── icon.png               # 512x512 launcher icon
│   └── phoneScreenshots/
│       ├── 1.png
│       ├── 2.png
│       └── ...
└── changelogs/
    └── 1.txt                  # changelog for versionCode 1
```

Notes:

- The directory is keyed by locale (`en-US`); add more locales as siblings.
- `changelogs/<versionCode>.txt` is matched by versionCode, so the first file is
  `1.txt` (matching `versionCode = 1`). Each future release adds a new file.
- Screenshots go under `phoneScreenshots/`; filenames are sorted, so number
  them.
- If you add Fastlane metadata before the fdroiddata MR, F-Droid will pick up
  the description and screenshots automatically on first build — no extra
  fields needed in the metadata YAML.

## Step-by-step: opening the fdroiddata merge request

Following the
[Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
and the Inclusion How-To. Install `fdroidserver` first so the lint tools are
available.

1. **(Optional but recommended) add Fastlane metadata upstream.** Commit the
   `fastlane/metadata/android/en-US/...` tree above to link-clear and push, so
   F-Droid can read the store listing.
2. **Fork `fdroiddata` on GitLab** — <https://gitlab.com/fdroid/fdroiddata> —
   then clone your fork and create a branch off `master`:

   ```sh
   git clone https://gitlab.com/<you>/fdroiddata.git
   cd fdroiddata
   git checkout -b app.linkclear
   ```

3. **Create `metadata/app.linkclear.yml`** with the contents from the worked
   example above.
4. **Normalise and lint** the metadata (requires `fdroidserver`):

   ```sh
   fdroid rewritemeta app.linkclear   # canonical formatting
   fdroid lint app.linkclear          # policy/format checks
   ```

5. **Test the build locally** (optional; CI will also run it). This confirms
   F-Droid can build unsigned from `v1.0.0`:

   ```sh
   fdroid build -v -l app.linkclear
   ```

6. **Commit and push** to your fork:

   ```sh
   git add metadata/app.linkclear.yml
   git commit -m "New app: Link Clear (app.linkclear)"
   git push -u origin app.linkclear
   ```

7. **Open a merge request** against `fdroiddata` `master` on GitLab. In the
   description: confirm you are the upstream author (author consent), state that
   no anti-features apply and why, and note this is a **Path 2 (reproducible /
   cross-signed)** submission — F-Droid verifies its build against the published
   `link-clear-v%v.apk` and copies the developer signature. CI runs `fdroid lint`
   and a reproducible `fdroid build`; fix anything it flags, including
   reproducibility mismatches and pre-existing style nits.
8. **Iterate on reviewer feedback** in the MR thread. Once merged, the app
   appears in the main repo within roughly 24-48 hours
   ([Inclusion How-To](https://f-droid.org/docs/Inclusion_How-To/)).

### Alternative: Request For Packaging (RFP)

If you would rather have an F-Droid contributor write the metadata, open an
issue on the **rfp** tracker
(<https://gitlab.com/fdroid/rfp>) instead of an MR. Providing the metadata file
yourself via an MR is faster because it reduces reviewer burden
([Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)).

## Summary

- Pick **Path 2** (reproducible builds — F-Droid verifies the developer-signed
  APK and republishes the developer signature) so F-Droid and Obtainium share
  one signing key. The repo is compatible: FOSS deps, GPL-3.0, tagged `v1.0.1`
  with a signed `link-clear-v1.0.1.apk` published to the GitHub Release.
- Add two things: **Fastlane store-listing metadata** upstream (done), and the
  **`metadata/app.linkclear.yml`** recipe (with `Binaries` +
  `AllowedAPKSigningKeys`) in an fdroiddata MR.
- Expect **no anti-feature flags**; the only network use is opt-in and
  self-hostable.
- `versionName` and the git tag now agree (`1.0.1` <-> `v1.0.1`). The main risk
  is **build reproducibility** (R8 + v2 signing); plan to iterate with F-Droid
  CI before the build goes green.
