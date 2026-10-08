#!/usr/bin/env bash
# Homologa backend, compositor e navegador reais com MySQL isolado e dependências externas simuladas.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_dir"
: "${KIT_TEST_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo desta homologação}"
round_dir="$(mktemp -d "$repo_dir/.git/private-kit-XXXXXX")"
backend_pid=''
worker_pid=''
cleanup() {
  if [[ -n "${KIT_TEST_EVIDENCE_DIR:-}" ]]; then mkdir -p "$KIT_TEST_EVIDENCE_DIR"; cp -a "$round_dir/." "$KIT_TEST_EVIDENCE_DIR/"; fi
  if [[ -n "$worker_pid" ]]; then kill "$worker_pid" 2>/dev/null || true; fi
  if [[ -n "$backend_pid" ]]; then kill "$backend_pid" 2>/dev/null || true; fi
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
worker_cp="lead-portal-payments-service/target/test-classes:lead-portal-payments-service/target/classes:$(cat "$round_dir/worker-classpath")"
java -Xmx512m -Djava.awt.headless=true -cp "$worker_cp" com.marketinghub.payments.service.kit.privateprototype.v1.PrivateKitLocalApplication "$round_dir/compositions" >"$round_dir/worker.log" 2>&1 &
worker_pid=$!
cat >"$round_dir/input.json" <<'JSON'
{"mode":"TECHNICAL","sourceReference":"experiment:9007","productId":8007,"productSlug":"kit-local-nails","profileCode":"nails-v1","cycleId":7007,"prototypeVersion":"v1","captureSessionId":"kit-local-round","sourceUrl":"http://127.0.0.1:57282/api/pde/kit/private/v1/prototype"}
JSON
PDE_INTERNAL_API_TOKEN=kit-local-internal-only node customer-agent-worker/src/main/resources/browser/private-kit-harness.mjs "$round_dir/input.json" "$round_dir/report.json" "$round_dir/captures"
python3 - "$round_dir/input.json" "$round_dir/other-product.json" <<'PY'
import json,sys
value=json.load(open(sys.argv[1]))
value.update(mode='SCENARIO',scenarioCode='ADHERENT',productId=8018,cycleId=7018,sourceReference='experiment:9018',productSlug='kit-local-barber',profileCode='barber-v1',prototypeVersion='barber-candidate-v1',captureSessionId='kit-local-other-product')
with open(sys.argv[2],'w') as output:json.dump(value,output)
PY
PDE_INTERNAL_API_TOKEN=kit-local-internal-only node customer-agent-worker/src/main/resources/browser/private-kit-harness.mjs "$round_dir/other-product.json" "$round_dir/other-product-report.json" "$round_dir/other-product-captures"
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
assert other['decision']=='APPROVED' and other['profileCode']=='barber-v1'
print('Kit privado: nove percursos e outro produto aprovados, sem chamada paga ou envio externo.')
PY
