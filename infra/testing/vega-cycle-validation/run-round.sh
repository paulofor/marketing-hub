#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega386/$round"
mkdir -p "$output"
export VEGA_TEST_VERSION=musa-pde-entry-v11-primeiro-ajuste-aplicavel
export VEGA_SCENARIO_LOCAL=true
export VEGA_COMPOSE_OVERRIDE=infra/testing/vega-cycle-validation/compose.yml
: "${VEGA_COMPOSE_PROJECT:?Use o projeto exclusivo da sandbox}"
: "${VEGA_LOCAL_HOST:?Informe IP da sandbox acessível pela engine}"
: "${VEGA_TEST_DOCKER_HOST:?Informe IP da engine isolada}"
run() { local name=$1; shift; if "$@" > "$output/$name.log" 2>&1; then printf 'PASS %s\n' "$name"; else tail -n 30 "$output/$name.log"; return 1; fi; }
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml '-Dtest=AgentTaskServiceTest,LearningCycle*Test,VegaPrivateServiceTest,PdeTechnicalHomologation*Test,PdeAgentValidation*Test,ArquiteturaTest' test
run dedalo env MAVEN_OPTS=-Xmx512m mvn -q -f landing-generator-agent-worker/pom.xml test
run psique env MAVEN_OPTS=-Xmx512m mvn -q -f customer-agent-worker/pom.xml test
run worker npm --prefix pde-platform/pde-vega-private-worker test
run deploy-contract python3 infra/testing/vega-cycle-validation/deploy_contract_test.py
run typecheck pde-platform/frontend/node_modules/.bin/tsc --noEmit -p pde-platform/frontend/tsconfig.vega.json
run build pde-platform/frontend/node_modules/.bin/vite build --config pde-platform/frontend/vite.vega.config.ts
run api node infra/testing/vega-private-prototype/integration.mjs
run access-browser node infra/testing/vega-cycle-validation/access-browser.cjs "$output/access-browser"
node - "$output" <<'JS'
const fs=require('fs');const root=process.argv[2];
const schema=JSON.parse(fs.readFileSync('customer-agent-worker/src/main/resources/prompts/bpm/v5/pde-agent-validation-scenario-review-schema.json'));
const pattern=new RegExp(schema.properties.sourceReference.pattern); const assert=require('node:assert/strict');
for(const value of ['experiment:92','product:10@agent-validation-v1']) assert.ok(pattern.test(value));
for(const value of ['experiment:0','experiment:92x','product:10@private-validation-v1']) assert.ok(!pattern.test(value));
fs.writeFileSync(root+'/input.json',JSON.stringify({mode:'TECHNICAL',captureSessionId:'local-vega386-'+root.split('/').pop(),sourceUrl:'http://127.0.0.1:18083/vega-private',sourceReference:'experiment:91092',productId:91004,productSlug:'metodo-musa-7-dias',prototypeVersion:process.env.VEGA_TEST_VERSION,cycleId:91002}));
JS
run harness env PDE_INTERNAL_API_TOKEN=vega-local-internal-only node customer-agent-worker/src/main/resources/browser/vega-agent-validation-harness.mjs "$output/input.json" "$output/output.json" "$output/harness"
run images bash infra/testing/vega-private-prototype/check-images.sh
cp -a artifacts/vega380/image-harness "$output/image-harness"
cp artifacts/vega380/image-diagnostics.json artifacts/vega380/image-contract.json "$output/"
run image-access env "VEGA_ACCESS_TEST_URL=http://$VEGA_TEST_DOCKER_HOST:18318" node infra/testing/vega-cycle-validation/access-browser.cjs "$output/image-access"
run dedalo-image docker compose -p "$VEGA_COMPOSE_PROJECT" -f infra/testing/vega-private-prototype/compose.yml -f infra/testing/vega-cycle-validation/compose.yml run --rm -T dedalo-image
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
