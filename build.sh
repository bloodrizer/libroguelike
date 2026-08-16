#!/usr/bin/env bash
# Build serialkiller. Pass --run to launch after a successful build.
#
# By default only the desktop modules build; the wasm build costs an extra
# TeaVM pass, so ask for it explicitly:
#   ./build.sh --web    # also produce web/target/web (serve it, or scripts/webtest.sh)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [[ "${1:-}" == "--web" ]]; then
  mvn -q package
  echo "web build: $ROOT/web/target/web (python3 -m http.server --directory that)"
  exit 0
fi

mvn -q -pl libroguelike,serialkiller package

if [[ "${1:-}" == "--run" ]]; then
  exec "$ROOT/scripts/run.sh"
fi
