#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
BACKEND_TAR=${BACKEND_TAR:-/tmp/backend-image.tar}
FRONTEND_TAR=${FRONTEND_TAR:-/tmp/frontend-image.tar}
BACKEND_IMAGE=${BACKEND_IMAGE:-marketinghub-backend}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-marketinghub-frontend}
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

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  docker tag "${BACKEND_IMAGE}:${IMAGE_TAG}" "${BACKEND_IMAGE}:latest"
  docker tag "${FRONTEND_IMAGE}:${IMAGE_TAG}" "${FRONTEND_IMAGE}:latest"
fi

# Stop old containers (ignore errors if they are not running yet)
docker compose down --remove-orphans || true

# Start the updated stack
docker compose up -d

docker image prune -f >/dev/null 2>&1 || true
rm -f "${BACKEND_TAR}" "${FRONTEND_TAR}" >/dev/null 2>&1 || true
