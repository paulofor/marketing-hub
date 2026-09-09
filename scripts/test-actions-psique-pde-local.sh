#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"
: "${ACTIONS_TEST_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
round="${1:?Informe a rodada local}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
result_dir="$repository_root/artifacts/actions-psique-pde-2026-09-08/$round"
mkdir -p "$result_dir"
: > "$result_dir/results.tsv"
export AGENT_VPS_DISK_TEST_COMPOSE_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT"
export VPS_SSH_TEST_COMPOSE_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT"
export ACTIONLINT_DOCKER_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT"

step() {
  local name="$1"
  shift
  printf 'INICIANDO %s\n' "$name"
  if "$@" > "$result_dir/$name.log" 2>&1; then
    printf '%s\tPASS\n' "$name" >> "$result_dir/results.tsv"
    printf 'PASS %s\n' "$name"
  else
    local status="$?"
    printf '%s\tFAIL\n' "$name" >> "$result_dir/results.tsv"
    tail -60 "$result_dir/$name.log"
    return "$status"
  fi
}

in_module() {
  local module="$1"
  shift
  (cd "$module" && "$@")
}

in_node_module() {
  local module="$1"
  shift
  (cd "$module" && npm ci && "$@")
}

step 01-psique-java in_module customer-agent-worker mvn -B test
step 02-dedalo-java in_module landing-generator-agent-worker mvn -B test
step 03-apolo-java in_module video-management-service mvn -B test
step 04-pde-java in_module pde-platform/backend mvn -B test
step 05-harness-catalog in_module backend/ads-service mvn -B -Dtest=AgentHarnessCatalogTest test
step 06-evidence node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
step 07-psique-browser in_node_module customer-agent-worker npm test
step 08-pde-build in_node_module pde-platform/frontend npm run build
step 09-mira-build in_module pde-platform/frontend npm run build:mira
step 10-product-artifacts node pde-platform/scripts/test-product-build-artifacts.mjs
step 11-mira-browser in_module pde-platform/frontend env MIRA_PRIVATE_E2E_TOKEN=mira-local-test-only \
  npm run test:mira-private:local -- \
  --workers=1 --grep 'retoma simulação|cenário interno'
step 12-http-consistency python3 scripts/test-musa-pde-public-consistency.py
step 13-pde-isolation bash pde-platform/scripts/test-deploy-isolation-contract.sh
step 14-targeted-smokes bash pde-platform/scripts/test-targeted-production-smokes.sh
step 15-runtime-health-contract bash pde-platform/scripts/test-public-health-commercial-source.sh
step 16-workflow-lint bash scripts/run-actionlint.sh
step 17-reasoning-config node --test scripts/test-agent-max-reasoning.mjs
step 18-retention bash scripts/test-agent-vps-disk-space.sh
step 19-retention-workflows node scripts/test-agent-vps-disk-workflows.mjs
step 20-image-contracts node --test scripts/test-agent-image-bundle.mjs scripts/test-agent-image-workflows.mjs
step 21-deployment-coordination node --test scripts/coordinate-agent-deployment.test.mjs
step 22-agent-ssh-contracts node scripts/test-agent-vps-ssh-workflows.mjs
step 23-retention-docker bash scripts/test-agent-vps-disk-space-e2e.sh
step 24-ssh-docker bash scripts/test-configure-vps-ssh-fallback-e2e.sh
step 25-image-transport-docker bash scripts/test-agent-image-bundle-e2e.sh
step 26-runtime-docker bash scripts/test-actions-psique-pde-runtime.sh
step 27-shellcheck shellcheck scripts/check-musa-pde-public-consistency.sh \
  scripts/test-actions-psique-pde-runtime.sh scripts/test-actions-psique-pde-local.sh
step 28-diff git diff --check
printf 'Rodada %s concluída: 28/28 controles. Evidências: %s\n' "$round" "$result_dir"
