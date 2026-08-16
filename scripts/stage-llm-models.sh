#!/usr/bin/env bash
# Stage local LLM models and config for LLM-driven NPCs (see LLM_NPC_SPEC.md §10.1).
# The game does this itself on start (loading screen), so this script is for pre-staging
# offline/CI boxes. Downloads the GGUF models into ./models and seeds ./llm-config.json
# next to the run script. Idempotent: skips files that are already present.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS_DIR="$ROOT/models"
CONFIG_TEMPLATE="$ROOT/serialkiller/src/main/resources/resources/llm/config.json"
CONFIG_OUT="$ROOT/llm-config.json"

# Model paths and urls live in the config template so the script and the in-game
# downloader always stage the same files. Each tier is one line, one object deep.
tier_field() {   # tier_field <tier> <field>
  grep -o "\"$1\":[^}]*" "$CONFIG_TEMPLATE" \
    | grep -o "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
    | head -1 | cut -d'"' -f4
}

mkdir -p "$MODELS_DIR"

fetch() {
  local tier="$1"
  local model url dest
  model="$(tier_field "$tier" model)"
  url="$(tier_field "$tier" url)"
  if [[ -z "$model" || -z "$url" ]]; then
    echo "no model/url for tier '$tier' in $CONFIG_TEMPLATE" >&2
    exit 1
  fi

  dest="$ROOT/$model"
  mkdir -p "$(dirname "$dest")"
  if [[ -f "$dest" ]]; then
    echo "skip: $model already present"
    return
  fi
  echo "downloading $model ..."
  # -C - resumes a partial .part after a dropped connection (these files are multi-GB).
  curl -L --fail --progress-bar -C - -o "$dest.part" "$url"
  mv "$dest.part" "$dest"
  echo "done: $model"
}

fetch reactor
fetch director

if [[ ! -f "$CONFIG_OUT" ]]; then
  cp "$CONFIG_TEMPLATE" "$CONFIG_OUT"
  echo "seeded $CONFIG_OUT (edit it and set \"enabled\": true to activate LLM NPCs)"
else
  echo "skip: $CONFIG_OUT already present"
fi

echo
echo "Models staged in $MODELS_DIR."
echo "Install llama.cpp so 'llama-server' is on PATH, then edit llm-config.json."
