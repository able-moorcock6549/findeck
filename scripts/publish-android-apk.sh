#!/bin/zsh
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

exec "$REPO_ROOT/skills/apk-smb-sync/scripts/build_and_publish_apk.sh" "$@"
