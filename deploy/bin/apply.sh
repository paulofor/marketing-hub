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
IMAGE_LOAD_TIMEOUT=${IMAGE_LOAD_TIMEOUT:-12m}
COMMAND_TIMEOUT_KILL_AFTER=${COMMAND_TIMEOUT_KILL_AFTER:-30s}
COMPOSE_RECREATE_TIMEOUT=${COMPOSE_RECREATE_TIMEOUT:-8m}
DIAGNOSTIC_COMMAND_TIMEOUT=${DIAGNOSTIC_COMMAND_TIMEOUT:-20s}
BACKEND_HEALTH_URL=${BACKEND_HEALTH_URL:-http://localhost:8000/ops-mh-observability-v2/health}
FRONTEND_HEALTH_URL=${FRONTEND_HEALTH_URL:-http://localhost:5173/healthz}

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

dump_app_diagnostics() {
  log "Diagnóstico de disco"
  timeout -k 5s "${DIAGNOSTIC_COMMAND_TIMEOUT}" df -h "${DEPLOY_DIR}" /tmp || true

  log "Diagnóstico docker compose ps"
  timeout -k 5s "${DIAGNOSTIC_COMMAND_TIMEOUT}" docker compose ps backend frontend || true

  log "Diagnóstico docker inspect backend"
  timeout -k 5s "${DIAGNOSTIC_COMMAND_TIMEOUT}" docker inspect marketinghub-backend \
    --format 'status={{.State.Status}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}} started={{.State.StartedAt}} finished={{.State.FinishedAt}} image={{.Config.Image}}' 2>&1 || true

  log "Últimas linhas do backend"
  timeout -k 5s "${DIAGNOSTIC_COMMAND_TIMEOUT}" docker logs --tail 120 marketinghub-backend 2>&1 || true

  log "Últimas linhas do frontend"
  timeout -k 5s "${DIAGNOSTIC_COMMAND_TIMEOUT}" docker logs --tail 80 marketinghub-frontend 2>&1 || true
}

run_with_timeout_and_diagnostics() {
  local description="$1"
  local duration="$2"
  shift 2

  if ! run_with_heartbeat "${description}" timeout -k "${COMMAND_TIMEOUT_KILL_AFTER}" "${duration}" "$@"; then
    log "Comando falhou ou excedeu o limite ${duration}: ${description}"
    dump_app_diagnostics
    return 1
  fi
}

prepare_image_load() {
  local name="$1"
  local tar_path="$2"

  log "Preparando carga da imagem ${name}"
  ls -lh "${tar_path}" || true
  df -h "${DEPLOY_DIR}" /tmp || true
  docker image prune -f >/dev/null 2>&1 || true
}

wait_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-24}"
  local interval="${4:-5}"

  log "Validando ${name} em ${url}"
  for attempt in $(seq 1 "${attempts}"); do
    if curl -fsS --max-time 5 "${url}" >/dev/null; then
      log "${name} respondeu com sucesso"
      return 0
    fi

    log "Aguardando ${name} responder (${attempt}/${attempts})"
    sleep "${interval}"
  done

  log "${name} não respondeu em ${url}"
  dump_app_diagnostics
  return 1
}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"

log "Preparando diretórios persistentes em ${DEPLOY_DIR}"
mkdir -p volumes/backend/uploads volumes/backend/logs

if [[ -f "${BACKEND_TAR}" ]]; then
  prepare_image_load "backend" "${BACKEND_TAR}"
  run_with_timeout_and_diagnostics "docker load backend (${BACKEND_TAR})" "${IMAGE_LOAD_TIMEOUT}" docker load -i "${BACKEND_TAR}"
else
  log "Arquivo de imagem backend não encontrado em ${BACKEND_TAR}; mantendo imagem local atual."
fi

if [[ -f "${FRONTEND_TAR}" ]]; then
  prepare_image_load "frontend" "${FRONTEND_TAR}"
  run_with_timeout_and_diagnostics "docker load frontend (${FRONTEND_TAR})" "${IMAGE_LOAD_TIMEOUT}" docker load -i "${FRONTEND_TAR}"
else
  log "Arquivo de imagem frontend não encontrado em ${FRONTEND_TAR}; mantendo imagem local atual."
fi

if [[ -f "${VIDEO_TAR}" ]]; then
  prepare_image_load "video-management" "${VIDEO_TAR}"
  run_with_timeout_and_diagnostics "docker load video-management (${VIDEO_TAR})" "${IMAGE_LOAD_TIMEOUT}" docker load -i "${VIDEO_TAR}"
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

run_with_timeout_and_diagnostics \
  "recriar somente backend/frontend" \
  "${COMPOSE_RECREATE_TIMEOUT}" \
  env BACKEND_IMAGE_TAG=latest FRONTEND_IMAGE_TAG=latest VIDEO_MGMT_IMAGE_TAG=latest \
  docker compose up -d --force-recreate --remove-orphans backend frontend

wait_http "backend" "${BACKEND_HEALTH_URL}"
wait_http "frontend" "${FRONTEND_HEALTH_URL}" 12 5

cleanup_previous_tags "${BACKEND_IMAGE}" "latest"
cleanup_previous_tags "${FRONTEND_IMAGE}" "latest"
cleanup_previous_tags "${VIDEO_IMAGE}" "latest"

log "Executando docker image prune"
docker image prune -f >/dev/null 2>&1 || true

log "Removendo arquivos temporários de imagem"
rm -f "${BACKEND_TAR}" "${FRONTEND_TAR}" "${VIDEO_TAR}" >/dev/null 2>&1 || true

log "Deploy backend/frontend concluído"
