#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/backend/ads-service/docker-compose.rigel-commercial-slot-mysql57.yml"
COMPOSE_PROJECT="aihub-b3b98477-9dae-4483-b305-094906c24189-5064e5f839"
CHANGESET_ID="2026-08-26-rigel-commercial-experience-v2-slot-repair"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-rigel-commercial-slot \
    mysql -umarketinghub -pmarketinghub-local --batch --skip-column-names \
    marketinghub_local -e "$1" 2>/dev/null
}

assert_value() {
  local expected="$1"
  local actual="$2"
  local scenario="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "[MYSQL57] ${scenario}: esperado=${expected}, obtido=${actual}" >&2
    exit 1
  fi
}

run_update() {
  compose run --rm liquibase-rigel-commercial-slot
}

trap cleanup EXIT
cleanup
compose build
compose up -d mysql57-rigel-commercial-slot

run_update
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT experience_version FROM pde_production_slot WHERE id=7;")" \
  "slot publicado sem rascunho"
assert_value "NULL" \
  "$(mysql_value "SELECT IFNULL(draft_experience_json, 'NULL') FROM pde_production_slot WHERE id=7;")" \
  "rascunho nulo preservado"
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "contrato publicado atualizado"

run_update
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';")" \
  "reaplicação idempotente"

mysql_value "UPDATE pde_production_slot SET experience_version='kit-whatsapp-pronto-pde-v1', layout_key='assisted-service-v1', draft_experience_json='{\"experienceVersion\":\"kit-whatsapp-pronto-pde-v1\",\"layoutKey\":\"assisted-service-v1\"}', published_experience_json='{\"experienceVersion\":\"kit-whatsapp-pronto-pde-v1\",\"layoutKey\":\"assisted-service-v1\"}' WHERE id=7; DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';"
run_update
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "rascunho existente atualizado"
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "publicação retomada após interrupção"

echo "[MYSQL57] Slot comercial da Rigel validado sem rascunho, com rascunho, reaplicação e retomada."
