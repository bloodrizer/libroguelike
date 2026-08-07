#!/usr/bin/env bash
# Render the game offscreen and dump a single frame to a PNG.
#   ./scripts/shot.sh out.png [frame]
# Requires `mvn package` first. LLM NPCs are forced off so the shot never waits on
# model staging or a llama-server boot.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"
OUT="${1:-/tmp/sk-shot.png}"
FRAME="${2:-90}"

exec java \
  -Dlrl.window.hidden=true \
  -Dllm.enabled=false \
  -Dlrl.capture.file="$OUT" \
  -Dlrl.capture.frame="$FRAME" \
  -Dlrl.capture.exit=true \
  -jar "$JAR"
