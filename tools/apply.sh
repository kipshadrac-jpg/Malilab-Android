#!/usr/bin/env bash
# Apply a patch zip to this repository, commit it, and push (GitHub then builds the APK).
#   tools/apply.sh ~/malilab_android_patch02.zip "Short message"
# A patch zip holds files at the same paths as in the repo (no extra top folder).
# An optional DELETE.txt in the zip lists files to remove, one path per line.
set -euo pipefail

ZIP="${1:-}"
[ -n "$ZIP" ] && [ -f "$ZIP" ] || { echo "Usage: tools/apply.sh <patch.zip> [\"commit message\"]"; exit 1; }
MSG="${2:-Apply $(basename "$ZIP" .zip)}"

cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

unzip -q "$ZIP" -d "$TMP"

# Refuse anything that could write outside the repository.
if find "$TMP" -mindepth 1 \( -name '.git' -o -path '*/.git/*' \) | grep -q .; then echo "Patch contains .git; refusing."; exit 1; fi
if unzip -Z1 "$ZIP" | grep -Eq '(^/|(^|/)\.\.(/|$))'; then echo "Patch has unsafe paths; refusing."; exit 1; fi

if [ -f "$TMP/DELETE.txt" ]; then
  while IFS= read -r path || [ -n "$path" ]; do
    [ -z "$path" ] && continue
    case "$path" in /*|*..*|.git*) echo "Skipping unsafe delete path: $path"; continue;; esac
    rm -rf -- "./$path"
    echo "removed $path"
  done < "$TMP/DELETE.txt"
  rm -f "$TMP/DELETE.txt"
fi

cp -a "$TMP"/. .
git add -A
if git diff --cached --quiet; then
  echo "Nothing changed. Nothing to commit."
  exit 0
fi
git commit -q -m "$MSG"
git push origin HEAD:main
echo
git log -1 --stat --format='Committed: %h %s' | head -20
echo
echo "Pushed. GitHub is building the APK now (about 3-6 minutes):"
echo "  https://github.com/kipshadrac-jpg/Malilab-Android/actions"
