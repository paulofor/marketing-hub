#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/backend/ads-service/docker-compose.product-process-mysql57.yml"
COMPOSE_PROJECT="${PRODUCT_PROCESS_COMPOSE_PROJECT:-marketing-hub-product-process-mysql57}"
LEGACY_CHECKSUM="9:68a4881abf2d9f1f338200f937b3b4c5"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-product-process \
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
  compose run --rm liquibase-product-process
}

run_rollback() {
  compose run --rm liquibase-product-process sh -lc \
    'ADS_LIQUIBASE_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "${ADS_LIQUIBASE_CP}" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="${ADS_LIQUIBASE_URL}" --username="${ADS_LIQUIBASE_USERNAME}" --password="${ADS_LIQUIBASE_PASSWORD}" --changeLogFile="${ADS_LIQUIBASE_CHANGELOG_FILE}" rollbackCount 2'
}

trap cleanup EXIT
cleanup

compose build
compose up -d mysql57-product-process

run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "aplicação limpa"
assert_value "2" "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID IN ('2026-08-25-product-process-time-cost','2026-08-25-product-process-time-cost-backfill');")" \
  "registro dos changesets"

run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "reaplicação sem duplicidade"

run_rollback
assert_value "0" "$(mysql_value "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='marketinghub_local' AND table_name='product_process_period';")" \
  "rollback"
run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "reaplicação após rollback"

mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='2026-08-25-product-process-time-cost-backfill'; UPDATE DATABASECHANGELOG SET MD5SUM='${LEGACY_CHECKSUM}' WHERE ID='2026-08-25-product-process-time-cost';"
run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "compatibilidade com execução legada concluída"
assert_value "2" "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID IN ('2026-08-25-product-process-time-cost','2026-08-25-product-process-time-cost-backfill');")" \
  "registro após execução legada"

mysql_value "DELETE FROM product_process_period; DELETE FROM DATABASECHANGELOG WHERE ID IN ('2026-08-25-product-process-time-cost','2026-08-25-product-process-time-cost-backfill');"
assert_value "1" "$(mysql_value "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='marketinghub_local' AND table_name='product_process_period';")" \
  "tabela órfã"
run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "retomada da tabela órfã"

run_update
assert_value "3" "$(mysql_value 'SELECT COUNT(*) FROM product_process_period;')" \
  "reaplicação final sem duplicidade"

echo "[MYSQL57] Migração de períodos validada em banco limpo, rollback, checksum legado, tabela órfã e reaplicação."
