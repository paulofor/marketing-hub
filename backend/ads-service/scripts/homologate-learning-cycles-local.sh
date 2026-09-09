#!/usr/bin/env bash
# Executa uma rodada completa e isolada; não chama APIs, agentes ou infraestrutura de produção.
set -euo pipefail
cd "$(dirname "$0")/../../.."
cycle_scope=${1:-full}
case "$cycle_scope" in full|--video-matrix|--persistence-only) ;; *) exit 2 ;; esac
: "${LEARNING_CYCLES_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
case "$LEARNING_CYCLES_COMPOSE_PROJECT" in aihub-*) ;; *) exit 2 ;; esac
export LEARNING_CYCLES_DB_HOST=${LEARNING_CYCLES_DB_HOST:-127.0.0.1}
case "$LEARNING_CYCLES_DB_HOST" in 127.0.0.1|sandbox-docker) ;; *) exit 2 ;; esac
cycle_output=$(mktemp -d /tmp/learning-sales-cycle-round-XXXXXX)
cycle_api_pid=""
cycle_ui_pid=""
cycle_atena_pid=""
compose=(docker compose -p "$LEARNING_CYCLES_COMPOSE_PROJECT" -f backend/ads-service/docker-compose.learning-cycles-local.yml)
cleanup() {
  local result=$?
  trap - EXIT
  if [[ -n "$cycle_api_pid" ]]; then kill "$cycle_api_pid" 2>/dev/null || true; wait "$cycle_api_pid" 2>/dev/null || true; fi
  if [[ -n "$cycle_atena_pid" ]]; then kill "$cycle_atena_pid" 2>/dev/null || true; wait "$cycle_atena_pid" 2>/dev/null || true; fi
  if [[ -n "$cycle_ui_pid" ]]; then kill "$cycle_ui_pid" 2>/dev/null || true; wait "$cycle_ui_pid" 2>/dev/null || true; fi
  "${compose[@]}" down --volumes --remove-orphans > "$cycle_output/cleanup.log" 2>&1 || result=1
  printf 'Resultado=%s Evidências=%s\n' "$result" "$cycle_output"
  exit "$result"
}
trap cleanup EXIT
run() {
  local label=$1
  shift
  printf 'Executando %s\n' "$label"
  if "$@" > "$cycle_output/$label.log" 2>&1; then
    printf 'PASS %s\n' "$label"
  else
    tail -35 "$cycle_output/$label.log"
    return 1
  fi
}
wait_http() {
  local url=$1
  for ((attempt=0; attempt<120; attempt++)); do
    if curl --fail --silent --max-time 1 "$url" > /dev/null; then return 0; fi
    sleep 0.25
  done
  printf 'Falha ao iniciar serviço local: %s\n' "$url" >&2
  return 1
}
printf 'Matriz local: %s\n' "$cycle_output"
run docker-version docker version
run buildx-version docker buildx version
run compose-version docker compose version
run database "${compose[@]}" up -d --wait --wait-timeout 120
run temporal-contract python3 -B -m unittest scripts.test_liquibase_temporal_contract
run liquibase-static bash scripts/validate-liquibase-mysql57.sh
run spotless mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*learningcycle.*[.]java,.*ProductSubprocessPosition.*[.]java,.*ProductProcessActivityExecutionHistoryResponse[.]java,.*AgentTaskService(Test)?[.]java,.*BusinessProcessActivityExecution(Service|Controller|ServiceTest)[.]java,.*PdeAgentValidationGateActivityExecutor(Test)?[.]java'
if [[ "$cycle_scope" != --persistence-only ]]; then
cycle_test_options=()
if [[ "$cycle_scope" == --video-matrix ]]; then
  cycle_test_options=('-Dtest=SalesFlow*Test,LearningCycle*Test,AgentHarnessCatalogTest,ArquiteturaTest,AgentTaskServiceTest,ProductSubprocessPositionResolverTest,BusinessProcess*Test,ProductValueChainPosition*Test,PdeProductionSlotServiceTest,VideoCreativeControllerTest,ExperimentVideoAssetServiceTest,PdeAgentValidationGateActivityExecutorTest')
fi
# Relatórios gerados de rodadas anteriores não compõem a contagem da rodada corrente.
rm -rf backend/ads-service/target/surefire-reports
run backend mvn -q -f backend/ads-service/pom.xml "${cycle_test_options[@]}" test
python3 - "$cycle_output/backend-count.json" <<'PY'
import glob, json, sys, xml.etree.ElementTree as ET
counts = dict(tests=0, failures=0, errors=0, skipped=0)
for file in glob.glob('backend/ads-service/target/surefire-reports/TEST-*.xml'):
    root = ET.parse(file).getroot()
    for key in counts: counts[key] += int(root.attrib.get(key, 0))
assert counts['tests'] > 0 and counts['failures'] == 0 and counts['errors'] == 0, counts
open(sys.argv[1], 'w').write(json.dumps(counts))
print('Java:', json.dumps(counts), flush=True)
PY
run frontend npm --prefix frontend test -- --run
run typecheck npm --prefix frontend run typecheck
run build env VITE_API_URL=http://127.0.0.1:15173 npm --prefix frontend run build
else
run compile mvn -q -f backend/ads-service/pom.xml -DskipTests test-compile
fi
run atena-tests mvn -q -f experiment-strategist-worker/pom.xml test
run atena-spotless mvn -q -f experiment-strategist-worker/pom.xml spotless:check
run decision-prettier npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/api/learningCycle/useDecisionProposal.ts \
  frontend/src/pages/learningCycle/LearningCycleDecisionPanel.tsx \
  frontend/src/pages/learningCycle/LearningCycleDecisionPanel.test.tsx \
  frontend/src/pages/learningCycle/LearningCycleCommandForm.tsx \
  frontend/src/pages/learningCycle/LearningCyclesPage.tsx \
  frontend/src/pages/learningCycle/LearningCyclesPage.test.tsx
run atena-classpath mvn -q -f experiment-strategist-worker/pom.xml dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/decision-classpath
run classpath mvn -q -f backend/ads-service/pom.xml dependency:build-classpath -DincludeScope=test "-Dmdep.outputFile=$cycle_output/classpath"
cycle_test_classpath="backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat "$cycle_output/classpath")"
run pde-mysql-database "${compose[@]}" exec -T learning-cycles-mysql mysql -uroot -pcycles-root-local-only -e 'DROP DATABASE IF EXISTS hermes_test; CREATE DATABASE hermes_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
run pde-mysql env \
  "HERMES_TEST_JDBC_URL=jdbc:mysql://$LEARNING_CYCLES_DB_HOST:18307/hermes_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  HERMES_TEST_JDBC_USERNAME=root \
  HERMES_TEST_JDBC_PASSWORD=cycles-root-local-only \
  mvn -q -f backend/ads-service/pom.xml -Dtest=PdeExperimentAnalyticsIntegrationTest test
java -Xmx512m -cp "$cycle_test_classpath" com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleLocalApplication > "$cycle_output/api.log" 2>&1 &
cycle_api_pid=$!
wait_http 'http://127.0.0.1:18091/api/products'
run decision-rest python3 backend/ads-service/scripts/validate-learning-cycle-decision-e2e.py
java -Xmx256m -cp "experiment-strategist-worker/target/test-classes:experiment-strategist-worker/target/classes:$(cat experiment-strategist-worker/target/decision-classpath)" com.marketinghub.experimentstrategistworker.learningcyclev1.decision.LearningCycleDecisionLocalRunner > "$cycle_output/atena-local.log" 2>&1 &
cycle_atena_pid=$!
run rest-mysql python3 backend/ads-service/scripts/validate-learning-cycles-e2e.py
if [[ "$cycle_scope" != --persistence-only ]]; then
node frontend/node_modules/vite/bin/vite.js preview frontend --config frontend/vite.learning-cycles-local.config.ts > "$cycle_output/ui.log" 2>&1 &
cycle_ui_pid=$!
wait_http 'http://127.0.0.1:15173/business-process-chains/learning-cycles'
run browser env "LEARNING_CYCLES_EVIDENCE_DIR=$cycle_output/browser" node frontend/e2e/learning-sales-cycles-responsive.mjs
run chain-browser env "LEARNING_CYCLES_EVIDENCE_DIR=$cycle_output/chain-browser" node frontend/e2e/learning-cycle-chain-entry-responsive.mjs
run legacy-browser env "LEARNING_CYCLES_EVIDENCE_DIR=$cycle_output/legacy-browser" node frontend/e2e/learning-cycle-legacy-entry-responsive.mjs
run decision-browser env "LEARNING_CYCLES_EVIDENCE_DIR=$cycle_output/decision-browser" node frontend/e2e/learning-cycle-decision-responsive.mjs
run sales-flow-browser env "LEARNING_CYCLES_EVIDENCE_DIR=$cycle_output/sales-flow-browser" node frontend/e2e/sales-process-flow-responsive.mjs
fi
kill "$cycle_atena_pid"
wait "$cycle_atena_pid" || true
cycle_atena_pid=""
kill "$cycle_api_pid"
wait "$cycle_api_pid" || true
cycle_api_pid=""
run migration java -Xmx256m -cp "$cycle_test_classpath" com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleMigrationVerifier verify-and-rollback
run migration-reapply java -Xmx256m -cp "$cycle_test_classpath" com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleMigrationVerifier update-and-verify
run migration-idempotency java -Xmx256m -cp "$cycle_test_classpath" com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleMigrationVerifier update-and-verify
run diff git diff --check
if [[ "$cycle_scope" != --persistence-only ]]; then
  printf 'RODADA COMPLETA APROVADA\n'
else
  printf 'VALIDAÇÃO DE PERSISTÊNCIA APROVADA; não substitui a matriz completa\n'
fi
