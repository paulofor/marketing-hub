#!/usr/bin/env bash
set -euo pipefail

backend_url="${BACKEND_URL:-http://127.0.0.1:18031}"

for attempt in $(seq 1 30); do
  if curl -fsS --connect-timeout 2 --max-time 4 "${backend_url}/ops-mh-observability-v2/health" >/dev/null 2>&1; then
    break
  fi
  if [ "${attempt}" = "30" ]; then
    echo "Backend local não ficou acessível após 30 tentativas." >&2
    exit 1
  fi
  sleep 1
done

resources="$(curl -fsS "${backend_url}/api/business-process-execution-resources")"
test "$(jq -r 'length' <<<"${resources}")" = "1"
test "$(jq -r '.[0].resourceCode' <<<"${resources}")" = "themis-image-studio"
test "$(jq -r '.[0].executorReference' <<<"${resources}")" = "themis-image-studio"

process_payload='{
  "processCode":"local-specialized-product-v1",
  "name":"Homologação local de entregáveis premium",
  "purpose":"Comprovar o roteamento de um entregável visual sem executar produção externa.",
  "ownerName":"Têmis",
  "triggerDescription":"Plano local aprovado.",
  "outcomeDescription":"Atividade roteada ao Estúdio local.",
  "versionNumber":1,
  "technicalReference":"themis-image-studio",
  "diagram":{
    "nodes":[
      {"id":"start","type":"START","label":"Início local"},
      {"id":"deliverables","type":"TASK","label":"Produzir entregáveis premium","owner":"Têmis","description":"Usar o Estúdio isolado.","executionResourceCode":"themis-image-studio"},
      {"id":"end","type":"END","label":"Fim local"}
    ],
    "flows":[
      {"from":"start","to":"deliverables"},
      {"from":"deliverables","to":"end"}
    ]
  }
}'
process="$(curl -fsS -X POST -H 'Content-Type: application/json' -d "${process_payload}" "${backend_url}/api/business-processes")"
process_id="$(jq -r '.id' <<<"${process}")"
test "$(jq -r '.diagram.nodes[1].executionResourceCode' <<<"${process}")" = "themis-image-studio"

published="$(curl -fsS -X POST "${backend_url}/api/business-processes/${process_id}/publish")"
test "$(jq -r '.status' <<<"${published}")" = "PUBLISHED"

theme="$(curl -fsS -X POST -H 'Content-Type: application/json' -d '{"name":"Homologação local","description":"Tema isolado para os dados da sandbox."}' "${backend_url}/api/agent-themes")"
theme_id="$(jq -r '.id' <<<"${theme}")"
agent_payload="$(jq -n --argjson themeId "${theme_id}" '{
  name:"Têmis local",
  nickname:"Têmis",
  agentKey:"meta-ad-approver",
  status:"ACTIVE",
  executionMode:"CONTAINER",
  themeId:$themeId,
  inputs:[],
  outputs:[],
  internalFunctions:[]
}')"
curl -fsS -X POST -H 'Content-Type: application/json' -d "${agent_payload}" "${backend_url}/api/agents" >/dev/null

task_payload="$(jq -n --argjson processId "${process_id}" '{
  assignedAgentKey:"meta-ad-approver",
  requestedByName:"Homologação local",
  title:"Produzir entregáveis premium",
  description:"Comprovar o recurso exigido sem chamar IA real.",
  priority:"HIGH",
  sourceReference:"local-process-resource:1",
  processDefinitionId:$processId,
  processActivityId:"deliverables",
  exceptional:false
}')"
task="$(curl -fsS -X POST -H 'Content-Type: application/json' -d "${task_payload}" "${backend_url}/api/agent-tasks")"
task_id="$(jq -r '.id' <<<"${task}")"
test "$(jq -r '.costEstimationStatus' <<<"${task}")" = "NOT_REPORTED"

generic="$(curl -fsS "${backend_url}/api/internal/agent-tasks/meta-ad-approver/stage-executions/pending?processCode=local-specialized-product-v1&activityId=deliverables")"
test "$(jq -r 'length' <<<"${generic}")" = "0"

specialized="$(curl -fsS "${backend_url}/api/internal/agent-tasks/meta-ad-approver/stage-executions/pending?processCode=local-specialized-product-v1&activityId=deliverables&executionResourceCode=themis-image-studio")"
test "$(jq -r '.[0].taskId' <<<"${specialized}")" = "${task_id}"
test "$(jq -r '.[0].executionResource.resourceCode' <<<"${specialized}")" = "themis-image-studio"
test "$(jq -r '.[0].executionResource.executorReference' <<<"${specialized}")" = "themis-image-studio"
test "$(jq -r '.[0].executionResource.usageInstructions | contains("pending")' <<<"${specialized}")" = "true"

invalid_payload="$(sed 's/themis-image-studio/missing-resource/g; s/local-specialized-product-v1/local-invalid-resource-v1/g; s/Homologação local de entregáveis premium/Homologação local inválida/g' <<<"${process_payload}")"
status="$(curl -sS -o /tmp/business-process-resource-invalid-response.json -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d "${invalid_payload}" "${backend_url}/api/business-processes")"
test "${status}" = "400"
grep -q "não está disponível" /tmp/business-process-resource-invalid-response.json

echo "Homologação de recurso especializado concluída: processId=${process_id} taskId=${task_id}"
