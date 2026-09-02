#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 2 || -z "$1" || -z "$2" ]]; then
  echo "Uso: $0 <registry> <usuario>" >&2
  exit 2
fi

registry="$1"
username="$2"
docker_login_max_attempts="${DOCKER_LOGIN_MAX_ATTEMPTS:-3}"
docker_login_retry_delay_seconds="${DOCKER_LOGIN_RETRY_DELAY_SECONDS:-10}"
docker_login_timeout_seconds="${DOCKER_LOGIN_TIMEOUT_SECONDS:-90}"

if ! [[ "$docker_login_max_attempts" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_LOGIN_MAX_ATTEMPTS deve ser um inteiro positivo." >&2
  exit 2
fi

if ! [[ "$docker_login_retry_delay_seconds" =~ ^[0-9]+$ ]]; then
  echo "DOCKER_LOGIN_RETRY_DELAY_SECONDS deve ser um inteiro não negativo." >&2
  exit 2
fi

if ! [[ "$docker_login_timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_LOGIN_TIMEOUT_SECONDS deve ser um inteiro positivo." >&2
  exit 2
fi

password=""
IFS= read -r password || true
if [[ -z "$password" ]]; then
  echo "A credencial do registry deve ser informada pela entrada padrão." >&2
  exit 2
fi

login_output="$(mktemp)"
trap 'rm -f "$login_output"' EXIT

is_authentication_failure() {
  grep -Eiq \
    'unauthorized|authentication required|denied:|incorrect username or password|invalid username|invalid token|401 Unauthorized|403 Forbidden' \
    "$login_output"
}

is_transient_registry_failure() {
  grep -Eiq \
    'TLS handshake timeout|context deadline exceeded|Client\.Timeout|request canceled|connection reset|connection refused|i/o timeout|temporary failure|unexpected EOF|too many requests|429|500 Internal Server Error|502 Bad Gateway|503 Service Unavailable|504 Gateway Timeout' \
    "$login_output"
}

for ((attempt = 1; attempt <= docker_login_max_attempts; attempt++)); do
  : >"$login_output"
  set +e
  printf '%s' "$password" \
    | timeout --foreground --kill-after=5s "${docker_login_timeout_seconds}s" \
      docker login "$registry" -u "$username" --password-stdin \
      >"$login_output" 2>&1
  login_status="$?"
  set -e

  if [[ "$login_status" -eq 0 ]]; then
    cat "$login_output"
    exit 0
  fi

  cat "$login_output" >&2

  if is_authentication_failure; then
    echo "Falha permanente de autenticação no registry; a execução não será repetida." >&2
    exit 1
  fi

  if [[ "$login_status" -ne 124 ]] && ! is_transient_registry_failure; then
    echo "Falha não transitória no login do registry; a execução não será repetida." >&2
    exit 1
  fi

  if ((attempt == docker_login_max_attempts)); then
    echo "Falha transitória no login persistiu após ${attempt} tentativas." >&2
    exit 1
  fi

  echo "Falha transitória no login do registry; nova tentativa $((attempt + 1))/${docker_login_max_attempts}." >&2
  sleep "$docker_login_retry_delay_seconds"
done
