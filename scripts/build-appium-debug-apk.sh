#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk
fi

if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ] && [ -f local.properties ]; then
  sdk_dir="$(awk -F= '$1 == "sdk.dir" { print $2 }' local.properties | tail -n 1)"
  if [ -n "$sdk_dir" ]; then
    export ANDROID_HOME="$sdk_dir"
    export ANDROID_SDK_ROOT="$sdk_dir"
  fi
fi

if [ -n "${ANDROID_HOME:-}" ]; then
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
fi

./gradlew assembleDebug

apk_path="$repo_root/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$apk_path" ]; then
  echo "Debug APK was not generated at $apk_path" >&2
  exit 1
fi

echo "$apk_path"
