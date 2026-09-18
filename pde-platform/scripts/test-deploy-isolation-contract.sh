#!/usr/bin/env bash
# shellcheck disable=SC2016
# As expressões com cifrão abaixo são contratos literais procurados no workflow.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
workflow="${repository_root}/.github/workflows/pde-platform-metodo-musa-ci.yml"
disk_script="${repository_root}/scripts/ensure-agent-vps-disk-space.sh"
independent_deploy_script="${script_dir}/test-independent-version-deploy.sh"

if ! grep -Fq 'PDE_LOCAL_COMPOSE_PROJECT:?PDE_LOCAL_COMPOSE_PROJECT obrigatório' \
  "${independent_deploy_script}" \
  || grep -Fq 'project="aihub-' "${independent_deploy_script}"; then
  echo '[ARQUITETURA] A homologação Docker deve exigir um namespace Compose exclusivo, sem ID de sandbox fixo.' >&2
  exit 1
fi

if grep -Fq 'musa-v12-commercial-homologation-v6.json' "${independent_deploy_script}" \
  || ! grep -Fq 'musa-v12-commercial-homologation-v*.json' "${independent_deploy_script}"; then
  echo '[ARQUITETURA] A homologação Docker deve selecionar dinamicamente a atestação MUSA v12 vigente.' >&2
  exit 1
fi

for project_contract in \
  'PDE_LOCAL_COMPOSE_PROJECT: pde-isolation-${{ github.run_id }}-${{ github.run_attempt }}' \
  'PDE_LOCAL_COMPOSE_PROJECT: pde-compatibility-${{ github.run_id }}-${{ github.run_attempt }}'; do
  if ! grep -Fq "${project_contract}" "${workflow}"; then
    echo "[ARQUITETURA] O workflow PDE deve definir um namespace Compose único: ${project_contract}" >&2
    exit 1
  fi
done

if grep -q 'LEAD_PORTAL_PAYMENTS_REMOTE_PATH' "${workflow}"; then
  echo '[ARQUITETURA] O deploy PDE não pode operar o diretório remoto do serviço de pagamentos.' >&2
  exit 1
fi

if grep -q 'docker compose -f docker-compose.deploy.yml up -d --no-deps proxy' "${workflow}"; then
  echo '[ARQUITETURA] O deploy PDE não pode recriar o proxy com o Compose de outro serviço.' >&2
  exit 1
fi

# Reutiliza o contrato comum da fila para impedir regras contraditórias entre módulos.
bash "${repository_root}/scripts/test-shared-vps-deploy-queue.sh"

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
  'Resolve frontend deployment scope' \
  'resolve-pde-frontend-deploy-target.mjs' \
  'PDE_DEPLOY_FRONTEND_VERSION: ${{ needs.deployment_scope.outputs.frontend-version }}' \
  'PDE_DEPLOY_BACKEND: ${{ needs.deployment_scope.outputs.deploy-backend }}' \
  "needs.deployment_scope.outputs.has-deployment == 'true'" \
  'scripts/deploy-versioned-frontend.sh' \
  'scripts/deploy-shared-component.sh' \
  'frontend-contract-sha256' \
  'frontend-source-sha256' \
  'pde-deployment-receipts-' \
  'Validate backend compatibility with every supported PDE version' \
  'PDE_PLATFORM_FRONTEND_V8_IMAGE=' \
  'PDE_PLATFORM_FRONTEND_V8_PORT=' \
  'FRONTEND_V8_IMAGE_NAME' \
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

if ! grep -Fq 'PDE_CONTRACT_ACCESS_MODE="${contract_access_mode}"' \
  "${script_dir}/run-targeted-production-smokes.sh"; then
  echo '[ARQUITETURA] O smoke direcionado deve declarar se valida contrato publicado ou candidato.' >&2
  exit 1
fi

for forbidden_contract in \
  'FRONTEND_SERVICES=' \
  'cleanup_published_port' \
  'docker rm -f pde-platform-backend pde-ai-worker pde-retention-worker' \
  "frontend_version=all"; do
  if grep -Fq "${forbidden_contract}" "${workflow}"; then
    echo "[ARQUITETURA] O deploy PDE ainda contém operação acoplada: ${forbidden_contract}" >&2
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
  pde-platform-frontend-v8 \
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

if [ "$(grep -Fc "if: env.PDE_DEPLOY_FRONTEND_VERSION != 'none' || env.PDE_DEPLOY_BACKEND == 'true'" "${workflow}")" -ne 2 ]; then
  echo '[ARQUITETURA] O deploy PDE deve validar a superfície selecionada ou todas após backend.' >&2
  exit 1
fi

if grep -Fq 'TARGETED_FRONTEND_VERSION=v7' "${workflow}"; then
  echo '[ARQUITETURA] Push comum não pode manter fallback fixo para uma superfície PDE.' >&2
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
python3 "${script_dir}/test_pde_release_contract.py"

if [[ "${PDE_SKIP_DOCKER_DEPLOY_REGRESSION:-false}" != "true" ]]; then
  bash "${script_dir}/test-independent-version-deploy.sh"
fi

if ! grep -Fq 'pde-platform-frontend-v8' "${script_dir}/reload-published-frontend-proxies.sh"; then
  echo '[ARQUITETURA] A troca do backend deve reconectar também o proxy interno da v8.' >&2
  exit 1
fi

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
