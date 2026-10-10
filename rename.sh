#!/usr/bin/env bash
# Rename the template to your app. Run once, from the repo root, before the first commit.
#
#   ./rename.sh <application.id> "<Display Name>" [github-owner]
#   ./rename.sh com.acme.myapp "My App" acme-org
#
# The Kotlin class prefix (StarterApplication, StarterApi, ...) is derived from the display name
# by dropping spaces, so it must contain only letters, digits and spaces. The Gradle root project
# name is the display name lower-cased with hyphens. Pass a GitHub user or team to fill CODEOWNERS.
set -euo pipefail

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "usage: $0 <application.id> \"<Display Name>\" [github-owner]" >&2
  exit 1
fi
APP_ID="$1"; NAME="$2"; OWNER="${3:-}"
CLASS="${NAME// /}"
SLUG="$(echo "$NAME" | tr 'A-Z ' 'a-z-')"

[[ "$APP_ID" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || { echo "invalid application id (lowercase, dot separated)" >&2; exit 1; }
[[ "$CLASS" =~ ^[A-Za-z][A-Za-z0-9]*$ ]] || { echo "display name must be letters, digits and spaces" >&2; exit 1; }
[[ -z "$OWNER" || "$OWNER" =~ ^[A-Za-z0-9/_-]+$ ]] || { echo "invalid github owner" >&2; exit 1; }

# Order matters: the package id and the display name both contain the class-prefix token.
FILES=$(grep -rlE 'com\.example\.starterapp|Starter App|starter-app|Starter' . \
  --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle --exclude=rename.sh \
  --exclude=gradle.lockfile --exclude=gradle-wrapper.jar || true)
for f in $FILES; do
  sed -i \
    -e "s|com\.example\.starterapp|${APP_ID}|g" \
    -e "s|Starter App|${NAME}|g" \
    -e "s|starter-app|${SLUG}|g" \
    -e "s|Starter|${CLASS}|g" "$f"
done
if [ -n "$OWNER" ]; then
  sed -i "s|@OWNER|@${OWNER}|g" .github/CODEOWNERS
fi

# Rename files whose names carry the class prefix.
for f in $(find app/src -type f -name 'Starter*'); do
  mv "$f" "$(dirname "$f")/$(basename "$f" | sed "s|^Starter|${CLASS}|")"
done

# Move each source set's package directory to match the new package id.
NEW_PATH="$(echo "$APP_ID" | tr . /)"
for set in main test androidTest; do
  OLD_DIR="app/src/$set/kotlin/com/example/starterapp"
  NEW_DIR="app/src/$set/kotlin/$NEW_PATH"
  if [ -d "$OLD_DIR" ] && [ "$OLD_DIR" != "$NEW_DIR" ]; then
    mkdir -p "$NEW_DIR"
    cp -r "$OLD_DIR"/. "$NEW_DIR"/
    rm -rf "$OLD_DIR"
    rmdir "app/src/$set/kotlin/com/example" 2>/dev/null || true
    rmdir "app/src/$set/kotlin/com" 2>/dev/null || true
  fi
done

echo "Renamed to ${NAME} (${APP_ID}). Then run:"
echo "  ./gradlew :app:dependencies --write-locks   # app/gradle.lockfile has no project-specific entries, but regenerate after any dependency change"
echo "  ./gradlew ktlintCheck testDebugUnitTest assembleDebug"
echo "Fill in LICENSE ([YEAR] [COPYRIGHT HOLDER]) and delete this script when done."
