#!/usr/bin/env bash
# Launch serialkiller from the Maven build output. Run `mvn package` first.
#
# LWJGL 2.7.1 ships natives only for linux + windows in this tree —
# macOS/arm64 will fail to load with UnsatisfiedLinkError. That's a
# Milestone 2 problem (LWJGL 3 + GLFW). Keep this script simple.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/serialkiller/target/serialkiller-0.1.0-SNAPSHOT.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Jar not found: $JAR" >&2
  echo "Run: mvn package" >&2
  exit 1
fi

case "$(uname -s)" in
  Linux*)   NATIVE_DIR="$ROOT/libroguelike/lib/lwjgl-2.7.1/native/linux" ;;
  MINGW*|CYGWIN*|MSYS*) NATIVE_DIR="$ROOT/libroguelike/lib/lwjgl-2.7.1/native/windows" ;;
  Darwin*)  NATIVE_DIR=""
            echo "WARN: no LWJGL 2.7.1 macOS natives present in this repo." >&2
            echo "      Display.create() will fail. See PORTING.md M2." >&2
            ;;
  *)        NATIVE_DIR="" ;;
esac

JAVA_OPTS=()
if [[ -n "$NATIVE_DIR" ]]; then
  JAVA_OPTS+=("-Djava.library.path=$NATIVE_DIR")
  JAVA_OPTS+=("-Dorg.lwjgl.librarypath=$NATIVE_DIR")
fi

exec java ${JAVA_OPTS[@]+"${JAVA_OPTS[@]}"} -jar "$JAR" "$@"
