#!/bin/sh
set -e

# Prefer explicit environment variable and fall back to a mounted secret file.
if [ -z "${OPENAI_API_KEY:-}" ]; then
  KEY_FILE="${OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}"
  if [ -r "$KEY_FILE" ]; then
    OPENAI_API_KEY="$(cat "$KEY_FILE")"
    export OPENAI_API_KEY
  fi
fi

if [ -z "${OPENAI_API_KEY:-}" ] && [ "${OPENAI_TEXT_TRANSPORT:-responses-api}" != "codex-app-server" ]; then
  echo "ERROR: OPENAI_API_KEY not set and no readable key file found" >&2
  exit 1
fi

exec java -jar /app/app.jar
