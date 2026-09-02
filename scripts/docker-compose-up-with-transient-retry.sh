#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "Uso: $0 <argumentos-do-docker-compose>" >&2
  exit 2
fi

compose_up_max_attempts="${DOCKER_COMPOSE_UP_MAX_ATTEMPTS:-2}"
compose_up_retry_delay_seconds="${DOCKER_COMPOSE_UP_RETRY_DELAY_SECONDS:-15}"
compose_up_timeout_seconds="${DOCKER_COMPOSE_UP_TIMEOUT_SECONDS:-300}"
compose_up_force_recreate="${DOCKER_COMPOSE_UP_FORCE_RECREATE:-false}"

if ! [[ "$compose_up_max_attempts" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_COMPOSE_UP_MAX_ATTEMPTS deve ser um inteiro positivo." >&2
  exit 2
fi

if ! [[ "$compose_up_retry_delay_seconds" =~ ^[0-9]+$ ]]; then
  echo "DOCKER_COMPOSE_UP_RETRY_DELAY_SECONDS deve ser um inteiro não negativo." >&2
  exit 2
fi

if ! [[ "$compose_up_timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_COMPOSE_UP_TIMEOUT_SECONDS deve ser um inteiro positivo." >&2
  exit 2
fi

if [[ "$compose_up_force_recreate" != "true" && "$compose_up_force_recreate" != "false" ]]; then
  echo "DOCKER_COMPOSE_UP_FORCE_RECREATE deve ser true ou false." >&2
  exit 2
fi

compose_output="$(mktemp)"
trap 'rm -f "$compose_output"' EXIT

is_transient_docker_failure() {
  grep -Eiq \
    'context deadline exceeded|Client\.Timeout|request canceled|connection reset|connection refused|i/o timeout|temporary failure|unexpected EOF|502 Bad Gateway|503 Service Unavailable|504 Gateway Timeout|the Docker daemon is not responding' \
    "$compose_output"
}

for ((attempt = 1; attempt <= compose_up_max_attempts; attempt++)); do
  compose_up_arguments=(up -d --remove-orphans)
  if [[ "$compose_up_force_recreate" == "true" && "$attempt" -eq 1 ]]; then
    compose_up_arguments=(up -d --force-recreate --remove-orphans)
  fi

  : >"$compose_output"
  set +e
  timeout --foreground --kill-after=30s "${compose_up_timeout_seconds}s" \
    docker compose "$@" "${compose_up_arguments[@]}" \
    >"$compose_output" 2>&1
  compose_status="$?"
  set -e

  if [[ "$compose_status" -eq 0 ]]; then
    cat "$compose_output"
    exit 0
  fi

  cat "$compose_output" >&2
  if [[ "$compose_status" -ne 124 ]] && ! is_transient_docker_failure; then
    echo "Falha não transitória ao reconciliar o Docker Compose; a execução não será repetida." >&2
    exit 1
  fi

  if ((attempt == compose_up_max_attempts)); then
    echo "A reconciliação do Docker Compose não concluiu após ${attempt} tentativas." >&2
    exit 1
  fi

  echo "A reconciliação Docker foi interrompida por pressão transitória; nova tentativa $((attempt + 1))/${compose_up_max_attempts} sem forçar nova recriação." >&2
  sleep "$compose_up_retry_delay_seconds"
done
