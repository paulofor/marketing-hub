#!/usr/bin/env bash
# Homologa backend, compositor e navegador reais com MySQL isolado e dependências externas simuladas.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_dir"
: "${KIT_TEST_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo desta homologação}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -XX:ActiveProcessorCount=2"
if [[ -z "${KIT_TEST_DB_HOST:-}" && "${DOCKER_HOST:-}" == tcp://sandbox-docker:* ]]; then
  export KIT_TEST_DB_HOST=sandbox-docker
fi
round_dir="$(mktemp -d "$repo_dir/.git/private-kit-XXXXXX")"
backend_pid=''
worker_pid=''
tls_pid=''
cleanup() {
  docker compose -p "$KIT_TEST_COMPOSE_PROJECT" -f infra/testing/private-kit/compose.yml logs --no-color mysql >"$round_dir/mysql.log" 2>&1 || true
  if [[ -n "${KIT_TEST_EVIDENCE_DIR:-}" ]]; then mkdir -p "$KIT_TEST_EVIDENCE_DIR"; cp -a "$round_dir/." "$KIT_TEST_EVIDENCE_DIR/"; fi
  if [[ -n "$worker_pid" ]]; then kill "$worker_pid" 2>/dev/null || true; fi
  if [[ -n "$backend_pid" ]]; then kill "$backend_pid" 2>/dev/null || true; fi
  if [[ -n "$tls_pid" ]]; then kill "$tls_pid" 2>/dev/null || true; fi
  docker compose -p "$KIT_TEST_COMPOSE_PROJECT" -f infra/testing/private-kit/compose.yml down --volumes --remove-orphans
}
trap cleanup EXIT
mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*KitPrivate.*[.]java,.*KitPrototypeCapabilities.*[.]java,.*KitArtifactContract.*[.]java,.*LearningCycle(ValueFlow.*|PrototypeRegistrationTest|Service)[.]java,.*PdeAgentValidationGateActivityExecutor(Test)?[.]java'
mvn -q -f backend/ads-service/pom.xml test-compile dependency:build-classpath -Dmdep.outputFile="$round_dir/backend-classpath" -DincludeScope=test -DskipTests
mvn -q -f lead-portal-payments-service/pom.xml test-compile dependency:build-classpath -Dmdep.outputFile="$round_dir/worker-classpath" -DincludeScope=test -DskipTests
docker compose -p "$KIT_TEST_COMPOSE_PROJECT" -f infra/testing/private-kit/compose.yml up -d --wait
backend_cp="backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat "$round_dir/backend-classpath")"
java -Xmx768m -cp "$backend_cp" com.marketinghub.pde.kit.privateprototype.v1.KitPrivateLocalApplication >"$round_dir/backend.log" 2>&1 &
backend_pid=$!
for attempt in {1..60}; do
  if curl --fail --silent http://127.0.0.1:57282/api/pde/kit/private/v1/prototype > /dev/null; then break; fi
  if ! kill -0 "$backend_pid" 2>/dev/null || (( attempt == 60 )); then cat "$round_dir/backend.log"; exit 1; fi
  sleep 1
done
mkdir -p "$round_dir/tls"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -keyout "$round_dir/tls/test.key" -out "$round_dir/tls/test.crt" -subj /CN=127.0.0.1 -addext subjectAltName=IP:127.0.0.1 >"$round_dir/tls/generation.log" 2>&1
export NODE_EXTRA_CA_CERTS="$round_dir/tls/test.crt"
pin="$(openssl x509 -in "$round_dir/tls/test.crt" -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64 -A)"
chromium_path="${PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH:-/usr/bin/chromium}"
printf '#!/usr/bin/env bash\nexec %q --ignore-certificate-errors-spki-list=%q "$@"\n' "$chromium_path" "$pin" >"$round_dir/tls/chromium-local.sh"
chmod +x "$round_dir/tls/chromium-local.sh"
bash -n "$round_dir/tls/chromium-local.sh"
shellcheck "$round_dir/tls/chromium-local.sh"
export PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH="$round_dir/tls/chromium-local.sh"
LOCAL_TLS_DIRECTORY="$round_dir/tls" LOCAL_TLS_UPSTREAM_PORT=57282 LOCAL_TLS_LISTEN_PORT=57284 node infra/testing/vega-integrity-cycle/local-tls.mjs >"$round_dir/tls/server.log" 2>&1 &
tls_pid=$!
for attempt in {1..30}; do
  if curl --cacert "$round_dir/tls/test.crt" --fail --silent https://127.0.0.1:57284/api/pde/kit/private/v1/prototype > /dev/null; then break; fi
  if ! kill -0 "$tls_pid" 2>/dev/null || (( attempt == 30 )); then cat "$round_dir/tls/server.log"; exit 1; fi
  sleep 1
done
worker_cp="lead-portal-payments-service/target/test-classes:lead-portal-payments-service/target/classes:$(cat "$round_dir/worker-classpath")"
java -Xmx512m -Djava.awt.headless=true -cp "$worker_cp" com.marketinghub.payments.service.kit.privateprototype.v1.PrivateKitLocalApplication "$round_dir/compositions" >"$round_dir/worker.log" 2>&1 &
worker_pid=$!
cat >"$round_dir/input.json" <<'JSON'
{"mode":"TECHNICAL","sourceReference":"experiment:9007","productId":8007,"productSlug":"kit-local-nails","profileCode":"nails-v1","cycleId":7007,"prototypeVersion":"v1","captureSessionId":"kit-local-round","sourceUrl":"https://127.0.0.1:57284/api/pde/kit/private/v1/prototype"}
JSON
PDE_INTERNAL_API_TOKEN=kit-local-internal-only node customer-agent-worker/src/main/resources/browser/private-kit-harness.mjs "$round_dir/input.json" "$round_dir/report.json" "$round_dir/captures"
python3 - "$round_dir/input.json" "$round_dir/other-product.json" <<'PY'
import json,sys
value=json.load(open(sys.argv[1]))
value.update(mode='TECHNICAL',productId=8018,cycleId=7018,sourceReference='experiment:9018',productSlug='kit-local-barber',profileCode='barber-v1',prototypeVersion='barber-candidate-v1',captureSessionId='kit-local-other-product')
with open(sys.argv[2],'w') as output:json.dump(value,output)
PY
PDE_INTERNAL_API_TOKEN=kit-local-internal-only node customer-agent-worker/src/main/resources/browser/private-kit-harness.mjs "$round_dir/other-product.json" "$round_dir/other-product-report.json" "$round_dir/other-product-captures"
KIT_SCENARIO_LOCAL=true KIT_LOCAL_INPUT="$round_dir/input.json" KIT_OTHER_LOCAL_INPUT="$round_dir/other-product.json" KIT_SCENARIO_RESULTS="$round_dir/scenario-results" mvn -q -f customer-agent-worker/pom.xml -Dtest=PrivateKitScenarioIntegrationTest test >"$round_dir/scenario-integration-tests.log" 2>&1
python3 - "$round_dir" <<'PY'
import json,pathlib,shutil,sys
root=pathlib.Path(sys.argv[1])
for context,report in (('input.json','report.json'),('other-product.json','other-product-report.json')):
    source=json.loads((root/context).read_text());proof=json.loads((root/report).read_text())
    assert proof['productId']==source['productId'] and proof['sourceReference']==source['sourceReference']
    proof['cycleId']=source['cycleId']
    flow=root/'flows'/source['profileCode'];flow.mkdir(parents=True)
    (flow/'TECHNICAL.json').write_text(json.dumps(proof))
    for scenario in ('ADHERENT','RECOVERY','SAFETY'):
        shutil.copyfile(root/'scenario-results'/source['profileCode']/(scenario+'.json'),flow/(scenario+'.json'))
PY
for profile in nails-v1 barber-v1; do
  flow_dir="$round_dir/flows/$profile"
  VEGA_GATE_FLOW_ARTIFACTS="$flow_dir" PDE_OPERATIONAL_REPORT_FILE="" mvn -q -f meta-ad-approver-worker/pom.xml -Dtest=CommercialBpmCycleIntegrationTest test >"$flow_dir/temis-tests.log" 2>&1
  VEGA_GATE_FLOW_ARTIFACTS="$flow_dir" mvn -q -f backend/ads-service/pom.xml -Dtest=VegaCycleGateFlowIntegrationTest test >"$flow_dir/gate-tests.log" 2>&1
done
kill "$worker_pid"
wait "$worker_pid" || true
worker_pid=''
PDE_INTERNAL_API_TOKEN=kit-local-internal-only node infra/testing/private-kit/controls.mjs "$round_dir/controls.json"
KIT_LOCAL_REPORT="$round_dir/report.json" mvn -q -f backend/ads-service/pom.xml -Dtest=KitPrivateProofContractTest test >"$round_dir/backend-proof-tests.log" 2>&1
KIT_LOCAL_REPORT="$round_dir/report.json" mvn -q -f customer-agent-worker/pom.xml -Dtest=PrivateKitHarnessContractTest test >"$round_dir/worker-proof-tests.log" 2>&1
python3 - "$round_dir/report.json" "$round_dir/other-product-report.json" <<'PY'
import json,sys
report=json.load(open(sys.argv[1]));other=json.load(open(sys.argv[2]))
assert report['decision']=='APPROVED' and len(report['scenarios'])==9 and report['providerCalls']==0
assert other['decision']=='APPROVED' and other['profileCode']=='barber-v1' and len(other['scenarios'])==9
print('Kit privado: 18 percursos, seis callbacks de Psique e dois fluxos Têmis/gate aprovados com modelos simulados, sem chamada paga ou envio externo.')
PY
