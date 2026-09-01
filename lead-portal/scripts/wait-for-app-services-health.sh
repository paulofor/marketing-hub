#!/usr/bin/env bash
set -euo pipefail

MAX_WAIT_SECONDS="${1:-900}"
POLL_INTERVAL_SECONDS="${2:-10}"
BACKEND_CONTAINER="${LEAD_PORTAL_BACKEND_CONTAINER:-lead-portal-backend}"
FRONTEND_CONTAINER="${LEAD_PORTAL_FRONTEND_CONTAINER:-lead-portal-frontend}"

if [[ "$#" -gt 2 ]]; then
  echo "Uso: $0 [tempo-maximo-segundos] [intervalo-segundos]" >&2
  exit 2
fi
if ! [[ "$MAX_WAIT_SECONDS" =~ ^[0-9]+$ ]]; then
  echo "O tempo máximo do health deve ser um inteiro não negativo." >&2
  exit 2
fi
if ! [[ "$POLL_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "O intervalo do health deve ser um inteiro positivo." >&2
  exit 2
fi

health_deadline=$((SECONDS + MAX_WAIT_SECONDS))
backend_snapshot=""
frontend_snapshot=""

while ((SECONDS <= health_deadline)); do
  backend_snapshot="$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$BACKEND_CONTAINER" 2>/dev/null || true)"
  frontend_snapshot="$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$FRONTEND_CONTAINER" 2>/dev/null || true)"
  read -r backend_state backend_health <<<"$backend_snapshot"
  read -r frontend_state frontend_health <<<"$frontend_snapshot"

  if [[ "$backend_health" == healthy && "$frontend_health" == healthy ]]; then
    exit 0
  fi
  if [[ "$backend_state" =~ ^(dead|exited)$ ]] || [[ "$frontend_state" =~ ^(dead|exited)$ ]]; then
    echo "Serviço encerrou durante o health: backend=${backend_snapshot:-ausente}, frontend=${frontend_snapshot:-ausente}." >&2
    exit 1
  fi
  if ((SECONDS >= health_deadline)); then
    break
  fi

  remaining_seconds=$((health_deadline - SECONDS))
  sleep_seconds="$POLL_INTERVAL_SECONDS"
  if ((sleep_seconds > remaining_seconds)); then
    sleep_seconds="$remaining_seconds"
  fi
  sleep "$sleep_seconds"
done

echo "Health não convergiu em ${MAX_WAIT_SECONDS}s: backend=${backend_snapshot:-ausente}, frontend=${frontend_snapshot:-ausente}." >&2
exit 1
