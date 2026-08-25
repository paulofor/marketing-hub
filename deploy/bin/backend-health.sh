#!/usr/bin/env bash

backend_health_log() {
  if declare -F log >/dev/null 2>&1; then
    log "$*"
  else
    printf '[%s] [backend-health] %s\n' "$(date -Is)" "$*"
  fi
}

backend_health_diagnostics() {
  if declare -F dump_app_diagnostics >/dev/null 2>&1; then
    dump_app_diagnostics
  else
    docker inspect marketinghub-backend \
      --format 'status={{.State.Status}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}} reinicios={{.RestartCount}}' \
      >&2 || true
    docker logs --tail 200 marketinghub-backend >&2 || true
  fi
}

wait_backend_container_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-60}"
  local interval="${4:-5}"
  local container="${5:-marketinghub-backend}"
  local max_restarts="${6:-2}"
  local successes_required="${7:-2}"
  local consecutive_successes=0
  local attempt
  local state_line
  local status
  local restart_count
  local exit_code
  local oom_killed

  backend_health_log "Validando ${name} em ${url}"

  for attempt in $(seq 1 "${attempts}"); do
    BACKEND_HEALTH_CURRENT_ATTEMPT="${attempt}"
    state_line="$(docker inspect "${container}" \
      --format '{{.State.Status}}|{{.RestartCount}}|{{.State.ExitCode}}|{{.State.OOMKilled}}' \
      2>/dev/null || printf 'missing|0|0|false')"
    IFS='|' read -r status restart_count exit_code oom_killed <<< "${state_line}"

    if [[ "${oom_killed}" == "true" || "${status}" == "dead" || "${status}" == "exited" ]]; then
      backend_health_log \
        "${name} encerrou antes de ficar saudável (status=${status}, exit=${exit_code}, oom=${oom_killed}, reinícios=${restart_count})."
      backend_health_diagnostics
      return 1
    fi

    if [[ "${restart_count}" =~ ^[0-9]+$ ]] && (( restart_count >= max_restarts )); then
      backend_health_log \
        "${name} entrou em ciclo de reinício (status=${status}, reinícios=${restart_count}, limite=${max_restarts})."
      backend_health_diagnostics
      return 1
    fi

    if [[ "${status}" == "running" ]] && curl -fsS --max-time 5 "${url}" >/dev/null; then
      consecutive_successes=$((consecutive_successes + 1))
      if (( consecutive_successes >= successes_required )); then
        backend_health_log \
          "${name} respondeu ${consecutive_successes} vezes consecutivas (reinícios=${restart_count})."
        return 0
      fi
      backend_health_log \
        "${name} respondeu; confirmando estabilidade (${consecutive_successes}/${successes_required})."
    else
      consecutive_successes=0
      backend_health_log \
        "Aguardando ${name} responder (${attempt}/${attempts}, status=${status}, reinícios=${restart_count})."
    fi

    if (( attempt < attempts )); then
      sleep "${interval}"
    fi
  done

  backend_health_log "${name} não respondeu de forma estável em ${url}"
  backend_health_diagnostics
  return 1
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  set -euo pipefail
  wait_backend_container_http \
    "backend" \
    "${1:-http://localhost:8000/ops-mh-observability-v2/health}" \
    "${BACKEND_HEALTH_ATTEMPTS:-60}" \
    "${BACKEND_HEALTH_INTERVAL:-5}" \
    "${BACKEND_CONTAINER_NAME:-marketinghub-backend}" \
    "${BACKEND_MAX_RESTARTS:-2}" \
    "${BACKEND_HEALTH_SUCCESSES_REQUIRED:-2}"
fi
