#!/usr/bin/env bash
set -euo pipefail

ACTIVITY_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ACTIVITY_MODULE_DIR="$(cd "${ACTIVITY_SCRIPT_DIR}/.." && pwd)"
ACTIVITY_COMPOSE_FILE="${ACTIVITY_MODULE_DIR}/docker-compose.activity-instances-mysql57.yml"
ACTIVITY_COMPOSE_PROJECT="${ACTIVITY_COMPOSE_PROJECT:-liquibase-activity-instances-local}"

activity_compose() {
  docker compose \
    -p "${ACTIVITY_COMPOSE_PROJECT}" \
    -f "${ACTIVITY_COMPOSE_FILE}" \
    "$@"
}

activity_cleanup() {
  activity_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

activity_db_scalar() {
  activity_compose exec -T mysql57-activity-instances \
    mysql -N -s -umarketinghub -pmarketinghub-local marketinghub_local \
    -e "$1" 2>/dev/null
}

activity_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

activity_liquibase_command() {
  local command="$1"
  activity_compose run --rm liquibase-activity-instances sh -lc \
    'ACTIVITY_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$ACTIVITY_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

trap activity_cleanup EXIT
activity_cleanup

docker version >/dev/null
docker compose version >/dev/null
activity_compose up -d --build mysql57-activity-instances
activity_compose run --rm --build liquibase-activity-instances

activity_assert_equal \
  "changesets aplicados" \
  "7" \
  "$(activity_db_scalar "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-08-25-business-process-activity-instances-%';")"
activity_assert_equal \
  "atividades relacionais" \
  "2" \
  "$(activity_db_scalar "SELECT COUNT(*) FROM business_process_activity_definition;")"
activity_assert_equal \
  "instâncias relacionais" \
  "2" \
  "$(activity_db_scalar "SELECT COUNT(*) FROM business_process_activity_instance;")"
activity_assert_equal \
  "tarefas regulares sem instância" \
  "0" \
  "$(activity_db_scalar "SELECT COUNT(*) FROM agent_task WHERE process_definition_id IS NOT NULL AND activity_instance_id IS NULL;")"
activity_assert_equal \
  "legados preservados sem instância fabricada" \
  "1" \
  "$(activity_db_scalar "SELECT COUNT(*) FROM agent_task WHERE process_definition_id IS NULL AND activity_instance_id IS NULL;")"
activity_assert_equal \
  "correção substitui bloqueio anterior" \
  "COMPLETED:1:0.30000000:NONE" \
  "$(activity_db_scalar "SELECT CONCAT(status, ':', objective_achieved, ':', known_cost_usd, ':', COALESCE(blocked_reason, 'NONE')) FROM business_process_activity_instance instance JOIN business_process_activity_definition definition ON definition.id = instance.activity_definition_id WHERE definition.activity_id = 'html';")"

activity_compose run --rm liquibase-activity-instances
activity_assert_equal \
  "reaplicação idempotente" \
  "7:2:2" \
  "$(activity_db_scalar "SELECT CONCAT((SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-08-25-business-process-activity-instances-%'), ':', (SELECT COUNT(*) FROM business_process_activity_definition), ':', (SELECT COUNT(*) FROM business_process_activity_instance));")"

activity_liquibase_command "rollbackCount 7"
activity_assert_equal \
  "rollback preserva tarefas originais" \
  "4:0:0" \
  "$(activity_db_scalar "SELECT CONCAT((SELECT COUNT(*) FROM agent_task), ':', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'marketinghub_local' AND table_name IN ('business_process_activity_definition', 'business_process_activity_instance')), ':', (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'marketinghub_local' AND table_name = 'agent_task' AND column_name = 'activity_instance_id'));")"

activity_liquibase_command "updateCount 1"
activity_compose exec -T mysql57-activity-instances \
  mysql -umarketinghub -pmarketinghub-local marketinghub_local \
  -e "DELETE FROM DATABASECHANGELOG WHERE ID = '2026-08-25-business-process-activity-instances-1-definition-table';" \
  >/dev/null 2>&1
activity_compose run --rm liquibase-activity-instances
activity_assert_equal \
  "recuperação de DDL órfão" \
  "7:2:2:0" \
  "$(activity_db_scalar "SELECT CONCAT((SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-08-25-business-process-activity-instances-%'), ':', (SELECT COUNT(*) FROM business_process_activity_definition), ':', (SELECT COUNT(*) FROM business_process_activity_instance), ':', (SELECT COUNT(*) FROM agent_task WHERE process_definition_id IS NOT NULL AND activity_instance_id IS NULL));")"

echo "Atividade, instância e tentativas aprovadas no MySQL 5.7."
