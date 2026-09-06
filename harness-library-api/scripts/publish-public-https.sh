#!/usr/bin/env bash

set -euo pipefail

PUBLIC_DOMAIN="${HARNESS_LIBRARY_PUBLIC_DOMAIN:-mkthub.api.br}"
PUBLIC_NETWORK="${HARNESS_LIBRARY_PUBLIC_NETWORK:-public-net}"
API_CONTAINER="${HARNESS_LIBRARY_CONTAINER:-harness-library-api}"
API_REMOTE_ROOT="${HARNESS_LIBRARY_REMOTE_ROOT:-/root/harness-library-api}"
PROXY_REMOTE_ROOT="${PUBLIC_PROXY_REMOTE_ROOT:-/root/lead-portal-payments-service}"
PUBLICATION_ASSET_ROOT="${HARNESS_LIBRARY_PUBLICATION_ASSET_ROOT:-${API_REMOTE_ROOT}/publication}"
OPERATION="${HARNESS_LIBRARY_PUBLICATION_OPERATION:-publish}"
PROXY_RELOAD_ATTEMPTS="${HARNESS_LIBRARY_PROXY_RELOAD_ATTEMPTS:-30}"
PROXY_RELOAD_INTERVAL_SECONDS="${HARNESS_LIBRARY_PROXY_RELOAD_INTERVAL_SECONDS:-1}"
CERTBOT_EMAIL="${HARNESS_LIBRARY_CERTBOT_EMAIL:-paulofore@gmail.com}"
CERTBOT_IMAGE="${HARNESS_LIBRARY_CERTBOT_IMAGE:-certbot/certbot:latest}"
LOCK_FILE="${HARNESS_LIBRARY_PUBLICATION_LOCK_FILE:-/var/lock/marketinghub-deploy-163-245-200-7.lock}"
LETSENCRYPT_ROOT="${HARNESS_LIBRARY_LETSENCRYPT_ROOT:-/etc/letsencrypt}"
LETSENCRYPT_STATE_ROOT="${HARNESS_LIBRARY_LETSENCRYPT_STATE_ROOT:-/var/lib/letsencrypt}"
LETSENCRYPT_LOG_ROOT="${HARNESS_LIBRARY_LETSENCRYPT_LOG_ROOT:-/var/log/letsencrypt}"
NGINX_CERT_ROOT="${HARNESS_LIBRARY_NGINX_CERT_ROOT:-/etc/nginx/certs}"
PROXY_CONFIG_SOURCE="${PUBLICATION_ASSET_ROOT}/nginx.conf"
PROXY_CERT_SCRIPT_SOURCE="${PUBLICATION_ASSET_ROOT}/ensure-certs.sh"
PROXY_CONFIG_TARGET="${PROXY_REMOTE_ROOT}/nginx.conf"
PROXY_CERT_SCRIPT_TARGET="${PROXY_REMOTE_ROOT}/docker/proxy/ensure-certs.sh"
ACTIVATION_MARKER="${API_REMOTE_ROOT}/public-https-enabled"
BACKUP_DIRECTORY=""
CONFIG_CHANGED=false
PROXY_CONTAINER=""

fail() {
  printf '[HARNESS-HTTPS] %s\n' "$1" >&2
  exit 1
}

PROXY_CONTRACT_FAILURE=""

public_proxy_contract_is_active() {
  local http_headers
  local unauthenticated_code
  local actuator_code
  local https_headers

  if ! http_headers="$(curl --noproxy '*' --silent --show-error --max-time 15 \
    --resolve "${PUBLIC_DOMAIN}:80:127.0.0.1" \
    --head "http://${PUBLIC_DOMAIN}/v1/cards")"; then
    PROXY_CONTRACT_FAILURE="a rota HTTP ainda não respondeu"
    return 1
  fi
  if ! grep -Eq '^HTTP/[0-9.]+ 301' <<<"${http_headers}"; then
    PROXY_CONTRACT_FAILURE="a rota HTTP ainda não retornou 301"
    return 1
  fi
  if ! grep -Fiq "location: https://${PUBLIC_DOMAIN}/v1/cards" <<<"${http_headers}"; then
    PROXY_CONTRACT_FAILURE="o redirecionamento HTTP ainda não preservou a rota"
    return 1
  fi

  if ! unauthenticated_code="$(curl --noproxy '*' --silent --show-error --max-time 20 \
    --resolve "${PUBLIC_DOMAIN}:443:127.0.0.1" \
    --output /dev/null --write-out '%{http_code}' \
    "https://${PUBLIC_DOMAIN}/v1/cards")"; then
    PROXY_CONTRACT_FAILURE="a rota HTTPS ainda não apresentou o certificado válido"
    return 1
  fi
  if [[ "${unauthenticated_code}" != "401" ]]; then
    PROXY_CONTRACT_FAILURE="a rota HTTPS sem chave ainda não retornou 401; status=${unauthenticated_code}"
    return 1
  fi

  if ! actuator_code="$(curl --noproxy '*' --silent --show-error --max-time 20 \
    --resolve "${PUBLIC_DOMAIN}:443:127.0.0.1" \
    --output /dev/null --write-out '%{http_code}' \
    "https://${PUBLIC_DOMAIN}/actuator/health")"; then
    PROXY_CONTRACT_FAILURE="o bloqueio público do Actuator ainda não respondeu"
    return 1
  fi
  if [[ "${actuator_code}" != "404" ]]; then
    PROXY_CONTRACT_FAILURE="o Actuator ainda não está bloqueado; status=${actuator_code}"
    return 1
  fi

  if ! https_headers="$(curl --noproxy '*' --silent --show-error --max-time 20 \
    --resolve "${PUBLIC_DOMAIN}:443:127.0.0.1" \
    --head "https://${PUBLIC_DOMAIN}/v1/cards")"; then
    PROXY_CONTRACT_FAILURE="os cabeçalhos HTTPS ainda não responderam"
    return 1
  fi
  if ! grep -Fiq 'strict-transport-security: max-age=31536000' <<<"${https_headers}"; then
    PROXY_CONTRACT_FAILURE="o HSTS ainda não foi aplicado"
    return 1
  fi

  PROXY_CONTRACT_FAILURE=""
  return 0
}

wait_for_public_proxy_contract() {
  local attempt

  for ((attempt = 1; attempt <= PROXY_RELOAD_ATTEMPTS; attempt++)); do
    if public_proxy_contract_is_active; then
      return 0
    fi
    printf '[HARNESS-HTTPS] aguardando recarga do proxy (%d/%d): %s\n' \
      "${attempt}" "${PROXY_RELOAD_ATTEMPTS}" "${PROXY_CONTRACT_FAILURE}" >&2
    if ((attempt < PROXY_RELOAD_ATTEMPTS)); then
      sleep "${PROXY_RELOAD_INTERVAL_SECONDS}"
    fi
  done

  return 1
}

restore_proxy_configuration() {
  if [[ "${CONFIG_CHANGED}" != "true" || -z "${BACKUP_DIRECTORY}" ]]; then
    return
  fi

  dd if="${BACKUP_DIRECTORY}/nginx.conf" of="${PROXY_CONFIG_TARGET}" status=none conv=fsync
  dd if="${BACKUP_DIRECTORY}/ensure-certs.sh" of="${PROXY_CERT_SCRIPT_TARGET}" status=none conv=fsync
  chmod 0644 "${PROXY_CONFIG_TARGET}"
  chmod 0755 "${PROXY_CERT_SCRIPT_TARGET}"
  if [[ -n "${PROXY_CONTAINER}" ]]; then
    docker exec "${PROXY_CONTAINER}" nginx -t >/dev/null 2>&1 || true
    docker exec "${PROXY_CONTAINER}" nginx -s reload >/dev/null 2>&1 || true
  fi
}

cleanup() {
  local exit_code=$?
  trap - EXIT
  if [[ "${exit_code}" -ne 0 ]]; then
    restore_proxy_configuration
  fi
  if [[ -n "${BACKUP_DIRECTORY}" && -d "${BACKUP_DIRECTORY}" ]]; then
    rm -rf -- "${BACKUP_DIRECTORY}"
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

[[ "${PUBLIC_DOMAIN}" == "mkthub.api.br" ]] \
  || fail "o domínio público deve permanecer fixo em mkthub.api.br"
[[ "${OPERATION}" == "publish" || "${OPERATION}" == "renew" ]] \
  || fail "operação inválida; use publish ou renew"
[[ "${PROXY_RELOAD_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]] \
  || fail "quantidade de tentativas da recarga do proxy inválida"
[[ "${PROXY_RELOAD_INTERVAL_SECONDS}" =~ ^[0-9]+$ ]] \
  || fail "intervalo da recarga do proxy inválido"
[[ "${CERTBOT_EMAIL}" == *@* ]] || fail "e-mail do Certbot inválido"

install -d -m 0755 "$(dirname "${LOCK_FILE}")"
exec 9>"${LOCK_FILE}"
flock -w 600 9 || fail "outro deploy permanece ativo no host compartilhado"

docker network inspect "${PUBLIC_NETWORK}" >/dev/null \
  || fail "rede pública ${PUBLIC_NETWORK} ausente"

api_state="$(docker inspect --format '{{.State.Running}} {{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "${API_CONTAINER}" 2>/dev/null || true)"
[[ "${api_state}" == "true healthy" ]] \
  || fail "a API precisa estar em execução e saudável antes do HTTPS; estado=${api_state:-ausente}"

api_network="$(docker inspect --format "{{with index .NetworkSettings.Networks \"${PUBLIC_NETWORK}\"}}attached{{end}}" "${API_CONTAINER}")"
[[ "${api_network}" == "attached" ]] \
  || fail "a API não está conectada à rede ${PUBLIC_NETWORK}"

published_binding="$(docker port "${API_CONTAINER}" 8103/tcp 2>/dev/null || true)"
[[ "${published_binding}" == "127.0.0.1:8103" ]] \
  || fail "a origem deve permanecer exclusiva do loopback; binding=${published_binding:-ausente}"

mapfile -t proxy_containers < <(
  docker ps -q \
    --filter 'label=com.docker.compose.project=lead-portal-payments-service' \
    --filter 'label=com.docker.compose.service=proxy'
)
[[ "${#proxy_containers[@]}" -eq 1 ]] \
  || fail "a publicação exige exatamente um proxy público canônico"
PROXY_CONTAINER="${proxy_containers[0]}"

proxy_network="$(docker inspect --format "{{with index .NetworkSettings.Networks \"${PUBLIC_NETWORK}\"}}attached{{end}}" "${PROXY_CONTAINER}")"
[[ "${proxy_network}" == "attached" ]] \
  || fail "o proxy público não está conectado à rede ${PUBLIC_NETWORK}"

if [[ "${OPERATION}" == "renew" ]]; then
  [[ -f "${ACTIVATION_MARKER}" ]] \
    || fail "renovação recusada porque a publicação inicial ainda não foi concluída"
  grep -Fq 'server_name mkthub.api.br;' "${PROXY_CONFIG_TARGET}" \
    || fail "a rota publicada de mkthub.api.br não está instalada no proxy"
else
  [[ -s "${PROXY_CONFIG_SOURCE}" && -s "${PROXY_CERT_SCRIPT_SOURCE}" ]] \
    || fail "ativos versionados do proxy não foram sincronizados"
  grep -Fq 'server_name mkthub.api.br;' "${PROXY_CONFIG_SOURCE}" \
    || fail "configuração versionada não declara mkthub.api.br"
  grep -Fq 'harness-library-api:8103' "${PROXY_CONFIG_SOURCE}" \
    || fail "configuração versionada não aponta para a API canônica"
fi

install -d -m 0755 \
  "${PROXY_REMOTE_ROOT}/docker/proxy/html" \
  "${LETSENCRYPT_ROOT}" \
  "${LETSENCRYPT_STATE_ROOT}" \
  "${LETSENCRYPT_LOG_ROOT}"

docker pull "${CERTBOT_IMAGE}"
docker run --rm \
  --label com.marketinghub.operation=harness-library-tls \
  -v "${PROXY_REMOTE_ROOT}/docker/proxy/html:/var/www/certbot" \
  -v "${LETSENCRYPT_ROOT}:/etc/letsencrypt" \
  -v "${LETSENCRYPT_STATE_ROOT}:/var/lib/letsencrypt" \
  -v "${LETSENCRYPT_LOG_ROOT}:/var/log/letsencrypt" \
  "${CERTBOT_IMAGE}" certonly \
  --non-interactive \
  --agree-tos \
  --no-eff-email \
  --keep-until-expiring \
  --email "${CERTBOT_EMAIL}" \
  --webroot \
  --webroot-path /var/www/certbot \
  --cert-name "${PUBLIC_DOMAIN}" \
  -d "${PUBLIC_DOMAIN}"

LE_CERT_DIRECTORY="${LETSENCRYPT_ROOT}/live/${PUBLIC_DOMAIN}"
[[ -s "${LE_CERT_DIRECTORY}/fullchain.pem" && -s "${LE_CERT_DIRECTORY}/privkey.pem" ]] \
  || fail "o Certbot não materializou o certificado esperado"
openssl x509 -in "${LE_CERT_DIRECTORY}/fullchain.pem" -noout -checkend 2592000 \
  || fail "o certificado expira em menos de 30 dias"
openssl x509 -in "${LE_CERT_DIRECTORY}/fullchain.pem" -noout -ext subjectAltName \
  | grep -Fq "DNS:${PUBLIC_DOMAIN}" \
  || fail "o certificado não contém o domínio esperado"

NGINX_CERT_DIRECTORY="${NGINX_CERT_ROOT}/live/${PUBLIC_DOMAIN}"
install -d -m 0755 "${NGINX_CERT_DIRECTORY}"
install -m 0644 -D "${LE_CERT_DIRECTORY}/fullchain.pem" "${NGINX_CERT_DIRECTORY}/fullchain.pem"
install -m 0600 -D "${LE_CERT_DIRECTORY}/privkey.pem" "${NGINX_CERT_DIRECTORY}/privkey.pem"

if [[ "${OPERATION}" == "publish" ]]; then
  BACKUP_DIRECTORY="$(mktemp -d /tmp/harness-library-proxy-backup.XXXXXX)"
  install -m 0600 "${PROXY_CONFIG_TARGET}" "${BACKUP_DIRECTORY}/nginx.conf"
  install -m 0700 "${PROXY_CERT_SCRIPT_TARGET}" "${BACKUP_DIRECTORY}/ensure-certs.sh"
  CONFIG_CHANGED=true

  # Sobrescrever o arquivo preserva o inode do bind mount usado pelo proxy ativo.
  dd if="${PROXY_CONFIG_SOURCE}" of="${PROXY_CONFIG_TARGET}" status=none conv=fsync
  dd if="${PROXY_CERT_SCRIPT_SOURCE}" of="${PROXY_CERT_SCRIPT_TARGET}" status=none conv=fsync
  chmod 0644 "${PROXY_CONFIG_TARGET}"
  chmod 0755 "${PROXY_CERT_SCRIPT_TARGET}"
fi

docker exec "${PROXY_CONTAINER}" nginx -t
docker exec "${PROXY_CONTAINER}" nginx -s reload

wait_for_public_proxy_contract \
  || fail "o proxy não aplicou o contrato HTTPS após a recarga: ${PROXY_CONTRACT_FAILURE}"

install -d -m 0700 "${API_REMOTE_ROOT}"
marker_temporary="${ACTIVATION_MARKER}.tmp"
printf 'domain=%s\noperation=%s\nupdated_at=%s\n' \
  "${PUBLIC_DOMAIN}" "${OPERATION}" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  >"${marker_temporary}"
chmod 0600 "${marker_temporary}"
mv -f "${marker_temporary}" "${ACTIVATION_MARKER}"

CONFIG_CHANGED=false
printf '[HARNESS-HTTPS] %s concluído para https://%s\n' "${OPERATION}" "${PUBLIC_DOMAIN}"
