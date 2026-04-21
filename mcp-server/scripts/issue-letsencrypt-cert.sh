#!/usr/bin/env bash
set -euo pipefail

# Emite (ou reemite) certificados Let's Encrypt para o MCP com boas práticas de segurança.
# Uso:
#   DOMAIN=mcpserverdigi.shop ALT_DOMAIN=www.mcpserverdigi.shop EMAIL=voce@dominio.com \
#   ./scripts/issue-letsencrypt-cert.sh
#
# Opcional:
#   CERTBOT_IMAGE=certbot/certbot:latest  # imagem do certbot
#   USE_STAGING=true                      # usa ambiente staging da Let's Encrypt
#   DEPLOY_ROOT=/opt/marketinghub/containers/volumes/mcp/certbot

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MCP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

DOMAIN="${DOMAIN:-mcpserverdigi.shop}"
ALT_DOMAIN="${ALT_DOMAIN:-www.mcpserverdigi.shop}"
EMAIL="${EMAIL:-}"
CERTBOT_IMAGE="${CERTBOT_IMAGE:-certbot/certbot:latest}"
USE_STAGING="${USE_STAGING:-false}"
SWITCH_TO_HTTPS="${SWITCH_TO_HTTPS:-true}"

# Diretório padrão local do módulo mcp-server.
# Em produção, pode apontar para: /opt/marketinghub/containers/volumes/mcp/certbot
DEPLOY_ROOT="${DEPLOY_ROOT:-${MCP_DIR}/certbot}"
WWW_DIR="${DEPLOY_ROOT}/www"
CONF_DIR="${DEPLOY_ROOT}/conf"

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL com um endereço válido." >&2
  exit 1
fi

mkdir -p "${WWW_DIR}" "${CONF_DIR}"

# Restringe permissões para evitar exposição acidental dos arquivos privados.
umask 077
chmod 700 "${CONF_DIR}" || true

STAGING_FLAGS=()
if [[ "${USE_STAGING}" == "true" ]]; then
  STAGING_FLAGS+=("--test-cert")
  echo "[INFO] Modo staging habilitado (não gera certificado válido para produção)."
fi

echo "[INFO] Emitindo certificado para ${DOMAIN} e ${ALT_DOMAIN}"
echo "[INFO] Diretório webroot: ${WWW_DIR}"
echo "[INFO] Diretório de configuração/certificados: ${CONF_DIR}"

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

# Mantém diretórios legíveis para o Nginx, mas com arquivos privados em permissão restrita.
find "${CONF_DIR}" -type d -exec chmod 700 {} \; || true
find "${CONF_DIR}" -type f -name 'privkey.pem' -exec chmod 600 {} \; || true
find "${CONF_DIR}" -type f ! -name 'privkey.pem' -exec chmod 644 {} \; || true

echo "[OK] Processo finalizado."
echo "[OK] Arquivos esperados em: ${CONF_DIR}/live/${DOMAIN}/"

if [[ "${SWITCH_TO_HTTPS}" != "true" ]]; then
  echo "[INFO] SWITCH_TO_HTTPS=false, mantendo Nginx no modo atual."
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[WARN] Docker não encontrado no PATH. Ative HTTPS manualmente com:"
  echo "       MCP_NGINX_CONF=default.conf docker compose up -d nginx"
  exit 0
fi

if [[ ! -f "${MCP_DIR}/docker-compose.yml" ]]; then
  echo "[WARN] docker-compose.yml não encontrado em ${MCP_DIR}."
  echo "       Ative HTTPS manualmente com:"
  echo "       MCP_NGINX_CONF=default.conf docker compose up -d nginx"
  exit 0
fi

if [[ ! -f "${CONF_DIR}/live/${DOMAIN}/fullchain.pem" || ! -f "${CONF_DIR}/live/${DOMAIN}/privkey.pem" ]]; then
  echo "[ERRO] Certificado emitido, mas arquivos esperados não foram encontrados em:"
  echo "       ${CONF_DIR}/live/${DOMAIN}/"
  exit 1
fi

echo "[INFO] Ativando Nginx com TLS (MCP_NGINX_CONF=default.conf)..."
(
  cd "${MCP_DIR}"
  MCP_NGINX_CONF=default.conf docker compose up -d --force-recreate --no-deps nginx
  MCP_NGINX_CONF=default.conf docker compose exec nginx nginx -t
)

echo "[OK] HTTPS ativado no Nginx."
