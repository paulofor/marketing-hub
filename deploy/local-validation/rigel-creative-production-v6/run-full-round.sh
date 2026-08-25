#!/usr/bin/env bash
set -euo pipefail

ROUND=${1:?Informe o identificador da rodada}
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd "${SCRIPT_DIR}/../../.." && pwd)
EVIDENCE_DIR="${SCRIPT_DIR}/evidence/rounds/${ROUND}"
COMPOSE_PROJECT=aihub-04f755ef-c3c0-43b2-ba12-14fdd53b0762-d879448de8
CAPTURE_CONTAINER="${COMPOSE_PROJECT}-rigel-creative-capture-${ROUND//[^a-zA-Z0-9_.-]/-}"
COMPOSE_FILES=(
  -f "${REPOSITORY_ROOT}/pde-platform/docker-compose.assisted-service-validation.yml"
  -f "${SCRIPT_DIR}/docker-compose.capture.yml"
)

mkdir -p "${EVIDENCE_DIR}"
cd "${REPOSITORY_ROOT}"

cleanup() {
  docker rm -f "${CAPTURE_CONTAINER}" >/dev/null 2>&1 || true
  docker compose -p "${COMPOSE_PROJECT}" "${COMPOSE_FILES[@]}" down \
    --volumes --remove-orphans >"${EVIDENCE_DIR}/compose-down.log" 2>&1 || true
}
trap cleanup EXIT

scripts/validate-liquibase-mysql57.sh 2>&1 | tee "${EVIDENCE_DIR}/liquibase.log"

(
  cd backend/ads-service
  mvn -q spotless:check \
    -Dtest=CommercialPlanControllerTest,CommercialPlanImageStudioServiceTest,CommercialPlanVisualAssetServiceTest,ApprovedCreativePackageArchiveTest,AgentTaskServiceTest,BusinessProcessChangelogTest \
    test
) 2>&1 | tee "${EVIDENCE_DIR}/backend.log"

(
  cd customer-agent-worker
  mvn -q spotless:check test
) 2>&1 | tee "${EVIDENCE_DIR}/psique-worker.log"

(
  cd meta-ad-approver-worker
  mvn -q spotless:check test
) 2>&1 | tee "${EVIDENCE_DIR}/temis-worker.log"

(
  cd video-management-service
  mvn -q test
) 2>&1 | tee "${EVIDENCE_DIR}/apolo-video-studio.log"

(
  cd frontend
  npm ci
  npx prettier --check src/pages/planning/CommercialPlanningPage.tsx \
    src/pages/planning/CommercialPlanningPage.test.tsx
  npm test -- --run
  npm run typecheck
  npm run build
) 2>&1 | tee "${EVIDENCE_DIR}/administrative-frontend.log"
node "${SCRIPT_DIR}/validate-admin-import-ui.mjs" \
  "${SCRIPT_DIR}" "${EVIDENCE_DIR}/admin-import-ui" \
  2>&1 | tee "${EVIDENCE_DIR}/admin-import-ui.json"

(
  cd pde-platform/frontend
  npm ci
  npx prettier --check tests/rigel-creative-proof.spec.ts \
    playwright.rigel-creative-proof.config.ts
  npm run build
) 2>&1 | tee "${EVIDENCE_DIR}/pde-frontend.log"

docker compose -p "${COMPOSE_PROJECT}" "${COMPOSE_FILES[@]}" build \
  pde-assisted-service-e2e pde-rigel-creative-capture \
  2>&1 | tee "${EVIDENCE_DIR}/compose-build.log"

docker compose -p "${COMPOSE_PROJECT}" "${COMPOSE_FILES[@]}" run --rm \
  pde-assisted-service-e2e \
  2>&1 | tee "${EVIDENCE_DIR}/pde-e2e.log"

docker rm -f "${CAPTURE_CONTAINER}" >/dev/null 2>&1 || true
docker compose -p "${COMPOSE_PROJECT}" "${COMPOSE_FILES[@]}" run \
  --name "${CAPTURE_CONTAINER}" \
  pde-rigel-creative-capture \
  2>&1 | tee "${EVIDENCE_DIR}/creative-proof.log"
mkdir -p "${SCRIPT_DIR}/evidence/proof"
docker cp \
  "${CAPTURE_CONTAINER}:/tmp/rigel-creative-proof/." \
  "${SCRIPT_DIR}/evidence/proof/"
docker rm "${CAPTURE_CONTAINER}" >/dev/null

for proof_file in \
  rigel-destination-desktop.png \
  rigel-destination-mobile.png \
  rigel-offer-proof.png \
  rigel-tasting-proof.png \
  rigel-tasting-response.png \
  rigel-tasting-question.png \
  rigel-tasting-followups.png; do
  test -s "${SCRIPT_DIR}/evidence/proof/${proof_file}"
done

node "${SCRIPT_DIR}/run-agents.mjs" planning \
  2>&1 | tee "${EVIDENCE_DIR}/agents-planning.log"
node "${SCRIPT_DIR}/generate-assets.mjs" \
  "${SCRIPT_DIR}/rigel-creative-contract.v1.json" \
  "${SCRIPT_DIR}/evidence/proof" \
  "${SCRIPT_DIR}/evidence/artifacts" \
  "${SCRIPT_DIR}/evidence/apollo-storyboard.json" \
  >"${EVIDENCE_DIR}/asset-generation.json"
node "${SCRIPT_DIR}/run-agents.mjs" reviews \
  2>&1 | tee "${EVIDENCE_DIR}/agents-reviews.log"
node "${SCRIPT_DIR}/validate-package.mjs" "${SCRIPT_DIR}" \
  2>&1 | tee "${EVIDENCE_DIR}/package-validation.json"
"${SCRIPT_DIR}/package-approved-creative.sh" \
  2>&1 | tee "${EVIDENCE_DIR}/approved-package.log"
(
  cd backend/ads-service
  mvn -q spotless:check \
    -Dtest=ApprovedCreativePackageArchiveTest#acceptsGeneratedPackageWhenProvided \
    -Dapproved.package.path="${SCRIPT_DIR}/evidence/rigel-approved-creative-package.zip" \
    test
) 2>&1 | tee "${EVIDENCE_DIR}/approved-package-backend-validation.log"

sandbox-media-player \
  "${SCRIPT_DIR}/evidence/artifacts/rigel-vertical-demo-1080x1920.mp4" \
  "${SCRIPT_DIR}/evidence/rigel-video-player.html" \
  >"${EVIDENCE_DIR}/media-player.log"
node "${SCRIPT_DIR}/validate-media-player.mjs" \
  "${SCRIPT_DIR}/evidence/rigel-video-player.html" \
  "${EVIDENCE_DIR}/media-playback" \
  "${SCRIPT_DIR}/rigel-creative-contract.v1.json" \
  2>&1 | tee "${EVIDENCE_DIR}/media-validation.json"

git diff --check 2>&1 | tee "${EVIDENCE_DIR}/diff-check.log"
printf '{"round":"%s","status":"APPROVED"}\n' "${ROUND}" \
  | tee "${EVIDENCE_DIR}/result.json"
