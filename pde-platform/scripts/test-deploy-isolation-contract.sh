#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
workflow="${repository_root}/.github/workflows/pde-platform-metodo-musa-ci.yml"

if grep -q 'LEAD_PORTAL_PAYMENTS_REMOTE_PATH' "${workflow}"; then
  echo '[ARQUITETURA] O deploy PDE não pode operar o diretório remoto do serviço de pagamentos.' >&2
  exit 1
fi

if grep -q 'docker compose -f docker-compose.deploy.yml up -d --no-deps proxy' "${workflow}"; then
  echo '[ARQUITETURA] O deploy PDE não pode recriar o proxy com o Compose de outro serviço.' >&2
  exit 1
fi

for required_contract in \
  'PROXY_CONTAINERS=' \
  'docker network connect ${PDE_PLATFORM_NETWORK}' \
  'docker start \"\${proxy_container}\"' \
  'docker kill -s HUP' \
  'Nenhum container de proxy HTTPS' \
  'run-targeted-production-smokes.sh "${PDE_DEPLOY_FRONTEND_VERSION}"'; do
  if ! grep -Fq "${required_contract}" "${workflow}"; then
    echo "[ARQUITETURA] O deploy PDE perdeu o contrato de integração segura com o proxy existente: ${required_contract}" >&2
    exit 1
  fi
done

if ! awk '
  /if docker inspect .*proxy_container/ { start_seen = 0 }
  /docker start .*proxy_container/ { start_seen = 1; start_count++ }
  /docker network connect .*proxy_container/ {
    network_count++
    if (!start_seen) invalid_order = 1
  }
  END { exit !(start_count >= 2 && network_count >= 2 && !invalid_order) }
' "${workflow}"; then
  echo '[ARQUITETURA] O deploy PDE deve iniciar o proxy parado antes de conectá-lo à rede e recarregá-lo.' >&2
  exit 1
fi

bash "${script_dir}/test-targeted-production-smokes.sh"
bash "${script_dir}/test-public-health-commercial-source.sh"

echo 'Contrato de isolamento do deploy PDE aprovado.'
