#!/bin/bash
set -euo pipefail

git fetch --quiet origin main

has_test_target=false
while IFS= read -r path; do
  case "$path" in
    src/main/kotlin/*.kt|src/main/kotlin/**/*.kt|src/test/kotlin/*.kt|src/test/kotlin/**/*.kt|src/main/resources/*|src/main/resources/**/*|build.gradle.kts|gradle/libs.versions.toml)
      has_test_target=true
      break
      ;;
  esac
done < <(git diff --name-only --diff-filter=ACMR origin/main...HEAD)

if [ "$has_test_target" = false ]; then
  echo "No test target files changed; skipping."
  exit 0
fi

./gradlew test --quiet --configuration-cache
