#!/usr/bin/env bash
set -euo pipefail

AUDIT_V2_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUDIT_V2_MODULE_DIR="$(cd "${AUDIT_V2_SCRIPT_DIR}/.." && pwd)"
AUDIT_V2_COMPOSE_FILE="${AUDIT_V2_MODULE_DIR}/docker-compose.agent-task-actionable-audit-v2-mysql57.yml"
AUDIT_V2_COMPOSE_PROJECT="${AGENT_TASK_ACTIONABLE_AUDIT_COMPOSE_PROJECT:-marketing-hub-agent-task-actionable-audit-v2}"

audit_v2_compose() {
  docker compose -p "${AUDIT_V2_COMPOSE_PROJECT}" -f "${AUDIT_V2_COMPOSE_FILE}" "$@"
}

audit_v2_cleanup() {
  audit_v2_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

audit_v2_db_scalar() {
  audit_v2_compose exec -T mysql57-agent-task-actionable-audit-v2 \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

audit_v2_db_execute() {
  audit_v2_compose exec -T mysql57-agent-task-actionable-audit-v2 \
    mysql -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" >/dev/null 2>&1
}

audit_v2_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

trap audit_v2_cleanup EXIT
audit_v2_cleanup

docker version >/dev/null
docker compose version >/dev/null
audit_v2_compose up -d --build mysql57-agent-task-actionable-audit-v2
audit_v2_compose run --rm --build liquibase-agent-task-actionable-audit-v2

audit_v2_assert_equal \
  "colunas da auditoria terminal" \
  "execution_mode:varchar(24):YES|blocker_category:varchar(40):YES|blocker_action:longtext:YES" \
  "$(audit_v2_db_scalar "SELECT GROUP_CONCAT(
      CONCAT(column_name, ':', column_type, ':', is_nullable)
      ORDER BY ordinal_position SEPARATOR '|')
    FROM information_schema.columns
    WHERE table_schema = 'marketinghub_local'
      AND table_name = 'agent_task'
      AND column_name IN ('execution_mode', 'blocker_category', 'blocker_action');")"

audit_v2_assert_equal \
  "estrutura temporal e vínculo das URLs" \
  "9:datetime:YES:datetime:NO:CASCADE" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link'), ':',
      (SELECT data_type FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link' AND column_name = 'accessed_at'), ':',
      (SELECT is_nullable FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link' AND column_name = 'accessed_at'), ':',
      (SELECT data_type FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link' AND column_name = 'created_at'), ':',
      (SELECT is_nullable FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link' AND column_name = 'created_at'), ':',
      (SELECT delete_rule FROM information_schema.referential_constraints
        WHERE constraint_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link'
          AND referenced_table_name = 'agent_task'));")"

audit_v2_assert_equal \
  "dados legados preservados" \
  "gpt-5.6-sol:NULL:Prompt integral legado da tarefa 258.:NULL:NULL" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      execution_model_code, ':', IFNULL(execution_reasoning_effort, 'NULL'), ':',
      execution_prompt, ':', IFNULL(execution_mode, 'NULL'), ':',
      IFNULL(blocker_category, 'NULL'))
    FROM agent_task WHERE id = 258;")"

audit_v2_db_execute "UPDATE agent_task
  SET execution_mode = 'MODEL',
      execution_reasoning_effort = 'high',
      blocker_category = 'FUNCTIONAL_ADJUSTMENT',
      blocker_action = 'Vincule a versão comprada e reinicie a tarefa.'
  WHERE id = 258;
INSERT INTO agent_task_audit_link
  (agent_task_id, link_type, label, url, access_method, accessed_at, display_order, created_at)
VALUES
  (258, 'ACCESSED_URL', 'Landing Rigel', 'https://rigel.example/jornada',
   'WEB_SEARCH', '2026-08-29 01:10:00', 0, '2026-08-29 01:11:00'),
  (258, 'BLOCKER_HELP', 'Abrir tarefas', '/agent-tasks',
   NULL, NULL, 0, '2026-08-29 01:11:00'),
  (259, 'BLOCKER_HELP', 'Temporário', '/agent-tasks',
   NULL, NULL, 0, '2026-08-29 01:11:00');
DELETE FROM agent_task WHERE id = 259;"

audit_v2_assert_equal \
  "auditoria acionável e segregada" \
  "MODEL:high:FUNCTIONAL_ADJUSTMENT:2:1:0" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      execution_mode, ':', execution_reasoning_effort, ':', blocker_category, ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link
        WHERE agent_task_id = 258 AND link_type = 'ACCESSED_URL'
          AND access_method = 'WEB_SEARCH'
          AND accessed_at = '2026-08-29 01:10:00'), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 259))
    FROM agent_task WHERE id = 258;")"

audit_v2_compose run --rm liquibase-agent-task-actionable-audit-v2

audit_v2_assert_equal \
  "reaplicação sem duplicar schema ou links" \
  "2:2:3" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      (SELECT COUNT(*) FROM DATABASECHANGELOG
        WHERE ID LIKE '2026-08-29-agent-task-actionable-audit-v2-%'), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM information_schema.statistics
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_audit_link'
          AND index_name = 'idx_agent_task_audit_link_task'));")"

echo "Auditoria acionável de tarefas aprovada no MySQL 5.7."
