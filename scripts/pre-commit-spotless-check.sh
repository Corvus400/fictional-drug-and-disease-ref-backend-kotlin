#!/bin/bash
# pre-commit stage で Spotless 対象の staged file がある時だけ実行する。
set -euo pipefail

has_spotless_target=false
while IFS= read -r path; do
  case "$path" in
    src/*.kt|src/**/*.kt|*.gradle.kts)
      has_spotless_target=true
      break
      ;;
  esac
done < <(git diff --cached --name-only --diff-filter=ACMR)

if [ "$has_spotless_target" = false ]; then
  echo "No Spotless target files changed; skipping."
  exit 0
fi

git fetch --quiet origin main
./gradlew spotlessCheck -Pspotless.ratchet=true --quiet --configuration-cache
