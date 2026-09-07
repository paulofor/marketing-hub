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

vega_diagnostics="$(curl_v7 /version-diagnostics.json)"
grep -q '"productSlug": "metodo-musa-7-dias"' <<<"${vega_diagnostics}"
grep -q '"imageVersionId": "v7"' <<<"${vega_diagnostics}"

mira_html="$(curl_v7 /mira-private)"
grep -q 'Sua rotina, organizada com calma' <<<"${mira_html}"
mira_diagnostics="$(curl_v7 /mira-private/version-diagnostics.json)"
grep -q '"surface": "pde-platform-frontend-mira"' <<<"${mira_diagnostics}"
grep -q '"productId": 10' <<<"${mira_diagnostics}"
grep -q '"productSlug": "pde-planejado-36"' <<<"${mira_diagnostics}"

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
if compose exec -T pde-platform-frontend-v7 \
  wget --quiet --spider http://127.0.0.1/mira-private/subrota 2>/dev/null; then
  echo '[ARQUITETURA] O fallback SPA do Vega ainda aceita subrotas de Mira.' >&2
  exit 1
fi

vega_container_id_before="$(compose ps -q pde-platform-frontend-v7)"
mira_container_id_before="$(compose ps -q pde-platform-frontend-mira)"
compose up -d --force-recreate --no-deps --wait pde-platform-frontend-mira
vega_container_id_after="$(compose ps -q pde-platform-frontend-v7)"
mira_container_id_after="$(compose ps -q pde-platform-frontend-mira)"

test "${vega_container_id_before}" = "${vega_container_id_after}"
test "${mira_container_id_before}" != "${mira_container_id_after}"
curl_v7 /mira-private/version-diagnostics.json | grep -q '"productId": 10'

echo 'Roteamento e ciclo de vida isolados de Mira e Vega validados localmente.'
