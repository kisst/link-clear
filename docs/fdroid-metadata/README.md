# fdroiddata MR — ready-to-submit metadata

This directory holds the F-Droid build recipe for Link Clear, prepared for a
merge request against [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata). The
file [`app.linkclear.yml`](app.linkclear.yml) here is the exact content that
goes into `metadata/app.linkclear.yml` **in the fdroiddata repo** — it does not
belong in this app repo's build, it lives here only as the prepared artifact.

For the background (which inclusion path, anti-features, reproducible-builds
follow-up), see [../fdroid-submission.md](../fdroid-submission.md).

## What this recipe does

- Builds **from source** on F-Droid's server (Path 1): F-Droid compiles the
  `:app` module at tag `v1.0.1` and signs the APK with its own key. The app's
  signing config leaves the release **unsigned** when the `LINKCLEAR_*` env
  vars are absent (`app/build.gradle.kts`), which is exactly what F-Droid needs.
- `commit: v1.0.1` pins the first build to the release tag. The app version is
  derived entirely from that tag (`app/build.gradle.kts`): `versionName` is the
  tag minus the leading `v` (`1.0.1`), and `versionCode` is
  `MAJOR*10000 + MINOR*100 + PATCH` (`10001`), so it stays monotonic across
  releases as F-Droid requires.
- `AutoUpdateMode: Version v%v` — the tag and `versionName` now agree
  (`v1.0.1` <-> `1.0.1`), so `%v` alone reconstructs the tag; no `.0` bridge is
  needed.
- `UpdateCheckMode: Tags` watches the repo's git tags for future releases.
  Because versionCode is derived from the tag, every new `vX.Y.Z` tag bumps it
  automatically — no manual edit needed.

The **store listing** (title, descriptions, screenshots, icon) is NOT in this
file — F-Droid reads it from the Fastlane tree at `fastlane/metadata/android/`
on the repo's default branch, which is already committed.

## Validation status

This recipe was checked with `fdroidserver` 2.4.5 inside a real `fdroiddata`
clone (its `config/categories.yml` is needed for category validation):

- `fdroid lint app.linkclear` -> **clean** (exit 0). `Internet` and `Security`
  are valid categories in the current F-Droid category set.
- `fdroid rewritemeta app.linkclear` -> **clean**; the file here is already in
  canonical form (single-quoted version strings), so it will not be rewritten.

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
   - **Build note:** builds unsigned without secrets (Path 1); F-Droid signs it.

5. Address F-Droid CI feedback on the MR (it runs `fdroid lint` and a build).
   Once merged, the app appears in the main repo within roughly 24-48 hours.

### Reproducible-builds follow-up (later)

To have F-Droid publish *your* signature so Obtainium and F-Droid users share
one key, add these to the recipe once the from-source build is green (see
[../fdroid-submission.md](../fdroid-submission.md) for the caveats):

```yaml
Binaries: https://github.com/kisst/link-clear/releases/download/v%v/link-clear-v%v.apk
AllowedAPKSigningKeys: 7e489f6b0342db48ebcdb30312965e840589f1a335438046aefc2dfc548d057e
```

The `AllowedAPKSigningKeys` value is this project's release signing-cert SHA-256
(verified against the published v1.0.0 APK; the key is unchanged for v1.0.1).
