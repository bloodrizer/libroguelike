#!/usr/bin/env bash
# Record or replay a session.
#
#   ./scripts/replay.sh record [out.jsonl]        play normally, write a replay on exit
#   ./scripts/replay.sh play <in.jsonl> [out]     drive the game from a replay
#
# Recording is on by default for every run of the game, to replays/MM-DD-HH:MM.jsonl -
# `record` only exists to pin a specific filename. Opt out with -Dreplay.record=false.
#
# `play` runs headless by default so a harness can use it; set SHOW=1 to watch the window.
# Both modes leave a JSONL log of turns, speech, damage, NPC brain state and LLM trace —
# that log, not the screen, is the thing to read afterwards.
#
# Env:
#   SHOW=1        show the window during playback
#   LLM=0         force LLM NPCs off (fast, FSM only)
#   SPEED=1.0     playback rate. >1 fast-forwards the *input*; async inference does not
#                 speed up with it, so a fast replay will outrun the NPC brains.
#   TAIL=600      frames to keep running after the last input (10s at 60fps)
#   DEBUG=...     extra -D flags, e.g. DEBUG="world=ready census=50 path=validate strict=true"
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"
MODE="${1:-}"

[ -f "$JAR" ] || { echo "build first: mvn -o package" >&2; exit 1; }

llm_flag=()
[ "${LLM:-1}" = "0" ] && llm_flag=(-Dllm.enabled=false)

# DEBUG="world=ready census=50" -> -Ddebug.world=ready -Ddebug.census=50. See DebugFlags.
debug_flags=()
for opt in ${DEBUG:-}; do
  debug_flags+=("-Ddebug.${opt}")
done

case "$MODE" in
  record)
    OUT="${2:-replays/$(date +%m-%d-%H:%M).jsonl}"
    mkdir -p "$(dirname "$OUT")"
    echo "recording to $OUT - play, then quit (Esc to menu, or close the window)"
    exec java -Dreplay.record="$OUT" "${llm_flag[@]}" "${debug_flags[@]}" -jar "$JAR"
    ;;
  play)
    IN="${2:-}"
    [ -n "$IN" ] || { echo "usage: $0 play <in.jsonl> [out.jsonl]" >&2; exit 1; }
    OUT="${3:-${IN%.jsonl}-out.jsonl}"
    hidden=(-Dlrl.window.hidden=true)
    [ "${SHOW:-0}" = "1" ] && hidden=()
    echo "replaying $IN -> $OUT"
    exec java \
      -Dreplay.play="$IN" \
      -Dreplay.record="$OUT" \
      -Dreplay.speed="${SPEED:-1.0}" \
      -Dreplay.tailFrames="${TAIL:-600}" \
      -Dreplay.exitAtEnd=true \
      "${hidden[@]}" "${llm_flag[@]}" "${debug_flags[@]}" \
      -jar "$JAR"
    ;;
  *)
    sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
