#!/usr/bin/env bash
# Cut a release: derive the app version from a single MAJOR.MINOR.PATCH argument,
# write it into app/build.gradle.kts as inline literals (so F-Droid's static
# checkupdates can read it), commit, and create the matching vX.Y.Z git tag.
#
# The git tag stays the source of truth — this script is the only thing that
# writes the version, and it always writes the file and the tag together, so the
# two can never drift. F-Droid's UpdateCheckMode:Tags + AutoUpdateMode:Version
# then pick up the new tag automatically; no fdroiddata MR is needed per release.
#
# Usage:  ./release.sh 1.0.2
#         ./release.sh 1.0.2 --no-tag     # write+commit only, tag manually later
set -euo pipefail

VER="${1:-}"
NO_TAG="${2:-}"
if ! [[ "$VER" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "usage: $0 <MAJOR.MINOR.PATCH> [--no-tag]   e.g. $0 1.0.2" >&2
  exit 2
fi
MAJOR="${BASH_REMATCH[1]}"; MINOR="${BASH_REMATCH[2]}"; PATCH="${BASH_REMATCH[3]}"
CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))   # monotonic, F-Droid-friendly
TAG="v${VER}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE="$ROOT/app/build.gradle.kts"

# Must be clean so the release commit contains only the version bump.
if [ -n "$(git -C "$ROOT" status --porcelain)" ]; then
  echo "error: working tree not clean; commit or stash first." >&2
  exit 1
fi
if git -C "$ROOT" rev-parse "$TAG" >/dev/null 2>&1; then
  echo "error: tag $TAG already exists." >&2
  exit 1
fi

echo "==> Setting version to $VER (versionCode $CODE)"
# Replace the two inline literals in defaultConfig. Anchored to their exact form
# so nothing else in the file is touched.
sed -i -E \
  -e "s/^([[:space:]]*)versionCode = [0-9]+/\1versionCode = ${CODE}/" \
  -e "s/^([[:space:]]*)versionName = \"[^\"]*\"/\1versionName = \"${VER}\"/" \
  "$GRADLE"

# Verify the write took (both literals present exactly once).
grep -qE "^[[:space:]]*versionCode = ${CODE}\$" "$GRADLE" || { echo "versionCode write failed" >&2; exit 1; }
grep -qE "^[[:space:]]*versionName = \"${VER}\"\$" "$GRADLE" || { echo "versionName write failed" >&2; exit 1; }

# Fastlane changelog for this versionCode (F-Droid matches by versionCode).
CHANGELOG="$ROOT/fastlane/metadata/android/en-US/changelogs/${CODE}.txt"
if [ ! -f "$CHANGELOG" ]; then
  echo "note: no changelog at $CHANGELOG — creating a stub; edit before pushing."
  printf 'Release %s.\n' "$VER" > "$CHANGELOG"
fi

git -C "$ROOT" add "$GRADLE" "$CHANGELOG"
git -C "$ROOT" commit -m "Release $VER"
echo "==> Committed release $VER"

if [ "$NO_TAG" = "--no-tag" ]; then
  echo "==> Skipping tag (--no-tag). Tag manually: git tag -a $TAG -m \"Link Clear $VER\""
else
  git -C "$ROOT" tag -a "$TAG" -m "Link Clear $VER"
  echo "==> Tagged $TAG"
fi

echo
echo "Next: review, then push:"
echo "  git push origin HEAD $TAG"
echo "  gh release create $TAG   # triggers the signed-APK release workflow"
