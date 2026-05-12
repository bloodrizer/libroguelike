#!/usr/bin/env bash
# Launch serialkiller from the Maven build output. Run `mvn package` first.
# LWJGL 3 ships natives inside classifier jars on the classpath, so no
# -Djava.library.path is needed.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Jar not found: $JAR" >&2
  echo "Run: mvn package" >&2
  exit 1
fi

JAVA_OPTS=()
case "$(uname -s)" in
  Darwin*)
    # GLFW must run on the macOS main thread.
    JAVA_OPTS+=("-XstartOnFirstThread")
    # Stop AWT from installing NSApplicationAWT on the main thread runloop —
    # the game uses AWT only for offscreen font rasterisation, not UI.
    JAVA_OPTS+=("-Dapple.awt.UIElement=true")
    JAVA_OPTS+=("-Djava.awt.headless=true")
    ;;
esac

exec java ${JAVA_OPTS[@]+"${JAVA_OPTS[@]}"} -jar "$JAR" "$@"
