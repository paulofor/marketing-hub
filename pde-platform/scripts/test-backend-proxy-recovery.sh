#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${EVIDENCE_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo}"
: "${AIHUB_HOMOLOGATION_SESSION:?Execute pelo wrapper de homologação Docker}"
[[ "$EVIDENCE_COMPOSE_PROJECT" =~ ^aihub-[a-z0-9-]+$ ]] || exit 2
fixture=pde-platform/scripts/fixtures/backend-proxy
export PROXY_BACKEND_IMAGE="aihub-homologation/$AIHUB_HOMOLOGATION_SESSION/proxy-backend:latest"
export PROXY_PUBLIC_IMAGE="aihub-homologation/$AIHUB_HOMOLOGATION_SESSION/proxy-public:latest"
export PROXY_MIRA_IMAGE="aihub-homologation/$AIHUB_HOMOLOGATION_SESSION/proxy-mira:latest"
export PROXY_LEGACY_IMAGE="aihub-homologation/$AIHUB_HOMOLOGATION_SESSION/proxy-legacy:latest"
compose=(docker compose -p "$EVIDENCE_COMPOSE_PROJECT" -f "$fixture/compose.yml")
trap '"${compose[@]}" down --volumes --remove-orphans' EXIT
bash scripts/docker-build-temporary-image.sh proxy-backend -f "$fixture/Dockerfile.backend" .
bash scripts/docker-build-temporary-image.sh proxy-public -f "$fixture/Dockerfile.proxy" .
bash scripts/docker-build-temporary-image.sh proxy-mira -f "$fixture/Dockerfile.proxy" \
  --build-arg NGINX_TEMPLATE=pde-platform/frontend/nginx.mira.conf .
bash scripts/docker-build-temporary-image.sh proxy-legacy -f "$fixture/Dockerfile.proxy" \
  --build-arg "NGINX_TEMPLATE=$fixture/legacy.conf" .
"${compose[@]}" up -d old-backend new-backend
"${compose[@]}" up -d pde-platform-frontend-v7 pde-platform-frontend-mira pde-platform-frontend-v5 unrelated
node "$fixture/verify.mjs"
