#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPOSITORY_ROOT="$(cd "${MODULE_ROOT}/.." && pwd)"
DOCKERFILE="${MODULE_ROOT}/Dockerfile"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.deploy.yml"
WORKFLOW_FILE="${REPOSITORY_ROOT}/.github/workflows/harness-library-api-ci.yml"

fail() {
  printf '[ARQUITETURA] %s\n' "$1" >&2
  exit 1
}

grep -Fq 'USER app' "${DOCKERFILE}" \
  || fail 'a imagem da Biblioteca deve declarar usuário não privilegiado.'
grep -Fq '127.0.0.1:${HARNESS_LIBRARY_API_PORT:-8103}:8103' "${COMPOSE_FILE}" \
  || fail 'a API deve permanecer publicada somente em loopback antes do domínio TLS.'
if grep -Eq '(^|[[:space:]-])9103:9103([[:space:]]|$)' "${COMPOSE_FILE}"; then
  fail 'a porta de métricas não pode ser publicada pelo Compose de produção.'
fi
grep -Fq 'HARNESS_LIBRARY_API_KEY_FILE: /run/secrets/harness_library_api_key' "${COMPOSE_FILE}" \
  || fail 'a chave pública deve entrar no container somente por arquivo montado.'
grep -Fq 'HARNESS_LIBRARY_INTERNAL_SIGNING_KEY_FILE: /run/secrets/harness_library_internal_signing_key' "${COMPOSE_FILE}" \
  || fail 'a chave HMAC deve entrar no container somente por arquivo montado.'
grep -Fq 'ghcr.io/${{ github.repository }}/harness-library-api:sha-${{ github.sha }}' "${WORKFLOW_FILE}" \
  || fail 'publicação e deploy devem usar imagem imutável identificada pelo commit.'
grep -Fq "if: github.ref == 'refs/heads/main' && github.event_name != 'pull_request'" "${WORKFLOW_FILE}" \
  || fail 'somente main pode publicar a imagem no registry.'
grep -Fq "if: github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch' && inputs.deploy == true" "${WORKFLOW_FILE}" \
  || fail 'deploy deve exigir main e comando manual explícito.'
grep -Fq 'group: deploy-harness-library-api-163-245-200-7' "${WORKFLOW_FILE}" \
  || fail 'deploy deve ser serializado no host selecionado.'
deploy_queue="$(grep -F -A2 'group: deploy-harness-library-api-163-245-200-7' "${WORKFLOW_FILE}" || true)"
grep -Fq 'queue: max' <<<"${deploy_queue}" \
  || fail 'deploy deve preservar execuções pendentes na fila ampliada.'
grep -Fq 'cancel-in-progress: false' <<<"${deploy_queue}" \
  || fail 'deploy não pode cancelar uma publicação ativa.'

printf 'Contrato da imagem e do deploy da Biblioteca do Harness validado.\n'
