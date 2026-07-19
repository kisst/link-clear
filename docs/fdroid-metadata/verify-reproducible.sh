#!/usr/bin/env bash
# Verify that a from-source build of Link Clear is byte-for-byte reproducible
# against the developer-published, signed APK on GitHub Releases (F-Droid Path 2).
#
# It compares the two APKs everywhere EXCEPT the v2 APK Signing Block: the ZIP
# local-entries region and the central directory must be byte-identical. That is
# exactly the invariant F-Droid's apksigcopier relies on when it strips the
# unsigned rebuild, reproduces it, and reattaches the developer signature.
#
# Prerequisites:
#   - A JDK 17 + Android SDK (build-tools 35.0.0) on PATH / JAVA_HOME. In this
#     repo: `source .superpowers/sdd/buildenv.sh`.
#   - The build MUST run from a real git checkout at the release tag (NOT a
#     `git archive` export): the app version and the embedded VCS info are
#     derived from git, so a checkout without `.git` produces false diffs.
#
# Usage:
#   docs/fdroid-metadata/verify-reproducible.sh v1.0.1
set -euo pipefail

TAG="${1:?usage: verify-reproducible.sh <tag, e.g. v1.0.1>}"
REPO_URL="https://github.com/kisst/link-clear"
APK_NAME="link-clear-${TAG}.apk"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> Cloning ${REPO_URL} at ${TAG} (with .git, as F-Droid does)"
git clone --quiet "$REPO_URL.git" "$WORK/src"
git -C "$WORK/src" checkout --quiet "$TAG"

echo "==> Building unsigned release (no LINKCLEAR_* signing env)"
env -u LINKCLEAR_KEYSTORE -u LINKCLEAR_STORE_PASSWORD \
    -u LINKCLEAR_KEY_ALIAS -u LINKCLEAR_KEY_PASSWORD \
    "$WORK/src/gradlew" -p "$WORK/src" :app:assembleRelease --quiet
LOCAL="$(find "$WORK/src/app/build/outputs/apk/release" -name '*.apk' | head -1)"

echo "==> Downloading published signed APK: ${APK_NAME}"
curl -fsSL -o "$WORK/$APK_NAME" \
  "$REPO_URL/releases/download/${TAG}/${APK_NAME}"

echo "==> Comparing (ignoring only the APK Signing Block)"
python3 - "$WORK/$APK_NAME" "$LOCAL" <<'PY'
import sys, struct, hashlib
MAGIC = b'APK Sig Block 42'
def parts(data):
    eocd = data.rfind(b'PK\x05\x06')
    cd = struct.unpack('<I', data[eocd+16:eocd+20])[0]
    if data[cd-16:cd] == MAGIC:
        size = struct.unpack('<Q', data[cd-24:cd-16])[0]
        return data[:cd-size-8], data[cd:eocd], data[cd-size-8:cd]  # entries, cd, sigblock
    return data[:cd], data[cd:eocd], b''

# F-Droid rejects any signing-block ID other than the signature/padding blocks —
# in particular AGP's "dependency metadata" (0x504b4453). Flag it here so we catch
# it locally instead of only at F-Droid's `check apk`.
ALLOWED = {0x7109871a: 'v2', 0xf05368c0: 'v3', 0x42726577: 'padding', 0xf3691f37: 'source-stamp'}
def blocks(sigblock):
    if not sigblock:
        return []
    p, out = 8, []
    while p < len(sigblock) - 24:
        ln = struct.unpack('<Q', sigblock[p:p+8])[0]
        out.append(struct.unpack('<I', sigblock[p+8:p+12])[0])
        p += 8 + ln
    return out

pe, pcd, psig = parts(open(sys.argv[1],'rb').read())
le, lcd, _    = parts(open(sys.argv[2],'rb').read())
ok = (pe == le) and (pcd == lcd)
extra = [hex(b) for b in blocks(psig) if b not in ALLOWED]
print("  entries-region SHA256:", hashlib.sha256(pe).hexdigest(), "(published)")
print("  entries-region SHA256:", hashlib.sha256(le).hexdigest(), "(rebuilt)  ")
print("  central-directory identical:", pcd == lcd)
print("  published signing blocks:", [ALLOWED.get(b, hex(b)) for b in blocks(psig)])
if extra:
    print("  ⚠ EXTRA signing block(s) F-Droid will reject:", extra)
    ok = False
print("  RESULT:", "REPRODUCIBLE ✓" if ok else "NOT reproducible ✗")
sys.exit(0 if ok else 1)
PY
