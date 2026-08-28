#!/usr/bin/env bash
set -euo pipefail

AUDIT_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUDIT_MODULE_DIR="$(cd "${AUDIT_SCRIPT_DIR}/.." && pwd)"
AUDIT_COMPOSE_FILE="${AUDIT_MODULE_DIR}/docker-compose.product-discovery-bpm-audit-mysql57.yml"
AUDIT_COMPOSE_PROJECT="${PRODUCT_DISCOVERY_BPM_COMPOSE_PROJECT:-marketing-hub-product-discovery-bpm-audit}"

audit_compose() {
  docker compose -p "${AUDIT_COMPOSE_PROJECT}" -f "${AUDIT_COMPOSE_FILE}" "$@"
}

audit_cleanup() {
  audit_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

audit_db_scalar() {
  audit_compose exec -T mysql57-product-discovery-bpm-audit \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

audit_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

audit_liquibase_command() {
  local command="$1"
  audit_compose run --rm liquibase-product-discovery-bpm-audit sh -lc \
    'AUDIT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$AUDIT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

trap audit_cleanup EXIT
audit_cleanup

docker version >/dev/null
docker compose version >/dev/null
audit_compose up -d --build mysql57-product-discovery-bpm-audit
audit_compose run --rm --build liquibase-product-discovery-bpm-audit

audit_assert_equal \
  "status e correlação dos ciclos" \
  "6:6:37=BLOCKED,38=BLOCKED,39=BLOCKED,40=COMPLETED,41=PENDING,42=IN_PROGRESS" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT GROUP_CONCAT(CONCAT(SUBSTRING_INDEX(source_reference, ':', -1), '=', status) ORDER BY id SEPARATOR ',') FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%')
  );")"
audit_assert_equal \
  "plano estruturado e oportunidade real" \
  "OBJECT:1" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT JSON_TYPE(JSON_EXTRACT(evidence_json, '$.researchPlan')) FROM agent_task WHERE source_reference = 'product-discovery-cycle:37'), ':',
    (SELECT JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.opportunityCount')) FROM agent_task WHERE source_reference = 'product-discovery-cycle:40')
  );")"
audit_assert_equal \
  "resultado bloqueado preserva a causa" \
  "OBJECT:POST complete falhou com status 422" \
  "$(audit_db_scalar "SELECT CONCAT(
    JSON_TYPE(JSON_EXTRACT(result_json, '$')), ':',
    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.error'))
  ) FROM agent_task WHERE source_reference = 'product-discovery-cycle:37';")"
audit_assert_equal \
  "falhas preservadas sem datas ou custo inventados" \
  "3:3:0" \
  "$(audit_db_scalar "SELECT CONCAT(
    SUM(status = 'BLOCKED' AND execution_error = 'POST complete falhou com status 422'), ':',
    SUM(status = 'BLOCKED' AND received_at IS NULL AND delivered_at IS NULL), ':',
    SUM(input_tokens IS NOT NULL OR estimated_cost_usd IS NOT NULL)
  ) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%';")"
audit_assert_equal \
  "marcador exclusivo do retroativo" \
  "6" \
  "$(audit_db_scalar "SELECT COUNT(*) FROM agent_task
    WHERE JSON_VALID(evidence_json)
      AND JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.backfillSource'))
        = 'product-discovery-bpm-audit/v1';")"

audit_compose exec -T mysql57-product-discovery-bpm-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG WHERE ID = '2026-08-28-product-discovery-bpm-audit-1-backfill';" \
  >/dev/null 2>&1
audit_compose run --rm liquibase-product-discovery-bpm-audit
audit_assert_equal \
  "reaplicação idempotente" \
  "6:6" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%')
  );")"

audit_liquibase_command "rollbackCount 1"
audit_assert_equal \
  "rollback preserva os ciclos de origem" \
  "0:0:6" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT COUNT(*) FROM product_discovery_cycle)
  );")"

audit_compose run --rm liquibase-product-discovery-bpm-audit
audit_assert_equal \
  "reaplicação após rollback" \
  "6:6:3" \
  "$(audit_db_scalar "SELECT CONCAT(
    (SELECT COUNT(*) FROM business_process_activity_instance WHERE evidence_quality = 'BACKFILLED_FROM_CYCLE'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE source_reference LIKE 'product-discovery-cycle:%'), ':',
    (SELECT COUNT(*) FROM agent_task WHERE status = 'BLOCKED' AND execution_error = 'POST complete falhou com status 422')
  );")"

echo "Auditoria BPM da descoberta PDE aprovada no MySQL 5.7."
