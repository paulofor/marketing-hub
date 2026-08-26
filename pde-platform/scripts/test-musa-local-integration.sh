#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"
VALIDATION_COMPOSE_FILE="${ROOT_DIR}/docker-compose.local-validation.yml"
MYSQL_SERVICE="pde-platform-local-mysql"
TOPOLOGY_STARTED=0

compose() {
  docker compose \
    -f "${BASE_COMPOSE_FILE}" \
    -f "${VALIDATION_COMPOSE_FILE}" \
    --profile local-e2e \
    "$@"
}

cleanup() {
  if [[ "${TOPOLOGY_STARTED}" == "1" && "${PDE_KEEP_LOCAL_DB:-0}" != "1" ]]; then
    compose down --volumes --remove-orphans
  fi
}

trap cleanup EXIT

if ! docker info >/dev/null 2>&1; then
  echo "Docker engine indisponivel. Inicie a engine Docker para rodar a validacao local integrada do PDE." >&2
  exit 1
fi

docker version >/dev/null
docker compose version >/dev/null
compose config --quiet
TOPOLOGY_STARTED=1
compose up -d --build --wait \
  pde-contract-server \
  "${MYSQL_SERVICE}" \
  pde-platform-backend \
  pde-platform-frontend
compose build pde-playwright-validation
compose run --rm --no-deps pde-playwright-validation
