#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.local-validation.yml"
MYSQL_SERVICE="pde-platform-local-mysql"
MYSQL_STARTED=0

cleanup() {
  if [[ "${MYSQL_STARTED}" == "1" && "${PDE_KEEP_LOCAL_DB:-0}" != "1" ]]; then
    docker compose -f "${COMPOSE_FILE}" down --volumes --remove-orphans
  fi
}

trap cleanup EXIT

if ! docker info >/dev/null 2>&1; then
  echo "Docker engine indisponivel. Inicie a engine Docker para rodar a validacao local integrada do PDE." >&2
  exit 1
fi

docker version >/dev/null
docker compose version >/dev/null
docker compose -f "${COMPOSE_FILE}" up -d "${MYSQL_SERVICE}"
MYSQL_STARTED=1

for _ in {1..60}; do
  status="$(docker inspect -f '{{.State.Health.Status}}' "${MYSQL_SERVICE}" 2>/dev/null || true)"
  if [[ "${status}" == "healthy" ]]; then
    break
  fi
  sleep 2
done

if [[ "$(docker inspect -f '{{.State.Health.Status}}' "${MYSQL_SERVICE}" 2>/dev/null || true)" != "healthy" ]]; then
  docker compose -f "${COMPOSE_FILE}" logs "${MYSQL_SERVICE}"
  exit 1
fi

(
  cd "${ROOT_DIR}/frontend"
  npm run test:local-integration
)
