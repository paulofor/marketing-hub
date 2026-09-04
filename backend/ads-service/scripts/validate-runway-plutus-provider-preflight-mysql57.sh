#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.runway-plutus-provider-preflight-mysql57.yml"
COMPOSE_PROJECT="${RUNWAY_PREFLIGHT_COMPOSE_PROJECT:-aihub-3b1bd9ac-f97e-43f2-8cdd-cdbeb5e43c49-feb0ca303a}"
CHANGELOG_PREFIX="2026-09-03-runway-plutus-provider-preflight-v1-"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

mysql_value() {
  compose exec -T mysql57-runway-plutus-preflight \
    mysql -umarketinghub -pmarketinghub-local --default-character-set=utf8mb4 \
    --batch --skip-column-names marketinghub_local -e "$1" 2>/dev/null
}

mysql_product_value() {
  compose exec -T mysql57-runway-product-ugc \
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
compose up -d mysql57-runway-plutus-preflight mysql57-runway-product-ugc
compose run --rm liquibase-runway-plutus-preflight
compose run --rm liquibase-runway-product-ugc

assert_value "ByteDance,Runway,RUNWAY_PRIMARY,RUNWAY_SEEDANCE_2_5" \
  "$(mysql_value "SELECT CONCAT(manufacturer_name, ',', aggregator_name, ',', provider_account_key, ',', route_key) FROM sales_video_provider_model WHERE code='RUNWAY_SEEDANCE_2_5';")" \
  "identidades separadas do modelo Runway"
assert_value "Google,Runway,RUNWAY_PRIMARY" \
  "$(mysql_value "SELECT CONCAT(manufacturer_name, ',', aggregator_name, ',', provider_account_key) FROM sales_video_provider_model WHERE code='RUNWAY_VEO_3_1';")" \
  "fabricante separado do agregador"
assert_value "Luma AI,LUMA,1" \
  "$(mysql_value "SELECT CONCAT(manufacturer_name, ',', aggregator_name, ',', provider_account_key IS NULL) FROM sales_video_provider_model WHERE code='LUMA_RAY_2';")" \
  "rota direta sem conta Runway"
assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sales_video_provider_model' AND COLUMN_NAME IN ('manufacturer_name','aggregator_name','route_key') AND IS_NULLABLE='NO';")" \
  "identidades canônicas obrigatórias"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM video_provider_account WHERE account_key='RUNWAY_PRIMARY' AND credit_unit_usd=0.010000 AND snapshot_status='UNKNOWN';")" \
  "conta agregadora Runway única"
assert_value "3" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('video_provider_account','video_provider_preflight','video_credit_reservation');")" \
  "tabelas de snapshot, preflight e reserva"
assert_value "14" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('video_provider_account','video_provider_preflight','video_credit_reservation') AND COLUMN_NAME IN ('snapshot_observed_at','snapshot_expires_at','observed_at','expires_at','reserved_at','settled_at','released_at','created_at','updated_at') AND DATA_TYPE='datetime';")" \
  "campos temporais compatíveis com MySQL 5.7"
assert_value "5" \
  "$(mysql_value "SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME IN ('fk_video_provider_preflight_cycle','fk_video_provider_preflight_account','fk_video_credit_reservation_cycle','fk_video_credit_reservation_preflight','fk_video_credit_reservation_account');")" \
  "chaves estrangeiras financeiras"
assert_value "Runway,Runway,RUNWAY_PRIMARY,RUNWAY_PRODUCT_UGC:product_ugc@2026-06,ACTIVE,15,15,0,1,1,1,1" \
  "$(mysql_product_value "SELECT CONCAT(manufacturer_name, ',', aggregator_name, ',', provider_account_key, ',', route_key, ',', lifecycle_status, ',', clip_duration_seconds, ',', max_direct_duration_seconds, ',', supports_scene_assembly, ',', adapter_verified, ',', pricing_verified, ',', commercial_license_verified, ',', quality_gate_verified) FROM sales_video_provider_model WHERE code='runway-product-ugc-2026-06';")" \
  "receita Product UGC pinada e integralmente homologada"
assert_value "6.480000,VIDEO,1.0000,1080:1920 / 15s,0,VERIFIED" \
  "$(mysql_product_value "SELECT CONCAT(pricing_amount_usd, ',', pricing_unit, ',', pricing_quantity, ',', pricing_resolution, ',', pricing_includes_audio, ',', pricing_research_status) FROM sales_video_provider_model WHERE code='runway-product-ugc-2026-06';")" \
  "tarifa final Product UGC comparável no catálogo de Plutus"

mysql_value "INSERT INTO video_provider_preflight (video_production_cycle_id, provider_account_id, status, production_profile, source_url, created_at, updated_at) SELECT 31, id, 'PENDING', 'FINAL_CAMPAIGN', source_url, '2026-09-03 10:01:00', '2026-09-03 10:01:00' FROM video_provider_account WHERE account_key='RUNWAY_PRIMARY';"
mysql_value "INSERT INTO video_credit_reservation (video_production_cycle_id, provider_preflight_id, provider_account_id, status, reserved_credits, reserved_cost_usd, expires_at, reserved_at, created_at, updated_at) SELECT 31, p.id, a.id, 'RESERVED', 200.0000, 2.000000, '2026-09-03 11:01:00', '2026-09-03 10:01:00', '2026-09-03 10:01:00', '2026-09-03 10:01:00' FROM video_provider_preflight p JOIN video_provider_account a ON a.id=p.provider_account_id WHERE p.video_production_cycle_id=31;"
if mysql_value "INSERT INTO video_credit_reservation (video_production_cycle_id, provider_preflight_id, provider_account_id, status, reserved_credits, reserved_cost_usd, expires_at, reserved_at, created_at, updated_at) SELECT 31, p.id, a.id, 'RESERVED', 200.0000, 2.000000, '2026-09-03 11:01:00', '2026-09-03 10:01:00', '2026-09-03 10:01:00', '2026-09-03 10:01:00' FROM video_provider_preflight p JOIN video_provider_account a ON a.id=p.provider_account_id WHERE p.video_production_cycle_id=31;"; then
  echo "[MYSQL57] a reserva única aceitou duplicidade por ciclo" >&2
  exit 1
fi

mysql_value "UPDATE sales_video_provider_model SET manufacturer_name='Fabricante homologado' WHERE code='RUNWAY_SEEDANCE_2_5';"
mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGELOG_PREFIX}02-provider-model-backfill';"
mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGELOG_PREFIX}05-provider-account';"
mysql_value "DELETE FROM DATABASECHANGELOG WHERE ID='${CHANGELOG_PREFIX}06-provider-account-seed';"
compose run --rm liquibase-runway-plutus-preflight
assert_value "Fabricante homologado" \
  "$(mysql_value "SELECT manufacturer_name FROM sales_video_provider_model WHERE code='RUNWAY_SEEDANCE_2_5';")" \
  "backfill retomável preserva curadoria posterior"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM video_provider_account WHERE account_key='RUNWAY_PRIMARY';")" \
  "retomada não duplica conta agregadora"
assert_value "1" \
  "$(mysql_value "SELECT COUNT(*) FROM video_credit_reservation WHERE video_production_cycle_id=31;")" \
  "retomada preserva reserva existente"

compose run --rm liquibase-runway-plutus-preflight
compose run --rm liquibase-runway-product-ugc
assert_value "8" \
  "$(mysql_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '${CHANGELOG_PREFIX}%';")" \
  "reaplicação idempotente dos oito changesets"
assert_value "1" \
  "$(mysql_product_value "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID='2026-09-04-runway-product-ugc-premium-v1-01-provider-route';")" \
  "reaplicação idempotente da receita Product UGC"
assert_value "1" \
  "$(mysql_product_value "SELECT COUNT(*) FROM sales_video_provider_model WHERE code='runway-product-ugc-2026-06';")" \
  "receita Product UGC sem duplicidade"

echo "[MYSQL57] Preflight Runway/Plutus e Product UGC validados com identidade, DATETIME, FKs, retomada e reaplicação."
