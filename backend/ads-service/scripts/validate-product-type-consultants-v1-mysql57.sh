#!/usr/bin/env bash
set -euo pipefail

PRODUCT_TYPE_V1_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRODUCT_TYPE_V1_MODULE_DIR="$(cd "${PRODUCT_TYPE_V1_SCRIPT_DIR}/.." && pwd)"
PRODUCT_TYPE_V1_COMPOSE_FILE="${PRODUCT_TYPE_V1_MODULE_DIR}/docker-compose.product-type-consultants-v1-mysql57.yml"
PRODUCT_TYPE_V1_COMPOSE_PROJECT="${PRODUCT_TYPE_CONSULTANTS_COMPOSE_PROJECT:-aihub-7e0fbc82-3630-4e7b-b7da-b3db83a19811-870fb7832c}"

product_type_v1_compose() {
  docker compose -p "${PRODUCT_TYPE_V1_COMPOSE_PROJECT}" -f "${PRODUCT_TYPE_V1_COMPOSE_FILE}" "$@"
}

product_type_v1_cleanup() {
  product_type_v1_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

product_type_v1_db_scalar() {
  product_type_v1_compose exec -T mysql57-product-type-consultants-v1 \
    mysql --default-character-set=utf8mb4 -N -s \
    -umarketinghub -pmarketinghub-local marketinghub_local -e "$1" 2>/dev/null
}

product_type_v1_db_execute() {
  product_type_v1_compose exec -T mysql57-product-type-consultants-v1 \
    mysql --default-character-set=utf8mb4 \
    -umarketinghub -pmarketinghub-local marketinghub_local -e "$1" >/dev/null 2>&1
}

product_type_v1_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Falha em ${label}: esperado '${expected}', obtido '${actual}'." >&2
    exit 1
  fi
}

trap product_type_v1_cleanup EXIT
product_type_v1_cleanup

docker version >/dev/null
docker compose version >/dev/null
product_type_v1_compose up -d --build mysql57-product-type-consultants-v1
product_type_v1_compose run --rm --build liquibase-product-type-consultants-v1

product_type_v1_db_execute "DELETE FROM DATABASECHANGELOG
  WHERE ID IN ('2026-08-29-product-type-consultant-blueprints-v1-structure',
               '2026-08-29-product-type-consultant-blueprints-v1-seed');"
product_type_v1_compose run --rm liquibase-product-type-consultants-v1

product_type_v1_assert_equal \
  "colunas da base de construção" \
  "13" \
  "$(product_type_v1_db_scalar "SELECT COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = 'marketinghub_local'
       AND table_name = 'product_type_definition'
       AND column_name IN
         ('blueprint_version', 'primary_channel', 'customer_job', 'value_mechanism',
          'experience_flow', 'required_inputs', 'expected_outputs', 'memory_strategy',
          'integration_requirements', 'safety_guardrails', 'success_metrics',
          'backend_sdk_module', 'frontend_sdk_module');")"

product_type_v1_assert_equal \
  "identidades dos consultores" \
  "AI_PWA_CONSULTANT_PRODUCT:Consultor PWA com IA:Turmalina:PWA:consultant-pwa-v1|AI_SANDBOX_CONVERSATIONAL_PRODUCT:Consultor WhatsApp com IA:Fluorita:WHATSAPP:consultant-whatsapp-v1" \
  "$(product_type_v1_db_scalar "SELECT GROUP_CONCAT(
      CONCAT(code, ':', name, ':', internal_name, ':', primary_channel, ':', blueprint_version)
      ORDER BY code SEPARATOR '|')
    FROM product_type_definition
    WHERE code IN ('AI_PWA_CONSULTANT_PRODUCT', 'AI_SANDBOX_CONVERSATIONAL_PRODUCT');")"

product_type_v1_assert_equal \
  "bases completas" \
  "2" \
  "$(product_type_v1_db_scalar "SELECT COUNT(*)
    FROM product_type_definition
    WHERE code IN ('AI_PWA_CONSULTANT_PRODUCT', 'AI_SANDBOX_CONVERSATIONAL_PRODUCT')
      AND blueprint_version IS NOT NULL
      AND primary_channel IS NOT NULL
      AND customer_job IS NOT NULL
      AND value_mechanism IS NOT NULL
      AND experience_flow IS NOT NULL
      AND required_inputs IS NOT NULL
      AND expected_outputs IS NOT NULL
      AND memory_strategy IS NOT NULL
      AND integration_requirements IS NOT NULL
      AND safety_guardrails IS NOT NULL
      AND success_metrics IS NOT NULL
      AND backend_sdk_module = 'pde-platform/pde-harness-sdk';")"

product_type_v1_assert_equal \
  "SDK React exclusivo da PWA" \
  "pde-platform/frontend/src/consultant-sdk/v1:NULL" \
  "$(product_type_v1_db_scalar "SELECT CONCAT(
      MAX(CASE WHEN code = 'AI_PWA_CONSULTANT_PRODUCT' THEN frontend_sdk_module END), ':',
      IFNULL(MAX(CASE WHEN code = 'AI_SANDBOX_CONVERSATIONAL_PRODUCT'
                      THEN frontend_sdk_module END), 'NULL'))
    FROM product_type_definition;")"

product_type_v1_assert_equal \
  "produto histórico preservado" \
  "8:Consultor WhatsApp com IA:AI_SANDBOX_CONVERSATIONAL_PRODUCT" \
  "$(product_type_v1_db_scalar "SELECT CONCAT(
      product_record.id, ':', product_record.product_type, ':', definition.code)
    FROM product product_record
    JOIN product_type_definition definition ON definition.id = product_record.product_type_id
    WHERE product_record.id = 8;")"

product_type_v1_assert_equal \
  "apelidos sem duplicação após retomada" \
  "7:7" \
  "$(product_type_v1_db_scalar "SELECT CONCAT(COUNT(*), ':', COUNT(DISTINCT alias))
    FROM product_type_alias;")"

echo "Tipos Consultor PWA/WhatsApp v1 aprovados fisicamente no MySQL 5.7."
