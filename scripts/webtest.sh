#!/usr/bin/env bash
# Load the wasm build in headless Chromium and report whether it ran.
#
#   ./scripts/webtest.sh                  # boot check, 15s of virtual time
#   ./scripts/webtest.sh 30               # give it longer
#   ./scripts/webtest.sh 20 shot.png      # also dump a screenshot
#   ./scripts/webtest.sh 20 shot.png js   # use the JS build (mvn -Pdebug-js),
#                                         # whose traces keep Java frames
#
# Requires: mvn -pl web package
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WEB="$ROOT/web/target/web"
BUDGET_MS=$(( ${1:-15} * 1000 ))
SHOT="${2:-}"
PAGE="${3:-wasm}"
PORT=8731

if [[ "$PAGE" == "js" ]]; then
  ENTRY=debug.html
  [[ -f "$WEB/serialkiller.js" ]] || { echo "no JS build; run: mvn -pl web -Pdebug-js package"; exit 1; }
else
  ENTRY=index.html
  [[ -f "$WEB/serialkiller.wasm" ]] || { echo "no wasm build; run: mvn -pl web package"; exit 1; }
fi

python3 -m http.server "$PORT" --directory "$WEB" >/dev/null 2>&1 &
SRV=$!
trap 'kill $SRV 2>/dev/null || true' EXIT

# Sandboxed runs need the listener up before Chromium starts, or it lands on an error page.
for _ in $(seq 1 40); do
  curl -s -o /dev/null "http://127.0.0.1:$PORT/$ENTRY" && break
  sleep 0.25
done
curl -sf -o /dev/null "http://127.0.0.1:$PORT/$ENTRY" || { echo "server failed to start"; exit 1; }

DOM="$(mktemp)"
LOG="$(mktemp)"
ARGS=(
  --headless=new --disable-gpu --use-gl=swiftshader --enable-unsafe-swiftshader
  --no-sandbox --enable-logging=stderr
  --window-size=1060,860
  --virtual-time-budget="$BUDGET_MS"
  --dump-dom
)
[[ -n "$SHOT" ]] && ARGS+=(--screenshot="$SHOT")

chromium "${ARGS[@]}" "http://127.0.0.1:$PORT/$ENTRY" >"$DOM" 2>"$LOG" || true

STATUS="$(python3 - "$DOM" <<'PY'
import re, sys, html
page = open(sys.argv[1], encoding='utf-8', errors='replace').read()
m = re.search(r'<div id="status"[^>]*>(.*?)</div>', page, re.S)
print(html.unescape(re.sub('<[^>]+>', '', m.group(1))).strip() if m else '<no #status found>')
PY
)"

# Game logs are noisy (one line per spawned entity); show what a failure needs.
echo "--- page console (filtered) ---"
grep -a "CONSOLE" "$LOG" \
  | sed 's/.*CONSOLE:[0-9]*\] //;s/, source:.*//' \
  | grep -vE "Spawning entity|Spawning ladder|precache font" \
  | tail -15 || true

echo "--- #status ---"
echo "$STATUS"
[[ -n "$SHOT" ]] && echo "--- screenshot: $SHOT ---"

case "$STATUS" in
  *"BOOT FAILED"*|*"FAILED at"*|*"loading"*|*"starting"*|*"<no #status"*)
    echo "RESULT: FAIL"; exit 1 ;;
  *) echo "RESULT: PASS" ;;
esac
