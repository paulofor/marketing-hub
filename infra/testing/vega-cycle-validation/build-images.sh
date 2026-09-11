#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
mkdir -p artifacts/vega386/build
# Imagens finais destinadas à publicação autorizada, sempre pelos Dockerfiles do repositório.
node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml -DskipTests package
docker build -f backend/ads-service/Dockerfile -t marketing-hub/backend:vega386-v11 . > artifacts/vega386/build/backend.log 2>&1
docker build -f customer-agent-worker/Dockerfile -t marketing-hub/customer-agent-worker:vega386-v11 customer-agent-worker > artifacts/vega386/build/psique.log 2>&1
docker build -f landing-generator-agent-worker/Dockerfile -t marketing-hub/landing-generator-agent-worker:vega386-v11 landing-generator-agent-worker > artifacts/vega386/build/dedalo.log 2>&1
docker build -f pde-platform/frontend/Dockerfile.vega-private -t marketing-hub/pde-platform-frontend-vega:vega386-v11 pde-platform/frontend > artifacts/vega386/build/vega.log 2>&1
printf 'Quatro imagens construídas pelo repositório. Publicação depende da matriz local completa.\n'
