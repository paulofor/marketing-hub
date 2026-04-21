#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR=${DEPLOY_DIR:-/opt/marketinghub/containers}
MCP_TAR=${MCP_TAR:-/tmp/mcp-server-image.tar}
MCP_IMAGE=${MCP_IMAGE:-marketinghub-mcp-server}
IMAGE_TAG=${IMAGE_TAG:-latest}

mkdir -p "${DEPLOY_DIR}"
cd "${DEPLOY_DIR}"
mkdir -p ./volumes/mcp/certbot/www ./volumes/mcp/certbot/conf

resolve_mcp_nginx_conf() {
  if [[ -n "${MCP_NGINX_CONF:-}" ]]; then
    echo "${MCP_NGINX_CONF}"
    return
  fi

  if [[ -f "./volumes/mcp/certbot/conf/live/mcpserverdigi.shop/fullchain.pem" \
     && -f "./volumes/mcp/certbot/conf/live/mcpserverdigi.shop/privkey.pem" ]]; then
    echo "default.conf"
    return
  fi

  echo "default.http.conf"
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

MCP_SERVER_IMAGE="${MCP_IMAGE}" \
MCP_SERVER_IMAGE_TAG=latest \
MCP_NGINX_CONF="${MCP_NGINX_CONF_RESOLVED}" \
docker compose up -d --no-deps mcp-server mcp-nginx

cleanup_previous_tags "${MCP_IMAGE}" "latest"

docker image prune -f >/dev/null 2>&1 || true
rm -f "${MCP_TAR}" >/dev/null 2>&1 || true
