#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
VIDEO_TAR=${VIDEO_TAR:-/tmp/video-management-image.tar}
VIDEO_IMAGE=${VIDEO_IMAGE:-marketinghub-video-management}
IMAGE_TAG=${IMAGE_TAG:-latest}
VIDEO_BACKEND_BASE_URL=${VIDEO_BACKEND_BASE_URL:-http://191.252.181.168}
IMAGE_TAR_LOADED=false

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

if [[ -f "${VIDEO_TAR}" ]]; then
  docker load -i "${VIDEO_TAR}"
  IMAGE_TAR_LOADED=true
fi

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  if docker image inspect "${VIDEO_IMAGE}:${IMAGE_TAG}" >/dev/null 2>&1; then
    docker tag "${VIDEO_IMAGE}:${IMAGE_TAG}" "${VIDEO_IMAGE}:latest"
  elif [[ "${IMAGE_TAR_LOADED}" == "true" ]]; then
    echo "[apply-video-only.sh] Erro: imagem esperada ${VIDEO_IMAGE}:${IMAGE_TAG} não encontrada após docker load; abortando para evitar deploy com imagem antiga." >&2
    exit 1
  else
    echo "[apply-video-only.sh] Aviso: imagem ${VIDEO_IMAGE}:${IMAGE_TAG} não encontrada; mantendo latest atual." >&2
  fi
fi

VIDEO_MGMT_IMAGE="${VIDEO_IMAGE}"
VIDEO_MGMT_IMAGE_TAG="latest"

if ! docker image inspect "${VIDEO_MGMT_IMAGE}:${VIDEO_MGMT_IMAGE_TAG}" >/dev/null 2>&1; then
  current_image="$(docker inspect marketinghub-video-management --format '{{.Config.Image}}' 2>/dev/null || true)"
  if [[ -n "${current_image}" && "${current_image}" == *:* ]]; then
    VIDEO_MGMT_IMAGE="${current_image%:*}"
    VIDEO_MGMT_IMAGE_TAG="${current_image##*:}"
    current_image_id="$(docker inspect marketinghub-video-management --format '{{.Image}}' 2>/dev/null || true)"
    if [[ -n "${current_image_id}" ]] && ! docker image inspect "${VIDEO_MGMT_IMAGE}:${VIDEO_MGMT_IMAGE_TAG}" >/dev/null 2>&1; then
      docker tag "${current_image_id}" "${VIDEO_MGMT_IMAGE}:${VIDEO_MGMT_IMAGE_TAG}"
    fi
    echo "[apply-video-only.sh] Imagem latest local ausente; reaplicando descritores com imagem atual ${VIDEO_MGMT_IMAGE}:${VIDEO_MGMT_IMAGE_TAG}."
  else
    echo "[apply-video-only.sh] Erro: nenhuma imagem local ${VIDEO_IMAGE}:latest ou container existente foi encontrada. Gere ou envie o artefato antes de publicar o video-management." >&2
    exit 1
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
VIDEO_MGMT_IMAGE="${VIDEO_MGMT_IMAGE}" \
VIDEO_MGMT_IMAGE_TAG="${VIDEO_MGMT_IMAGE_TAG}" \
VIDEO_BACKEND_BASE_URL="${VIDEO_BACKEND_BASE_URL}" \
docker compose -f docker-compose.video.yml up -d --no-deps video-management

cleanup_previous_tags "${VIDEO_MGMT_IMAGE}" "${VIDEO_MGMT_IMAGE_TAG}"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${VIDEO_TAR}" >/dev/null 2>&1 || true
