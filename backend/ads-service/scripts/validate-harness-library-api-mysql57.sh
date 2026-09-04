#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.harness-library-api-mysql57.yml"
COMPOSE_PROJECT="${HARNESS_LIBRARY_MYSQL57_PROJECT:-aihub-949955b8-7dd6-45ea-b31b-bd057304f08e-a1d71c2c3c}"
CHANGESET_PREFIX="2026-09-04-harness-library-api-v1-"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_execute() {
  compose exec -T mysql57-harness-library \
    mysql -umarketinghub -pmarketinghub-local --default-character-set=utf8mb4 \
    marketinghub_local -e "$1" 2>/dev/null
}

mysql_value() {
  compose exec -T mysql57-harness-library \
    mysql -umarketinghub -pmarketinghub-local --default-character-set=utf8mb4 \
    --batch --skip-column-names marketinghub_local -e "$1" 2>/dev/null
}

mysql_root_execute() {
  compose exec -T mysql57-harness-library \
    mysql -uroot -pmarketinghub-root-local --default-character-set=utf8mb4 \
    -e "$1" 2>/dev/null
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
compose build liquibase-harness-library
compose up -d mysql57-harness-library
compose run --rm liquibase-harness-library

assert_value "2" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('research_intelligence_card','research_intelligence_card_version');")" \
  "duas tabelas canônicas"
assert_value "39" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('research_intelligence_card','research_intelligence_card_version');")" \
  "contrato completo de colunas"
assert_value "7" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('research_intelligence_card','research_intelligence_card_version') AND COLUMN_NAME IN ('created_at','updated_at','review_submitted_at','activated_at','archived_at') AND DATA_TYPE='datetime';")" \
  "campos temporais DATETIME"
assert_value "3" \
  "$(mysql_value "SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='research_intelligence_card_version' AND INDEX_NAME IN ('uk_research_intelligence_card_version','uk_research_intelligence_card_id','uk_research_intelligence_idempotency') AND NON_UNIQUE=0;")" \
  "unicidade de versão, identidade e idempotência"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='research_intelligence_card_version' AND CONSTRAINT_NAME='fk_research_intelligence_card_version_root';")" \
  "vínculo entre cartão e versão"

mysql_execute "INSERT INTO research_intelligence_card (card_key, created_at, updated_at, row_version) VALUES ('homologacao-harness-card-v1', '2026-09-04 10:00:00', '2026-09-04 10:00:00', 0);"
mysql_execute "INSERT INTO research_intelligence_card_version (card_key, version_number, card_id, status, collection_key, title, finding, mechanism, commercial_application, evidence_strength, published_on, valid_until, experiment_hypothesis, risks, limits_text, source_kind, source_uri, source_title, source_sha256, idempotency_key, payload_sha256, created_by, created_at, updated_at, row_version) VALUES ('homologacao-harness-card-v1', 1, 'RI1-HOMOLOGACAO', 'DRAFT', 'video', 'Título de homologação', 'Achado', 'Mecanismo', 'Aplicação', 'Evidência externa', '2026-09-04', '2026-10-19', 'Hipótese', 'Riscos', 'Limites', 'TEXT', 'urn:harness:homologacao', 'Fonte sintética', REPEAT('a', 64), 'homologacao-idempotency-v1', REPEAT('b', 64), 'homologacao', '2026-09-04 10:00:00', '2026-09-04 10:00:00', 0);"

if mysql_execute "INSERT INTO research_intelligence_card_version (card_key, version_number, card_id, status, collection_key, title, finding, mechanism, commercial_application, evidence_strength, published_on, valid_until, experiment_hypothesis, risks, limits_text, source_kind, source_uri, source_title, source_sha256, idempotency_key, payload_sha256, created_by, created_at, updated_at, row_version) VALUES ('homologacao-harness-card-v1', 2, 'RI1-HOMOLOGACAO2', 'DRAFT', 'video', 'Título duplicado', 'Achado', 'Mecanismo', 'Aplicação', 'Evidência externa', '2026-09-04', '2026-10-19', 'Hipótese', 'Riscos', 'Limites', 'TEXT', 'urn:harness:homologacao', 'Fonte sintética', REPEAT('a', 64), 'homologacao-idempotency-v1', REPEAT('c', 64), 'homologacao', '2026-09-04 10:00:00', '2026-09-04 10:00:00', 0);"; then
  echo "[MYSQL57] A chave de idempotência aceitou duplicidade." >&2
  exit 1
fi

mysql_execute "DELETE FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGESET_PREFIX}%';"
compose run --rm liquibase-harness-library
assert_value "2" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGESET_PREFIX}%';")" \
  "retomada após DDL sem ledger"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM research_intelligence_card_version WHERE card_key='homologacao-harness-card-v1';")" \
  "retomada preserva versão existente"

mysql_root_execute "CREATE DATABASE harness_malformed CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON harness_malformed.* TO 'marketinghub'@'%'; CREATE TABLE harness_malformed.research_intelligence_card (card_key VARCHAR(120) NOT NULL PRIMARY KEY) ENGINE=InnoDB;"
if compose run --rm \
  -e ADS_LIQUIBASE_URL='jdbc:mysql://mysql57-harness-library:3306/harness_malformed?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  liquibase-harness-library >/dev/null 2>&1; then
  echo "[MYSQL57] Schema parcial foi aceito como migração concluída." >&2
  exit 1
fi

compose run --rm liquibase-harness-library
assert_value "2" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGESET_PREFIX}%';")" \
  "reaplicação idempotente"

echo "[MYSQL57] Biblioteca do Harness validada com DATETIME, FKs, unicidade, retomada e rejeição de schema parcial."
