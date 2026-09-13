#!/usr/bin/env bash
# Executa a matriz local da correção criativa, sem modelos pagos nem escrita produtiva.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/mira-creative-recovery/$round"
diagnostic="$PWD/artifacts/mira-creative-recovery/diagnostic"
mkdir -p "$output"
run() {
  local name=$1
  shift
  printf 'Validando %s\n' "$name"
  if "$@" > "$output/$name.log" 2>&1; then printf 'PASS %s\n' "$name"
  else local status=$?; tail -n 40 "$output/$name.log"; return "$status"; fi
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
run backend-ci-contract python3 scripts/test-backend-ci-workflow.py
run apply-contract python3 infra/testing/mira-creative-recovery/deploy_contract_test.py
run backend mvn -q -f backend/ads-service/pom.xml "-Dcreative.lifecycle.output=$output/browser-contract.json" test
counts backend/ads-service/target/surefire-reports backend
run mysql57 mvn -q -f backend/ads-service/pom.xml -Dprivate.journey.mysql57=true -Dtest=PrivateCommunicationJourneyPersistenceTest test
run iris mvn -q -f communication-agent-worker/pom.xml "-Dcreative.replay.source=$diagnostic/asset-96.png" "-Dcreative.replay.spec=$diagnostic/replay-spec.json" "-Dcreative.replay.output=$output/corrected-preview.png" test
counts communication-agent-worker/target/surefire-reports iris
run psique mvn -q -f customer-agent-worker/pom.xml test
counts customer-agent-worker/target/surefire-reports psique
run temis mvn -q -f meta-ad-approver-worker/pom.xml test
counts meta-ad-approver-worker/target/surefire-reports temis
run process-worker npm --prefix process-execution-worker test
run frontend npm --prefix frontend test -- --run src/api/businessProcess/useProductProcessActivityExecutions.test.tsx src/pages/product/ProductProcessAutomationPanel.test.tsx src/pages/product/ProductProcessActivityExecutionPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx
run browser env "MIRA_UI_CONTRACT=$output/browser-contract.json" "MIRA_UI_OUTPUT=$output/browser" "MIRA_CREATIVE_PREVIEW=$output/corrected-preview.png" node infra/testing/mira-creative-recovery/browser.cjs
run backend-package mvn -q -f backend/ads-service/pom.xml package -DskipTests
run backend-package-content python3 scripts/verify-backend-packaged-resources.py
run diff git diff --check
printf 'PASS rodada completa %s\n' "$round" | tee "$output/result.txt"
