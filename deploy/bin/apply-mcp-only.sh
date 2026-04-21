#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
MCP_TAR=${MCP_TAR:-/tmp/mcp-server-image.tar}
MCP_IMAGE=${MCP_IMAGE:-marketinghub-mcp-server}
IMAGE_TAG=${IMAGE_TAG:-latest}
DOMAIN=${DOMAIN:-mcpserverdigi.shop}
ALT_DOMAIN=${ALT_DOMAIN:-www.mcpserverdigi.shop}
EMAIL=${EMAIL:-}
CERTBOT_IMAGE=${CERTBOT_IMAGE:-certbot/certbot:latest}
USE_STAGING=${USE_STAGING:-false}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"
mkdir -p ./volumes/mcp/certbot/www ./volumes/mcp/certbot/conf

certificate_exists() {
  [[ -f "./volumes/mcp/certbot/conf/live/${DOMAIN}/fullchain.pem" \
    && -f "./volumes/mcp/certbot/conf/live/${DOMAIN}/privkey.pem" ]]
}

resolve_mcp_nginx_conf() {
  if [[ -n "${MCP_NGINX_CONF:-}" ]]; then
    echo "${MCP_NGINX_CONF}"
    return
  fi

  if certificate_exists; then
    echo "default.conf"
    return
  fi

  echo "default.http.conf"
}

run_mcp_compose() {
  local nginx_conf="$1"

  MCP_SERVER_IMAGE="${MCP_IMAGE}" \
  MCP_SERVER_IMAGE_TAG=latest \
  MCP_NGINX_CONF="${nginx_conf}" \
  docker compose up -d --no-deps mcp-server mcp-nginx
}

issue_certificate_if_needed() {
  if certificate_exists; then
    return
  fi

  if [[ -z "${EMAIL}" ]]; then
    echo "[apply-mcp-only.sh] Aviso: certificado ausente e EMAIL não informado. Mantendo HTTP até nova execução com EMAIL." >&2
    return
  fi

  local staging_flags=()
  if [[ "${USE_STAGING}" == "true" ]]; then
    staging_flags+=("--test-cert")
  fi

  echo "[apply-mcp-only.sh] Certificado ausente. Solicitando Let's Encrypt via HTTP-01 para ${DOMAIN} (${ALT_DOMAIN})."
  docker run --rm \
    -v "./volumes/mcp/certbot/www:/var/www/certbot" \
    -v "./volumes/mcp/certbot/conf:/etc/letsencrypt" \
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
    "${staging_flags[@]}"

  if certificate_exists; then
    echo "[apply-mcp-only.sh] Certificado encontrado após emissão. Reiniciando MCP Nginx em HTTPS."
    MCP_SERVER_IMAGE="${MCP_IMAGE}" \
    MCP_SERVER_IMAGE_TAG=latest \
    MCP_NGINX_CONF=default.conf \
    docker compose up -d --force-recreate --no-deps mcp-nginx
  else
    echo "[apply-mcp-only.sh] Aviso: emissão finalizada sem certificado detectado. Mantendo HTTP." >&2
  fi
}

if [[ -f "${MCP_TAR}" ]]; then
  docker load -i "${MCP_TAR}"
fi

if [[ "${IMAGE_TAG}" != "latest" ]]; then
  if docker image inspect "${MCP_IMAGE}:${IMAGE_TAG}" >/dev/null 2>&1; then
    docker tag "${MCP_IMAGE}:${IMAGE_TAG}" "${MCP_IMAGE}:latest"
  else
    echo "[apply-mcp-only.sh] Aviso: imagem ${MCP_IMAGE}:${IMAGE_TAG} não encontrada; mantendo latest atual." >&2
  fi
fi

cleanup_previous_tags() {
  local repository="$1"
  local keep_tag="$2"

  docker image ls "${repository}" --format '{{.Repository}}:{{.Tag}}' \
    | awk -F':' -v keep_tag="${keep_tag}" '$2 != "<none>" && $2 != keep_tag {print $0}' \
    | xargs -r docker image rm >/dev/null 2>&1 || true
}

# Atualiza somente o MCP Server e o Nginx dedicado do MCP sem reiniciar outros serviços.
MCP_NGINX_CONF_RESOLVED="$(resolve_mcp_nginx_conf)"
echo "[apply-mcp-only.sh] Usando MCP_NGINX_CONF=${MCP_NGINX_CONF_RESOLVED}"
run_mcp_compose "${MCP_NGINX_CONF_RESOLVED}"

if [[ -z "${MCP_NGINX_CONF:-}" ]]; then
  issue_certificate_if_needed
fi

cleanup_previous_tags "${MCP_IMAGE}" "latest"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${MCP_TAR}" >/dev/null 2>&1 || true
