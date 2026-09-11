#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
output="$PWD/artifacts/vega-multiagent-recovery/build"
mkdir -p "$output"
# Imagens finais da intervenção autorizada; todas partem dos Dockerfiles do repositório.
node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence > "$output/review-evidence.log" 2>&1
node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence > "$output/psique-evidence.log" 2>&1
MAVEN_OPTS=-Xmx1g mvn -q -f backend/ads-service/pom.xml -DskipTests package > "$output/backend-package.log" 2>&1
docker build --build-arg BACKEND_BUILD_COMMIT=local-vega393-gate-v12-ui3 --build-arg BACKEND_BUILD_BRANCH=sandbox -f backend/ads-service/Dockerfile -t marketing-hub/backend:vega393-gate-v12-ui3 . > "$output/backend.log" 2>&1
npm --prefix frontend run build > "$output/admin-build.log" 2>&1
docker build -f frontend/Dockerfile -t marketing-hub/frontend:vega393-gate-v12-ui . > "$output/admin-image.log" 2>&1
docker build -f meta-ad-approver-worker/Dockerfile -t marketing-hub/meta-ad-approver-worker:vega393-gate-v12 meta-ad-approver-worker > "$output/temis.log" 2>&1
docker build -f customer-agent-worker/Dockerfile -t marketing-hub/customer-agent-worker:vega393-gate-v12 customer-agent-worker > "$output/psique.log" 2>&1
docker build -f pde-platform/frontend/Dockerfile.vega-private -t marketing-hub/pde-platform-frontend-vega:vega393-gate-v12 pde-platform/frontend > "$output/vega.log" 2>&1
printf 'Imagens finais construídas. A publicação exige a matriz local completa.\n'
