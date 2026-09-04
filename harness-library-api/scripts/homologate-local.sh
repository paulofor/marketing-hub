#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPOSITORY_ROOT="$(cd "${MODULE_ROOT}/.." && pwd)"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.homologation.yml"
COMPOSE_PROJECT="${HARNESS_LIBRARY_E2E_PROJECT:-aihub-949955b8-7dd6-45ea-b31b-bd057304f08e-a1d71c2c3c}"
PUBLIC_KEY="local-public-api-key-000000000000000000001"
INTERNAL_KEY="local-internal-signing-key-0000000000000001"
ACTOR="codex-homologacao"
REQUEST_ID="00000000-0000-4000-8000-000000000001"
CARD_KEY="homologacao-harness-api-e2e"
EXPIRED_CARD_KEY="homologacao-harness-api-expired"
PUBLIC_DOMAIN="mkthub.api.br"

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

fail() {
  echo "[E2E] $1" >&2
  exit 1
}

assert_code() {
  local expected="$1"
  local scenario="$2"
  [[ "${HTTP_CODE}" == "${expected}" ]] \
    || fail "${scenario}: HTTP esperado=${expected}, obtido=${HTTP_CODE}, body=${HTTP_BODY}"
}

assert_json() {
  local expression="$1"
  local scenario="$2"
  jq -e "${expression}" >/dev/null <<<"${HTTP_BODY}" \
    || fail "${scenario}: resposta inesperada=${HTTP_BODY}"
}

invoke_gateway() {
  local method="$1"
  local path="$2"
  local idempotency_key="$3"
  local body="$4"
  local command=(
    exec -T harness-library-api curl --noproxy '*' --insecure --silent --show-error
    -w $'\n%{http_code}' -X "${method}" "https://${PUBLIC_DOMAIN}${path}"
    -H "X-API-Key: ${PUBLIC_KEY}"
    -H "X-Actor: ${ACTOR}"
    -H "X-Request-ID: ${REQUEST_ID}"
  )
  if [[ -n "${idempotency_key}" ]]; then
    command+=(-H "Idempotency-Key: ${idempotency_key}")
  fi
  if [[ -n "${body}" ]]; then
    command+=(-H "Content-Type: application/json" --data-binary "${body}")
  fi
  local raw
  raw="$(compose "${command[@]}")"
  HTTP_CODE="${raw##*$'\n'}"
  HTTP_BODY="${raw%$'\n'*}"
}

wait_for_gateway() {
  for attempt in $(seq 1 30); do
    if compose exec -T harness-library-api \
      curl -fsS --max-time 5 http://127.0.0.1:9103/actuator/health/liveness \
      >/dev/null 2>&1; then
      return
    fi
    [[ "${attempt}" -lt 30 ]] || fail "health do gateway não respondeu"
    sleep 2
  done
}

trap cleanup EXIT
command -v jq >/dev/null || fail "jq é obrigatório para a homologação"
cleanup

if [[ "${HARNESS_LIBRARY_E2E_SKIP_PACKAGE:-false}" != "true" ]]; then
  mvn -B -q -f "${REPOSITORY_ROOT}/backend/ads-service/pom.xml" -DskipTests package
fi

compose up -d --build --wait --wait-timeout 600
wait_for_gateway

REDIRECT_HEADERS="$(compose exec -T harness-library-api \
  curl --noproxy '*' --silent --show-error --head \
  "http://${PUBLIC_DOMAIN}/v1/cards")"
grep -Eq '^HTTP/[0-9.]+ 301' <<<"${REDIRECT_HEADERS}" \
  || fail "proxy HTTP não redirecionou para HTTPS"
grep -Fiq "location: https://${PUBLIC_DOMAIN}/v1/cards" <<<"${REDIRECT_HEADERS}" \
  || fail "proxy HTTP não preservou a rota no redirecionamento"

HTTP_CODE="$(compose exec -T harness-library-api \
  curl --noproxy '*' --insecure --silent --show-error -o /dev/null -w '%{http_code}' \
  "https://${PUBLIC_DOMAIN}/v1/cards")"
[[ "${HTTP_CODE}" == "401" ]] || fail "rota sem API key deveria retornar 401"

HTTPS_HEADERS="$(compose exec -T harness-library-api \
  curl --noproxy '*' --insecure --silent --show-error --head \
  "https://${PUBLIC_DOMAIN}/v1/cards")"
grep -Fiq 'strict-transport-security: max-age=31536000' <<<"${HTTPS_HEADERS}" \
  || fail "proxy HTTPS não aplicou HSTS"

ACTUATOR_CODE="$(compose exec -T harness-library-api \
  curl --noproxy '*' --insecure --silent --show-error -o /dev/null -w '%{http_code}' \
  "https://${PUBLIC_DOMAIN}/actuator/health")"
[[ "${ACTUATOR_CODE}" == "404" ]] || fail "proxy HTTPS expôs o Actuator"

SOURCE_CONTENT="Fonte sintética segregada para homologar o cadastro externo do Harness v1."
SOURCE_SHA="$(printf '%s' "${SOURCE_CONTENT}" | sha256sum | awk '{print $1}')"
PAYLOAD="$(jq -nc \
  --arg cardKey "${CARD_KEY}" \
  --arg sourceSha "${SOURCE_SHA}" \
  '{cardKey:$cardKey,collection:"video",title:"Demonstração do produto reduz ambiguidade",finding:"Mostrar a experiência real torna o valor mais concreto.",mechanism:"A demonstração reduz o esforço mental para compreender a oferta.",commercialApplication:"Comparar um vídeo demonstrativo contra uma peça apenas narrativa.",evidenceStrength:"Hipótese externa que exige experimento controlado.",publishedOn:"2026-09-04",validUntil:"2026-10-19",experimentHypothesis:"A demonstração elevará cliques qualificados sem aumentar rejeição.",risks:"Não generalizar o achado para públicos não observados.",limits:"Somente pagamentos reconciliados comprovam vendas.",sourceKind:"TEXT",sourceUri:"urn:marketing-hub:homologacao:harness-api-e2e",sourceTitle:"Fonte sintética segregada de homologação",sourceSha256:$sourceSha}')"

UNKNOWN_COLLECTION_PAYLOAD="$(jq -c '.cardKey="homologacao-colecao-invalida" | .collection="inventada"' <<<"${PAYLOAD}")"
invoke_gateway POST /v1/cards e2e-invalid-collection-0001 "${UNKNOWN_COLLECTION_PAYLOAD}"
assert_code 400 "coleção desconhecida"

UNKNOWN_FIELD_PAYLOAD="$(jq -c '.unexpected=true' <<<"${PAYLOAD}")"
invoke_gateway POST /v1/cards e2e-unknown-field-0000001 "${UNKNOWN_FIELD_PAYLOAD}"
assert_code 400 "campo JSON desconhecido"

OVERSIZED_VALUE="$(printf '%32769s' '' | tr ' ' x)"
OVERSIZED_PAYLOAD="$(jq -nc --arg value "${OVERSIZED_VALUE}" '{finding:$value}')"
invoke_gateway POST /v1/cards e2e-oversized-json-000001 "${OVERSIZED_PAYLOAD}"
assert_code 413 "JSON acima do limite físico"

invoke_gateway POST /v1/cards e2e-register-v1-00000001 "${PAYLOAD}"
assert_code 201 "cadastro da versão 1"
assert_json '.cardKey == "homologacao-harness-api-e2e" and .version == 1 and .status == "DRAFT"' \
  "rascunho da versão 1"
FIRST_CARD_ID="$(jq -r '.cardId' <<<"${HTTP_BODY}")"

invoke_gateway POST /v1/cards e2e-register-v1-00000001 "${PAYLOAD}"
assert_code 201 "repetição idempotente do cadastro"
assert_json ".cardId == \"${FIRST_CARD_ID}\" and .version == 1" "mesma versão idempotente"

CHANGED_PAYLOAD="$(jq -c '.title="Payload divergente"' <<<"${PAYLOAD}")"
invoke_gateway POST /v1/cards e2e-register-v1-00000001 "${CHANGED_PAYLOAD}"
assert_code 409 "chave idempotente com payload divergente"

TRANSITION='{"reason":"Fonte, aplicação e limites conferidos em homologação."}'
invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/1/activate" e2e-activate-before-review-01 "${TRANSITION}"
assert_code 409 "ativação antes da revisão"

invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/1/submit-review" e2e-review-v1-000000000001 "${TRANSITION}"
assert_code 200 "submissão da versão 1"
assert_json '.status == "IN_REVIEW"' "estado em revisão"

invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/1/submit-review" e2e-review-v1-000000000001 "${TRANSITION}"
assert_code 200 "repetição da revisão"
assert_json '.status == "IN_REVIEW"' "revisão idempotente por estado"

invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/1/activate" e2e-activate-v1-0000000001 "${TRANSITION}"
assert_code 200 "ativação da versão 1"
assert_json '.status == "ACTIVE" and .effectiveStatus == "ACTIVE"' "versão 1 ativa"

CATALOG="$(compose exec -T harness-library-api curl -fsS http://backend:8000/api/research-intelligence/v1/catalog)"
jq -e --arg source "urn:marketing-hub:homologacao:harness-api-e2e" \
  '[.cards[] | select(.sourcePath == $source)] | length == 1' >/dev/null <<<"${CATALOG}" \
  || fail "versão ativa não apareceu no catálogo global"

VERSION_TWO_PAYLOAD="$(jq -c '.title="Demonstração do produto reduz ambiguidade v2" | .finding="A versão revisada prioriza o primeiro segundo do vídeo."' <<<"${PAYLOAD}")"
invoke_gateway POST /v1/cards e2e-register-v2-00000001 "${VERSION_TWO_PAYLOAD}"
assert_code 201 "cadastro da versão 2"
assert_json '.version == 2 and .status == "DRAFT"' "rascunho da versão 2"

invoke_gateway GET "/v1/cards/${CARD_KEY}/versions/1" "" ""
assert_code 200 "consulta da versão 1 durante edição"
assert_json '.status == "ACTIVE"' "versão 1 permanece ativa até substituição"

invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/2/submit-review" e2e-review-v2-000000000001 "${TRANSITION}"
assert_code 200 "submissão da versão 2"
invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/2/activate" e2e-activate-v2-0000000001 "${TRANSITION}"
assert_code 200 "ativação da versão 2"
assert_json '.status == "ACTIVE"' "versão 2 ativa"

invoke_gateway GET "/v1/cards/${CARD_KEY}/versions/1" "" ""
assert_code 200 "consulta da versão substituída"
assert_json '.status == "ARCHIVED"' "versão 1 arquivada atomicamente"

invoke_gateway GET "/v1/cards?status=ACTIVE&collection=video&limit=20" "" ""
assert_code 200 "filtros de gestão"
assert_json '[.items[] | select(.cardKey == "homologacao-harness-api-e2e" and .version == 2)] | length == 1' \
  "listagem contém somente a versão ativa"

invoke_gateway GET "/v1/cards?limit=20" "" ""
assert_code 200 "listagem de gestão sem filtros"
assert_json '[.items[] | select(.cardKey == "homologacao-harness-api-e2e")] | length == 2' \
  "listagem sem filtros preserva o histórico versionado"

invoke_gateway POST "/v1/cards/${CARD_KEY}/versions/2/archive" e2e-archive-v2-0000000001 "${TRANSITION}"
assert_code 200 "arquivamento da versão 2"
assert_json '.status == "ARCHIVED"' "versão 2 arquivada"

CATALOG="$(compose exec -T harness-library-api curl -fsS http://backend:8000/api/research-intelligence/v1/catalog)"
jq -e --arg source "urn:marketing-hub:homologacao:harness-api-e2e" \
  '[.cards[] | select(.sourcePath == $source)] | length == 0' >/dev/null <<<"${CATALOG}" \
  || fail "cartão arquivado permaneceu no catálogo global"

EXPIRED_PAYLOAD="$(jq -c --arg cardKey "${EXPIRED_CARD_KEY}" \
  '.cardKey=$cardKey | .publishedOn="2026-08-01" | .validUntil="2026-08-31" | .sourceUri="urn:marketing-hub:homologacao:harness-api-expired"' <<<"${PAYLOAD}")"
invoke_gateway POST /v1/cards e2e-register-expired-000001 "${EXPIRED_PAYLOAD}"
assert_code 201 "cadastro vencido auditável"
invoke_gateway POST "/v1/cards/${EXPIRED_CARD_KEY}/versions/1/submit-review" e2e-review-expired-0000001 "${TRANSITION}"
assert_code 200 "revisão do cartão vencido"
invoke_gateway POST "/v1/cards/${EXPIRED_CARD_KEY}/versions/1/activate" e2e-activate-expired-00001 "${TRANSITION}"
assert_code 409 "bloqueio do cartão vencido"

DIRECT_CODE="$(compose exec -T harness-library-api sh -c \
  'timestamp=$(date +%s); curl -sS -o /dev/null -w "%{http_code}" "http://backend:8000/api/internal/research-intelligence/v1/cards?limit=1" -H "X-Actor: codex-homologacao" -H "X-Harness-Request-Id: 00000000-0000-4000-8000-000000000002" -H "X-Harness-Timestamp: ${timestamp}" -H "X-Harness-Content-SHA256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" -H "X-Harness-Signature: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"')"
[[ "${DIRECT_CODE}" == "401" ]] || fail "backend aceitou HMAC inválido"

compose restart harness-library-api >/dev/null
wait_for_gateway
invoke_gateway GET "/v1/cards/${CARD_KEY}/versions/2" "" ""
assert_code 200 "persistência após reinício do gateway"
assert_json '.status == "ARCHIVED"' "gateway sem estado preservou auditoria"

PUBLIC_METRICS_CODE="$(compose exec -T harness-library-api curl -sS -o /dev/null -w '%{http_code}' \
  http://127.0.0.1:8103/actuator/prometheus)"
[[ "${PUBLIC_METRICS_CODE}" == "404" ]] || fail "métricas ficaram expostas na porta pública"

compose exec -T harness-library-api curl -fsS http://127.0.0.1:9103/actuator/prometheus \
  | grep -q 'http_server_requests_seconds_count' \
  || fail "métrica HTTP não foi exposta"

CONTAINER_ID="$(compose ps -q harness-library-api)"
RUNTIME_USER="$(docker inspect --format '{{.Config.User}}' "${CONTAINER_ID}")"
[[ "${RUNTIME_USER}" == "10001:10001" ]] \
  || fail "gateway não preservou a identidade fixa 10001:10001"
IMAGE_ID="$(docker inspect --format '{{.Image}}' "${CONTAINER_ID}")"
IMAGE_METADATA="$(docker image inspect "${IMAGE_ID}")"
grep -Fq "${PUBLIC_KEY}" <<<"${IMAGE_METADATA}" && fail "API key foi incorporada à imagem"
grep -Fq "${INTERNAL_KEY}" <<<"${IMAGE_METADATA}" && fail "chave interna foi incorporada à imagem"

APPLICATION_LOGS="$(compose logs --no-color backend harness-library-api)"
grep -Fq "${PUBLIC_KEY}" <<<"${APPLICATION_LOGS}" && fail "API key apareceu nos logs"
grep -Fq "${INTERNAL_KEY}" <<<"${APPLICATION_LOGS}" && fail "chave interna apareceu nos logs"
grep -Fq 'operation=ingest method=POST path=/v1/cards payload={"cardKey":"homologacao-harness-api-e2e"' \
  <<<"${APPLICATION_LOGS}" || fail "payload bruto válido não foi auditado antes da desserialização"
grep -Fq '"unexpected":true' <<<"${APPLICATION_LOGS}" \
  || fail "payload bruto inválido não foi auditado antes da rejeição"

PERSISTED="$(compose exec -T mysql mysql -uroot -proot --batch --skip-column-names ads \
  -e "SELECT CONCAT((SELECT COUNT(*) FROM research_intelligence_card),(SELECT COUNT(*) FROM research_intelligence_card_version),(SELECT COUNT(*) FROM research_intelligence_card_version WHERE status='ARCHIVED'),(SELECT COUNT(*) FROM research_intelligence_card_version WHERE status='IN_REVIEW'));" 2>/dev/null)"
[[ "${PERSISTED}" == "2321" ]] || fail "auditoria persistida divergente: ${PERSISTED}"

compose stop backend >/dev/null
invoke_gateway GET "/v1/cards?limit=1" "" ""
[[ "${HTTP_CODE}" == "502" || "${HTTP_CODE}" == "504" ]] \
  || fail "falha do backend deveria ser traduzida em 502/504, obtido=${HTTP_CODE}"

echo "[E2E] Cadastro, revisão, ativação, versionamento, arquivamento, segurança e observabilidade validados."
