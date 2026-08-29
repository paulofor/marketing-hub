#!/usr/bin/env bash
set -euo pipefail

module_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_compose="${module_dir}/tests/proxy-lifecycle/docker-compose.yml"
project="${PROXY_LIFECYCLE_COMPOSE_PROJECT:-lead-portal-payments-lifecycle-test}"

compose() {
  docker compose -p "${project}" -f "${test_compose}" "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

cleanup
compose up -d --build --wait --wait-timeout 120

proxy_id="$(compose ps -q proxy)"
test -n "${proxy_id}"
test "$(docker inspect "${proxy_id}" --format '{{.HostConfig.RestartPolicy.Name}}')" = 'always'
test "$(docker inspect "${proxy_id}" --format '{{.State.Health.Status}}')" = 'healthy'

probe_public_route() {
  compose exec -T proxy wget -qO- --no-check-certificate \
    --header='Host: kit-whatsapp-pronto.digicomdigital.com.br' \
    https://127.0.0.1/ | grep -Fq 'Welcome to nginx!'
}

probe_public_route
restart_count_before="$(docker inspect "${proxy_id}" --format '{{.RestartCount}}')"
# Encerra o processo por dentro do namespace para simular crash. `docker kill`
# seria uma parada manual e, corretamente, suspenderia a política até novo start.
compose exec -T proxy sh -c 'kill -TERM 1' >/dev/null 2>&1 || true

for attempt in $(seq 1 30); do
  running="$(docker inspect "${proxy_id}" --format '{{.State.Running}}' 2>/dev/null || true)"
  health="$(docker inspect "${proxy_id}" --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' 2>/dev/null || true)"
  restart_count="$(docker inspect "${proxy_id}" --format '{{.RestartCount}}' 2>/dev/null || true)"
  if [ "${running}" = 'true' ] \
    && [ "${health}" = 'healthy' ] \
    && [ "${restart_count:-0}" -gt "${restart_count_before}" ]; then
    break
  fi
  if [ "${attempt}" = '30' ]; then
    compose ps
    compose logs --tail=100 proxy
    echo '[ARQUITETURA] proxy público não se recuperou após encerramento do processo' >&2
    exit 1
  fi
  sleep 1
done

probe_public_route
echo 'Ciclo de vida do proxy público homologado ponta a ponta.'
