#!/usr/bin/env bash
# Homologa a continuidade privada e seus gates, sem acessar serviços de produção.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${PROCESS_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/mira-communication/$round"
mkdir -p "$output"
export MIRA_IRIS_INPUT_FILE="$output/iris-input.json"
run() {
  local name=$1
  shift
  printf 'Validando %s\n' "$name"
  if "$@" > "$output/$name.log" 2>&1; then printf 'PASS %s\n' "$name"
  else local status=$?; tail -n 50 "$output/$name.log"; return "$status"; fi
}
counts() {
  python3 - "$1" "$output/$2-counts.json" <<'PY'
import json, pathlib, sys, xml.etree.ElementTree as xml
totals = dict(tests=0, errors=0, failures=0, skipped=0)
for report in pathlib.Path(sys.argv[1]).glob('TEST-*.xml'):
    suite = xml.parse(report).getroot()
    for key in totals: totals[key] += int(suite.get(key, 0))
assert totals['tests'] > 0 and totals['errors'] == 0 and totals['failures'] == 0, totals
pathlib.Path(sys.argv[2]).write_text(json.dumps(totals) + '\n')
PY
}
run backend mvn -q -f backend/ads-service/pom.xml "-Dmira.lifecycle.output=$output/browser-contract.json" test
counts backend/ads-service/target/surefire-reports backend
run mysql57 mvn -q -f backend/ads-service/pom.xml -Dprivate.journey.mysql57=true -Dtest=PrivateCommunicationJourneyPersistenceTest test
run iris env "VEGA_IRIS_INPUT_FILE=$MIRA_IRIS_INPUT_FILE" mvn -q -f communication-agent-worker/pom.xml test
counts communication-agent-worker/target/surefire-reports iris
run iris-mcp node --test infra/testing/mira-communication/communication-mcp.test.mjs
run psique mvn -q -f customer-agent-worker/pom.xml test
counts customer-agent-worker/target/surefire-reports psique
run temis mvn -q -f meta-ad-approver-worker/pom.xml test
counts meta-ad-approver-worker/target/surefire-reports temis
run process-worker npm --prefix process-execution-worker test
run frontend npm --prefix frontend test -- --run src/api/businessProcess/useProductProcessActivityExecutions.test.tsx src/pages/product/ProductProcessAutomationPanel.test.tsx src/pages/product/ProductProcessActivityExecutionPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx
run browser env "MIRA_UI_CONTRACT=$output/browser-contract.json" "MIRA_UI_OUTPUT=$output/browser" node infra/testing/mira-communication/browser.cjs
run backend-package mvn -q -f backend/ads-service/pom.xml package -DskipTests
run backend-package-content python3 scripts/verify-backend-packaged-resources.py
run review-evidence node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence
run backend-image docker build --label "com.docker.compose.project=$PROCESS_COMPOSE_PROJECT" -t "$PROCESS_COMPOSE_PROJECT/mira-backend:$round" -f backend/ads-service/Dockerfile .
run iris-image docker build --label "com.docker.compose.project=$PROCESS_COMPOSE_PROJECT" -t "$PROCESS_COMPOSE_PROJECT/mira-communication-agent-worker:$round" communication-agent-worker
run temis-image docker build --label "com.docker.compose.project=$PROCESS_COMPOSE_PROJECT" -t "$PROCESS_COMPOSE_PROJECT/mira-meta-ad-approver-worker:$round" meta-ad-approver-worker
run images-content python3 infra/testing/mira-communication/check-images.py "$output" "$round"
run diff git diff --check
printf 'PASS rodada completa %s\n' "$round" | tee "$output/result.txt"
