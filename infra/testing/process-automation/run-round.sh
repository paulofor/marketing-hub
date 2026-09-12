#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_root"
compose_project="${PROCESS_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
round_name="${1:-round}"
output="${repo_root}/artifacts/process-automation/${round_name}"
mkdir -p "$output"
backend_pid=""
frontend_pid=""
compose() { docker compose -p "$compose_project" -f infra/testing/process-automation/compose.yml "$@"; }
cleanup() {
  [[ -z "$backend_pid" ]] || kill "$backend_pid" 2>/dev/null || true
  [[ -z "$frontend_pid" ]] || kill "$frontend_pid" 2>/dev/null || true
  compose down --volumes --remove-orphans > "$output/cleanup.log" 2>&1
}
trap cleanup EXIT
start_backend() {
  java -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/process-test.classpath)" \
    com.marketinghub.businessprocess.automation.v1.service.ProcessAutomationLocalApplication >> "$output/backend.log" 2>&1 &
  backend_pid=$!
  for attempt in $(seq 1 50); do
    if curl -fsS -H 'X-Process-Worker-Token: process-fixture-only' http://127.0.0.1:18092/api/internal/business-processes/automation/v1/stage-executions/pending > /dev/null; then return; fi
    if ! kill -0 "$backend_pid" 2>/dev/null; then tail -60 "$output/backend.log"; return 1; fi
    sleep 1
  done
  return 1
}
# As dependências são locais ou simuladas; nenhuma credencial de produção é utilizada.
mvn -q -f backend/ads-service/pom.xml test dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/process-test.classpath > "$output/backend-tests.log" 2>&1
python3 - "$output/backend-counts.json" <<'PY'
import json, pathlib, sys, xml.etree.ElementTree as xml
totals = dict(tests=0, errors=0, failures=0, skipped=0)
for report in pathlib.Path('backend/ads-service/target/surefire-reports').glob('TEST-*.xml'):
    suite = xml.parse(report).getroot()
    for key in totals: totals[key] += int(suite.get(key, 0))
pathlib.Path(sys.argv[1]).write_text(json.dumps(totals) + '\n')
assert totals['tests'] > 0 and totals['errors'] == 0 and totals['failures'] == 0
PY
npm --prefix frontend run typecheck > "$output/typecheck.log" 2>&1
npm --prefix frontend test -- --run > "$output/frontend-tests.log" 2>&1
npm --prefix frontend run build > "$output/frontend-build.log" 2>&1
node --test process-execution-worker/test > "$output/worker-tests.log" 2>&1
bash scripts/validate-liquibase-mysql57.sh > "$output/liquibase-static.log" 2>&1
bash scripts/test-deploy-transactional-contract.sh > "$output/deploy-contract.log" 2>&1
bash scripts/test-deployment-change-resume.sh > "$output/deploy-resume.log" 2>&1
python3 infra/testing/process-automation/test-delivery-contract.py > "$output/delivery-contract.log" 2>&1
bash scripts/run-docker-homologation.sh bash infra/testing/process-automation/worker-container-test.sh > "$output/worker-container.log" 2>&1
compose up -d --wait > "$output/mysql.log" 2>&1
start_backend
node infra/testing/process-automation/api-matrix.mjs > "$output/api.log" 2>&1
node infra/testing/process-automation/lifecycle-matrix.mjs > "$output/lifecycle.log" 2>&1
kill "$backend_pid"
wait "$backend_pid" || true
backend_pid=""
start_backend
node infra/testing/process-automation/restart-check.mjs > "$output/restart.log" 2>&1
node infra/testing/process-automation/frontend-server.mjs > "$output/frontend-server.log" 2>&1 &
frontend_pid=$!
PROCESS_TEST_ARTIFACTS="$output/browser" node infra/testing/process-automation/browser-matrix.mjs > "$output/browser.log" 2>&1
compose exec -T process-mysql mysql -uroot -pprocess-local-only process_automation_local --batch --skip-column-names -e "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-09-12-product-process-automation%'; SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE FROM information_schema.columns WHERE table_schema='process_automation_local' AND table_name IN ('product_process_run_v1','product_process_run_event_v1') ORDER BY TABLE_NAME,ORDINAL_POSITION;" > "$output/mysql-schema.txt" 2>/dev/null
git diff --check > "$output/diff-check.log"
printf 'PASS rodada %s: backend, frontend, worker, contratos, MySQL, HTTP, reinício, desktop e mobile\n' "$round_name" | tee "$output/result.txt"
