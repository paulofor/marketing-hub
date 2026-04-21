#!/usr/bin/env bash
set -euo pipefail

# Emite/renova certificado do MCP no host de deploy (/opt/marketinghub/containers).
# Uso:
#   EMAIL=voce@dominio.com ./bin/issue-mcp-letsencrypt-cert.sh
#
# Opcional:
#   DOMAIN=mcpserverdigi.shop
#   ALT_DOMAIN=www.mcpserverdigi.shop
#   DEPLOY_DIR=/opt/marketinghub/containers
#   CERTBOT_IMAGE=certbot/certbot:latest
#   USE_STAGING=true

DOMAIN="${DOMAIN:-mcpserverdigi.shop}"
ALT_DOMAIN="${ALT_DOMAIN:-www.mcpserverdigi.shop}"
EMAIL="${EMAIL:-}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/marketinghub/containers}"
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:latest}"
USE_STAGING="${USE_STAGING:-false}"
SCRIPT_VERSION="2026-04-21.1"

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL com um endereço válido." >&2
  exit 1
fi

if [[ ! -d "${DEPLOY_DIR}" ]]; then
  echo "[ERRO] DEPLOY_DIR não encontrado: ${DEPLOY_DIR}" >&2
  exit 1
fi

COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "[ERRO] docker-compose.yml não encontrado em ${DEPLOY_DIR}" >&2
  exit 1
fi

WWW_DIR="${DEPLOY_DIR}/volumes/mcp/certbot/www"
CONF_DIR="${DEPLOY_DIR}/volumes/mcp/certbot/conf"

mkdir -p "${WWW_DIR}/.well-known/acme-challenge" "${CONF_DIR}"

# Protege criação de arquivos sensíveis.
umask 077
chmod 700 "${CONF_DIR}" || true

STAGING_FLAGS=()
if [[ "${USE_STAGING}" == "true" ]]; then
  STAGING_FLAGS+=("--test-cert")
  echo "[INFO] Staging habilitado (certificado não é válido para produção)."
fi

echo "[INFO] Script version: ${SCRIPT_VERSION}"
echo "[INFO] DEPLOY_DIR=${DEPLOY_DIR}"
echo "[INFO] Certificando domínios: ${DOMAIN}, ${ALT_DOMAIN}"

echo "[INFO] Garantindo Nginx HTTP para challenge ACME..."
MCP_NGINX_CONF=default.http.conf docker compose -f "${COMPOSE_FILE}" up -d --no-deps --force-recreate mcp-nginx

ACTIVE_CONF="$(docker inspect marketinghub-mcp-nginx --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/conf.d/default.conf"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)"
if [[ -n "${ACTIVE_CONF}" ]]; then
  echo "[INFO] Nginx default.conf montado de: ${ACTIVE_CONF}"
fi

# Pré-validação: confirma que o token em webroot é servido publicamente.
TOKEN="mcp-preflight-$(date +%s)-$$"
TOKEN_FILE="${WWW_DIR}/.well-known/acme-challenge/${TOKEN}"
printf '%s' "${TOKEN}" > "${TOKEN_FILE}"
chmod 644 "${TOKEN_FILE}" || true

preflight_check() {
  local host="$1"
  local url="http://${host}/.well-known/acme-challenge/${TOKEN}"

  local body
  body="$(curl -fsS --max-time 10 "${url}" || true)"

  if [[ "${body}" != "${TOKEN}" ]]; then
    echo "[ERRO] Preflight falhou para ${host}." >&2
    echo "[ERRO] URL testada: ${url}" >&2
    echo "[ERRO] Esperado: ${TOKEN}" >&2
    echo "[ERRO] Recebido: ${body:-<vazio/erro>}" >&2
    echo "[DICA] Verifique DNS A/AAAA, porta 80 aberta e se outro Nginx/Apache está respondendo pelo domínio." >&2
    exit 1
  fi

  echo "[OK] Preflight ACME para ${host} validado."
}

preflight_check "${DOMAIN}"
preflight_check "${ALT_DOMAIN}"

docker run --rm \
  -v "${WWW_DIR}:/var/www/certbot" \
  -v "${CONF_DIR}:/etc/letsencrypt" \
  "${CERTBOT_IMAGE}" certonly --webroot \
  -w /var/www/certbot \
  -d "${DOMAIN}" -d "${ALT_DOMAIN}" \
  --email "${EMAIL}" \
  --agree-tos \
  --no-eff-email \
  --non-interactive \
  --keep-until-expiring \
  --preferred-challenges http-01 \
  --key-type ecdsa \
  --elliptic-curve secp384r1 \
  "${STAGING_FLAGS[@]}"

rm -f "${TOKEN_FILE}" || true

find "${CONF_DIR}" -type d -exec chmod 700 {} \; || true
find "${CONF_DIR}" -type f -name 'privkey.pem' -exec chmod 600 {} \; || true
find "${CONF_DIR}" -type f ! -name 'privkey.pem' -exec chmod 644 {} \; || true

echo "[OK] Certificado emitido/renovado em ${CONF_DIR}/live/${DOMAIN}/"
echo "[NEXT] Ative TLS: MCP_NGINX_CONF=default.conf docker compose -f ${COMPOSE_FILE} up -d --no-deps mcp-nginx"
