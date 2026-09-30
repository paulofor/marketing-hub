#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.product-isolation-validation.yml"
COMPOSE_PROJECT="${PDE_LOCAL_COMPOSE_PROJECT:-pde-product-isolation-validation}"
TOPOLOGY_STARTED=0

compose() {
  docker compose -p "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  if [[ "${TOPOLOGY_STARTED}" == "1" ]]; then
    compose down --volumes --remove-orphans
  fi
}

trap cleanup EXIT

docker version >/dev/null
docker compose version >/dev/null
compose config --quiet
TOPOLOGY_STARTED=1
compose up -d --build --wait

curl_v7() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "v7.clubemusa.com.br:443:127.0.0.1" \
    "https://v7.clubemusa.com.br$1"
}

curl_v5() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "v5.clubemusa.com.br:443:127.0.0.1" \
    "https://v5.clubemusa.com.br$1"
}

curl_v6() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "v6.clubemusa.com.br:443:127.0.0.1" \
    "https://v6.clubemusa.com.br$1"
}

curl_v8() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "v8.clubemusa.com.br:443:127.0.0.1" \
    "https://v8.clubemusa.com.br$1"
}

curl_mira_commercial() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "mira.digicomdigital.com.br:443:127.0.0.1" \
    "https://mira.digicomdigital.com.br$1"
}

curl_alcyone() {
  compose exec -T proxy curl --fail --silent --show-error --insecure \
    --resolve "alcyone.digicomdigital.com.br:443:127.0.0.1" \
    "https://alcyone.digicomdigital.com.br$1"
}

validate_mira_commercial_media() {
  local contract required_asset required_hls_stream
  contract="$(curl_mira_commercial /pde-health-contract.json)"
  jq -e '
    (.requiredAssets | type == "array" and length > 0)
    and (.requiredHlsStreams | type == "array" and length > 0)
    and all(.requiredAssets[]; type == "string" and startswith("/"))
    and all(.requiredHlsStreams[]; type == "string" and startswith("/"))
  ' <<<"${contract}" >/dev/null

  while IFS= read -r required_asset; do
    curl_mira_commercial "${required_asset}" >/dev/null
  done < <(jq -r '.requiredAssets[]' <<<"${contract}")

  while IFS= read -r required_hls_stream; do
    curl_mira_commercial "${required_hls_stream}" | grep -q '^#EXTM3U'
  done < <(jq -r '.requiredHlsStreams[]' <<<"${contract}")
}

if ! command -v jq >/dev/null 2>&1; then
  echo '[ARQUITETURA] jq e obrigatorio para validar os ativos declarados pela superficie comercial de Mira.' >&2
  exit 1
fi

vega_v5_diagnostics="$(curl_v5 /version-diagnostics.json)"
grep -q '"imageVersionId": "v5"' <<<"${vega_v5_diagnostics}"
grep -q '"experienceVersion": "musa-pde-entry-v5-video-explicativo"' \
  <<<"${vega_v5_diagnostics}"

vega_v6_diagnostics="$(curl_v6 /version-diagnostics.json)"
grep -q '"imageVersionId": "v6"' <<<"${vega_v6_diagnostics}"
grep -q '"experienceVersion": "musa-pde-entry-v6-video-motivacional"' \
  <<<"${vega_v6_diagnostics}"

vega_diagnostics="$(curl_v7 /version-diagnostics.json)"
grep -q '"productSlug": "metodo-musa-7-dias"' <<<"${vega_diagnostics}"
grep -q '"imageVersionId": "v7"' <<<"${vega_diagnostics}"

vega_v12_diagnostics="$(curl_v8 /version-diagnostics.json)"
grep -q '"productSlug": "metodo-musa-7-dias"' <<<"${vega_v12_diagnostics}"
grep -q '"imageVersionId": "v8"' <<<"${vega_v12_diagnostics}"
grep -q '"experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel"' \
  <<<"${vega_v12_diagnostics}"
grep -q '"publicUrl": "https://v8.clubemusa.com.br"' <<<"${vega_v12_diagnostics}"

mira_html="$(curl_v7 /mira-private)"
grep -q 'Sua rotina, organizada com calma' <<<"${mira_html}"
mira_diagnostics="$(curl_v7 /mira-private/version-diagnostics.json)"
grep -q '"surface": "pde-platform-frontend-mira"' <<<"${mira_diagnostics}"
grep -q '"productId": 10' <<<"${mira_diagnostics}"
grep -q '"productSlug": "pde-planejado-36"' <<<"${mira_diagnostics}"
grep -Eq '"frontendSourceSha256": "[0-9a-f]{64}"' <<<"${mira_diagnostics}"

mira_asset="$(sed -n 's/.*src="\([^"]*\.js\)".*/\1/p' <<<"${mira_html}")"
test -n "${mira_asset}"
curl_v7 "${mira_asset}" | grep -q 'mira-private'
if curl_v7 /mira-private/subrota-inexistente >/dev/null 2>&1; then
  echo '[ARQUITETURA] O proxy público aceitou uma subrota privada não contratada de Mira.' >&2
  exit 1
fi

if compose exec -T pde-platform-frontend-v7 \
  wget --quiet --spider http://127.0.0.1/mira-private 2>/dev/null; then
  echo '[ARQUITETURA] O container de Vega ainda entrega a rota de Mira.' >&2
  exit 1
fi

alcyone_html="$(curl_alcyone /)"
alcyone_diagnostics="$(curl_alcyone /version-diagnostics.json)"
grep -q '"surface": "pde-platform-frontend-alcyone"' <<<"${alcyone_diagnostics}"
grep -q '"productId": 11' <<<"${alcyone_diagnostics}"
grep -q '"productSlug": "pde-planejado-46"' <<<"${alcyone_diagnostics}"
grep -q '"experienceVersion": "alcyone-private-v3"' <<<"${alcyone_diagnostics}"
grep -Eq '"frontendSourceSha256": "[0-9a-f]{64}"' <<<"${alcyone_diagnostics}"
curl_alcyone /assets/alcyone/manifest.json \
  | grep -q '"contractVersion": "PDE_STATIC_RESULT_FIXTURES_V1"'

alcyone_asset="$(sed -n 's/.*src="\([^"]*\.js\)".*/\1/p' <<<"${alcyone_html}")"
test -n "${alcyone_asset}"
alcyone_bundle="$(curl_alcyone "${alcyone_asset}")"
grep -q 'Três caminhos claros para a sua ocasião' <<<"${alcyone_bundle}"
grep -q 'alcyone-agent-validation-session' <<<"${alcyone_bundle}"
if curl_alcyone /subrota-inexistente >/dev/null 2>&1; then
  echo '[ARQUITETURA] O proxy público aceitou uma subrota privada não contratada de Alcyone.' >&2
  exit 1
fi

mira_commercial_html="$(curl_mira_commercial /)"
mira_commercial_asset="$(sed -n 's/.*src="\([^"]*\.js\)".*/\1/p' <<<"${mira_commercial_html}")"
test -n "${mira_commercial_asset}"
mira_commercial_bundle="$(curl_mira_commercial "${mira_commercial_asset}")"
grep -q 'Cuide de você com mais clareza' <<<"${mira_commercial_bundle}"
validate_mira_commercial_media
mira_commercial_diagnostics="$(curl_mira_commercial /version-diagnostics.json)"
grep -q '"surface": "pde-platform-frontend-mira-commercial"' \
  <<<"${mira_commercial_diagnostics}"
grep -q '"experienceVersion": "mira-commercial-v1"' \
  <<<"${mira_commercial_diagnostics}"
if grep -qiE 'acesso privado|homologação interna|evidência sintética|voz gerada por IA' \
  <<<"${mira_commercial_html}${mira_commercial_bundle}"; then
  echo '[ARQUITETURA] A superfície comercial de Mira contém linguagem da pesquisa privada.' >&2
  exit 1
fi
if compose exec -T pde-platform-frontend-v7 \
  wget --quiet --spider http://127.0.0.1/mira-private/subrota 2>/dev/null; then
  echo '[ARQUITETURA] O fallback SPA do Vega ainda aceita subrotas de Mira.' >&2
  exit 1
fi

vega_v5_container_id_before="$(compose ps -q pde-platform-frontend-v5)"
vega_v6_container_id_before="$(compose ps -q pde-platform-frontend-v6)"
vega_container_id_before="$(compose ps -q pde-platform-frontend-v7)"
vega_v12_container_id_before="$(compose ps -q pde-platform-frontend-v8)"
mira_container_id_before="$(compose ps -q pde-platform-frontend-mira)"
mira_commercial_container_id_before="$(compose ps -q pde-platform-frontend-mira-commercial)"
alcyone_container_id_before="$(compose ps -q pde-platform-frontend-alcyone)"
compose up -d --force-recreate --no-deps --wait pde-platform-frontend-mira-commercial
vega_v5_container_id_after="$(compose ps -q pde-platform-frontend-v5)"
vega_v6_container_id_after="$(compose ps -q pde-platform-frontend-v6)"
vega_container_id_after="$(compose ps -q pde-platform-frontend-v7)"
vega_v12_container_id_after="$(compose ps -q pde-platform-frontend-v8)"
mira_container_id_after="$(compose ps -q pde-platform-frontend-mira)"
mira_commercial_container_id_after="$(compose ps -q pde-platform-frontend-mira-commercial)"
alcyone_container_id_after="$(compose ps -q pde-platform-frontend-alcyone)"

test "${vega_v5_container_id_before}" = "${vega_v5_container_id_after}"
test "${vega_v6_container_id_before}" = "${vega_v6_container_id_after}"
test "${vega_container_id_before}" = "${vega_container_id_after}"
test "${vega_v12_container_id_before}" = "${vega_v12_container_id_after}"
test "${mira_container_id_before}" = "${mira_container_id_after}"
test "${mira_commercial_container_id_before}" != "${mira_commercial_container_id_after}"
test "${alcyone_container_id_before}" = "${alcyone_container_id_after}"
curl_v7 /mira-private/version-diagnostics.json | grep -q '"productId": 10'
curl_mira_commercial /version-diagnostics.json \
  | grep -q '"experienceVersion": "mira-commercial-v1"'
validate_mira_commercial_media
curl_v8 /version-diagnostics.json \
  | grep -q '"experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel"'
curl_alcyone /version-diagnostics.json \
  | grep -q '"experienceVersion": "alcyone-private-v3"'

echo 'Roteamento e ciclo de vida isolados de Alcyone, Mira privada, Mira comercial e Vega v5–v8 validados localmente.'
