#!/usr/bin/env bash
set -euo pipefail

PDE_GATE_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PDE_GATE_MODULE_DIR="$(cd "${PDE_GATE_SCRIPT_DIR}/.." && pwd)"
PDE_GATE_COMPOSE_FILE="${PDE_GATE_MODULE_DIR}/docker-compose.pde-gate-coauthored-mysql57.yml"
PDE_GATE_COMPOSE_PROJECT="aihub-cc87d04a-c6ca-46dc-98e6-c746cbcf656c-9d66eef0dd"

pde_gate_compose() {
  docker compose -p "${PDE_GATE_COMPOSE_PROJECT}" -f "${PDE_GATE_COMPOSE_FILE}" "$@"
}

pde_gate_cleanup() {
  pde_gate_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

pde_gate_scalar() {
  pde_gate_compose exec -T mysql57-pde-gate-coauthored \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

pde_gate_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

trap pde_gate_cleanup EXIT
pde_gate_cleanup

docker version >/dev/null
docker compose version >/dev/null
pde_gate_compose up -d --build mysql57-pde-gate-coauthored
pde_gate_compose run --rm --build liquibase-pde-gate-coauthored

pde_gate_assert_equal \
  "processo v5 publicado e v4 aposentado" \
  "PUBLISHED:RETIRED" \
  "$(pde_gate_scalar "SELECT CONCAT(MAX(CASE WHEN version_number = 5 THEN status END), ':', MAX(CASE WHEN version_number = 4 THEN status END)) FROM business_process_definition WHERE process_code = 'pde-commercial-homologation-activation';")"
pde_gate_assert_equal \
  "coautores técnicos no processo" \
  "customer-agent:meta-ad-approver" \
  "$(pde_gate_scalar "SELECT CONCAT(JSON_UNQUOTE(JSON_EXTRACT(diagram_json, '$.nodes[1].responsibleAgentKeys[0]')), ':', JSON_UNQUOTE(JSON_EXTRACT(diagram_json, '$.nodes[1].responsibleAgentKeys[1]'))) FROM business_process_definition WHERE process_code = 'pde-commercial-homologation-activation' AND version_number = 5;")"
pde_gate_assert_equal \
  "atividades relacionais da v5" \
  "3" \
  "$(pde_gate_scalar "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id = activity.process_definition_id WHERE process.process_code = 'pde-commercial-homologation-activation' AND process.version_number = 5;")"
pde_gate_assert_equal \
  "cadeia v6 publicada com seis processos" \
  "PUBLISHED:6:5" \
  "$(pde_gate_scalar "SELECT CONCAT(chain_definition.status, ':', COUNT(chain_item.id), ':', MAX(CASE WHEN process_definition.process_code = 'pde-commercial-homologation-activation' THEN process_definition.version_number END)) FROM business_process_chain_definition chain_definition JOIN business_process_chain_item chain_item ON chain_item.chain_definition_id = chain_definition.id JOIN business_process_definition process_definition ON process_definition.id = chain_item.process_definition_id WHERE chain_definition.chain_code = 'pde-value-creation-delivery' AND chain_definition.version_number = 6 GROUP BY chain_definition.status;")"

pde_gate_compose run --rm liquibase-pde-gate-coauthored
pde_gate_assert_equal \
  "reaplicação idempotente" \
  "3:3:1:6" \
  "$(pde_gate_scalar "SELECT CONCAT((SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-08-27-pde-gate-coauthored-execution-%'), ':', (SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id = activity.process_definition_id WHERE process.version_number = 5), ':', (SELECT COUNT(*) FROM business_process_chain_definition WHERE chain_code = 'pde-value-creation-delivery' AND version_number = 6), ':', (SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id = item.chain_definition_id WHERE chain_definition.version_number = 6));")"

echo "Coautoria do gate PDE aprovada no MySQL 5.7."
