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

cleanup_previous_tags() {
  local repository="$1"
  local keep_tag="$2"

  docker image ls "${repository}" --format '{{.Repository}}:{{.Tag}}' \
    | awk -F':' -v keep_tag="${keep_tag}" '$2 != "<none>" && $2 != keep_tag {print $0}' \
    | xargs -r docker image rm >/dev/null 2>&1 || true
}

# Atualiza somente o módulo de vídeo sem reiniciar backend/frontend
# (backend/frontend permanecem no host 191.252.181.168)
VIDEO_MGMT_IMAGE="${VIDEO_IMAGE}" \
VIDEO_MGMT_IMAGE_TAG=latest \
VIDEO_BACKEND_BASE_URL="${VIDEO_BACKEND_BASE_URL}" \
docker compose up -d --no-deps video-management

cleanup_previous_tags "${VIDEO_IMAGE}" "latest"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${VIDEO_TAR}" >/dev/null 2>&1 || true
