#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPOSITORY_ROOT="$(cd "${MODULE_ROOT}/.." && pwd)"
DOCKERFILE="${MODULE_ROOT}/Dockerfile"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.deploy.yml"
WORKFLOW_FILE="${REPOSITORY_ROOT}/.github/workflows/harness-library-api-ci.yml"
SECRET_RUNTIME_TEST="${MODULE_ROOT}/scripts/validate-secret-file-runtime.sh"

fail() {
  printf '[ARQUITETURA] %s\n' "$1" >&2
  exit 1
}

grep -Fq 'groupadd --gid 10001 app' "${DOCKERFILE}" \
  || fail 'a imagem da Biblioteca deve fixar o grupo não privilegiado usado pelos secrets.'
grep -Fq 'useradd --uid 10001 --gid 10001' "${DOCKERFILE}" \
  || fail 'a imagem da Biblioteca deve fixar o usuário não privilegiado usado pelos secrets.'
grep -Fq 'USER 10001:10001' "${DOCKERFILE}" \
  || fail 'a imagem da Biblioteca deve executar com a identidade fixa 10001:10001.'
grep -Fq 'user: "10001:10001"' "${COMPOSE_FILE}" \
  || fail 'o Compose deve preservar a identidade fixa da imagem.'
[[ -x "${SECRET_RUNTIME_TEST}" ]] \
  || fail 'o teste executável de leitura dos secrets no runtime está ausente.'
grep -Fq 'validate-secret-file-runtime.sh harness-library-api:test' "${WORKFLOW_FILE}" \
  || fail 'o CI deve iniciar a imagem com os mesmos arquivos protegidos da produção.'
grep -Fq '127.0.0.1:${HARNESS_LIBRARY_API_PORT:-8103}:8103' "${COMPOSE_FILE}" \
  || fail 'a API deve permanecer publicada somente em loopback antes do domínio TLS.'
if grep -Eq '(^|[[:space:]-])9103:9103([[:space:]]|$)' "${COMPOSE_FILE}"; then
  fail 'a porta de métricas não pode ser publicada pelo Compose de produção.'
fi
grep -Fq 'HARNESS_LIBRARY_API_KEY_FILE: /run/secrets/harness_library_api_key' "${COMPOSE_FILE}" \
  || fail 'a chave pública deve entrar no container somente por arquivo montado.'
grep -Fq 'HARNESS_LIBRARY_INTERNAL_SIGNING_KEY_FILE: /run/secrets/harness_library_internal_signing_key' "${COMPOSE_FILE}" \
  || fail 'a chave HMAC deve entrar no container somente por arquivo montado.'
for secret_file in api_key internal_signing_key; do
  grep -Fq "chown 10001:10001 /root/infra/harness-library/secrets/${secret_file}" "${WORKFLOW_FILE}" \
    || fail "o secret ${secret_file} deve pertencer ao usuário do runtime."
  grep -Fq "chmod 0400 /root/infra/harness-library/secrets/${secret_file}" "${WORKFLOW_FILE}" \
    || fail "o secret ${secret_file} deve ser somente leitura para seu proprietário."
done
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
