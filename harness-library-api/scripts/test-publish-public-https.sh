#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
REPOSITORY_ROOT="$(cd "${MODULE_ROOT}/.." && pwd)"
PUBLICATION_SCRIPT="${SCRIPT_DIRECTORY}/publish-public-https.sh"
TEST_ROOT="$(mktemp -d)"
FAKE_BIN="${TEST_ROOT}/bin"
API_ROOT="${TEST_ROOT}/api"
PROXY_ROOT="${TEST_ROOT}/proxy"
ASSET_ROOT="${API_ROOT}/publication"
LETSENCRYPT_ROOT="${TEST_ROOT}/letsencrypt"
NGINX_CERT_ROOT="${TEST_ROOT}/nginx-certs"
DOCKER_LOG="${TEST_ROOT}/docker.log"
CURL_STATE_FILE="${TEST_ROOT}/curl.state"
OLD_PROXY_CONFIG="${TEST_ROOT}/old-nginx.conf"
OLD_CERT_SCRIPT="${TEST_ROOT}/old-ensure-certs.sh"

cleanup() {
  rm -rf -- "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf '[ARQUITETURA] %s\n' "$1" >&2
  exit 1
}

install -d -m 0755 \
  "${FAKE_BIN}" \
  "${ASSET_ROOT}" \
  "${PROXY_ROOT}/docker/proxy/html" \
  "${LETSENCRYPT_ROOT}/live/mkthub.api.br" \
  "${NGINX_CERT_ROOT}"

cp "${REPOSITORY_ROOT}/lead-portal-payments-service/nginx.conf" "${ASSET_ROOT}/nginx.conf"
cp "${REPOSITORY_ROOT}/lead-portal-payments-service/docker/proxy/ensure-certs.sh" \
  "${ASSET_ROOT}/ensure-certs.sh"

printf 'configuração anterior preservada\n' >"${OLD_PROXY_CONFIG}"
printf '#!/usr/bin/env bash\necho anterior\n' >"${OLD_CERT_SCRIPT}"
cp "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf"
cp "${OLD_CERT_SCRIPT}" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh"

openssl req -x509 -nodes -newkey rsa:2048 -days 90 \
  -keyout "${LETSENCRYPT_ROOT}/live/mkthub.api.br/privkey.pem" \
  -out "${LETSENCRYPT_ROOT}/live/mkthub.api.br/fullchain.pem" \
  -subj '/CN=mkthub.api.br' \
  -addext 'subjectAltName=DNS:mkthub.api.br' >/dev/null 2>&1

cat >"${FAKE_BIN}/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"${FAKE_DOCKER_LOG}"

case "${1:-}" in
  network)
    exit 0
    ;;
  inspect)
    if [[ "$*" == *'.State.Running'* ]]; then
      printf '%s\n' "${FAKE_API_STATE:-true healthy}"
    else
      printf 'attached\n'
    fi
    exit 0
    ;;
  port)
    printf '127.0.0.1:8103\n'
    exit 0
    ;;
  ps)
    printf 'proxy-container-id\n'
    exit 0
    ;;
  pull|run)
    exit 0
    ;;
  exec)
    if [[ "${FAKE_DOCKER_FAIL_NGINX_TEST:-false}" == "true" && "$*" == *'nginx -t'* ]]; then
      exit 1
    fi
    exit 0
    ;;
esac

exit 1
FAKE_DOCKER

cat >"${FAKE_BIN}/curl" <<'FAKE_CURL'
#!/usr/bin/env bash
set -euo pipefail

arguments="$*"
if [[ "${arguments}" == *'http://mkthub.api.br/'* ]]; then
  request_count=0
  if [[ -f "${FAKE_CURL_STATE_FILE}" ]]; then
    request_count="$(<"${FAKE_CURL_STATE_FILE}")"
  fi
  request_count=$((request_count + 1))
  printf '%s\n' "${request_count}" >"${FAKE_CURL_STATE_FILE}"
  if ((request_count <= FAKE_CURL_STALE_ATTEMPTS)); then
    printf 'HTTP/1.1 404 Not Found\r\n\r\n'
  else
    printf 'HTTP/1.1 301 Moved Permanently\r\nLocation: https://mkthub.api.br/v1/cards\r\n\r\n'
  fi
elif [[ "${arguments}" == *'/actuator/health'* ]]; then
  printf '404'
elif [[ "${arguments}" == *'--head'* ]]; then
  printf 'HTTP/2 401\r\nstrict-transport-security: max-age=31536000\r\n\r\n'
else
  printf '401'
fi
FAKE_CURL

chmod 0755 "${FAKE_BIN}/docker" "${FAKE_BIN}/curl"

run_publication() {
  PATH="${FAKE_BIN}:${PATH}" \
  FAKE_DOCKER_LOG="${DOCKER_LOG}" \
  HARNESS_LIBRARY_REMOTE_ROOT="${API_ROOT}" \
  PUBLIC_PROXY_REMOTE_ROOT="${PROXY_ROOT}" \
  HARNESS_LIBRARY_PUBLICATION_ASSET_ROOT="${ASSET_ROOT}" \
  HARNESS_LIBRARY_PUBLICATION_LOCK_FILE="${TEST_ROOT}/deploy.lock" \
  HARNESS_LIBRARY_LETSENCRYPT_ROOT="${LETSENCRYPT_ROOT}" \
  HARNESS_LIBRARY_LETSENCRYPT_STATE_ROOT="${TEST_ROOT}/letsencrypt-state" \
  HARNESS_LIBRARY_LETSENCRYPT_LOG_ROOT="${TEST_ROOT}/letsencrypt-log" \
  HARNESS_LIBRARY_NGINX_CERT_ROOT="${NGINX_CERT_ROOT}" \
  HARNESS_LIBRARY_PROXY_RELOAD_ATTEMPTS="${PROXY_RELOAD_ATTEMPTS:-30}" \
  HARNESS_LIBRARY_PROXY_RELOAD_INTERVAL_SECONDS=0 \
  FAKE_CURL_STATE_FILE="${CURL_STATE_FILE}" \
  FAKE_CURL_STALE_ATTEMPTS="${FAKE_CURL_STALE_ATTEMPTS:-0}" \
  "$@" "${PUBLICATION_SCRIPT}"
}

# Caminho feliz: aguarda dois workers antigos e instala a rota somente após o contrato completo.
rm -f "${CURL_STATE_FILE}"
FAKE_CURL_STALE_ATTEMPTS=2 PROXY_RELOAD_ATTEMPTS=3 \
  run_publication env HARNESS_LIBRARY_PUBLICATION_OPERATION=publish
[[ "$(<"${CURL_STATE_FILE}")" == "3" ]] \
  || fail 'publicação não aguardou a troca assíncrona dos workers do proxy.'
cmp -s "${ASSET_ROOT}/nginx.conf" "${PROXY_ROOT}/nginx.conf" \
  || fail 'publicação não instalou a configuração versionada.'
cmp -s "${ASSET_ROOT}/ensure-certs.sh" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh" \
  || fail 'publicação não instalou a rotina de certificados.'
grep -Fq 'domain=mkthub.api.br' "${API_ROOT}/public-https-enabled" \
  || fail 'publicação não registrou o marcador de ativação.'
grep -Fq 'exec proxy-container-id nginx -s reload' "${DOCKER_LOG}" \
  || fail 'publicação não recarregou o proxy validado.'

# Falha de Nginx: restaura os dois arquivos anteriores e não ativa o domínio.
cp "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf"
cp "${OLD_CERT_SCRIPT}" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh"
rm -f "${API_ROOT}/public-https-enabled"
if FAKE_DOCKER_FAIL_NGINX_TEST=true run_publication env \
  HARNESS_LIBRARY_PUBLICATION_OPERATION=publish >/dev/null 2>&1; then
  fail 'publicação aceitou uma configuração de Nginx inválida.'
fi
cmp -s "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf" \
  || fail 'rollback não restaurou a configuração anterior.'
cmp -s "${OLD_CERT_SCRIPT}" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh" \
  || fail 'rollback não restaurou a rotina de certificados anterior.'
[[ ! -e "${API_ROOT}/public-https-enabled" ]] \
  || fail 'falha de publicação deixou marcador de ativação.'

# Recarga que não converge: esgota a sondagem, restaura os arquivos e não ativa o domínio.
cp "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf"
cp "${OLD_CERT_SCRIPT}" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh"
rm -f "${API_ROOT}/public-https-enabled" "${CURL_STATE_FILE}"
if FAKE_CURL_STALE_ATTEMPTS=3 PROXY_RELOAD_ATTEMPTS=2 \
  run_publication env HARNESS_LIBRARY_PUBLICATION_OPERATION=publish >/dev/null 2>&1; then
  fail 'publicação aceitou uma recarga do proxy que não convergiu.'
fi
cmp -s "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf" \
  || fail 'falha de convergência não restaurou a configuração anterior.'
cmp -s "${OLD_CERT_SCRIPT}" "${PROXY_ROOT}/docker/proxy/ensure-certs.sh" \
  || fail 'falha de convergência não restaurou a rotina de certificados anterior.'
[[ ! -e "${API_ROOT}/public-https-enabled" ]] \
  || fail 'falha de convergência deixou marcador de ativação.'

# API indisponível: bloqueia antes de alterar o proxy.
if FAKE_API_STATE='false unhealthy' run_publication env \
  HARNESS_LIBRARY_PUBLICATION_OPERATION=publish >/dev/null 2>&1; then
  fail 'publicação avançou com a API indisponível.'
fi
cmp -s "${OLD_PROXY_CONFIG}" "${PROXY_ROOT}/nginx.conf" \
  || fail 'API indisponível alterou o proxy.'

# Renovação não pode transformar agenda em primeira publicação implícita.
if run_publication env HARNESS_LIBRARY_PUBLICATION_OPERATION=renew >/dev/null 2>&1; then
  fail 'renovação avançou sem marcador de publicação inicial.'
fi

printf 'Publicação HTTPS e rollback da Biblioteca do Harness validados.\n'
