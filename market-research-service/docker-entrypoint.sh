#!/usr/bin/env sh
set -euo pipefail

if [ -z "${OPENAI_API_KEY:-}" ]; then
  KEY_FILE="${OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}"
  if [ -r "$KEY_FILE" ]; then
    export OPENAI_API_KEY="$(cat "$KEY_FILE")"
  fi
fi

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "[warn] OPENAI_API_KEY não definido. As chamadas para IA irão falhar." >&2
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar
