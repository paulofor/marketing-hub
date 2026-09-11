#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega380/$round"
mkdir -p "$output"
run() { local name=$1; shift; if "$@" > "$output/$name.log" 2>&1; then printf 'PASS %s\n' "$name"; else tail -n 30 "$output/$name.log"; return 1; fi; }
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml '-Dtest=LearningCycle*Test,VegaPrivateServiceTest,PdeTechnicalHomologation*Test,PdeAgentValidation*Test,ArquiteturaTest' test
run dedalo env MAVEN_OPTS=-Xmx512m mvn -q -f landing-generator-agent-worker/pom.xml test
run psique env MAVEN_OPTS=-Xmx512m mvn -q -f customer-agent-worker/pom.xml test
run deploy-contract python3 infra/testing/vega-private-prototype/deploy_contract_test.py
run worker npm --prefix pde-platform/pde-vega-private-worker test
run admin npm --prefix frontend test -- --run src/pages/learningCycle/LearningCycleCommandForm.test.tsx src/pages/learningCycle/LearningCyclesPage.test.tsx src/pages/product/ProductProcessTaskTracking.test.tsx
run admin-typecheck npm --prefix frontend run typecheck
run vega-typecheck npm exec --prefix pde-platform/frontend -- tsc --noEmit -p pde-platform/frontend/tsconfig.vega.json
run admin-build npm --prefix frontend run build
run vega-build npm exec --prefix pde-platform/frontend -- vite build --config pde-platform/frontend/vite.vega.config.ts
# O banco MySQL 5.7 e os test doubles locais são iniciados conforme README; nenhum endpoint produtivo é usado.
run api node infra/testing/vega-private-prototype/integration.mjs
node - "$output" <<'JS'
const fs=require('fs');const path=require('path');const output=process.argv[2];fs.writeFileSync(path.join(output,'input.json'),JSON.stringify({mode:'TECHNICAL',scenarioCode:'',captureSessionId:'local-'+path.basename(output),sourceUrl:'http://127.0.0.1:18083/vega-private',sourceReference:'experiment:91092',productId:91004,productSlug:'metodo-musa-7-dias',prototypeVersion:'musa-pde-entry-v9-primeiro-ajuste-aplicavel',cycleId:91002}));
JS
run browser env PDE_INTERNAL_API_TOKEN=vega-local-internal-only node customer-agent-worker/src/main/resources/browser/vega-agent-validation-harness.mjs "$output/input.json" "$output/output.json" "$output/browser"
run images bash infra/testing/vega-private-prototype/check-images.sh
cp -a artifacts/vega380/image-harness "$output/image-harness"
cp artifacts/vega380/image-diagnostics.json artifacts/vega380/image-contract.json "$output/"
run admin-browser node infra/testing/vega-private-prototype/admin-browser.cjs "$output/admin-browser"
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
