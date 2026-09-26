#!/usr/bin/env bash
set -euo pipefail

IDENTITY_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IDENTITY_MODULE_DIR="$(cd "${IDENTITY_SCRIPT_DIR}/.." && pwd)"
IDENTITY_COMPOSE_FILE="${IDENTITY_MODULE_DIR}/docker-compose.process2-product-identity-mysql57.yml"
IDENTITY_COMPOSE_PROJECT="${PROCESS2_IDENTITY_COMPOSE_PROJECT:-marketing-hub-process2-product-identity}"

identity_compose() {
  docker compose -p "${IDENTITY_COMPOSE_PROJECT}" -f "${IDENTITY_COMPOSE_FILE}" "$@"
}

identity_cleanup() {
  identity_compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

identity_scalar() {
  identity_compose exec -T mysql57-process2-product-identity \
    mysql --default-character-set=utf8mb4 -N -s \
      -umarketinghub -pmarketinghub-local marketinghub_local -e "$1" 2>/dev/null
}

identity_assert_equal() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "${actual}" != "${expected}" ]]; then
    printf "Falha em %s: esperado '%s', obtido '%s'.\n" "${label}" "${expected}" "${actual}" >&2
    exit 1
  fi
}

# shellcheck disable=SC2016 # O classpath deve ser expandido somente dentro do container.
identity_liquibase() {
  local command="$1"
  identity_compose run --rm liquibase-process2-product-identity sh -lc \
    'IDENTITY_CP=target/classes:$(sed -n "1p" target/liquibase.classpath) && java -cp "$IDENTITY_CP" liquibase.integration.commandline.Main --driver=com.mysql.cj.jdbc.Driver --url="$ADS_LIQUIBASE_URL" --username="$ADS_LIQUIBASE_USERNAME" --password="$ADS_LIQUIBASE_PASSWORD" --changeLogFile="$ADS_LIQUIBASE_CHANGELOG_FILE" '"${command}"
}

identity_assert_applied() {
  identity_assert_equal \
    "versões publicadas" \
    "8:RETIRED,9:PUBLISHED|22:RETIRED,23:PUBLISHED" \
    "$(identity_scalar "SELECT CONCAT(
      (SELECT GROUP_CONCAT(CONCAT(version_number, ':', status) ORDER BY version_number SEPARATOR ',')
       FROM business_process_definition WHERE process_code='pde-commercial-plan-offer'), '|',
      (SELECT GROUP_CONCAT(CONCAT(version_number, ':', status) ORDER BY version_number SEPARATOR ',')
       FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery')
    );")"
  identity_assert_equal \
    "identidade reparada" \
    "Alcyone:AI_PRODUCT:Safira:PRODUCT_IDENTITY_V1:PRODUCT_IDENTITY_V1" \
    "$(identity_scalar "SELECT CONCAT(
      product.internal_name, ':', type_definition.code, ':', type_definition.internal_name, ':',
      JSON_UNQUOTE(JSON_EXTRACT(product.validation_definition_json, '$.productIdentity.contractVersion')), ':',
      JSON_UNQUOTE(JSON_EXTRACT(product.pde_experience_json, '$.productIdentity.contractVersion'))
    )
    FROM product product
    JOIN product_type_definition type_definition ON type_definition.id=product.product_type_id
    WHERE product.id=11;")"
  identity_assert_equal \
    "atividades e cadeia clonadas" \
    "3:1:9" \
    "$(identity_scalar "SELECT CONCAT(
      (SELECT COUNT(*) FROM business_process_activity_definition activity
       JOIN business_process_definition process ON process.id=activity.process_definition_id
       WHERE process.process_code='pde-commercial-plan-offer' AND process.version_number=9), ':',
      (SELECT COUNT(*) FROM business_process_chain_item item
       JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id
       WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=23), ':',
      (SELECT process.version_number FROM business_process_chain_item item
       JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id
       JOIN business_process_definition process ON process.id=item.process_definition_id
       WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=23)
    );")"
}

trap identity_cleanup EXIT
identity_cleanup

docker version >/dev/null
docker compose version >/dev/null
identity_compose up -d --build mysql57-process2-product-identity
identity_compose run --rm --build liquibase-process2-product-identity
identity_assert_applied

identity_compose run --rm liquibase-process2-product-identity
identity_assert_applied

identity_liquibase "rollbackCount 1"
identity_assert_equal \
  "rollback do reparo" \
  "Decisão de look para uma ocasião específica · PDE planejado #46:PDE:Opala" \
  "$(identity_scalar "SELECT CONCAT(product.internal_name, ':', type_definition.code, ':', type_definition.internal_name)
    FROM product product
    JOIN product_type_definition type_definition ON type_definition.id=product.product_type_id
    WHERE product.id=11;")"

identity_liquibase "rollbackCount 1"
identity_assert_equal \
  "rollback das versões" \
  "8:PUBLISHED,9:RETIRED|22:PUBLISHED,23:RETIRED" \
  "$(identity_scalar "SELECT CONCAT(
    (SELECT GROUP_CONCAT(CONCAT(version_number, ':', status) ORDER BY version_number SEPARATOR ',')
     FROM business_process_definition WHERE process_code='pde-commercial-plan-offer'), '|',
    (SELECT GROUP_CONCAT(CONCAT(version_number, ':', status) ORDER BY version_number SEPARATOR ',')
     FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery')
  );")"

identity_compose run --rm liquibase-process2-product-identity
identity_assert_applied

printf 'Validação física do Processo 2 e do reparo da execução #32 concluída.\n'
