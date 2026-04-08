#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
BACKEND_TAR=${BACKEND_TAR:-/tmp/backend-image.tar}
FRONTEND_TAR=${FRONTEND_TAR:-/tmp/frontend-image.tar}
VIDEO_TAR=${VIDEO_TAR:-/tmp/video-management-image.tar}
BACKEND_IMAGE=${BACKEND_IMAGE:-marketinghub-backend}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-marketinghub-frontend}
VIDEO_IMAGE=${VIDEO_IMAGE:-marketinghub-video-management}
IMAGE_TAG=${IMAGE_TAG:-latest}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

# Ensure bind mount directories exist before docker compose runs
mkdir -p volumes/backend/uploads volumes/backend/logs

if [[ -f "${BACKEND_TAR}" ]]; then
  docker load -i "${BACKEND_TAR}"
fi

if [[ -f "${FRONTEND_TAR}" ]]; then
  docker load -i "${FRONTEND_TAR}"
fi

if [[ -f "${VIDEO_TAR}" ]]; then
  docker load -i "${VIDEO_TAR}"
fi

tag_image_if_exists() {
  local source_image="$1"
  local target_image="$2"

  if docker image inspect "${source_image}" >/dev/null 2>&1; then
    docker tag "${source_image}" "${target_image}"
  else
    echo "[apply.sh] Aviso: imagem ${source_image} não encontrada; mantendo ${target_image} como está." >&2
  fi
}

cleanup_previous_tags() {
  local repository="$1"
  local keep_tag="$2"

  docker image ls "${repository}" --format '{{.Repository}}:{{.Tag}}' \
    | awk -F':' -v keep_tag="${keep_tag}" '$2 != "<none>" && $2 != keep_tag {print $0}' \
    | xargs -r docker image rm >/dev/null 2>&1 || true
}

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  tag_image_if_exists "${BACKEND_IMAGE}:${IMAGE_TAG}" "${BACKEND_IMAGE}:latest"
  tag_image_if_exists "${FRONTEND_IMAGE}:${IMAGE_TAG}" "${FRONTEND_IMAGE}:latest"
  tag_image_if_exists "${VIDEO_IMAGE}:${IMAGE_TAG}" "${VIDEO_IMAGE}:latest"
fi

# Stop old containers (ignore errors if they are not running yet)
docker compose down --remove-orphans || true

# Start only backend/frontend stack on app host
docker compose up -d backend frontend

cleanup_previous_tags "${BACKEND_IMAGE}" "latest"
cleanup_previous_tags "${FRONTEND_IMAGE}" "latest"
cleanup_previous_tags "${VIDEO_IMAGE}" "latest"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${BACKEND_TAR}" "${FRONTEND_TAR}" "${VIDEO_TAR}" >/dev/null 2>&1 || true
