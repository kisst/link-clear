# fdroiddata MR — ready-to-submit metadata

This directory holds the F-Droid build recipe for Link Clear, prepared for a
merge request against [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata). The
file [`app.linkclear.yml`](app.linkclear.yml) here is the exact content that
goes into `metadata/app.linkclear.yml` **in the fdroiddata repo** — it does not
belong in this app repo's build, it lives here only as the prepared artifact.

For the background (which inclusion path, anti-features), see
[../fdroid-submission.md](../fdroid-submission.md).

## What this recipe does

This uses **Path 2 — reproducible builds / cross-signing**. F-Droid builds the
app from source, verifies its unsigned build is byte-for-byte identical to the
developer-published APK on GitHub Releases, then copies the **developer's**
signature onto it and publishes that. This way Obtainium users (who installed
the GitHub APK) and F-Droid users share one signing key, so updates flow between
them without an uninstall/reinstall.

- `commit: v1.0.2` pins the build to the release tag. The app version lives as
  inline literals in `app/build.gradle.kts` (`versionCode = 10002`,
  `versionName = "1.0.2"`), written by `release.sh` at release time. They are
  literals, not computed, so F-Droid's static `checkupdates` can read them;
  versionCode follows `MAJOR*10000 + MINOR*100 + PATCH`, monotonic as F-Droid
  requires.
- `Binaries: .../releases/download/v%v/link-clear-v%v.apk` points F-Droid at the
  developer-published APK to reproduce. `%v` expands to the versionName, which
  equals the tag (`v1.0.2`), so it resolves to
  `link-clear-v1.0.2.apk` — the exact asset the release workflow uploads.
- `AllowedAPKSigningKeys: 7e489f6b…057e` is the SHA-256 of this project's release
  signing certificate. **Verified against the actual published
  `link-clear-v1.0.2.apk`** with
  `apksigner verify --print-certs` (the APK is signed with the v2 scheme only).
  F-Droid will only publish the developer signature if the rebuilt APK matches
  and carries this key.
- `AutoUpdateMode: Version` — because the tag and `versionName` agree
  (`v1.0.2` <-> `1.0.2`), F-Droid auto-detects the tag pattern with no template
  needed. (A literal `Version v%v` is rejected by F-Droid's `check-jsonschema`
  step, whose `AutoUpdateMode` pattern only allows an optional `+`-prefixed
  suffix — bare `Version` is the correct value here.)
- `UpdateCheckMode: Tags` watches the repo's git tags for future releases. Each
  release bumps the inline literals via `release.sh`, so `checkupdates` sees the
  new versionCode and auto-updates.

### Reproducibility — verified locally, not assumed

Path 2 only succeeds if F-Droid's from-source rebuild is byte-identical to the
published APK before signing. This has been **verified for `v1.0.2`**: a fresh
`git clone` + `git checkout v1.0.2` built unsigned with the repo toolchain
(OpenJDK 17.0.20, Gradle 8.14.4, AGP 8.6.1, build-tools 35.0.0) is byte-for-byte
identical to the published `link-clear-v1.0.2.apk` across the entire ZIP
entries region (SHA-256 `c6236e79…a78a`) and central directory. The only
difference is the 8192-byte v2 APK Signing Block — exactly the region F-Droid
strips and reattaches the developer signature to.

R8 (`isMinifyEnabled` + `isShrinkResources`) produced deterministic output here,
which is the historically risky part; the check confirms it is stable.

Reproduce the check yourself:

```sh
source .superpowers/sdd/buildenv.sh   # JDK 17 + Android SDK on PATH
docs/fdroid-metadata/verify-reproducible.sh v1.0.2
```

**Caveat:** the build should run from a real git checkout at the tag, not a
`git archive` export — the embedded
`META-INF/version-control-info.textproto` records the git revision, so a
checkout without `.git` reports a false diff on that one entry. (The app version
itself is now a static literal, so it no longer depends on git.)
If F-Droid's build server uses a materially different AGP/build-tools bundle,
minor re-pinning in the recipe may still be needed; the local proof makes that
unlikely.

The **store listing** (title, descriptions, screenshots, icon) is NOT in this
file — F-Droid reads it from the Fastlane tree at `fastlane/metadata/android/`
on the repo's default branch, which is already committed.

## Validation status

This recipe was checked with `fdroidserver` 2.4.5 inside a real `fdroiddata`
clone (its `config/categories.yml` is needed for category validation):

- `fdroid lint app.linkclear` -> **clean** (exit 0). `Internet` and `Security`
  are valid categories in the current F-Droid category set.
- `fdroid rewritemeta app.linkclear` -> **no diff**; the file here is already in
  canonical form (`Binaries` after `Repo`, `AllowedAPKSigningKeys` after the
  `Builds` block), so it will not be rewritten.
- `AllowedAPKSigningKeys` was verified against the published
  `link-clear-v1.0.2.apk` with `apksigner verify --print-certs`.

Re-run these before the MR in case F-Droid's policy or category set has changed
since. Install the tool with `pipx install fdroidserver` (or
`pip install fdroidserver`), then from a clone of your fdroiddata fork:

```sh
cp app.linkclear.yml metadata/app.linkclear.yml
fdroid rewritemeta app.linkclear   # canonical formatting
fdroid lint app.linkclear          # policy/format checks - fix anything it flags
```

Optionally test the actual build (slow; F-Droid CI also runs it):

```sh
fdroid build -v -l app.linkclear
```

## Submitting the merge request

1. Fork <https://gitlab.com/fdroid/fdroiddata> on GitLab, then clone your fork
   and branch off `master`:

   ```sh
   git clone https://gitlab.com/kisst/fdroiddata.git
   cd fdroiddata
   git checkout -b app.linkclear
   ```

2. Copy the recipe into place and run the lint steps above:

   ```sh
   cp /path/to/link-clear/docs/fdroid-metadata/app.linkclear.yml \
      metadata/app.linkclear.yml
   fdroid rewritemeta app.linkclear
   fdroid lint app.linkclear
   ```

3. Commit and push to your fork:

   ```sh
   git add metadata/app.linkclear.yml
   git commit -m "New app: Link Clear (app.linkclear)"
   git push -u origin app.linkclear
   ```

4. Open a merge request against `fdroiddata` `master`. In the description,
   include:
   - **Author consent:** you are the upstream author and want it on F-Droid.
   - **Anti-features:** none apply. The only network use (rules refresh,
     link unshortening) is opt-in, off by default, and hits public/self-hostable
     endpoints; the app works fully offline with the bundled ClearURLs data.
   - **Build note (Path 2 — reproducible/cross-signed):** F-Droid builds from
     source and verifies the result is byte-identical to the developer-published
     `link-clear-v1.0.2.apk`, then publishes the developer signature
     (`AllowedAPKSigningKeys`). Note that R8 minification may require a round or
     two of reproducibility fixes in CI.

5. Address F-Droid CI feedback on the MR (it runs `fdroid lint` and a
   reproducible `fdroid build`). Expect reproducibility iteration (see
   "Reproducibility is the risk to expect" above). Once merged, the app appears
   in the main repo within roughly 24-48 hours.
