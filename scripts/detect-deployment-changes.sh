#!/usr/bin/env bash
set -euo pipefail

BASE_REVISION="${1:?Informe a revisão base efetivamente publicada.}"
HEAD_REVISION="${2:-HEAD}"
OUTPUT_FILE="${3:-${GITHUB_OUTPUT:-}}"
FRONTEND_BASE_REVISION="${4:-${BASE_REVISION}}"

if ! git cat-file -e "${BASE_REVISION}^{commit}" 2>/dev/null; then
  printf '[ARQUITETURA] revisão publicada inválida ou ausente no histórico: %s\n' "${BASE_REVISION}" >&2
  exit 1
fi

if ! git cat-file -e "${HEAD_REVISION}^{commit}" 2>/dev/null; then
  printf '[ARQUITETURA] revisão atual inválida ou ausente no histórico: %s\n' "${HEAD_REVISION}" >&2
  exit 1
fi

changed_files="$(git diff --name-only "${BASE_REVISION}" "${HEAD_REVISION}")"
printf 'Arquivos alterados desde o ultimo deploy confirmado (%s):\n%s\n' "${BASE_REVISION}" "${changed_files}"

frontend_revision_known=true
if ! git cat-file -e "${FRONTEND_BASE_REVISION}^{commit}" 2>/dev/null; then
  frontend_revision_known=false
  printf 'Revisão publicada do frontend indisponível (%s); a imagem será reconstruída por segurança.\n' \
    "${FRONTEND_BASE_REVISION}" >&2
fi

frontend_changed_files=""
if [[ "${frontend_revision_known}" == "true" ]]; then
  frontend_changed_files="$(git diff --name-only "${FRONTEND_BASE_REVISION}" "${HEAD_REVISION}")"
  printf 'Arquivos alterados desde a revisão observada do frontend (%s):\n%s\n' \
    "${FRONTEND_BASE_REVISION}" "${frontend_changed_files}"
fi

backend=false
frontend=false
video=false
app_deploy_descriptor=false
app_deploy_sync=false
video_deploy_descriptor=false

while IFS= read -r file; do
  [[ -z "${file}" ]] && continue
  case "${file}" in
    .github/workflows/deploy-containers.yml) backend=true; frontend=true; video=true; app_deploy_descriptor=true; video_deploy_descriptor=true ;;
    .dockerignore) backend=true; frontend=true; video=true ;;
    backend/settings.xml) backend=true ;;
    backend/ads-service/*) backend=true ;;
    frontend/*) frontend=true ;;
    video-management-service/*) video=true ;;
    deploy/bin/apply-video-only.sh) video=true; video_deploy_descriptor=true ;;
    deploy/bin/*) app_deploy_sync=true ;;
    deploy/docker-compose.yml) app_deploy_descriptor=true ;;
    deploy/nginx/*) app_deploy_descriptor=true ;;
  esac
done <<< "${changed_files}"

if [[ "${frontend_revision_known}" != "true" ]]; then
  frontend=true
else
  while IFS= read -r file; do
    [[ -z "${file}" ]] && continue
    case "${file}" in
      .github/workflows/deploy-containers.yml|.dockerignore|frontend/*) frontend=true ;;
    esac
  done <<< "${frontend_changed_files}"
fi

app_deploy=false
app_image=false
video_deploy=false

if [[ "${backend}" == "true" || "${frontend}" == "true" ]]; then
  app_image=true
fi

if [[ "${backend}" == "true" || "${frontend}" == "true" || "${app_deploy_descriptor}" == "true" ]]; then
  app_deploy=true
fi

if [[ "${video}" == "true" || "${video_deploy_descriptor}" == "true" ]]; then
  video_deploy=true
fi

result="$(printf '%s\n' \
  "backend=${backend}" \
  "frontend=${frontend}" \
  "video=${video}" \
  "app_image=${app_image}" \
  "app_deploy=${app_deploy}" \
  "app_deploy_sync=${app_deploy_sync}" \
  "video_deploy=${video_deploy}")"

if [[ -n "${OUTPUT_FILE}" ]]; then
  printf '%s\n' "${result}" >> "${OUTPUT_FILE}"
else
  printf '%s\n' "${result}"
fi
