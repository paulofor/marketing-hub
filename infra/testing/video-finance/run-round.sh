#!/usr/bin/env bash
# Homologa a tela e os contratos financeiros exclusivamente na sandbox.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round="${1:?Informe o nome da rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
output="$PWD/artifacts/video-finance/$round"
mkdir -p "$output"
project="${VIDEO_FINANCE_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
compose=(docker compose -p "$project" -f backend/ads-service/docker-compose.learning-cycles-local.yml)
api_pid=''
ui_pid=''
cleanup() {
  [[ -z "$ui_pid" ]] || kill "$ui_pid" 2>/dev/null || true
  [[ -z "$api_pid" ]] || kill "$api_pid" 2>/dev/null || true
  [[ -z "$ui_pid" ]] || wait "$ui_pid" 2>/dev/null || true
  [[ -z "$api_pid" ]] || wait "$api_pid" 2>/dev/null || true
  "${compose[@]}" down --volumes --remove-orphans > "$output/cleanup.log" 2>&1
}
trap cleanup EXIT
run() {
  local step="$1"
  shift
  if "$@" > "$output/$step.log" 2>&1; then
    printf 'PASS %s\n' "$step"
  else
    tail -n 60 "$output/$step.log"
    return 1
  fi
}
wait_http() {
  python3 - "$1" <<'PY'
import sys, time, urllib.request
for _ in range(100):
    try:
        with urllib.request.urlopen(sys.argv[1], timeout=1) as response:
            if response.status == 200: break
    except Exception:
        time.sleep(.3)
else:
    raise SystemExit('Aplicação local não iniciou: ' + sys.argv[1])
PY
}
run docker-version docker version
run buildx-version docker buildx version
run compose-version docker compose version
run mysql "${compose[@]}" up -d --wait --wait-timeout 120
rm -rf backend/ads-service/target/surefire-reports
run backend mvn -q -f backend/ads-service/pom.xml '-Dtest=LearningCycle*Test,SalesFlow*Test,Process*Test,ArquiteturaTest' test
python3 - "$output/backend-count.json" <<'PY'
import pathlib, json, sys, xml.etree.ElementTree as E
counts = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
for p in pathlib.Path('backend/ads-service/target/surefire-reports').glob('TEST-*.xml'):
    root = E.parse(p).getroot()
    for k in counts: counts[k] += int(root.get(k, 0))
assert counts['tests'] > 0 and not counts['failures'] and not counts['errors'], counts
pathlib.Path(sys.argv[1]).write_text(json.dumps(counts))
print(json.dumps(counts))
PY
run frontend npm --prefix frontend test -- --run src/pages/financial src/pages/learningCycle src/components/MainNavigation.test.tsx src/pages/product/ProductProcessAutomationPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/productProcessContext.test.tsx
run typecheck npm --prefix frontend run typecheck
run build env VITE_API_URL=http://127.0.0.1:15173 npm --prefix frontend run build
run spotless mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*(learningcycle|automation).*java'
run prettier npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/api/financial/useVideoBudget.ts frontend/src/pages/financial/VideoFinancePage.tsx \
  frontend/src/pages/financial/VideoFinancePage.css frontend/src/pages/financial/VideoFinancePage.test.tsx \
  frontend/src/components/MainNavigation.tsx frontend/src/components/MainNavigation.test.tsx \
  frontend/src/pages/learningCycle/LearningCycleCommandForm.tsx frontend/src/api/learningCycle/useLearningCycles.ts \
  frontend/src/api/businessProcess/useProcessAutomation.ts frontend/src/pages/product/ProductProcessAutomationPanel.tsx \
  frontend/src/pages/product/ProductProcessAutomationPanel.test.tsx frontend/src/pages/product/productProcessContext.ts \
  frontend/src/pages/product/productProcessContext.test.tsx \
  frontend/e2e/video-finance-responsive.mjs
run swagger python3 -c 'import yaml; [yaml.safe_load(open(p)) for p in ["docs/swagger/learning-sales-cycles-v1-swagger.yaml", "docs/swagger/process-automation-v1-swagger.yaml"]]; print("Swagger YAML válido")'
run classpath mvn -q -f backend/ads-service/pom.xml dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/video-finance-classpath
LEARNING_CYCLES_DB_HOST=sandbox-docker java -Xmx768m \
  -Dlogging.level.com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBudget=INFO \
  -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/video-finance-classpath)" \
  com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleLocalApplication > "$output/api.log" 2>&1 &
api_pid=$!
wait_http http://127.0.0.1:18091/api/products
run rest-mysql python3 infra/testing/video-finance/validate.py
node frontend/node_modules/vite/bin/vite.js preview frontend --config frontend/vite.learning-cycles-local.config.ts > "$output/ui.log" 2>&1 &
ui_pid=$!
wait_http http://127.0.0.1:15173
run browser env "VIDEO_FINANCE_EVIDENCE_DIR=$output/browser" node frontend/e2e/video-finance-responsive.mjs
run audit rg 'teto registrado productId=91001 cycleId=.*experimentId=91001 eventId=.*budgetLimitUsd=' "$output/api.log"
run diff git diff --check
printf 'RODADA COMPLETA APROVADA: %s\n' "$round"
