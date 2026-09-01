#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
shared_workflows=(
  ".github/workflows/email-service-ci.yml"
  ".github/workflows/feo-ci.yml"
  ".github/workflows/image-watermark-ci.yml"
  ".github/workflows/image-zipper-ci.yml"
  ".github/workflows/lead-portal-ci.yml"
  ".github/workflows/mois-sales-library-worker-ci.yml"
  ".github/workflows/oprm-coletor-mei-ci.yml"
  ".github/workflows/ops-monitor-worker-ci.yml"
  ".github/workflows/pde-monitor-worker-ci.yml"
  ".github/workflows/product-ai-worker-ci.yml"
  ".github/workflows/product-discovery-worker-ci.yml"
  ".github/workflows/scientific-research-worker-ci.yml"
)

for relative_workflow in "${shared_workflows[@]}"; do
  workflow="$test_root/$relative_workflow"
  deploy_timeout="$(awk '
    /^  deploy:/ { in_deploy = 1; next }
    in_deploy && /^  [[:alnum:]_-]+:/ { exit }
    in_deploy && /timeout-minutes:/ { print $2; exit }
  ' "$workflow")"

  if [ -z "$deploy_timeout" ] || [ "$deploy_timeout" -lt 45 ]; then
    echo "Janela de deploy insuficiente para o host compartilhado: ${relative_workflow}:${deploy_timeout:-ausente}" >&2
    exit 1
  fi

  if grep -Fq 'docker image prune -af' "$workflow"; then
    echo "Prune agressivo pode ampliar a pressão de I/O no host compartilhado: ${relative_workflow}" >&2
    exit 1
  fi

  if ! grep -Fq 'scripts/docker-compose-pull-with-transient-retry.sh' "$workflow" \
    || ! grep -Fq 'bash ./docker-compose-pull-with-transient-retry.sh' "$workflow"; then
    echo "Deploy sem helper versionado de retry do Docker Compose: ${relative_workflow}" >&2
    exit 1
  fi

  if grep -Eq 'docker compose .* pull([[:space:]\\]|$)' "$workflow"; then
    echo "Pull direto sem classificação de falha transitória: ${relative_workflow}" >&2
    exit 1
  fi
done

product_ai_workflow="$test_root/.github/workflows/product-ai-worker-ci.yml"
grep -F "export COMPOSE_PROJECT_NAME='marketinghub-product-ai-worker'" "$product_ai_workflow" >/dev/null

feo_workflow="$test_root/.github/workflows/feo-ci.yml"
# O contrato procura as variáveis literais no workflow.
# shellcheck disable=SC2016
grep -F 'bash scripts/ssh-keyscan-with-retry.sh "$DEPLOY_HOST" "$HOME/.ssh/known_hosts"' "$feo_workflow" >/dev/null
grep -F 'StrictHostKeyChecking=yes' "$feo_workflow" >/dev/null

lead_portal_workflow="$test_root/.github/workflows/lead-portal-ci.yml"
test "$(grep -Fc 'bash scripts/recreate-app-services.sh' "$lead_portal_workflow")" -ge 2
grep -F 'bash scripts/wait-for-app-services-health.sh 900' "$lead_portal_workflow" >/dev/null
grep -F 'bash scripts/wait-for-app-services-health.sh 420' "$lead_portal_workflow" >/dev/null

bash "$test_root/scripts/test-ssh-keyscan-with-retry.sh"
bash "$test_root/lead-portal/scripts/test-recreate-app-services.sh"
bash "$test_root/lead-portal/scripts/test-wait-for-app-services-health.sh"

echo "Resiliência dos deploys do host compartilhado validada."
