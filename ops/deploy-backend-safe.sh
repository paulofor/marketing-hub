#!/usr/bin/env bash
set -euo pipefail

IMAGE_TAG=${IMAGE_TAG:-$(date -u +%Y%m%d%H%M%S)}
BACKEND_IMAGE=${BACKEND_IMAGE:-marketinghub-backend}
BACKEND_TAR=${BACKEND_TAR:-/tmp/backend-image.tar}
REMOTE_DEPLOY_DIR=${REMOTE_DEPLOY_DIR:-/opt/marketinghub/containers}
BACKEND_PUBLIC_BASE_URL=${BACKEND_PUBLIC_BASE_URL:-http://191.252.181.168}
VALIDATION_PATH=${VALIDATION_PATH:-/api/products/public/metodo-musa-7-dias/marketing-definition}
VALIDATION_EXPECTED=${VALIDATION_EXPECTED:-Jornada de 7 dias}
PDE_PUBLIC_BASE_URL=${PDE_PUBLIC_BASE_URL:-https://clubemusa.com.br}
CONFIRM_DEPLOY=${CONFIRM_DEPLOY:-}
ALLOW_DIRTY_WORKTREE=${ALLOW_DIRTY_WORKTREE:-false}

log() {
  printf '[%s] [deploy-backend-safe] %s\n' "$(date -Is)" "$*"
}

fail() {
  printf '[%s] [deploy-backend-safe] ERRO: %s\n' "$(date -Is)" "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "comando obrigatório não encontrado: $1"
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

resolve_ssh_target() {
  if [[ -n "${DEPLOY_SSH_TARGET:-}" ]]; then
    printf '%s' "${DEPLOY_SSH_TARGET}"
    return
  fi

  [[ -n "${DEPLOY_HOST:-}" ]] || fail "defina DEPLOY_HOST ou DEPLOY_SSH_TARGET"
  if [[ -n "${DEPLOY_USER:-}" ]]; then
    printf '%s@%s' "${DEPLOY_USER}" "${DEPLOY_HOST}"
  else
    printf '%s' "${DEPLOY_HOST}"
  fi
}

validate_worktree() {
  if [[ "${ALLOW_DIRTY_WORKTREE}" == "true" ]]; then
    log "Worktree suja permitida por ALLOW_DIRTY_WORKTREE=true"
    return
  fi

  if [[ -n "$(git status --porcelain)" ]]; then
    fail "worktree possui alterações não commitadas; use ALLOW_DIRTY_WORKTREE=true apenas se esse for o estado que deve ir para deploy"
  fi
}

validate_liquibase_with_maven() {
  local offline_url='offline:mysql?version=5.7'

  log "Validando Liquibase via Maven em modo offline MySQL 5.7"
  run_with_heartbeat \
    "mvn liquibase:validate" \
    mvn -q -f backend/ads-service/pom.xml \
      "-Dliquibase.url=${offline_url}" \
      -Dliquibase.changeLogFile=src/main/resources/db/changelog/db.changelog-master.yaml \
      liquibase:validate
}

remote_apply_backend() {
  local ssh_target="$1"

  log "Criando diretório remoto ${REMOTE_DEPLOY_DIR}"
  ssh "${ssh_target}" "mkdir -p '${REMOTE_DEPLOY_DIR}/bin'"

  log "Enviando imagem e artefatos de deploy para ${ssh_target}"
  scp "${BACKEND_TAR}" "${ssh_target}:/tmp/backend-image.tar"
  scp deploy/docker-compose.yml "${ssh_target}:${REMOTE_DEPLOY_DIR}/docker-compose.yml"
  scp deploy/bin/apply.sh "${ssh_target}:${REMOTE_DEPLOY_DIR}/bin/apply.sh"

  log "Aplicando backend no host remoto"
  ssh "${ssh_target}" \
    "cd '${REMOTE_DEPLOY_DIR}' && chmod +x bin/apply.sh && IMAGE_TAG='${IMAGE_TAG}' BACKEND_TAR=/tmp/backend-image.tar ./bin/apply.sh"
}

validate_remote_endpoint() {
  local url="${BACKEND_PUBLIC_BASE_URL%/}${VALIDATION_PATH}"
  local response_file
  response_file="$(mktemp)"

  log "Validando health remoto"
  curl --fail --silent --show-error --max-time 20 \
    "${BACKEND_PUBLIC_BASE_URL%/}/ops-mh-observability-v2/health" >/dev/null

  log "Validando endpoint de negócio: ${url}"
  curl --fail --silent --show-error --max-time 30 "${url}" >"${response_file}"

  if ! grep -Fq "${VALIDATION_EXPECTED}" "${response_file}"; then
    rm -f "${response_file}"
    fail "endpoint respondeu, mas não contém o texto esperado: ${VALIDATION_EXPECTED}"
  fi

  rm -f "${response_file}"
  log "Endpoint validado com sucesso"

  log "Validando consistência pública PDE MUSA"
  BACKEND_PUBLIC_BASE_URL="${BACKEND_PUBLIC_BASE_URL}" \
    PDE_PUBLIC_BASE_URL="${PDE_PUBLIC_BASE_URL}" \
    PRODUCT_SLUG="metodo-musa-7-dias" \
    scripts/check-musa-pde-public-consistency.sh
}

main() {
  [[ "${CONFIRM_DEPLOY}" == "deploy-backend" ]] || fail "confirme a publicação com CONFIRM_DEPLOY=deploy-backend"

  require_command git
  require_command mvn
  require_command docker
  require_command ssh
  require_command scp
  require_command curl
  require_command python3

  local ssh_target
  ssh_target="$(resolve_ssh_target)"

  log "Alternativas avaliadas:"
  log "1) aplicar Liquibase manual remoto: menor esforço, maior risco de schema errado e sem validação comercial"
  log "2) CI/CD completo: mais robusto, maior custo e depende de credenciais/workflow"
  log "3) script versionado com travas: melhor equilíbrio para publicar rápido com segurança operacional"
  log "Escolha aplicada: script versionado com travas e validação pós-deploy"

  validate_worktree
  ./scripts/validate-liquibase-mysql57.sh
  validate_liquibase_with_maven

  log "Buildando imagem backend ${BACKEND_IMAGE}:${IMAGE_TAG}"
  run_with_heartbeat \
    "docker build backend" \
    docker build -f backend/ads-service/Dockerfile -t "${BACKEND_IMAGE}:${IMAGE_TAG}" .

  log "Exportando imagem para ${BACKEND_TAR}"
  docker save "${BACKEND_IMAGE}:${IMAGE_TAG}" -o "${BACKEND_TAR}"

  remote_apply_backend "${ssh_target}"
  validate_remote_endpoint

  log "Deploy seguro do backend concluído"
}

main "$@"
