#!/usr/bin/env bash
set -euo pipefail

module_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_compose="${module_dir}/tests/proxy-lifecycle/docker-compose.yml"
recovery_script="${module_dir}/scripts/recover-public-proxy.sh"
project="${PUBLIC_PROXY_RECOVERY_COMPOSE_PROJECT:-public-proxy-recovery-test}"
duplicate_name="${project}-duplicate-proxy"
test_output_dir=""

compose() {
  docker compose -p "${project}" -f "${test_compose}" "$@"
}

cleanup() {
  docker rm -f "${duplicate_name}" >/dev/null 2>&1 || true
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  if [ -n "${test_output_dir}" ]; then
    rm -rf "${test_output_dir}"
  fi
}
trap cleanup EXIT

recover() {
  PUBLIC_PROXY_RECOVERY_TEST_MODE=true \
    PUBLIC_PROXY_RECOVERY_COMPOSE_PROJECT="${project}" \
    PUBLIC_PROXY_RECOVERY_COMPOSE_FILE="${test_compose}" \
    "${recovery_script}"
}

proxy_id() {
  docker ps -aq \
    --filter "label=com.docker.compose.project=${project}" \
    --filter 'label=com.docker.compose.service=proxy'
}

assert_proxy() {
  local id
  id="$(proxy_id)"
  test -n "${id}"
  test "$(docker inspect "${id}" --format '{{.State.Running}}')" = 'true'
  test "$(docker inspect "${id}" --format '{{.HostConfig.RestartPolicy.Name}}')" = 'always'
  test "$(docker inspect "${id}" --format '{{.State.Health.Status}}')" = 'healthy'
  compose exec -T proxy wget -qO- --no-check-certificate \
    --header='Host: kit-whatsapp-pronto.digicomdigital.com.br' \
    https://127.0.0.1/ | grep -Fq 'Welcome to nginx!'
}

cleanup
test_output_dir="$(mktemp -d)"
compose up -d --build --wait --wait-timeout 120
assert_proxy

# Caminho idempotente: proxy saudável permanece disponível.
recover
assert_proxy

# Caminho real da tarefa 261: o container existe, mas está parado manualmente.
docker stop "$(proxy_id)" >/dev/null
test "$(docker inspect "$(proxy_id)" --format '{{.State.Running}}')" = 'false'
recover
assert_proxy

# Caminho de contingência: container ausente é recriado sem build ou pull.
compose rm -sf proxy >/dev/null
test -z "$(proxy_id)"
recover
assert_proxy

# Ambiguidade de identidade falha fechada e não escolhe um dos proxies.
docker create \
  --name "${duplicate_name}" \
  --label "com.docker.compose.project=${project}" \
  --label 'com.docker.compose.service=proxy' \
  nginx:1.27-alpine >/dev/null
if recover >"${test_output_dir}/duplicate-recovery.log" 2>&1; then
  echo '[ARQUITETURA] recuperação aceitou dois proxies canônicos.' >&2
  exit 1
fi
grep -Fq 'exige exatamente um proxy canônico' "${test_output_dir}/duplicate-recovery.log"
docker rm -f "${duplicate_name}" >/dev/null

# Sem container e sem Compose canônico, a operação preserva o bloqueio.
compose rm -sf proxy >/dev/null
if PUBLIC_PROXY_RECOVERY_TEST_MODE=true \
  PUBLIC_PROXY_RECOVERY_COMPOSE_PROJECT="${project}" \
  PUBLIC_PROXY_RECOVERY_COMPOSE_FILE="/tmp/public-proxy-recovery-compose-ausente.yml" \
  "${recovery_script}" >"${test_output_dir}/missing-compose-recovery.log" 2>&1; then
  echo '[ARQUITETURA] recuperação prosseguiu sem proxy nem Compose canônico.' >&2
  exit 1
fi
grep -Fq 'proxy ausente e Compose canônico indisponível' \
  "${test_output_dir}/missing-compose-recovery.log"

echo 'Recuperação controlada do proxy público homologada ponta a ponta.'
