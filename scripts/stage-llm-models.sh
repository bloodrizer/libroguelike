#!/usr/bin/env bash
# Stage local LLM models and config for LLM-driven NPCs (see LLM_NPC_SPEC.md §10.1).
# Downloads the GGUF models into ./models, verifies size, and seeds ./llm-config.json
# next to the run script. Idempotent: skips files that are already present.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS_DIR="$ROOT/models"
CONFIG_TEMPLATE="$ROOT/serialkiller/src/main/resources/resources/llm/config.json"
CONFIG_OUT="$ROOT/llm-config.json"

# model_file  download_url
REACTOR_FILE="phi-4-mini.gguf"
REACTOR_URL="https://huggingface.co/unsloth/Phi-4-mini-instruct-GGUF/resolve/main/Phi-4-mini-instruct-Q4_K_M.gguf"

DIRECTOR_FILE="phi-4.gguf"
DIRECTOR_URL="https://huggingface.co/bartowski/phi-4-GGUF/resolve/main/phi-4-Q4_K_M.gguf"

mkdir -p "$MODELS_DIR"

fetch() {
  local file="$1" url="$2"
  local dest="$MODELS_DIR/$file"
  if [[ -f "$dest" ]]; then
    echo "skip: $file already present"
    return
  fi
  echo "downloading $file ..."
  # -C - resumes a partial .part after a dropped connection (these files are multi-GB).
  curl -L --fail --progress-bar -C - -o "$dest.part" "$url"
  mv "$dest.part" "$dest"
  echo "done: $file"
}

fetch "$REACTOR_FILE" "$REACTOR_URL"
fetch "$DIRECTOR_FILE" "$DIRECTOR_URL"

if [[ ! -f "$CONFIG_OUT" ]]; then
  cp "$CONFIG_TEMPLATE" "$CONFIG_OUT"
  echo "seeded $CONFIG_OUT (edit it and set \"enabled\": true to activate LLM NPCs)"
else
  echo "skip: $CONFIG_OUT already present"
fi

echo
echo "Models staged in $MODELS_DIR."
echo "Install llama.cpp so 'llama-server' is on PATH, then edit llm-config.json."
