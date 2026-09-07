#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
workflow="${repository_root}/.github/workflows/pde-platform-metodo-musa-ci.yml"
disk_script="${repository_root}/scripts/ensure-agent-vps-disk-space.sh"

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
  "github.event_name == 'push'" \
  'MIRA_AGENT_VALIDATION_STATUS=' \
  '/api/pde/mira/private/v1/internal/agent-validations/sessions' \
  "if [ \"\${MIRA_AGENT_VALIDATION_STATUS}\" != '403' ]; then" \
  'TARGETED_FRONTEND_VERSION=v7' \
  "mira) FRONTEND_SERVICES='pde-platform-frontend-mira'" \
  'PDE_PLATFORM_FRONTEND_MIRA_IMAGE=' \
  'PDE_PLATFORM_FRONTEND_MIRA_PORT=' \
  'bootstrap-legacy-route' \
  'PDE_MIRA_PROXY_MODE' \
  'run-targeted-production-smokes.sh "${TARGETED_FRONTEND_VERSION}"'; do
  if ! grep -Fq "${required_contract}" "${workflow}"; then
    echo "[ARQUITETURA] O deploy PDE perdeu o contrato de integração segura com o proxy existente: ${required_contract}" >&2
    exit 1
  fi
done

if [ "$(grep -Fc 'bash -s -- retention' "${workflow}")" -ne 2 ] \
  || [ "$(grep -Fc 'AGENT_VPS_DISK_PROTECTED_TAG=' "${workflow}")" -ne 2 ] \
  || [ "$(grep -Fc 'scripts/ensure-agent-vps-disk-space.sh' "${workflow}")" -lt 4 ]; then
  echo '[ARQUITETURA] O deploy PDE deve limpar versões antigas antes e depois, preservando a revisão publicada.' >&2
  exit 1
fi

for image_repository in \
  pde-platform-backend \
  pde-platform-frontend-v5 \
  pde-platform-frontend-v6 \
  pde-platform-frontend-v7 \
  pde-platform-frontend-mira \
  pde-platform-frontend-kit-whatsapp \
  pde-ai-worker \
  pde-retention-worker; do
  if ! grep -Fq "${image_repository}" "${disk_script}"; then
    echo "[ARQUITETURA] A retenção PDE não reconhece a imagem oficial ${image_repository}." >&2
    exit 1
  fi
done

if ! grep -Fq "if: \${{ always() && steps.pde_ssh.outcome == 'success' }}" "${workflow}"; then
  echo '[ARQUITETURA] A retenção final PDE deve executar também após falha posterior ao SSH.' >&2
  exit 1
fi

if [ "$(grep -Fc "if: github.event_name == 'push' || inputs.frontend_version != 'none'" "${workflow}")" -ne 2 ]; then
  echo '[ARQUITETURA] O deploy automático do backend PDE deve preservar o smoke da superfície Vega vigente.' >&2
  exit 1
fi

if ! grep -Fq "PDE_DEPLOY_FRONTEND_VERSION: \${{ github.event_name == 'workflow_dispatch' && inputs.frontend_version || 'none' }}" "${workflow}"; then
  echo '[ARQUITETURA] Push comum não pode escolher implicitamente um produto para deploy.' >&2
  exit 1
fi

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
node --test "${script_dir}/test-product-runtime-isolation-contract.mjs"

if ! grep -Fq 'PDE_MIRA_PRIVATE_QA_TOKEN: ${{ secrets.PDE_MIRA_PRIVATE_QA_TOKEN }}' "${workflow}" \
  || ! grep -Fq "export PDE_MIRA_PRIVATE_QA_TOKEN='" "${workflow}" \
  || ! grep -Fq 'MIRA_PRIVATE_E2E_TOKEN: ${{ secrets.PDE_MIRA_PRIVATE_QA_TOKEN }}' "${workflow}" \
  || ! grep -Fq 'PDE_MIRA_PRIVATE_QA_TOKEN: ${PDE_MIRA_PRIVATE_QA_TOKEN:?' "${repository_root}/pde-platform/docker-compose.deploy.yml"; then
  echo '[ARQUITETURA] O deploy PDE deve injetar o acesso de QA de Mira no backend e no smoke produtivo sem reutilizar convites humanos.' >&2
  exit 1
fi

for chromium_mobile_config in \
  "${repository_root}/pde-platform/frontend/playwright.public.config.ts" \
  "${repository_root}/pde-platform/frontend/playwright.container-integration.config.ts" \
  "${repository_root}/pde-platform/frontend/playwright.local-integration.config.ts" \
  "${repository_root}/pde-platform/frontend/playwright.mira.config.ts"; do
  if ! grep -Eq "devices\[['\"]iPhone 15 Pro['\"]\], browserName: ['\"]chromium['\"]" \
    "${chromium_mobile_config}"; then
    echo "Erro: ${chromium_mobile_config} deve emular iPhone no Chromium, sem combinar WebKit com executável Chromium." >&2
    exit 1
  fi
done

echo 'Contrato de isolamento do deploy PDE aprovado.'
