#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${DEPLOY_DIR}"

unset LEAD_PORTAL_PAYMENTS_AUTH_TOKEN

docker compose -f docker-compose.video.yml config --quiet
MYSQL_PASS=contract-test docker compose -f docker-compose.mcp.yml config --quiet

if grep -q 'docker compose up' bin/apply-video-only.sh; then
  echo "[CONTRATO] apply-video-only.sh deve informar explicitamente docker-compose.video.yml." >&2
  exit 1
fi

if grep -q 'docker compose up' bin/apply-mcp-only.sh; then
  echo "[CONTRATO] apply-mcp-only.sh deve informar explicitamente docker-compose.mcp.yml." >&2
  exit 1
fi

grep -q 'docker compose -f docker-compose.video.yml up' bin/apply-video-only.sh
grep -q 'docker compose -f docker-compose.mcp.yml up' bin/apply-mcp-only.sh

# Alterar o publicador isolado exige reconstruir a imagem para que um deploy
# recuperado não termine verde mantendo código antigo no container de vídeo.
grep -Fq 'deploy/bin/apply-video-only.sh) video=true; video_deploy_descriptor=true ;;' \
  ../.github/workflows/deploy-containers.yml

# Alterar o próprio workflow pode modificar qualquer contrato de build/deploy.
# Todos os artefatos devem ser reconstruídos para validar a revisão publicada.
grep -Fq '.github/workflows/deploy-containers.yml) backend=true; frontend=true; video=true; app_deploy_descriptor=true; video_deploy_descriptor=true ;;' \
  ../.github/workflows/deploy-containers.yml
