#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
REPOSITORY_DIR=$(cd "${MODULE_DIR}/../.." && pwd)
COMPOSE_PROJECT=${AGENT_BOUNDARIES_COMPOSE_PROJECT:-aihub-26b70aa2-8ba1-4833-be14-5a89717f482c-d7473a975c}
COMPOSE_FILE=${MODULE_DIR}/docker-compose.agent-responsibility-boundaries-mysql57.yml

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans
}
trap cleanup EXIT

query() {
  compose exec -T mysql57-agent-responsibility-boundaries \
    mysql -umarketinghub -pmarketinghub-local -Dmarketinghub_local -N -B -e "$1"
}

assert_equals() {
  local expected=$1
  local actual=$2
  local message=$3
  if [[ "${actual}" != "${expected}" ]]; then
    printf 'ERRO: %s. Esperado=%s Atual=%s\n' "${message}" "${expected}" "${actual}" >&2
    exit 1
  fi
}

cd "${REPOSITORY_DIR}"
docker version >/dev/null
docker compose version >/dev/null
compose up -d --build mysql57-agent-responsibility-boundaries
compose run --rm --build liquibase-agent-responsibility-boundaries

assert_equals \
  $'experiment-strategist:4\ngrowth-operator:5\nmeta-ad-approver:3' \
  "$(query "SELECT CONCAT(agent_key, ':', current_version) FROM agent ORDER BY agent_key")" \
  "as versões dos agentes não avançaram preservando o histórico"
assert_equals \
  "3" \
  "$(query "SELECT COUNT(*) FROM agent_version WHERE (agent_id=1 AND version_number=4) OR (agent_id=2 AND version_number=5) OR (agent_id=3 AND version_number=3)")" \
  "as novas versões auditáveis não foram criadas"
assert_equals \
  $'4:RETIRED\n5:PUBLISHED' \
  "$(query "SELECT CONCAT(version_number, ':', status) FROM business_process_definition WHERE process_code='pde-communication-sales-journey' ORDER BY version_number")" \
  "a versão canônica do processo de comunicação está incorreta"
assert_equals \
  "4" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='pde-communication-sales-journey' AND p.version_number=5")" \
  "as atividades do processo de comunicação não foram materializadas"
assert_equals \
  $'6:RETIRED\n7:PUBLISHED' \
  "$(query "SELECT CONCAT(version_number, ':', status) FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' ORDER BY version_number")" \
  "a versão canônica da cadeia de valor está incorreta"
assert_equals \
  "6" \
  "$(query "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.chain_code='pde-value-creation-delivery' AND c.version_number=7")" \
  "a cadeia v7 não contém os seis processos esperados"

compose run --rm liquibase-agent-responsibility-boundaries

assert_equals \
  "6" \
  "$(query "SELECT COUNT(*) FROM agent_version WHERE agent_id IN (1,2,3)")" \
  "a reaplicação duplicou versões de agentes"
assert_equals \
  "6" \
  "$(query "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.chain_code='pde-value-creation-delivery' AND c.version_number=7")" \
  "a reaplicação duplicou itens da cadeia"

printf 'Homologação física das fronteiras Atena, Têmis e Hermes aprovada no MySQL 5.7.\n'
