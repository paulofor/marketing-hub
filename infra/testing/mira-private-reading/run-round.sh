#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
mira_round="${1:?Informe a identificação da rodada}"
[[ "$mira_round" =~ ^[a-zA-Z0-9-]+$ ]] || exit 2
: "${AIHUB_HOMOLOGATION_SESSION:?Execute dentro de scripts/run-docker-homologation.sh}"
: "${MIRA_DOCKER_PROJECT:?Informe o projeto Compose exclusivo desta sandbox}"
mira_project="$MIRA_DOCKER_PROJECT"
[[ "$mira_project" =~ ^aihub-[a-z0-9][a-z0-9-]+$ ]] || { echo 'Projeto Compose inválido'; exit 2; }
mira_output="$PWD/tmp/mira-reading-${mira_round}"
mkdir -p "$mira_output"
compose=(docker compose -p "$mira_project" -f infra/testing/mira-private-reading/docker-compose.yml)
export MIRA_LOCAL_BACKEND_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/mira-pde-backend:latest"
export MIRA_LOCAL_FRONTEND_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/mira-pde-frontend:latest"
mira_host=$(node -e 'const h=process.env.DOCKER_HOST||""; process.stdout.write(h.startsWith("tcp:")?new URL(h).hostname:"127.0.0.1")')
export MIRA_TEST_PDE_URL="http://${mira_host}:18076"
mira_hub_pid=""
mira_front_pid=""

cleanup() {
  [[ -z "$mira_hub_pid" ]] || kill "$mira_hub_pid" 2>/dev/null || true
  [[ -z "$mira_front_pid" ]] || kill "$mira_front_pid" 2>/dev/null || true
  "${compose[@]}" logs --no-color > "$mira_output/containers.log" 2>&1 || true
  "${compose[@]}" down --volumes --remove-orphans >> "$mira_output/cleanup.log" 2>&1
}
trap cleanup EXIT

if [[ "${MIRA_SKIP_SUITE:-false}" != true ]]; then
  mvn -q -f backend/ads-service/pom.xml test > "$mira_output/hub-tests.log" 2>&1
  mvn -q -f pde-platform/backend/pom.xml test > "$mira_output/pde-tests.log" 2>&1
  (cd frontend && npm test -- --run) > "$mira_output/frontend-tests.log" 2>&1
  (cd frontend && npm run build) > "$mira_output/frontend-build.log" 2>&1
  ACTIONLINT_DOCKER_PROJECT="$mira_project" bash scripts/run-actionlint.sh \
    .github/workflows/deploy-containers.yml > "$mira_output/actionlint.log" 2>&1
  mvn -q -f backend/ads-service/pom.xml spotless:check \
    '-DspotlessFiles=.*product/privatereading/.*\.java,.*PdePrivateReadingHumanActivityHandler.*\.java' \
    > "$mira_output/spotless.log" 2>&1
  bash scripts/test-docker-temporary-image-cleanup.sh > "$mira_output/image-contract.log" 2>&1
  bash scripts/test-deploy-transactional-contract.sh > "$mira_output/deploy-contract.log" 2>&1
  bash scripts/test-shared-vps-deploy-resilience.sh > "$mira_output/deploy-resilience.log" 2>&1
  shellcheck scripts/run-docker-homologation.sh scripts/docker-build-temporary-image.sh \
    scripts/test-docker-temporary-image-cleanup.sh infra/testing/mira-private-reading/run-round.sh \
    > "$mira_output/shellcheck.log" 2>&1
fi
mvn -q -f backend/ads-service/pom.xml test-compile dependency:build-classpath \
  -Dmdep.includeScope=test -Dmdep.outputFile="$mira_output/classpath" > "$mira_output/hub-compile.log" 2>&1
mvn -q -f pde-platform/backend/pom.xml -DskipTests package > "$mira_output/pde-build.log" 2>&1
(cd pde-platform/frontend && npm run build) > "$mira_output/pde-frontend-build.log" 2>&1
(cd pde-platform/frontend && npm run check:api-boundary) > "$mira_output/pde-boundary.log" 2>&1
bash scripts/docker-build-temporary-image.sh mira-pde-backend \
  -f infra/testing/mira-private-reading/Dockerfile.backend pde-platform/backend > "$mira_output/backend-image.log" 2>&1
mkdir -p "$mira_output/frontend-context"
cp -a pde-platform/frontend/dist "$mira_output/frontend-context/"
cp pde-platform/frontend/nginx.conf "$mira_output/frontend-context/"
bash scripts/docker-build-temporary-image.sh mira-pde-frontend \
  -f infra/testing/mira-private-reading/Dockerfile.frontend "$mira_output/frontend-context" > "$mira_output/frontend-image.log" 2>&1
"${compose[@]}" up -d --wait mysql > "$mira_output/compose.log" 2>&1
"${compose[@]}" exec -T mysql mysql -uroot -pmira-local-root mira_local \
  < pde-platform/local-validation/mysql-init/001-pde-local-schema.sql > "$mira_output/schema.log" 2>&1
"${compose[@]}" up -d >> "$mira_output/compose.log" 2>&1
java -Xmx512m -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat "$mira_output/classpath")" \
  com.marketinghub.product.privatereading.MiraReadingSandbox --server.port=18090 \
  --integrations.pde-platform.base-url="http://${mira_host}:18096" \
  --integrations.pde-platform.internal-token=mira-local-internal > "$mira_output/hub-runtime.log" 2>&1 &
mira_hub_pid=$!
node frontend/node_modules/vite/bin/vite.js --config infra/testing/mira-private-reading/vite.config.mjs \
  > "$mira_output/frontend-runtime.log" 2>&1 &
mira_front_pid=$!
for mira_url in "${MIRA_TEST_PDE_URL}/api/pde/mira/private/v1/contract" \
  http://127.0.0.1:18090/api/products/10/private-readings/privateReading1 \
  http://127.0.0.1:15173/local-validation/mira-reading.html; do
  mira_ready=false
  for ((mira_attempt = 0; mira_attempt < 90; mira_attempt++)); do
    if curl -fsS --max-time 3 "$mira_url" > /dev/null 2>&1; then mira_ready=true; break; fi
    sleep 1
  done
  [[ "$mira_ready" == true ]] || { echo "Serviço local indisponível: $mira_url"; exit 1; }
done
"${compose[@]}" stop --timeout 10 mira-backend >> "$mira_output/compose.log" 2>&1
node infra/testing/mira-private-reading/browser.cjs "$mira_output/screenshots" --unavailable \
  > "$mira_output/unavailable-browser.log" 2>&1
"${compose[@]}" start mira-backend >> "$mira_output/compose.log" 2>&1
mira_ready=false
for ((mira_attempt = 0; mira_attempt < 90; mira_attempt++)); do
  if curl -fsS --max-time 3 "${MIRA_TEST_PDE_URL}/api/pde/mira/private/v1/contract" > /dev/null 2>&1; then mira_ready=true; break; fi
  sleep 1
done
[[ "$mira_ready" == true ]] || { echo 'PDE local não retomou após indisponibilidade simulada'; exit 1; }
(cd pde-platform/frontend && PDE_PUBLIC_HEALTH_URL="$MIRA_TEST_PDE_URL" \
  MIRA_PRIVATE_E2E_TOKEN=mira-local-qa npm run test:mira-private:public -- --workers=1) \
  > "$mira_output/prototype-regression.log" 2>&1
node infra/testing/mira-private-reading/browser.cjs "$mira_output/screenshots" > "$mira_output/browser.log" 2>&1
"${compose[@]}" exec -T mysql mysql -uroot -pmira-local-root mira_local -N -e \
  'SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata_json,"$.trafficClass")),COUNT(*) FROM pde_funnel_event WHERE product_slug="mira-private-validation" GROUP BY 1 ORDER BY 1' \
  > "$mira_output/events.tsv" 2> "$mira_output/mysql.log"
python3 - "$mira_output/events.tsv" <<'PY'
import sys
rows=dict(line.strip().split('\t') for line in open(sys.argv[1]) if line.strip())
assert rows == {'PRIVATE_READING':'8','QA_INTERNAL':'5'}, rows
PY
"${compose[@]}" logs --no-color > "$mira_output/containers.log" 2>&1
if rg -q 'mira-local-human-one|mira-local-human-two|mira-local-qa' "$mira_output/containers.log"; then
  echo 'Segredo sintético encontrado em log operacional'; exit 1
fi
git diff --check > "$mira_output/diff-check.log"
printf 'Rodada %s aprovada: integração, três dispositivos, oito eventos privados sintéticos e cinco eventos QA.\n' "$mira_round"
