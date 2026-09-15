#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/.codex/vega-approved/$round"
mkdir -p "$output"
run() {
  local name=$1
  shift
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    tail -n 25 "$output/$name.log"
    return 1
  fi
}
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml test
cp -a backend/ads-service/target/surefire-reports "$output/backend-reports"
run psique env MAVEN_OPTS=-Xmx512m mvn -q -f customer-agent-worker/pom.xml test
cp -a customer-agent-worker/target/surefire-reports "$output/psique-reports"
run temis env MAVEN_OPTS=-Xmx512m mvn -q -f meta-ad-approver-worker/pom.xml test
cp -a meta-ad-approver-worker/target/surefire-reports "$output/temis-reports"
run browser-contracts npm --prefix customer-agent-worker test
run private-worker npm --prefix pde-platform/pde-vega-private-worker test
run frontend npm --prefix frontend test -- --run src/pages/learningCycle src/api/learningCycle src/pages/product/ProductProcessTaskTracking.test.tsx
run types npm --prefix frontend run typecheck
run private-types npm exec --prefix pde-platform/frontend -- tsc --noEmit -p pde-platform/frontend/tsconfig.vega.json
run frontend-build npm --prefix frontend run build
run private-build npm exec --prefix pde-platform/frontend -- vite build --config pde-platform/frontend/vite.vega.config.ts
run operational-contract python3 infra/testing/video-continuation/deploy_test.py
run packaged-resources python3 scripts/verify-backend-packaged-resources.py
run images python3 infra/testing/video-continuation/check-images.py
run api env VIDEO_FINANCE_COMPOSE_PROJECT=aihub-36e8935e-c4cc-43ea-8bd1-8b9d6b1922ec-6ff58094f8 python3 infra/testing/video-continuation/verify.py
run harness env NODE_EXTRA_CA_CERTS="$PWD/.codex/vega-approved/media/local.crt" PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH="$PWD/.codex/vega-approved/media/chromium-fixture.sh" PDE_INTERNAL_API_TOKEN=vega-local-internal-only node customer-agent-worker/src/main/resources/browser/vega-agent-validation-harness.mjs .codex/vega-approved/harness-input.json .codex/vega-approved/harness-output.json .codex/vega-approved/harness-browser
run navigation node infra/testing/video-continuation/browser.cjs
run restart env VIDEO_FINANCE_COMPOSE_PROJECT=aihub-36e8935e-c4cc-43ea-8bd1-8b9d6b1922ec-6ff58094f8 python3 infra/testing/video-continuation/restart-check.py
cp .codex/vega-approved/harness-output.json .codex/vega-approved/local-cycle.json "$output/"
cp -a .codex/vega-approved/browser .codex/vega-approved/harness-browser "$output/"
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
