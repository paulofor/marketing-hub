#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
VIDEO_TAR=${VIDEO_TAR:-/tmp/video-management-image.tar}
VIDEO_IMAGE=${VIDEO_IMAGE:-marketinghub-video-management}
IMAGE_TAG=${IMAGE_TAG:-latest}
VIDEO_BACKEND_BASE_URL=${VIDEO_BACKEND_BASE_URL:-http://191.252.181.168:8000}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

if [[ -f "${VIDEO_TAR}" ]]; then
  docker load -i "${VIDEO_TAR}"
fi

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  if docker image inspect "${VIDEO_IMAGE}:${IMAGE_TAG}" >/dev/null 2>&1; then
    docker tag "${VIDEO_IMAGE}:${IMAGE_TAG}" "${VIDEO_IMAGE}:latest"
  else
    echo "[apply-video-only.sh] Aviso: imagem ${VIDEO_IMAGE}:${IMAGE_TAG} não encontrada; mantendo latest atual." >&2
  fi
fi

# Atualiza somente o módulo de vídeo sem reiniciar backend/frontend
# (backend/frontend permanecem no host 191.252.181.168)
VIDEO_BACKEND_BASE_URL="${VIDEO_BACKEND_BASE_URL}" docker compose up -d --no-deps video-management

docker image prune -f >/dev/null 2>&1 || true
rm -f "${VIDEO_TAR}" >/dev/null 2>&1 || true
