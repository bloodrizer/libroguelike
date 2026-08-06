#!/usr/bin/env bash
# Render the game offscreen and dump a single frame to a PNG.
#   ./scripts/shot.sh out.png [frame]
# Requires `mvn package` first.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"
OUT="${1:-/tmp/sk-shot.png}"
FRAME="${2:-90}"

exec java \
  -Dlrl.window.hidden=true \
  -Dlrl.capture.file="$OUT" \
  -Dlrl.capture.frame="$FRAME" \
  -Dlrl.capture.exit=true \
  -jar "$JAR"
