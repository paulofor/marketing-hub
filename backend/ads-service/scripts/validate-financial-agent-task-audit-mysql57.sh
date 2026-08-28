#!/usr/bin/env bash
set -euo pipefail

FINANCIAL_AUDIT_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FINANCIAL_AUDIT_MODULE_DIR="$(cd "${FINANCIAL_AUDIT_SCRIPT_DIR}/.." && pwd)"
FINANCIAL_AUDIT_COMPOSE_FILE="${FINANCIAL_AUDIT_MODULE_DIR}/docker-compose.financial-agent-task-audit-mysql57.yml"
FINANCIAL_AUDIT_COMPOSE_PROJECT="${FINANCIAL_AGENT_TASK_AUDIT_COMPOSE_PROJECT:-marketing-hub-financial-agent-task-audit}"

financial_audit_compose() {
  docker compose -p "${FINANCIAL_AUDIT_COMPOSE_PROJECT}" -f "${FINANCIAL_AUDIT_COMPOSE_FILE}" "$@"
}

financial_audit_cleanup() {
  financial_audit_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

financial_audit_db_scalar() {
  financial_audit_compose exec -T mysql57-financial-agent-task-audit \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

financial_audit_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

trap financial_audit_cleanup EXIT
financial_audit_cleanup

docker version >/dev/null
docker compose version >/dev/null
financial_audit_compose up -d --build mysql57-financial-agent-task-audit
financial_audit_compose run --rm --build liquibase-financial-agent-task-audit

financial_audit_assert_equal \
  "conclusão auditada do Plutus" \
  "2026-08-28 14:50:09:2026-08-28 14:51:23:BLOCKED_BY_MISSING_SOURCE:FINANCIAL_AGENT_EXECUTION:64:0.37700000:gpt-5.6-sol" \
  "$(financial_audit_db_scalar "SELECT CONCAT(
    received_at, ':', delivered_at, ':',
    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.decision')), ':',
    JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.artifactType')), ':',
    LENGTH(JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.financialSnapshotSha256'))), ':',
    estimated_cost_usd, ':', execution_model_code
  ) FROM agent_task WHERE id = 253;")"

financial_audit_assert_equal \
  "falha auditada sem entrega inventada" \
  "2026-08-28 15:00:10:NULL:FAILED:MCP indisponível:financial-agent-task-audit/v1" \
  "$(financial_audit_db_scalar "SELECT CONCAT(
    received_at, ':', IFNULL(delivered_at, 'NULL'), ':',
    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.status')), ':', execution_error, ':',
    JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.backfillSource'))
  ) FROM agent_task WHERE id = 254;")"

financial_audit_assert_equal \
  "dados prévios preservados" \
  "true:existing:10:existing-model:existing prompt" \
  "$(financial_audit_db_scalar "SELECT CONCAT(
    JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.preserved')), ':',
    JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.origin')), ':',
    input_tokens, ':', execution_model_code, ':', execution_prompt
  ) FROM agent_task WHERE id = 255;")"

financial_audit_assert_equal \
  "tokens históricos não inventados" \
  "0" \
  "$(financial_audit_db_scalar "SELECT COUNT(*) FROM agent_task
    WHERE id IN (253, 254)
      AND (input_tokens IS NOT NULL OR cached_input_tokens IS NOT NULL OR output_tokens IS NOT NULL);")"

financial_audit_compose exec -T mysql57-financial-agent-task-audit \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG
      WHERE ID = '2026-08-28-financial-agent-task-audit-reconciliation-1';" \
  >/dev/null 2>&1
financial_audit_compose run --rm liquibase-financial-agent-task-audit

financial_audit_assert_equal \
  "reaplicação idempotente" \
  "3:3:1:0" \
  "$(financial_audit_db_scalar "SELECT CONCAT(
    COUNT(*), ':',
    SUM(JSON_VALID(evidence_json)), ':',
    SUM(JSON_UNQUOTE(JSON_EXTRACT(evidence_json, '$.origin')) = 'existing'), ':',
    (SELECT COUNT(*)
       FROM agent_task task
       JOIN financial_agent_execution execution ON execution.agent_task_id = task.id
      WHERE task.received_at IS NULL
         OR (execution.status = 'COMPLETED' AND task.delivered_at IS NULL)
         OR task.result_json IS NULL
         OR task.evidence_json IS NULL
         OR (execution.status = 'FAILED' AND task.execution_error IS NULL)
         OR (execution.estimated_cost IS NOT NULL AND task.estimated_cost_usd IS NULL)
         OR (execution.model IS NOT NULL AND task.execution_model_code IS NULL))
  ) FROM agent_task;")"

echo "Auditoria de tarefas do Plutus aprovada no MySQL 5.7."
