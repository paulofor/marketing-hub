#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPOSITORY_ROOT="$(cd "${MODULE_ROOT}/.." && pwd)"
DOCKERFILE="${MODULE_ROOT}/Dockerfile"
COMPOSE_FILE="${MODULE_ROOT}/docker-compose.deploy.yml"
WORKFLOW_FILE="${REPOSITORY_ROOT}/.github/workflows/harness-library-api-ci.yml"
PUBLICATION_WORKFLOW_FILE="${REPOSITORY_ROOT}/.github/workflows/harness-library-api-publication.yml"
SECRET_RUNTIME_TEST="${MODULE_ROOT}/scripts/validate-secret-file-runtime.sh"
PUBLICATION_SCRIPT="${MODULE_ROOT}/scripts/publish-public-https.sh"
PUBLICATION_SCRIPT_TEST="${MODULE_ROOT}/scripts/test-publish-public-https.sh"
DNS_VALIDATION_SCRIPT="${MODULE_ROOT}/scripts/validate-public-dns.sh"
DNS_VALIDATION_SCRIPT_TEST="${MODULE_ROOT}/scripts/test-validate-public-dns.sh"
PUBLIC_PROXY_CONFIG="${REPOSITORY_ROOT}/lead-portal-payments-service/nginx.conf"

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
grep -Fq 'name: ${HARNESS_LIBRARY_PUBLIC_NETWORK:-public-net}' "${COMPOSE_FILE}" \
  || fail 'a API deve compartilhar somente a rede privada do proxy público.'
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
grep -Fq 'group: deploy-vps-163-245-200-7' "${WORKFLOW_FILE}" \
  || fail 'deploy deve usar a fila compartilhada do host selecionado.'
deploy_queue="$(grep -F -A2 'group: deploy-vps-163-245-200-7' "${WORKFLOW_FILE}" || true)"
grep -Fq 'queue: max' <<<"${deploy_queue}" \
  || fail 'deploy deve preservar execuções pendentes na fila ampliada.'
grep -Fq 'cancel-in-progress: false' <<<"${deploy_queue}" \
  || fail 'deploy não pode cancelar uma publicação ativa.'

[[ -x "${PUBLICATION_SCRIPT}" ]] \
  || fail 'script executável de publicação HTTPS está ausente.'
[[ -s "${PUBLICATION_WORKFLOW_FILE}" ]] \
  || fail 'workflow próprio de publicação HTTPS está ausente.'
grep -Fq 'PUBLIC_DOMAIN: mkthub.api.br' "${PUBLICATION_WORKFLOW_FILE}" \
  || fail 'workflow HTTPS deve fixar o domínio escolhido pelo usuário.'
[[ -x "${DNS_VALIDATION_SCRIPT}" && -x "${DNS_VALIDATION_SCRIPT_TEST}" ]] \
  || fail 'validação executável dos registros DNS está ausente.'
grep -Fq 'validate-public-dns.sh' "${PUBLICATION_WORKFLOW_FILE}" \
  || fail 'DNS deve ser validado por RRset antes de tocar o proxy público.'
if grep -Fq 'getent ahostsv6' "${PUBLICATION_WORKFLOW_FILE}"; then
  fail 'workflow não pode inferir registro AAAA a partir da resolução AF_INET6 do sistema.'
fi
grep -Fq "schedule:" "${PUBLICATION_WORKFLOW_FILE}" \
  || fail 'certificado público deve possuir rotina de renovação.'
grep -Fq 'public-https-enabled' "${PUBLICATION_WORKFLOW_FILE}" \
  || fail 'renovação agendada não pode ativar um domínio sem publicação manual inicial.'
grep -Fq 'server_name mkthub.api.br;' "${PUBLIC_PROXY_CONFIG}" \
  || fail 'proxy compartilhado não declara o domínio público da API.'
grep -Fq 'harness-library-api:8103' "${PUBLIC_PROXY_CONFIG}" \
  || fail 'proxy público não aponta para a origem canônica da API.'
grep -Fq 'location ^~ /actuator' "${PUBLIC_PROXY_CONFIG}" \
  || fail 'proxy público deve bloquear o Actuator.'
grep -Fq 'client_max_body_size 32k;' "${PUBLIC_PROXY_CONFIG}" \
  || fail 'proxy público deve preservar o limite físico do JSON.'
grep -Fq 'a API precisa estar em execução e saudável antes do HTTPS' "${PUBLICATION_SCRIPT}" \
  || fail 'publicação deve falhar antes do TLS quando a API estiver indisponível.'
grep -Fq 'docker exec "${PROXY_CONTAINER}" nginx -t' "${PUBLICATION_SCRIPT}" \
  || fail 'publicação deve validar o Nginx antes de recarregar a rota.'
grep -Fq 'restore_proxy_configuration' "${PUBLICATION_SCRIPT}" \
  || fail 'publicação deve restaurar a configuração anterior diante de falha.'

bash "${PUBLICATION_SCRIPT_TEST}"
bash "${DNS_VALIDATION_SCRIPT_TEST}"

printf 'Contrato da imagem e do deploy da Biblioteca do Harness validado.\n'
