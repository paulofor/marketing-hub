#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "Uso: $0 <argumentos-do-docker-compose>" >&2
  exit 2
fi

DOCKER_COMPOSE_PULL_MAX_ATTEMPTS="${DOCKER_COMPOSE_PULL_MAX_ATTEMPTS:-3}"
DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS="${DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS:-10}"

if ! [[ "${DOCKER_COMPOSE_PULL_MAX_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_COMPOSE_PULL_MAX_ATTEMPTS deve ser um inteiro positivo." >&2
  exit 2
fi

if ! [[ "${DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS deve ser um inteiro não negativo." >&2
  exit 2
fi

pull_output="$(mktemp)"
trap 'rm -f "$pull_output"' EXIT

is_transient_registry_failure() {
  grep -Eiq \
    'TLS handshake timeout|context deadline exceeded|Client\.Timeout|request canceled|connection reset|i/o timeout|temporary failure|unexpected EOF|too many requests|429|502 Bad Gateway|503 Service Unavailable' \
    "$pull_output"
}

for ((attempt = 1; attempt <= DOCKER_COMPOSE_PULL_MAX_ATTEMPTS; attempt++)); do
  : >"$pull_output"
  if docker compose "$@" pull >"$pull_output" 2>&1; then
    cat "$pull_output"
    exit 0
  fi
  cat "$pull_output" >&2

  if ! is_transient_registry_failure; then
    echo "Falha não transitória ao baixar as imagens do Docker Compose; a execução não será repetida." >&2
    exit 1
  fi

  if ((attempt == DOCKER_COMPOSE_PULL_MAX_ATTEMPTS)); then
    echo "Falha transitória persistiu após ${attempt} tentativas de download pelo Docker Compose." >&2
    exit 1
  fi

  echo "Falha transitória no registry; nova tentativa $((attempt + 1))/${DOCKER_COMPOSE_PULL_MAX_ATTEMPTS}." >&2
  sleep "${DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS}"
done
