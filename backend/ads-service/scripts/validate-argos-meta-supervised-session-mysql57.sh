#!/usr/bin/env bash
set -euo pipefail

ARGOS_META_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARGOS_META_MODULE_DIR="$(cd "${ARGOS_META_SCRIPT_DIR}/.." && pwd)"
ARGOS_META_COMPOSE_FILE="${ARGOS_META_MODULE_DIR}/docker-compose.argos-meta-supervised-session-mysql57.yml"
ARGOS_META_COMPOSE_PROJECT="aihub-f973ccda-146e-4cef-8795-ca402a75dfab-43b5dc121a"

argos_meta_compose() {
  docker compose -p "${ARGOS_META_COMPOSE_PROJECT}" -f "${ARGOS_META_COMPOSE_FILE}" "$@"
}

argos_meta_cleanup() {
  argos_meta_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

trap argos_meta_cleanup EXIT
argos_meta_cleanup

docker version >/dev/null
docker compose version >/dev/null
argos_meta_compose up -d --wait mysql57-argos-meta-session
argos_meta_compose run --rm --build argos-meta-session-test

echo "Sessão supervisionada de Argos aprovada fisicamente no MySQL 5.7."
