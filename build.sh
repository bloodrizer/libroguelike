#!/usr/bin/env bash
# Build serialkiller. Pass --run to launch after a successful build.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

mvn -q package

if [[ "${1:-}" == "--run" ]]; then
  exec "$ROOT/scripts/run.sh"
fi
