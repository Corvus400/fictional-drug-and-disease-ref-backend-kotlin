#!/bin/bash
# pre-push stage で detektMain 対象の差分がある時だけ実行する。
set -euo pipefail

git fetch --quiet origin main

has_detekt_target=false
while IFS= read -r path; do
  case "$path" in
    src/main/kotlin/*.kt|src/main/kotlin/**/*.kt|config/detekt/*|config/detekt/**/*|build.gradle.kts|gradle/libs.versions.toml)
      has_detekt_target=true
      break
      ;;
  esac
done < <(git diff --name-only --diff-filter=ACMR origin/main...HEAD)

if [ "$has_detekt_target" = false ]; then
  echo "No detektMain target files changed; skipping."
  exit 0
fi

./gradlew detektMain -Pprecommit=true --quiet --configuration-cache
