#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${SKILL_DIR}/../.." && pwd)"
ANDROID_DIR="${REPO_ROOT}/apps/android"
PUBLISH_SCRIPT="${SCRIPT_DIR}/publish_apk_to_smb.sh"
DEFAULT_APK_PATH="${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk"

GRADLE_TASK="assembleDebug"
APK_PATH="${DEFAULT_APK_PATH}"
NO_BUILD=0
PUBLISH_ARGS=()

usage() {
  cat <<'EOF'
Usage: build_and_publish_apk.sh [options] [-- publish-options]

Options:
  --gradle-task TASK     Gradle task to build before publishing, default: assembleDebug
  --apk-path PATH        APK path to publish after build, default: app/build/outputs/apk/debug/app-debug.apk
  --no-build             Skip Gradle build and publish the existing APK directly
  --help                 Show this help

Any remaining arguments are passed to publish_apk_to_smb.sh.
EOF
}

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

detect_sdk_dir() {
  local candidates=()
  [[ -n "${ANDROID_SDK_ROOT:-}" ]] && candidates+=("${ANDROID_SDK_ROOT}")
  [[ -n "${ANDROID_HOME:-}" ]] && candidates+=("${ANDROID_HOME}")
  candidates+=("${HOME}/Library/Android/sdk")
  candidates+=("/opt/homebrew/share/android-commandlinetools")

  local candidate=""
  for candidate in "${candidates[@]}"; do
    if [[ -d "${candidate}" ]]; then
      printf '%s' "${candidate}"
      return 0
    fi
  done
  return 1
}

prepare_android_env() {
  if [[ -f "${ANDROID_DIR}/local.properties" ]]; then
    return 0
  fi

  local sdk_dir=""
  sdk_dir="$(detect_sdk_dir)" || fail "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT, or create apps/android/local.properties."

  export ANDROID_HOME="${sdk_dir}"
  export ANDROID_SDK_ROOT="${sdk_dir}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --gradle-task)
      GRADLE_TASK="$2"
      shift 2
      ;;
    --apk-path)
      APK_PATH="$2"
      shift 2
      ;;
    --no-build)
      NO_BUILD=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --)
      shift
      while [[ $# -gt 0 ]]; do
        PUBLISH_ARGS+=("$1")
        shift
      done
      break
      ;;
    *)
      PUBLISH_ARGS+=("$1")
      shift
      ;;
  esac
done

[[ -x "${PUBLISH_SCRIPT}" ]] || fail "publish script is not executable: ${PUBLISH_SCRIPT}"

if [[ "${NO_BUILD}" -ne 1 ]]; then
  prepare_android_env
  (
    cd "${ANDROID_DIR}"
    ./gradlew "${GRADLE_TASK}"
  )
fi

if [[ ${#PUBLISH_ARGS[@]} -gt 0 ]]; then
  "${PUBLISH_SCRIPT}" --source "${APK_PATH}" "${PUBLISH_ARGS[@]}"
else
  "${PUBLISH_SCRIPT}" --source "${APK_PATH}"
fi
