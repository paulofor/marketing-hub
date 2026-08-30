#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/backend/ads-service/docker-compose.rigel-commercial-slot-mysql57.yml"
COMPOSE_PROJECT="${RIGEL_COMMERCIAL_SLOT_COMPOSE_PROJECT:-liquibase-rigel-commercial-slot}"
SLOT_CHANGESET_ID="2026-08-26-rigel-commercial-experience-v2-slot-repair"
EXACT_DELIVERY_CHANGESET_ID="2026-08-30-rigel-exact-delivery-contract-v1"

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

run_slot_update() {
  compose run --rm liquibase-rigel-commercial-slot
}

run_exact_delivery_update() {
  compose run --rm liquibase-rigel-exact-delivery
}

trap cleanup EXIT
cleanup
compose build
compose up -d mysql57-rigel-commercial-slot

run_slot_update
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT experience_version FROM pde_production_slot WHERE id=7;")" \
  "slot publicado sem rascunho"
assert_value "NULL" \
  "$(mysql_value "SELECT IFNULL(draft_experience_json, 'NULL') FROM pde_production_slot WHERE id=7;")" \
  "rascunho nulo preservado"
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "contrato publicado atualizado"

run_slot_update
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='${SLOT_CHANGESET_ID}';")" \
  "reaplicação idempotente"

mysql_value "UPDATE pde_production_slot SET experience_version='kit-whatsapp-pronto-pde-v1', layout_key='assisted-service-v1', draft_experience_json=JSON_SET(published_experience_json, '$.experienceVersion', 'kit-whatsapp-pronto-pde-v1', '$.layoutKey', 'assisted-service-v1'), published_experience_json=JSON_SET(published_experience_json, '$.experienceVersion', 'kit-whatsapp-pronto-pde-v1', '$.layoutKey', 'assisted-service-v1') WHERE id=7; DELETE FROM DATABASECHANGELOG WHERE ID='${SLOT_CHANGESET_ID}';"
run_slot_update
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "rascunho existente atualizado"
assert_value "kit-whatsapp-pronto-pde-v2" \
  "$(mysql_value "SELECT JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.experienceVersion')) FROM pde_production_slot WHERE id=7;")" \
  "publicação retomada após interrupção"

mysql_value "UPDATE pde_production_slot SET draft_experience_json=NULL WHERE id=7;"
run_exact_delivery_update
assert_value "15/15/8/8/4/4" \
  "$(mysql_value "SELECT CONCAT(JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[0].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[0].maxItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[1].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[1].maxItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[2].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.missions[4].deliveryContract.sections[2].maxItems'))) FROM product WHERE id=9;")" \
  "quantidades exatas no produto"
assert_value "15/15/8/8/4/4" \
  "$(mysql_value "SELECT CONCAT(JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[0].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[0].maxItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[1].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[1].maxItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[2].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(published_experience_json, '$.missions[4].deliveryContract.sections[2].maxItems'))) FROM pde_production_slot WHERE id=7;")" \
  "quantidades exatas no slot publicado"
assert_value "NULL" \
  "$(mysql_value "SELECT IFNULL(draft_experience_json, 'NULL') FROM pde_production_slot WHERE id=7;")" \
  "quantidades exatas preservam rascunho nulo"

run_exact_delivery_update
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='${EXACT_DELIVERY_CHANGESET_ID}';")" \
  "contrato exato reaplicado sem duplicidade"

mysql_value "UPDATE product SET pde_experience_json=JSON_SET(pde_experience_json, '$.missions[4].deliveryContract.sections[0].minItems', 10, '$.missions[4].deliveryContract.sections[0].maxItems', 20, '$.missions[4].deliveryContract.sections[1].minItems', 5, '$.missions[4].deliveryContract.sections[1].maxItems', 10, '$.missions[4].deliveryContract.sections[2].minItems', 3, '$.missions[4].deliveryContract.sections[2].maxItems', 5) WHERE id=9; UPDATE pde_production_slot SET draft_experience_json=JSON_SET(published_experience_json, '$.missions[4].deliveryContract.sections[0].minItems', 10, '$.missions[4].deliveryContract.sections[0].maxItems', 20, '$.missions[4].deliveryContract.sections[1].minItems', 5, '$.missions[4].deliveryContract.sections[1].maxItems', 10, '$.missions[4].deliveryContract.sections[2].minItems', 3, '$.missions[4].deliveryContract.sections[2].maxItems', 5), published_experience_json=JSON_SET(published_experience_json, '$.missions[4].deliveryContract.sections[0].minItems', 10, '$.missions[4].deliveryContract.sections[0].maxItems', 20, '$.missions[4].deliveryContract.sections[1].minItems', 5, '$.missions[4].deliveryContract.sections[1].maxItems', 10, '$.missions[4].deliveryContract.sections[2].minItems', 3, '$.missions[4].deliveryContract.sections[2].maxItems', 5) WHERE id=7; DELETE FROM DATABASECHANGELOG WHERE ID='${EXACT_DELIVERY_CHANGESET_ID}';"
run_exact_delivery_update
assert_value "15/8/4" \
  "$(mysql_value "SELECT CONCAT(JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[0].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[1].minItems')), '/', JSON_UNQUOTE(JSON_EXTRACT(draft_experience_json, '$.missions[4].deliveryContract.sections[2].minItems'))) FROM pde_production_slot WHERE id=7;")" \
  "retomada do contrato exato com rascunho"

echo "[MYSQL57] Slot comercial da Rigel validado com contrato exato, rascunho nulo/existente, reaplicação e retomada."
