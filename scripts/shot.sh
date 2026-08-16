#!/usr/bin/env bash
# Render the game offscreen and dump a single frame to a PNG.
#   ./scripts/shot.sh out.png [frame] [-Dextra=...]
# Requires `mvn package` first. LLM NPCs are forced off so the shot never waits on
# model staging or a llama-server boot.
#
# LRL_SEED fixes the town so two builds can be compared frame for frame:
#   LRL_SEED=7 ./scripts/shot.sh before.png
# LRL_OPTS passes anything else through, e.g. LRL_OPTS="-Dlrl.reveal=true -Dlrl.cell=40".
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"
OUT="${1:-/tmp/sk-shot.png}"
FRAME="${2:-90}"
shift $(( $# > 2 ? 2 : $# ))

OPTS=()
if [[ -n "${LRL_SEED:-}" ]]; then
  OPTS+=("-Dlrl.seed=$LRL_SEED")
fi
if [[ -n "${LRL_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  OPTS+=(${LRL_OPTS})
fi

exec java \
  -Dlrl.window.hidden=true \
  -Dllm.enabled=false \
  -Dreplay.record=false \
  -Dlrl.capture.file="$OUT" \
  -Dlrl.capture.frame="$FRAME" \
  -Dlrl.capture.exit=true \
  ${OPTS[@]+"${OPTS[@]}"} "$@" \
  -jar "$JAR"
