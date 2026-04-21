#!/usr/bin/env bash
set -euo pipefail

# Emite/renova certificado do MCP no host de deploy (/opt/marketinghub/containers).
# Uso:
#   EMAIL=voce@dominio.com ./deploy/bin/issue-mcp-letsencrypt-cert.sh
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

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL com um endereço válido." >&2
  exit 1
fi

if [[ ! -d "${DEPLOY_DIR}" ]]; then
  echo "[ERRO] DEPLOY_DIR não encontrado: ${DEPLOY_DIR}" >&2
  exit 1
fi

WWW_DIR="${DEPLOY_DIR}/volumes/mcp/certbot/www"
CONF_DIR="${DEPLOY_DIR}/volumes/mcp/certbot/conf"

mkdir -p "${WWW_DIR}" "${CONF_DIR}"

# Protege criação de arquivos sensíveis.
umask 077
chmod 700 "${CONF_DIR}" || true

STAGING_FLAGS=()
if [[ "${USE_STAGING}" == "true" ]]; then
  STAGING_FLAGS+=("--test-cert")
  echo "[INFO] Staging habilitado (certificado não é válido para produção)."
fi

echo "[INFO] DEPLOY_DIR=${DEPLOY_DIR}"
echo "[INFO] Certificando domínios: ${DOMAIN}, ${ALT_DOMAIN}"

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

find "${CONF_DIR}" -type d -exec chmod 700 {} \; || true
find "${CONF_DIR}" -type f -name 'privkey.pem' -exec chmod 600 {} \; || true
find "${CONF_DIR}" -type f ! -name 'privkey.pem' -exec chmod 644 {} \; || true

echo "[OK] Certificado emitido/renovado em ${CONF_DIR}/live/${DOMAIN}/"
echo "[NEXT] Ative TLS: MCP_NGINX_CONF=default.conf docker compose -f ${DEPLOY_DIR}/docker-compose.yml up -d --no-deps mcp-nginx"
