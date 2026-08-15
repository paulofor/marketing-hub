#!/usr/bin/env bash
set -euo pipefail

APPLY_SCRIPT="${1:-deploy/bin/apply.sh}"
WORKFLOW_FILE="${2:-.github/workflows/deploy-containers.yml}"
BACKEND_DOCKERFILE="${3:-backend/ads-service/Dockerfile}"
FRONTEND_DOCKERFILE="${4:-frontend/Dockerfile}"

bash -n "${APPLY_SCRIPT}"

require_contract() {
  local pattern="$1"
  local description="$2"

  if ! grep -Fq "${pattern}" "${APPLY_SCRIPT}"; then
    printf '[ARQUITETURA] apply.sh não garante %s.\n' "${description}" >&2
    exit 1
  fi
}

require_contract 'BACKEND_HEALTH_ATTEMPTS=${BACKEND_HEALTH_ATTEMPTS:-60}' 'janela de saúde independente para o backend'
require_contract 'preserve_current_image "${BACKEND_IMAGE}:latest" "${BACKEND_IMAGE}:rollback"' 'preservação da imagem backend anterior'
require_contract 'rollback_app_stack || true' 'rollback quando a aplicação da nova versão falha'
require_contract 'wait_http "backend restaurado"' 'validação de saúde após o rollback'

if ! grep -A4 '^concurrency:' "${WORKFLOW_FILE}" | grep -Fq 'cancel-in-progress: false'; then
  printf '[ARQUITETURA] workflow pode cancelar um deploy válido por causa de push posterior sem mudança operacional.\n' >&2
  exit 1
fi

if [[ "$(grep -Fc 'mvn -B -q test' "${WORKFLOW_FILE}")" -ne 1 ]]; then
  printf '[ARQUITETURA] workflow deve executar a suíte completa do backend exatamente uma vez.\n' >&2
  exit 1
fi

if grep -Eq 'mvn .*package|npm run build' "${BACKEND_DOCKERFILE}" "${FRONTEND_DOCKERFILE}"; then
  printf '[ARQUITETURA] Dockerfiles de runtime não podem recompilar artefatos já validados pelo workflow.\n' >&2
  exit 1
fi

if ! grep -Fq "if: needs.detect-changes.outputs.backend == 'true'" "${WORKFLOW_FILE}" \
  || ! grep -Fq "if: needs.detect-changes.outputs.frontend == 'true'" "${WORKFLOW_FILE}"; then
  printf '[ARQUITETURA] imagens de backend e frontend devem respeitar detecção independente de módulos.\n' >&2
  exit 1
fi

if ! grep -Fq '.deployed-app-revision' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'scripts/detect-deployment-changes.sh' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'Mark successful APP revision' "${WORKFLOW_FILE}" \
  || ! grep -Fq 'abortando para não perder módulos pendentes' "${WORKFLOW_FILE}"; then
  printf '[ARQUITETURA] workflow deve detectar mudanças desde a última revisão APP realmente publicada.\n' >&2
  exit 1
fi

printf 'Contrato de deploy transacional validado.\n'
