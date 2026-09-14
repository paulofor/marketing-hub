#!/usr/bin/env bash
# Homologa continuidade, custo e contratos de Apolo com APIs e MySQL somente locais.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round="${1:?Informe a rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
output="$PWD/artifacts/video-production-continuity/$round"
mkdir -p "$output"
project="${VIDEO_FINANCE_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo}"
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
    tail -n 50 "$output/$step.log"
    return 1
  fi
}
wait_http() {
  python3 - "$1" <<'PY'
import sys, time, urllib.request
for _ in range(100):
    try:
        with urllib.request.urlopen(sys.argv[1], timeout=1) as r:
            if r.status == 200: break
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
for module in backend/ads-service video-management-service financial-agent-worker; do
  rm -rf "$module/target/surefire-reports"
  run "$(basename "$module")" mvn -B -ntp -f "$module/pom.xml" test -Dvideo.proof.real-ffmpeg=true
  cp -r "$module/target/surefire-reports" "$output/$(basename "$module")-reports"
done
run backend-ci-contract python3 scripts/test-backend-ci-workflow.py
run package mvn -B -ntp -f backend/ads-service/pom.xml package -DskipTests
run package-contract python3 scripts/test-backend-packaged-resources.py
run package-integrity python3 scripts/verify-backend-packaged-resources.py
run frontend npm --prefix frontend test -- --run src/pages/salesVideo/ProductSalesVideoPage.helpers.test.ts src/pages/financial src/pages/learningCycle src/pages/audioVideoStudio/AudioVideoStudioPage.test.tsx src/api/salesVideo src/pages/product/ProductProcessAutomationPanel.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/productProcessContext.test.tsx
run application-contract python3 infra/testing/vega-process6-recovery/test-application.py
run cors-contract python3 scripts/test-video-read-cors.py
run typecheck npm --prefix frontend run typecheck
run build env VITE_API_URL=http://127.0.0.1:15173 npm --prefix frontend run build
run classpath mvn -q -f backend/ads-service/pom.xml dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/video-finance-classpath
run finalization-lock-compile javac --class-path "backend/ads-service/target/classes:$(cat backend/ads-service/target/video-finance-classpath)" -d "$output/lock-classes" infra/testing/video-finance/VerifyFinalizationLock.java
run finalization-lock java --class-path "$output/lock-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/video-finance-classpath)" VerifyFinalizationLock
run worker-classpath mvn -q -f video-management-service/pom.xml dependency:build-classpath -Dmdep.outputFile=target/runway-classpath
run worker-contract java --class-path "video-management-service/target/classes:$(cat video-management-service/target/runway-classpath)" infra/testing/runway-clip-plan/VerifyWorker.java backend/ads-service/target/runway-clip-contract.json "$output/worker-requests.json"
run proof-compile javac --class-path "video-management-service/target/classes:$(cat video-management-service/target/runway-classpath)" -d "$output/proof-classes" infra/testing/video-finance/VerifyPrivateProof.java
run proof-integration java --class-path "$output/proof-classes:video-management-service/target/classes:$(cat video-management-service/target/runway-classpath)" com.marketinghub.videomanagement.service.provider.VerifyPrivateProof backend/ads-service/target/runway-clip-contract.json "$output/media"
run media-probe ffprobe -v error -show_streams -show_format -of json "$output/media/final-fixture.mp4"
run media-player sandbox-media-player "$output/media/final-fixture.mp4" "$output/media/player.html"
run media-browser node infra/testing/video-finance/verify-media-responsive.cjs "$output/media"
run hls-browser node infra/testing/video-finance/verify-hls-responsive.cjs "$output/media"
LEARNING_CYCLES_DB_HOST=sandbox-docker java -Xmx768m \
  -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/video-finance-classpath)" \
  com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleLocalApplication > "$output/api.log" 2>&1 &
api_pid=$!
wait_http http://127.0.0.1:18091/api/products
run financial-rest python3 infra/testing/video-finance/validate.py
run preflight-rest python3 infra/testing/video-finance/validate-video-preflight.py
node frontend/node_modules/vite/bin/vite.js preview frontend --config frontend/vite.learning-cycles-local.config.ts > "$output/ui.log" 2>&1 &
ui_pid=$!
wait_http http://127.0.0.1:15173
run browser env VIDEO_PREFLIGHT_FIXTURE_RESULT="$output/preflight-rest.log" VIDEO_PREFLIGHT_EVIDENCE_DIR="$output/browser" node frontend/e2e/video-preflight-guidance-responsive.mjs
run finalization-browser node infra/testing/video-finance/verify-finalization-responsive.cjs "$output/media"
run spotless mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*(ProcessRunVideoGuidance|SalesVideoJobDto|DeliveryPreparation|VideoFinalDeliveryContract|VideoProjectFunnelRole|SalesVideoJobRepository|ExperimentVideoAssetJobSyncService|RequestSalesVideoPostProductionRequest|VideoProductionCycleService|SalesVideoController|SalesVideoAssetControllerTest|SalesVideoService|SalesVideoJobService|VideoProjectService|VideoProductProof.*|AgentTaskVideoProductProofSource).*java'
run diff git diff --check
python3 - "$output" <<'PY'
import pathlib, json, sys, xml.etree.ElementTree as E
output = pathlib.Path(sys.argv[1])
counts = {}
for folder in output.glob('*-reports'):
    values = {k:0 for k in ['tests','failures','errors','skipped']}
    for p in folder.glob('TEST-*.xml'):
        root = E.parse(p).getroot()
        for k in values: values[k] += int(root.get(k,0))
    assert values['tests'] > 0 and not values['failures'] and not values['errors'], values
    values['executed'] = values['tests'] - values['skipped']
    counts[folder.name] = values
output.joinpath('counts.json').write_text(json.dumps(counts,indent=2))
print(json.dumps(counts))
PY
printf 'RODADA COMPLETA APROVADA: %s\n' "$round"
