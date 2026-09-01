#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.experiment-direct-contact-sample-mysql57.yml"
COMPOSE_PROJECT="${DIRECT_CONTACT_SAMPLE_COMPOSE_PROJECT:-liquibase-experiment-direct-contact}"
CHANGESET_ID="2026-09-01-experiment-direct-contact-sample-v1-1-table"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-experiment-direct-contact \
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
compose build
compose up -d mysql57-experiment-direct-contact
compose run --rm liquibase-experiment-direct-contact

assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='experiment_direct_contact' AND COLUMN_NAME IN ('consent_recorded_at','contacted_at','created_at') AND DATA_TYPE='datetime' AND IS_NULLABLE='NO';")" \
  "campos temporais DATETIME NOT NULL"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_experiment_direct_contact_experiment';")" \
  "chave estrangeira do experimento"
assert_value "experiment_id,contact_fingerprint" \
  "$(mysql_value "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='experiment_direct_contact' AND INDEX_NAME='uk_experiment_direct_contact_fingerprint' AND NON_UNIQUE=0;")" \
  "deduplicação física por experimento"

mysql_value "INSERT INTO experiment_direct_contact (experiment_id, contact_fingerprint, consent_evidence_reference, consent_recorded_at, contacted_at, audience_fit_confirmed, recorded_by, created_at) VALUES (89, REPEAT('a', 64), 'internal://consentimentos/fixture-1', '2026-09-01 10:00:00', '2026-09-01 10:01:00', 1, 'Fixture MySQL 5.7', '2026-09-01 10:02:00');"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM experiment_direct_contact WHERE experiment_id=89;")" \
  "contato auditável persistido"

if mysql_value "INSERT INTO experiment_direct_contact (experiment_id, contact_fingerprint, consent_evidence_reference, consent_recorded_at, contacted_at, audience_fit_confirmed, recorded_by, created_at) VALUES (89, REPEAT('a', 64), 'internal://consentimentos/fixture-duplicada', '2026-09-01 10:00:00', '2026-09-01 10:01:00', 1, 'Fixture MySQL 5.7', '2026-09-01 10:02:00');"; then
  echo "[MYSQL57] a chave única aceitou contato duplicado" >&2
  exit 1
fi

mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';"
compose run --rm liquibase-experiment-direct-contact
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM experiment_direct_contact WHERE experiment_id=89;")" \
  "retomada preserva o contato após DDL sem ledger"

compose run --rm liquibase-experiment-direct-contact
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='${CHANGESET_ID}';")" \
  "reaplicação idempotente"

echo "[MYSQL57] Amostra direta validada com DATETIME, FK, deduplicação e reaplicação."
