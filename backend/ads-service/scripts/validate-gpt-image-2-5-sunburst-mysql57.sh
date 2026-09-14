#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.gpt-image-2-5-sunburst-mysql57.yml"
COMPOSE_PROJECT="aihub-61cdbfa8-4641-47f0-982e-b507d1dc2414-5ad654b8ca"
CHANGESET_ID="2030-09-19-add-gpt-image-2-5-sunburst"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-gpt-image-sunburst \
    mysql -umarketinghub -pmarketinghub-local --default-character-set=utf8mb4 \
    --batch --skip-column-names marketinghub_local -e "$1" 2>/dev/null
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

trap cleanup EXIT
cleanup
compose build mysql57-gpt-image-sunburst liquibase-gpt-image-sunburst
compose up -d mysql57-gpt-image-sunburst
compose run --rm liquibase-gpt-image-sunburst

assert_value "1" "$(mysql_value "SELECT COUNT(*) FROM image_generation_model WHERE code='gpt-image-2.5-sunburst' AND api_model='gpt-image-2.5-sunburst';")" "modelo canônico"
assert_value "5" "$(mysql_value "SELECT COUNT(*) FROM image_generation_quality q JOIN image_generation_model m ON m.id=q.model_id WHERE m.code='gpt-image-2.5-sunburst';")" "cinco qualidades"
assert_value "high" "$(mysql_value "SELECT q.code FROM image_generation_quality q JOIN image_generation_model m ON m.id=q.model_id WHERE m.code='gpt-image-2.5-sunburst' AND q.is_default=1;")" "qualidade padrão"
assert_value "15" "$(mysql_value "SELECT COUNT(*) FROM image_generation_price p JOIN image_generation_quality q ON q.id=p.quality_id JOIN image_generation_model m ON m.id=q.model_id WHERE m.code='gpt-image-2.5-sunburst';")" "preços por qualidade e orientação"
assert_value "1" "$(mysql_value "SELECT COUNT(*) FROM image_generation_model WHERE code='gpt-image-2';")" "histórico anterior preservado"

mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';"
compose run --rm liquibase-gpt-image-sunburst
assert_value "15" "$(mysql_value "SELECT COUNT(*) FROM image_generation_price p JOIN image_generation_quality q ON q.id=p.quality_id JOIN image_generation_model m ON m.id=q.model_id WHERE m.code='gpt-image-2.5-sunburst';")" "reaplicação idempotente"

compose run --rm liquibase-gpt-image-sunburst sh -lc \
  'ADS_LIQUIBASE_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "${ADS_LIQUIBASE_CP}" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="${ADS_LIQUIBASE_URL}" --username="${ADS_LIQUIBASE_USERNAME}" --password="${ADS_LIQUIBASE_PASSWORD}" --changeLogFile="${ADS_LIQUIBASE_CHANGELOG_FILE}" rollbackCount 1'
assert_value "0" "$(mysql_value "SELECT COUNT(*) FROM image_generation_model WHERE code='gpt-image-2.5-sunburst';")" "rollback isolado"
assert_value "1" "$(mysql_value "SELECT COUNT(*) FROM image_generation_model WHERE code='gpt-image-2';")" "rollback preserva histórico"

compose run --rm liquibase-gpt-image-sunburst
assert_value "1" "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';")" "aplicação após rollback"

echo "[MYSQL57] Migração GPT Image 2.5 Sunburst validada com aplicação, idempotência e rollback."
