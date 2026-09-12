#!/usr/bin/env bash
# Executa o contrato inteiro localmente antes de qualquer publicação.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega-multiagent-recovery/$round"
mkdir -p "$output/flow"
: "${VEGA_COMPOSE_PROJECT:?Projeto exclusivo da sandbox obrigatório}"
export VEGA_TEST_VERSION=musa-pde-entry-v12-primeiro-ajuste-aplicavel
export VEGA_SCENARIO_LOCAL=true
export VEGA_GATE_FLOW_ARTIFACTS="$output/flow"
export VEGA_IRIS_INPUT_FILE="$output/iris-input.json"
export VEGA_SCENARIO_LOCAL_URL=https://127.0.0.1:18084/vega-private
export NODE_EXTRA_CA_CERTS="$PWD/artifacts/vega-multiagent-recovery/tls/test.crt"
export CHROMIUM_BIN="$PWD/artifacts/vega-multiagent-recovery/chromium-local-tls.sh"
export CHROME_BIN="$CHROMIUM_BIN"
export PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH="$CHROMIUM_BIN"
export VEGA_COMPOSE_OVERRIDE=infra/testing/vega-integrity-cycle/compose.yml
run() {
  local name=$1
  shift
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name" | tee -a "$output/result.txt"
  else
    printf 'FAIL %s\n' "$name" | tee -a "$output/result.txt"
    tail -n 30 "$output/$name.log"
    return 1
  fi
}
run backend env MAVEN_OPTS=-Xmx1g mvn -q -f backend/ads-service/pom.xml "-Dvega377.output=$output/screen.json" '-Dtest=PdeTechnicalHomologation*Test,PdeAgentValidation*Test,PdeRevalidationActivityExecutionTest,BusinessProcessActivityExecution*Test,BusinessProcessTaskPromptAuditTest,ProductProcessExecutionProgressTest,ProductProcessActivityRecoveryResolverTest,ProductProcessRecoveryLifecycleTest,ProductProcessActivityPredecessorServiceTest,LearningCycle*Test,Iris*Test,AgentTaskServiceTest,AgentTaskRecentActivityExecutionRepositoryTest,AgentHarnessCatalogTest,VegaPrivateServiceTest,ArquiteturaTest' test
run iris env MAVEN_OPTS=-Xmx512m mvn -q -f communication-agent-worker/pom.xml test
run api node infra/testing/vega-private-prototype/integration.mjs
node - "$output" <<'JS'
const fs=require('fs');const root=process.argv[2];
fs.writeFileSync(root+'/input.json',JSON.stringify({mode:'TECHNICAL',captureSessionId:'local-vega393-'+root.split('/').pop(),sourceUrl:process.env.VEGA_SCENARIO_LOCAL_URL,sourceReference:'experiment:91092',productId:91004,productSlug:'metodo-musa-7-dias',prototypeVersion:process.env.VEGA_TEST_VERSION,cycleId:91002}));
JS
run technical env PDE_INTERNAL_API_TOKEN=vega-local-internal-only node customer-agent-worker/src/main/resources/browser/vega-agent-validation-harness.mjs "$output/input.json" "$output/flow/TECHNICAL.json" "$output/harness"
run psique env MAVEN_OPTS=-Xmx512m mvn -q -f customer-agent-worker/pom.xml test
run temis env MAVEN_OPTS=-Xmx512m mvn -q -f meta-ad-approver-worker/pom.xml test
run gate-flow env MAVEN_OPTS=-Xmx1g mvn -q -f backend/ads-service/pom.xml -Dtest=VegaCycleGateFlowIntegrationTest test
run dedalo env MAVEN_OPTS=-Xmx512m mvn -q -f landing-generator-agent-worker/pom.xml test
run worker npm --prefix pde-platform/pde-vega-private-worker test
run deploy-contract python3 infra/testing/vega-integrity-cycle/deploy_contract_test.py
run admin npm --prefix frontend test -- --run src/pages/businessProcess/DeferredTaskPromptAudit.test.tsx src/pages/product/ProductProcessActivityExecutionPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/ProductProcessTaskTracking.test.tsx src/pages/learningCycle/LearningCycleCommandForm.test.tsx
run typecheck npm --prefix frontend run typecheck
run vega-typecheck pde-platform/frontend/node_modules/.bin/tsc --noEmit -p pde-platform/frontend/tsconfig.vega.json
run admin-browser env "VEGA_GATE_BROWSER_SCREEN=$output/screen.json.recovery.json" "VEGA_GATE_BROWSER_OUTPUT=$output/admin-browser" node infra/testing/vega-integrity-cycle/browser.cjs
run access-browser node infra/testing/vega-cycle-validation/access-browser.cjs "$output/access-browser"
run images bash infra/testing/vega-private-prototype/check-images.sh
cp -a artifacts/vega380/image-harness "$output/image-harness"
cp artifacts/vega380/image-diagnostics.json artifacts/vega380/image-contract.json "$output/"
run image-access env "VEGA_ACCESS_TEST_URL=http://$VEGA_TEST_DOCKER_HOST:18318" node infra/testing/vega-cycle-validation/access-browser.cjs "$output/image-access"
run temis-image docker compose -p "$VEGA_COMPOSE_PROJECT" -f infra/testing/vega-private-prototype/compose.yml -f infra/testing/vega-integrity-cycle/compose.yml run --rm -T temis-image
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round" | tee -a "$output/result.txt"
