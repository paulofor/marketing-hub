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
audit_v2_compose run --rm --build liquibase-psique-visual-evidence
audit_v2_compose run --rm --build liquibase-agent-task-prompt-parts-v1
audit_v2_compose run --rm --build liquibase-agent-task-history-source-index

audit_v2_assert_equal \
  "índice do histórico por origem" \
  "source_reference:191" \
  "$(audit_v2_db_scalar "SELECT CONCAT(column_name, ':', sub_part)
    FROM information_schema.statistics
    WHERE table_schema = 'marketinghub_local'
      AND table_name = 'agent_task'
      AND index_name = 'idx_agent_task_source_reference'
      AND seq_in_index = 1;")"

audit_v2_assert_equal \
  "partes explícitas dos prompts" \
  "execution_agent_prompt:longtext:YES|execution_activity_prompt:longtext:YES|agent_prompt_part:longtext:YES|activity_prompt_part:longtext:YES" \
  "$(audit_v2_db_scalar "SELECT GROUP_CONCAT(value ORDER BY sort_order SEPARATOR '|')
    FROM (
      SELECT CONCAT(column_name, ':', column_type, ':', is_nullable) AS value,
             ordinal_position AS sort_order
      FROM information_schema.columns
      WHERE table_schema = 'marketinghub_local'
        AND table_name = 'agent_task'
        AND column_name IN ('execution_agent_prompt', 'execution_activity_prompt')
      UNION ALL
      SELECT CONCAT(column_name, ':', column_type, ':', is_nullable) AS value,
             1000 + ordinal_position AS sort_order
      FROM information_schema.columns
      WHERE table_schema = 'marketinghub_local'
        AND table_name = 'gera_landing_stage_execution'
        AND column_name IN ('agent_prompt_part', 'activity_prompt_part')
    ) prompt_columns;")"

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
  "gpt-5.6-sol:NULL:Prompt integral legado da tarefa 258.:NULL:NULL:NULL:NULL" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      execution_model_code, ':', IFNULL(execution_reasoning_effort, 'NULL'), ':',
      execution_prompt, ':', IFNULL(execution_mode, 'NULL'), ':',
      IFNULL(blocker_category, 'NULL'), ':',
      IFNULL(execution_agent_prompt, 'NULL'), ':',
      IFNULL(execution_activity_prompt, 'NULL'))
    FROM agent_task WHERE id = 258;")"

audit_v2_assert_equal \
  "schema privado das provas visuais" \
  "20:ascii:ascii:datetime:NO:datetime:NO:CASCADE" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'), ':',
      (SELECT character_set_name FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'capture_session_id'), ':',
      (SELECT character_set_name FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'evidence_key'), ':',
      (SELECT data_type FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'captured_at'), ':',
      (SELECT is_nullable FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'captured_at'), ':',
      (SELECT data_type FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'created_at'), ':',
      (SELECT is_nullable FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND column_name = 'created_at'), ':',
      (SELECT delete_rule FROM information_schema.referential_constraints
        WHERE constraint_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'
          AND referenced_table_name = 'agent_task'));")"

audit_v2_db_execute "UPDATE agent_task
  SET execution_mode = 'MODEL',
      execution_reasoning_effort = 'high',
      execution_prompt = 'Núcleo de Psique.\n\nAvalie a jornada Rigel.',
      execution_agent_prompt = 'Núcleo de Psique.',
      execution_activity_prompt = 'Avalie a jornada Rigel.',
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
INSERT INTO agent_task_visual_evidence
  (agent_task_id, capture_session_id, evidence_key, evidence_type, device_profile,
   page_number, fold_number, viewport_width, viewport_height, page_height_px, scroll_y,
   source_url, final_url, object_key, content_type, size_bytes, sha256, captured_at, created_at)
VALUES
  (258, 'capture-rigel-258', 'page-1-full', 'FULL_PAGE', 'IPHONE_15_PRO',
   1, NULL, 393, 852, 1704, 0,
   'https://rigel.example/jornada', 'https://rigel.example/jornada',
   'private/task-258/full.png', 'image/png', 240000, REPEAT('a', 64),
   '2026-08-29 01:09:58', '2026-08-29 01:11:00'),
  (258, 'capture-rigel-258', 'page-1-fold-1', 'FOLD', 'IPHONE_15_PRO',
   1, 1, 393, 852, 1704, 0,
   'https://rigel.example/jornada', 'https://rigel.example/jornada',
   'private/task-258/fold-1.png', 'image/png', 120000, REPEAT('b', 64),
   '2026-08-29 01:09:59', '2026-08-29 01:11:00'),
  (258, 'capture-rigel-258', 'page-1-fold-2', 'FOLD', 'IPHONE_15_PRO',
   1, 2, 393, 852, 1704, 852,
   'https://rigel.example/jornada', 'https://rigel.example/jornada',
   'private/task-258/fold-2.png', 'image/png', 118000, REPEAT('c', 64),
   '2026-08-29 01:10:00', '2026-08-29 01:11:00'),
  (259, 'capture-delete-259', 'page-1-full', 'FULL_PAGE', 'IPHONE_15_PRO',
   1, NULL, 393, 852, 852, 0,
   'https://vega.example/jornada', 'https://vega.example/jornada',
   'private/task-259/full.png', 'image/png', 90000, REPEAT('d', 64),
   '2026-08-29 01:10:00', '2026-08-29 01:11:00');
DELETE FROM agent_task WHERE id = 259;"

audit_v2_assert_equal \
  "auditoria acionável e segregada" \
  "MODEL:high:FUNCTIONAL_ADJUSTMENT:2:1:0:3:1:2:0" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      execution_mode, ':', execution_reasoning_effort, ':', blocker_category, ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link
        WHERE agent_task_id = 258 AND link_type = 'ACCESSED_URL'
          AND access_method = 'WEB_SEARCH'
          AND accessed_at = '2026-08-29 01:10:00'), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 259), ':',
      (SELECT COUNT(*) FROM agent_task_visual_evidence WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM agent_task_visual_evidence
        WHERE agent_task_id = 258 AND evidence_type = 'FULL_PAGE'), ':',
      (SELECT COUNT(*) FROM agent_task_visual_evidence
        WHERE agent_task_id = 258 AND evidence_type = 'FOLD'), ':',
      (SELECT COUNT(*) FROM agent_task_visual_evidence WHERE agent_task_id = 259))
    FROM agent_task WHERE id = 258;")"

if audit_v2_db_execute "INSERT INTO agent_task_visual_evidence
  (agent_task_id, capture_session_id, evidence_key, evidence_type, device_profile,
   page_number, fold_number, viewport_width, viewport_height, page_height_px, scroll_y,
   source_url, final_url, object_key, content_type, size_bytes, sha256, captured_at, created_at)
VALUES
  (258, 'capture-rigel-258', 'page-1-fold-1', 'FOLD', 'IPHONE_15_PRO',
   1, 1, 393, 852, 1704, 0,
   'https://outro.example/jornada', 'https://outro.example/jornada',
   'private/conflict.png', 'image/png', 1, REPEAT('e', 64),
   '2026-08-29 01:12:00', '2026-08-29 01:12:00');"; then
  echo "Falha: a chave visual duplicada foi aceita no MySQL 5.7." >&2
  exit 1
fi

audit_v2_compose run --rm liquibase-agent-task-actionable-audit-v2
audit_v2_compose run --rm liquibase-psique-visual-evidence
audit_v2_compose run --rm liquibase-agent-task-prompt-parts-v1
audit_v2_compose run --rm liquibase-agent-task-history-source-index

audit_v2_assert_equal \
  "reaplicação sem duplicar schema, links ou provas" \
  "2:1:2:1:2:2:3:20" \
  "$(audit_v2_db_scalar "SELECT CONCAT(
      (SELECT COUNT(*) FROM DATABASECHANGELOG
        WHERE ID LIKE '2026-08-29-agent-task-actionable-audit-v2-%'), ':',
      (SELECT COUNT(*) FROM DATABASECHANGELOG
        WHERE ID LIKE '2026-08-29-psique-task-visual-evidence-v1-%'), ':',
      (SELECT COUNT(*) FROM DATABASECHANGELOG
        WHERE ID LIKE '2026-08-29-agent-task-prompt-parts-v1-%'), ':',
      (SELECT COUNT(*) FROM DATABASECHANGELOG
        WHERE ID = '2026-09-01-agent-task-history-source-index'), ':',
      (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local' AND table_name = 'agent_task'
          AND column_name IN ('execution_agent_prompt', 'execution_activity_prompt')), ':',
      (SELECT COUNT(*) FROM agent_task_audit_link WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM agent_task_visual_evidence WHERE agent_task_id = 258), ':',
      (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'marketinghub_local'
          AND table_name = 'agent_task_visual_evidence'));")"

echo "Auditoria acionável e visual de tarefas aprovada no MySQL 5.7."
