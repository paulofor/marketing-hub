#!/usr/bin/env bash
# Executa a candidata real com MySQL 5.7, sem conexão a produção ou provedor pago.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_dir"
: "${MIRA_TEST_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo desta homologação}"
round_dir="$(mktemp -d)"
backend_pid=''
frontend_pid=''
cleanup() {
  if [[ -n "${MIRA_TEST_EVIDENCE_DIR:-}" ]]; then mkdir -p "$MIRA_TEST_EVIDENCE_DIR"; cp -a "$round_dir/." "$MIRA_TEST_EVIDENCE_DIR/"; fi
  if [[ -n "$frontend_pid" ]]; then kill "$frontend_pid" 2>/dev/null || true; fi
  if [[ -n "$backend_pid" ]]; then kill "$backend_pid" 2>/dev/null || true; fi
  docker compose -p "$MIRA_TEST_COMPOSE_PROJECT" -f infra/testing/mira-candidate/compose.yml down --volumes --remove-orphans
}
trap cleanup EXIT
mvn -q -f backend/ads-service/pom.xml test-compile dependency:build-classpath \
  -Dmdep.outputFile="$round_dir/classpath" -DincludeScope=test -DskipTests
cp="backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat "$round_dir/classpath")"
docker compose -p "$MIRA_TEST_COMPOSE_PROJECT" -f infra/testing/mira-candidate/compose.yml up -d --wait
if [[ "${MIRA_CONTROLS_ONLY:-false}" == "true" ]]; then
  MIRA_CONTROLS_DB_HOST="${MIRA_TEST_DB_HOST:-127.0.0.1}" \
    mvn -q -f backend/ads-service/pom.xml -Dtest=MiraPrivateControlsMysql57Test test
  python3 infra/testing/mira-candidate/emit-controls-evidence.py \
    backend/ads-service/target/surefire-reports/TEST-com.marketinghub.pde.mira.privateprototype.v1.MiraPrivateControlsMysql57Test.xml \
    "$round_dir/operational-controls.json"
  exit 0
fi
java -Xmx768m -cp "$cp" com.marketinghub.pde.mira.privateprototype.v1.MiraPrivateLocalApplication >"$round_dir/backend.log" 2>&1 &
backend_pid=$!
for attempt in {1..60}; do
  if curl --fail --silent http://127.0.0.1:57182/api/pde/mira/candidate/v1/contract >"$round_dir/contract.json"; then break; fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then cat "$round_dir/backend.log"; exit 1; fi
  if (( attempt == 60 )); then cat "$round_dir/backend.log"; cat "$round_dir/frontend.log" 2>/dev/null || true; exit 1; fi
  sleep 1
done
curl --fail --silent http://127.0.0.1:57182/api/pde/mira/candidate/v1/contract >"$round_dir/contract.json"
npm --prefix pde-platform/frontend run build:mira-commercial
cp pde-platform/frontend/dist-mira-commercial/mira-commercial.html pde-platform/frontend/dist-mira-commercial/index.html
(cd pde-platform/frontend && exec node_modules/.bin/vite preview --config vite.mira-commercial.config.ts --host 127.0.0.1 --port 57181 --strictPort) >"$round_dir/frontend.log" 2>&1 &
frontend_pid=$!
for attempt in {1..60}; do
  if curl --fail --silent http://127.0.0.1:57181/mira-candidate >/dev/null; then break; fi
  if ! kill -0 "$frontend_pid" 2>/dev/null; then cat "$round_dir/frontend.log"; exit 1; fi
  if (( attempt == 60 )); then cat "$round_dir/backend.log"; cat "$round_dir/frontend.log" 2>/dev/null || true; exit 1; fi
  sleep 1
done
cat >"$round_dir/input.json" <<'JSON'
{"mode":"TECHNICAL","sourceReference":"experiment:9006","productId":8006,"productSlug":"pde-planejado-36","cycleId":7006,"prototypeVersion":"mira-private-candidate-v3","captureSessionId":"mira-local-round","sourceUrl":"http://127.0.0.1:57181/mira-candidate"}
JSON
# Reutiliza a matriz e o backend reais com recibo sintético opcional, sem aprovar vídeos produtivos.
if [[ -n "${MIRA_TEST_VIDEO_BINDING_FILE:-}" ]]; then
  python3 - "$round_dir/input.json" "$MIRA_TEST_VIDEO_BINDING_FILE" <<'PY'
import json,sys
with open(sys.argv[1]) as source:value=json.load(source)
with open(sys.argv[2]) as source:value['videoIntegration']=json.load(source)
with open(sys.argv[1],'w') as output:json.dump(value,output)
PY
fi
PDE_INTERNAL_API_TOKEN=mira-local-internal-only node customer-agent-worker/src/main/resources/browser/mira-candidate-harness.mjs "$round_dir/input.json" "$round_dir/report.json" "$round_dir/captures"
PDE_INTERNAL_API_TOKEN=mira-local-internal-only node infra/testing/mira-candidate/history-browser-test.mjs "$round_dir/input.json" "$round_dir/history.json"
for scenario in ADHERENT RECOVERY SAFETY; do
  python3 - "$round_dir/input.json" "$round_dir/scenario-$scenario-input.json" "$scenario" <<'PY'
import json,sys
value=json.load(open(sys.argv[1]))
value.update(mode='SCENARIO',scenarioCode=sys.argv[3],captureSessionId='mira-local-'+sys.argv[3])
if sys.argv[3]=='RECOVERY':
    value.update(productId=8017,cycleId=7017,sourceReference='experiment:9017')
    if 'videoIntegration' in value:
        value['videoIntegration'].update(productId=8017,cycleId=7017,experimentId=9017)
with open(sys.argv[2],'w') as output:json.dump(value,output)
PY
  PDE_INTERNAL_API_TOKEN=mira-local-internal-only node customer-agent-worker/src/main/resources/browser/mira-candidate-harness.mjs \
    "$round_dir/scenario-$scenario-input.json" "$round_dir/scenario-$scenario.json" "$round_dir/captures-$scenario"
done
MIRA_LOCAL_REPORT="$round_dir/report.json" mvn -q -f backend/ads-service/pom.xml -Dtest=PdeInputComparisonScenarioMatrixV1Test test >"$round_dir/backend-contract-tests.log" 2>&1
MIRA_LOCAL_REPORT="$round_dir/report.json" MIRA_LOCAL_SCENARIO_REPORT_DIR="$round_dir" \
  mvn -q -f customer-agent-worker/pom.xml test >"$round_dir/worker-contract-tests.log" 2>&1
python3 - "$round_dir/report.json" <<'PY'
import json,sys
report=json.load(open(sys.argv[1]))
assert report['decision']=='APPROVED'
assert len(report['scenarios'])==18
assert report['providerCalls']==0
if 'videoIntegrationFingerprint' in report:
    assert len(report['videoIdentity'])==2
    assert len(report['videoResults'])==18
    assert all(report['checks'][name] for name in ['videoIdentity','videoPlayback','videoOptional','videoFailureRecovery'])
print('Mira: 18 percursos aprovados, nenhum provedor externo acionado.')
PY
if [[ -n "${MIRA_TEST_EVIDENCE_DIR:-}" ]]; then mkdir -p "$MIRA_TEST_EVIDENCE_DIR"; cp -a "$round_dir/." "$MIRA_TEST_EVIDENCE_DIR/"; fi
