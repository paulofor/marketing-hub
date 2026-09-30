#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
platform_dir="$(cd "${script_dir}/.." && pwd)"
compose_file="${platform_dir}/docker-compose.yml"
compose_project="${PDE_LOCAL_COMPOSE_PROJECT:?PDE_LOCAL_COMPOSE_PROJECT is required}"

compose() {
  docker compose -p "${compose_project}" -f "${compose_file}" --profile test "$@"
}

cleanup() {
  compose down --volumes --remove-orphans
}

trap cleanup EXIT

docker version >/dev/null
docker compose version >/dev/null
compose config --quiet
compose down --volumes --remove-orphans
compose up --build --abort-on-container-exit \
  --exit-code-from pde-platform-alcyone-e2e \
  pde-platform-alcyone-e2e
