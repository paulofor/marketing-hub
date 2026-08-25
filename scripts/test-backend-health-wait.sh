#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

source "${REPO_ROOT}/deploy/bin/backend-health.sh"

SIMULATION=""
CURL_CALLS=0
DIAGNOSTIC_CALLS=0

log() {
  :
}

dump_app_diagnostics() {
  DIAGNOSTIC_CALLS=$((DIAGNOSTIC_CALLS + 1))
}

sleep() {
  :
}

docker() {
  local attempt="${BACKEND_HEALTH_CURRENT_ATTEMPT:-1}"

  case "${SIMULATION}" in
    restart-loop)
      printf 'running|%s|0|false\n' "$((attempt - 1))"
      ;;
    exited)
      printf 'exited|0|1|false\n'
      ;;
    healthy-after-delay)
      printf 'running|0|0|false\n'
      ;;
    *)
      printf 'missing|0|0|false\n'
      ;;
  esac
}

curl() {
  CURL_CALLS=$((CURL_CALLS + 1))
  if [[ "${SIMULATION}" == "healthy-after-delay" ]] \
    && (( BACKEND_HEALTH_CURRENT_ATTEMPT >= 3 )); then
    return 0
  fi
  return 1
}

SIMULATION="restart-loop"
if wait_backend_container_http backend http://local.test 60 5 marketinghub-backend 2 2; then
  printf '[ARQUITETURA] ciclo de reinício não interrompeu o deploy antecipadamente.\n' >&2
  exit 1
fi
if (( BACKEND_HEALTH_CURRENT_ATTEMPT != 3 || DIAGNOSTIC_CALLS != 1 )); then
  printf '[ARQUITETURA] ciclo de reinício não falhou no terceiro diagnóstico.\n' >&2
  exit 1
fi

SIMULATION="exited"
CURL_CALLS=0
DIAGNOSTIC_CALLS=0
if wait_backend_container_http backend http://local.test 60 5 marketinghub-backend 2 2; then
  printf '[ARQUITETURA] container encerrado foi tratado como saudável.\n' >&2
  exit 1
fi
if (( BACKEND_HEALTH_CURRENT_ATTEMPT != 1 || CURL_CALLS != 0 || DIAGNOSTIC_CALLS != 1 )); then
  printf '[ARQUITETURA] container encerrado não falhou imediatamente.\n' >&2
  exit 1
fi

SIMULATION="healthy-after-delay"
CURL_CALLS=0
DIAGNOSTIC_CALLS=0
if ! wait_backend_container_http backend http://local.test 10 5 marketinghub-backend 2 2; then
  printf '[ARQUITETURA] backend estável foi rejeitado.\n' >&2
  exit 1
fi
if (( BACKEND_HEALTH_CURRENT_ATTEMPT != 4 || CURL_CALLS != 4 || DIAGNOSTIC_CALLS != 0 )); then
  printf '[ARQUITETURA] estabilidade não exigiu duas respostas consecutivas.\n' >&2
  exit 1
fi

printf 'Espera inteligente do backend validada.\n'
