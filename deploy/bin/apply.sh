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

log() {
  printf '[%s] [apply.sh] %s\n' "$(date -Is)" "$*"
}

run_with_heartbeat() {
  local description="$1"
  shift

  log "Iniciando: ${description}"
  "$@" &
  local pid=$!
  local status=0

  while kill -0 "${pid}" >/dev/null 2>&1; do
    sleep 30
    if kill -0 "${pid}" >/dev/null 2>&1; then
      log "Ainda executando: ${description} (pid=${pid})"
    fi
  done

  set +e
  wait "${pid}"
  status=$?
  set -e

  if [[ ${status} -eq 0 ]]; then
    log "Finalizado: ${description}"
  else
    log "Falhou: ${description} (status=${status})"
  fi

  return "${status}"
}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

log "Preparando diretórios persistentes em ${DEPLOY_DIR}"
mkdir -p volumes/backend/uploads volumes/backend/logs

if [[ -f "${BACKEND_TAR}" ]]; then
  run_with_heartbeat "docker load backend (${BACKEND_TAR})" docker load -i "${BACKEND_TAR}"
else
  log "Arquivo de imagem backend não encontrado em ${BACKEND_TAR}; mantendo imagem local atual."
fi

if [[ -f "${FRONTEND_TAR}" ]]; then
  run_with_heartbeat "docker load frontend (${FRONTEND_TAR})" docker load -i "${FRONTEND_TAR}"
else
  log "Arquivo de imagem frontend não encontrado em ${FRONTEND_TAR}; mantendo imagem local atual."
fi

if [[ -f "${VIDEO_TAR}" ]]; then
  run_with_heartbeat "docker load video-management (${VIDEO_TAR})" docker load -i "${VIDEO_TAR}"
else
  log "Arquivo de imagem video-management não encontrado em ${VIDEO_TAR}; mantendo imagem local atual."
fi

tag_image_if_exists() {
  local source_image="$1"
  local target_image="$2"

  if docker image inspect "${source_image}" >/dev/null 2>&1; then
    log "Aplicando tag ${target_image} a partir de ${source_image}"
    docker tag "${source_image}" "${target_image}"
  else
    log "Aviso: imagem ${source_image} não encontrada; mantendo ${target_image} como está."
  fi
}

cleanup_previous_tags() {
  local repository="$1"
  local keep_tag="$2"

  log "Removendo tags antigas de ${repository}, preservando ${keep_tag}"
  docker image ls "${repository}" --format '{{.Repository}}:{{.Tag}}' \
    | awk -F':' -v keep_tag="${keep_tag}" '$2 != "<none>" && $2 != keep_tag {print $0}' \
    | xargs -r docker image rm >/dev/null 2>&1 || true
}

remove_conflicting_container() {
  local service_name="$1"
  local container_name="$2"
  local container_id
  local compose_project
  local compose_service
  local expected_project

  container_id="$(docker ps -aq --filter "name=^/${container_name}$" | head -n 1)"
  if [[ -z "${container_id}" ]]; then
    return 0
  fi

  compose_project="$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "${container_id}" 2>/dev/null || true)"
  compose_service="$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.service" }}' "${container_id}" 2>/dev/null || true)"
  expected_project="$(basename "${DEPLOY_DIR}")"

  if [[ "${compose_project}" == "${expected_project}" && "${compose_service}" == "${service_name}" ]]; then
    log "Container ${container_name} pertence ao compose atual; docker compose fará a recriação quando necessário."
    return 0
  fi

  log "Removendo container conflitante ${container_name} antes do compose up (id=${container_id}, composeProject=${compose_project:-none}, composeService=${compose_service:-none})"
  docker rm -f "${container_id}" >/dev/null
}

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  tag_image_if_exists "${BACKEND_IMAGE}:${IMAGE_TAG}" "${BACKEND_IMAGE}:latest"
  tag_image_if_exists "${FRONTEND_IMAGE}:${IMAGE_TAG}" "${FRONTEND_IMAGE}:latest"
  tag_image_if_exists "${VIDEO_IMAGE}:${IMAGE_TAG}" "${VIDEO_IMAGE}:latest"
fi

remove_conflicting_container "backend" "marketinghub-backend"
remove_conflicting_container "frontend" "marketinghub-frontend"

run_with_heartbeat \
  "recriar somente backend/frontend" \
  env BACKEND_IMAGE_TAG=latest FRONTEND_IMAGE_TAG=latest VIDEO_MGMT_IMAGE_TAG=latest \
  docker compose up -d --force-recreate --remove-orphans backend frontend

cleanup_previous_tags "${BACKEND_IMAGE}" "latest"
cleanup_previous_tags "${FRONTEND_IMAGE}" "latest"
cleanup_previous_tags "${VIDEO_IMAGE}" "latest"

log "Executando docker image prune"
docker image prune -f >/dev/null 2>&1 || true

log "Removendo arquivos temporários de imagem"
rm -f "${BACKEND_TAR}" "${FRONTEND_TAR}" "${VIDEO_TAR}" >/dev/null 2>&1 || true

log "Deploy backend/frontend concluído"
