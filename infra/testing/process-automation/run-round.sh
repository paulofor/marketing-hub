#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_root"
compose_project="${PROCESS_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
round_name="${1:-round}"
output="${repo_root}/artifacts/process-automation/${round_name}"
mkdir -p "$output"
run_check() {
  local report="$1"
  shift
  printf 'Validando %s\n' "$report"
  if "$@" > "$output/$report" 2>&1; then
    printf 'PASS %s\n' "$report"
  else
    local status=$?
    printf 'ERRO em %s (código %s). Log: %s\n' "$report" "$status" "$output/$report" >&2
    tail -60 "$output/$report" >&2
    return "$status"
  fi
}
# A homologação deve usar a mesma versão principal do CI e da imagem do executor.
run_check runtime.log node --input-type=module -e '
  console.log(`Node ${process.version}`);
  if (Number(process.versions.node.split(".")[0]) !== 22) {
    console.error("A homologação de processos exige Node 22, como o CI e o Dockerfile do executor.");
    process.exit(1);
  }
'
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
run_check worker-tests.log npm --prefix process-execution-worker test
run_check backend-tests.log mvn -q -f backend/ads-service/pom.xml test dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/process-test.classpath
python3 - "$output/backend-counts.json" <<'PY'
import json, pathlib, sys, xml.etree.ElementTree as xml
totals = dict(tests=0, errors=0, failures=0, skipped=0)
for report in pathlib.Path('backend/ads-service/target/surefire-reports').glob('TEST-*.xml'):
    suite = xml.parse(report).getroot()
    for key in totals: totals[key] += int(suite.get(key, 0))
pathlib.Path(sys.argv[1]).write_text(json.dumps(totals) + '\n')
assert totals['tests'] > 0 and totals['errors'] == 0 and totals['failures'] == 0
PY
run_check runner-contract.log python3 infra/testing/process-automation/test-runner-contract.py
run_check typecheck.log npm --prefix frontend run typecheck
run_check frontend-tests.log npm --prefix frontend test -- --run
run_check frontend-build.log npm --prefix frontend run build
run_check liquibase-static.log bash scripts/validate-liquibase-mysql57.sh
run_check deploy-contract.log bash scripts/test-deploy-transactional-contract.sh
run_check deploy-resume.log bash scripts/test-deployment-change-resume.sh
run_check delivery-contract.log python3 infra/testing/process-automation/test-delivery-contract.py
run_check worker-container.log bash scripts/run-docker-homologation.sh bash infra/testing/process-automation/worker-container-test.sh
run_check mysql.log compose up -d --wait
start_backend
run_check api.log node infra/testing/process-automation/api-matrix.mjs
run_check lifecycle.log node infra/testing/process-automation/lifecycle-matrix.mjs
kill "$backend_pid"
wait "$backend_pid" || true
backend_pid=""
start_backend
run_check restart.log node infra/testing/process-automation/restart-check.mjs
node infra/testing/process-automation/frontend-server.mjs > "$output/frontend-server.log" 2>&1 &
frontend_pid=$!
PROCESS_TEST_ARTIFACTS="$output/browser" run_check browser.log node infra/testing/process-automation/browser-matrix.mjs
compose exec -T process-mysql mysql -uroot -pprocess-local-only process_automation_local --batch --skip-column-names -e "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-09-12-product-process-automation%'; SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE FROM information_schema.columns WHERE table_schema='process_automation_local' AND table_name IN ('product_process_run_v1','product_process_run_event_v1') ORDER BY TABLE_NAME,ORDINAL_POSITION;" > "$output/mysql-schema.txt" 2>/dev/null
git diff --check > "$output/diff-check.log"
printf 'PASS rodada %s: backend, frontend, worker, contratos, MySQL, HTTP, reinício, desktop e mobile\n' "$round_name" | tee "$output/result.txt"
