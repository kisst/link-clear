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

- `commit: v1.0.1` pins the build to the release tag. The app version is derived
  entirely from that tag (`app/build.gradle.kts`): `versionName` is the tag
  minus the leading `v` (`1.0.1`), and `versionCode` is
  `MAJOR*10000 + MINOR*100 + PATCH` (`10001`), so it stays monotonic across
  releases as F-Droid requires.
- `Binaries: .../releases/download/v%v/link-clear-v%v.apk` points F-Droid at the
  developer-published APK to reproduce. `%v` expands to the versionName, which
  equals the tag (`v1.0.1`), so it resolves to
  `link-clear-v1.0.1.apk` — the exact asset the release workflow uploads.
- `AllowedAPKSigningKeys: 7e489f6b…057e` is the SHA-256 of this project's release
  signing certificate. **Verified against the actual published
  `link-clear-v1.0.1.apk`** with
  `apksigner verify --print-certs` (the APK is signed with the v2 scheme only).
  F-Droid will only publish the developer signature if the rebuilt APK matches
  and carries this key.
- `AutoUpdateMode: Version v%v` — the tag and `versionName` agree
  (`v1.0.1` <-> `1.0.1`), so `%v` alone reconstructs the tag.
- `UpdateCheckMode: Tags` watches the repo's git tags for future releases.
  Because versionCode is derived from the tag, every new `vX.Y.Z` tag bumps it
  automatically — no manual edit needed.

### Reproducibility is the risk to expect

Path 2 only succeeds if F-Droid's from-source rebuild is byte-identical to the
published APK before signing. The release build enables R8
(`isMinifyEnabled` + `isShrinkResources`, `app/build.gradle.kts`), whose output
is not guaranteed reproducible across environments, and the v2 signature covers
all APK bytes. Expect the F-Droid CI `fdroid build` to need one or more rounds of
adjustment (pinning toolchain versions, and possibly relaxing R8) before it
reproduces. This is normal for Path 2 and is worked out in the MR thread.

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
  `link-clear-v1.0.1.apk` with `apksigner verify --print-certs`.

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
     `link-clear-v1.0.1.apk`, then publishes the developer signature
     (`AllowedAPKSigningKeys`). Note that R8 minification may require a round or
     two of reproducibility fixes in CI.

5. Address F-Droid CI feedback on the MR (it runs `fdroid lint` and a
   reproducible `fdroid build`). Expect reproducibility iteration (see
   "Reproducibility is the risk to expect" above). Once merged, the app appears
   in the main repo within roughly 24-48 hours.
