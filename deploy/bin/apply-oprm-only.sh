#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
OPRM_TAR=${OPRM_TAR:-/tmp/oprm-image.tar}
OPRM_IMAGE=${OPRM_IMAGE:-marketinghub-oprm}
IMAGE_TAG=${IMAGE_TAG:-2026.04.15}
OPRM_BACKEND_BASE_URL=${OPRM_BACKEND_BASE_URL:-http://191.252.181.168:8000}
OPRM_SPRING_PROFILE=${OPRM_SPRING_PROFILE:-default}
OPRM_JOBS_POLLING_ENABLED=${OPRM_JOBS_POLLING_ENABLED:-false}
OPRM_JOBS_POLL_INTERVAL=${OPRM_JOBS_POLL_INTERVAL:-PT5M}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

if [[ -f "${OPRM_TAR}" ]]; then
  docker load -i "${OPRM_TAR}"
fi

if ! docker image inspect "${OPRM_IMAGE}:${IMAGE_TAG}" >/dev/null 2>&1; then
  echo "[apply-oprm-only.sh] Erro: imagem ${OPRM_IMAGE}:${IMAGE_TAG} não encontrada." >&2
  exit 1
fi

cleanup_previous_tags() {
  local repository="$1"
  local keep_tag="$2"

  docker image ls "${repository}" --format '{{.Repository}}:{{.Tag}}' \
    | awk -F':' -v keep_tag="${keep_tag}" '$2 != "<none>" && $2 != keep_tag {print $0}' \
    | xargs -r docker image rm >/dev/null 2>&1 || true
}

# Atualiza somente o OPRM sem reiniciar backend/frontend.
OPRM_BACKEND_BASE_URL="${OPRM_BACKEND_BASE_URL}" \
OPRM_SPRING_PROFILE="${OPRM_SPRING_PROFILE}" \
OPRM_JOBS_POLLING_ENABLED="${OPRM_JOBS_POLLING_ENABLED}" \
OPRM_JOBS_POLL_INTERVAL="${OPRM_JOBS_POLL_INTERVAL}" \
OPRM_IMAGE="${OPRM_IMAGE}" \
OPRM_IMAGE_TAG="${IMAGE_TAG}" \
  docker compose up -d --no-deps oprm-worker

cleanup_previous_tags "${OPRM_IMAGE}" "${IMAGE_TAG}"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${OPRM_TAR}" >/dev/null 2>&1 || true
