#!/usr/bin/env bash
# Bake the sprite atlas offscreen and dump it to PNGs — no GL, no game loop.
#   ./scripts/atlas.sh [out.png] [cell] [zoom]
# Writes out.png (the raw atlas) and out.sheet.png (a labelled contact sheet).
# Requires `mvn package` first.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"
OUT="${1:-/tmp/sk-atlas.png}"
CELL="${2:-32}"
ZOOM="${3:-4}"

exec java -cp "$JAR" \
  com.nuclearunicorn.serialkiller.render.AtlasDump "$OUT" "$CELL" "$ZOOM"
