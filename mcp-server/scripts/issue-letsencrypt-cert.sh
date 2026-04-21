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
SCRIPT_VERSION="2026-04-21.1"

# Diretório padrão local do módulo mcp-server.
# Em produção, pode apontar para: /opt/marketinghub/containers/volumes/mcp/certbot
DEPLOY_ROOT="${DEPLOY_ROOT:-${MCP_DIR}/certbot}"
WWW_DIR="${DEPLOY_ROOT}/www"
CONF_DIR="${DEPLOY_ROOT}/conf"
COMPOSE_FILE="${MCP_DIR}/docker-compose.yml"

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL com um endereço válido." >&2
  exit 1
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "[ERRO] docker-compose.yml não encontrado em ${MCP_DIR}" >&2
  exit 1
fi

mkdir -p "${WWW_DIR}/.well-known/acme-challenge" "${CONF_DIR}"

# Restringe permissões para evitar exposição acidental dos arquivos privados.
umask 077
chmod 700 "${CONF_DIR}" || true

STAGING_FLAGS=()
if [[ "${USE_STAGING}" == "true" ]]; then
  STAGING_FLAGS+=("--test-cert")
  echo "[INFO] Modo staging habilitado (não gera certificado válido para produção)."
fi

echo "[INFO] Script version: ${SCRIPT_VERSION}"
echo "[INFO] Emitindo certificado para ${DOMAIN} e ${ALT_DOMAIN}"
echo "[INFO] Diretório webroot: ${WWW_DIR}"
echo "[INFO] Diretório de configuração/certificados: ${CONF_DIR}"

echo "[INFO] Garantindo Nginx HTTP para challenge ACME..."
(
  cd "${MCP_DIR}"
  MCP_NGINX_CONF=default.http.conf docker compose -f "${COMPOSE_FILE}" up -d --no-deps --force-recreate nginx
)

ACTIVE_CONF="$(docker inspect marketinghub-mcp-nginx --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/conf.d/default.conf"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)"
if [[ -n "${ACTIVE_CONF}" ]]; then
  echo "[INFO] Nginx default.conf montado de: ${ACTIVE_CONF}"
fi

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
    echo "[DICA] Verifique DNS A/AAAA, porta 80 aberta e se outro serviço está respondendo pelo domínio." >&2
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

# Mantém diretórios legíveis para o Nginx, mas com arquivos privados em permissão restrita.
find "${CONF_DIR}" -type d -exec chmod 700 {} \; || true
find "${CONF_DIR}" -type f -name 'privkey.pem' -exec chmod 600 {} \; || true
find "${CONF_DIR}" -type f ! -name 'privkey.pem' -exec chmod 644 {} \; || true

echo "[OK] Processo finalizado."
echo "[OK] Arquivos esperados em: ${CONF_DIR}/live/${DOMAIN}/"
