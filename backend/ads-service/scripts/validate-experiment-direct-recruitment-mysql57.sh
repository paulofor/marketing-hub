#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.experiment-direct-recruitment-mysql57.yml"
COMPOSE_PROJECT="${DIRECT_RECRUITMENT_COMPOSE_PROJECT:-liquibase-experiment-direct-recruitment}"
CHANGESET_PREFIX="2026-09-01-experiment-direct-recruitment-v1-"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-experiment-direct-recruitment \
    mysql -umarketinghub -pmarketinghub-local --default-character-set=utf8mb4 \
    --batch --skip-column-names marketinghub_local -e "$1" 2>/dev/null
}

liquibase_command() {
  local command="$1"
  compose run --rm liquibase-experiment-direct-recruitment sh -lc \
    'RECRUITMENT_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$RECRUITMENT_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
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
compose up -d mysql57-experiment-direct-recruitment
compose run --rm liquibase-experiment-direct-recruitment

assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('experiment_direct_recruitment_campaign','experiment_direct_recruitment_visit','experiment_direct_recruitment_submission');")" \
  "três tabelas do recrutamento"
assert_value "6" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('experiment_direct_recruitment_campaign','experiment_direct_recruitment_visit','experiment_direct_recruitment_submission') AND COLUMN_NAME IN ('created_at','updated_at','first_visited_at','submitted_at') AND DATA_TYPE='datetime' AND IS_NULLABLE='NO';")" \
  "campos temporais DATETIME NOT NULL"
assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME IN ('fk_exp_direct_recruit_campaign_experiment','fk_exp_direct_recruit_visit_campaign','fk_exp_direct_recruit_submission_campaign');")" \
  "chaves estrangeiras do fluxo"
assert_value "experiment_id" \
  "$(mysql_value "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='experiment_direct_recruitment_campaign' AND INDEX_NAME='uk_exp_direct_recruit_campaign_exp' AND NON_UNIQUE=0;")" \
  "um convite por experimento"
assert_value "campaign_id,contact_fingerprint" \
  "$(mysql_value "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='experiment_direct_recruitment_submission' AND INDEX_NAME='uk_exp_direct_recruit_submission_contact' AND NON_UNIQUE=0;")" \
  "deduplicação de pessoa por convite"

mysql_value "INSERT INTO experiment_direct_recruitment_campaign (experiment_id, public_token, status, contract_version, headline, body_text, audience_summary, consent_text, consent_version, offer_url, offer_cta, privacy_policy_url, created_by, status_reason, created_at, updated_at) VALUES (89, '11111111-2222-4333-8444-555555555555', 'ACTIVE', 'direct-recruitment-v1', 'Convite Rigel', 'Validação consentida', 'Prestadores de serviços', 'Aceito participar', 'consent-v1', 'https://rigel.example', 'Conhecer oferta', 'https://rigel.example/privacidade', 'Fixture MySQL 5.7', 'Ativo sem distribuição', '2026-09-01 10:00:00', '2026-09-01 10:00:00');"
mysql_value "INSERT INTO experiment_direct_recruitment_visit (campaign_id, visitor_fingerprint, utm_source, first_visited_at, created_at) VALUES (1, REPEAT('a', 64), 'fixture', '2026-09-01 10:01:00', '2026-09-01 10:01:00');"
mysql_value "INSERT INTO experiment_direct_recruitment_submission (campaign_id, submission_key, contact_fingerprint, service_segment, weekly_conversations_range, uses_whatsapp, decision_maker, wants_personalized_implementation, consent_accepted, consent_version, status, qualification_reason, utm_source, submitted_at, created_at) VALUES (1, 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee', REPEAT('b', 64), 'CONSULTING', 'ELEVEN_TO_THIRTY', 1, 1, 1, 1, 'consent-v1', 'QUALIFIED', 'Perfil aderente', 'fixture', '2026-09-01 10:02:00', '2026-09-01 10:02:00');"

if mysql_value "INSERT INTO experiment_direct_recruitment_visit (campaign_id, visitor_fingerprint, first_visited_at, created_at) VALUES (1, REPEAT('a', 64), '2026-09-01 10:03:00', '2026-09-01 10:03:00');"; then
  echo "[MYSQL57] a chave única aceitou visita duplicada" >&2
  exit 1
fi
if mysql_value "INSERT INTO experiment_direct_recruitment_submission (campaign_id, submission_key, contact_fingerprint, service_segment, weekly_conversations_range, uses_whatsapp, decision_maker, wants_personalized_implementation, consent_accepted, consent_version, status, qualification_reason, submitted_at, created_at) VALUES (1, 'ffffffff-bbbb-4ccc-8ddd-eeeeeeeeeeee', REPEAT('b', 64), 'CONSULTING', 'ONE_TO_TEN', 1, 1, 1, 1, 'consent-v1', 'QUALIFIED', 'Duplicado', '2026-09-01 10:04:00', '2026-09-01 10:04:00');"; then
  echo "[MYSQL57] a chave única aceitou pessoa duplicada" >&2
  exit 1
fi

mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGESET_PREFIX}%';"
compose run --rm liquibase-experiment-direct-recruitment
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM experiment_direct_recruitment_submission;")" \
  "retomada preserva adesão após DDL sem ledger"
assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGESET_PREFIX}%';")" \
  "retomada restaura o ledger"

liquibase_command "rollbackCount 3"
assert_value "0" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'experiment_direct_recruitment_%';")" \
  "rollback remove as tabelas na ordem segura"
compose run --rm liquibase-experiment-direct-recruitment
assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'experiment_direct_recruitment_%';")" \
  "reaplicação recria o contrato"

echo "[MYSQL57] Recrutamento direto validado com DATETIME, FKs, deduplicação, retomada e rollback."
