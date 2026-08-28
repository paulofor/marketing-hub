#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
MODULE_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
REPOSITORY_DIR=$(cd "${MODULE_DIR}/../.." && pwd)
COMPOSE_PROJECT=${AGENT_BOUNDARIES_COMPOSE_PROJECT:-aihub-34eda72f-8630-4a67-b6ee-d2bd1c54dbe3-4877cec8ee}
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
    mysql --default-character-set=utf8mb4 \
    -umarketinghub -pmarketinghub-local -Dmarketinghub_local -N -B -e "$1"
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
  $'customer-agent:3\nexperiment-strategist:4\nfinancial-agent:3\ngrowth-operator:5\nlanding-generator:2\nmarket-radar:2\nmeta-ad-approver:3\nvideomaker:2' \
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

compose run --rm --build liquibase-agent-responsibility-matrix

assert_equals \
  $'customer-agent:4\nexperiment-strategist:5\nfinancial-agent:4\ngrowth-operator:6\nlanding-generator:3\nmarket-radar:3\nmeta-ad-approver:4\nvideomaker:3' \
  "$(query "SELECT CONCAT(agent_key, ':', current_version) FROM agent ORDER BY agent_key")" \
  "a matriz não versionou as oito identidades canônicas"
assert_equals \
  "19" \
  "$(query "SELECT COUNT(*) FROM agent_version")" \
  "as versões auditáveis da matriz não foram criadas uma única vez"
assert_equals \
  "11" \
  "$(query "SELECT COUNT(*) FROM business_process_definition WHERE status='PUBLISHED' AND technical_reference LIKE 'agent-responsibility-matrix-v3%'")" \
  "os onze processos corrigidos não foram publicados"
assert_equals \
  "58" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%'")" \
  "as atividades das versões corrigidas não foram materializadas"
assert_equals \
  "39" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%' AND JSON_EXTRACT(activity.definition_json, '$.responsibleAgentKeys') IS NOT NULL")" \
  "a quantidade de atividades atribuídas aos agentes divergiu"
assert_equals \
  $'pde-visual-materialization:landing-generator:themis-image-studio\nthemis-image-studio:landing-generator:themis-image-studio\nvideo-management-service:videomaker:video-management-service' \
  "$(query "SELECT CONCAT(resource_code, ':', responsible_agent_key, ':', executor_reference) FROM business_process_execution_resource WHERE resource_code IN ('pde-visual-materialization','themis-image-studio','video-management-service') ORDER BY resource_code")" \
  "os recursos técnicos não pertencem aos agentes materializadores corretos"
assert_equals \
  "0" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id JOIN business_process_execution_resource resource ON resource.resource_code=activity.execution_resource_code WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%' AND JSON_UNQUOTE(JSON_EXTRACT(activity.definition_json, '$.responsibleAgentKeys[0]')) <> resource.responsible_agent_key")" \
  "uma atividade usa recurso especializado de outro agente"
assert_equals \
  "0" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%' AND JSON_EXTRACT(activity.definition_json, '$.responsibleAgentKeys') IS NOT NULL AND JSON_LENGTH(JSON_EXTRACT(activity.definition_json, '$.responsibleAgentKeys')) <> 1")" \
  "uma atividade nova ainda possui coautoria"
assert_equals \
  "0" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%' AND activity.owner_name='Têmis' AND activity.activity_id NOT IN ('commercial', 'commercialIntegrityReview', 'task-6', 'integrity')")" \
  "Têmis ainda recebeu responsabilidade de criação"
assert_equals \
  $'Apolo:2\nDédalo:11' \
  "$(query "SELECT CONCAT(activity.owner_name, ':', COUNT(*)) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%' AND activity.owner_name IN ('Apolo', 'Dédalo') GROUP BY activity.owner_name ORDER BY activity.owner_name")" \
  "Dédalo e Apolo não receberam suas materializações exclusivas"
assert_equals \
  $'commercialIntegrityReview:meta-ad-approver:COMMERCIAL_INTEGRITY_REVIEW\nhumanExperienceReview:customer-agent:HUMAN_EXPERIENCE_REVIEW' \
  "$(query "SELECT CONCAT(activity.activity_id, ':', JSON_UNQUOTE(JSON_EXTRACT(activity.definition_json, '$.responsibleAgentKeys[0]')), ':', JSON_UNQUOTE(JSON_EXTRACT(activity.definition_json, '$.responsibilityDomain'))) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code='pde-commercial-homologation-activation' AND process.version_number=6 AND activity.activity_id IN ('humanExperienceReview','commercialIntegrityReview') ORDER BY activity.activity_id")" \
  "Psique e Têmis não ficaram em gates independentes"
assert_equals \
  "6" \
  "$(query "SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=8")" \
  "a cadeia v8 não contém os seis processos esperados"
assert_equals \
  $'ATENA:COMPLETED\nHERMES:COMPLETED\nPLUTUS:FAILED\nPSIQUE:FAILED' \
  "$(query "SELECT CONCAT(agent_key, ':', execution_status) FROM opportunity_agent_review ORDER BY agent_key")" \
  "os pareceres legados não foram preservados ou encerrados corretamente"
assert_equals \
  "READY_FOR_TEST" \
  "$(query "SELECT status FROM opportunity_dossier WHERE id=1")" \
  "a estratégia concluída de Atena não liberou o dossiê histórico"

compose run --rm liquibase-agent-responsibility-matrix

assert_equals \
  "19" \
  "$(query "SELECT COUNT(*) FROM agent_version")" \
  "a reaplicação da matriz duplicou versões"
assert_equals \
  "58" \
  "$(query "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.technical_reference LIKE 'agent-responsibility-matrix-v3%'")" \
  "a reaplicação da matriz duplicou atividades"
assert_equals \
  "2" \
  "$(query "SELECT COUNT(*) FROM opportunity_agent_review WHERE error_message LIKE 'SUPERSEDED_BY_AGENT_RESPONSIBILITY_MATRIX_V1:%'")" \
  "a reaplicação alterou o encerramento auditável dos pareceres legados"

printf 'Homologação física da matriz dos oito agentes aprovada no MySQL 5.7.\n'
