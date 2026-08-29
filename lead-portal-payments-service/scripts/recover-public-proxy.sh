#!/usr/bin/env bash
set -euo pipefail

# Estes valores são a fronteira de autoridade produtiva. A sobrescrita só é
# aceita no modo de teste explícito usado pela topologia Docker efêmera.
if [ "${PUBLIC_PROXY_RECOVERY_TEST_MODE:-false}" = 'true' ]; then
  compose_project="${PUBLIC_PROXY_RECOVERY_COMPOSE_PROJECT:?projeto Compose de teste obrigatório}"
  compose_file="${PUBLIC_PROXY_RECOVERY_COMPOSE_FILE:?arquivo Compose de teste obrigatório}"
else
  if [ -n "${PUBLIC_PROXY_RECOVERY_COMPOSE_PROJECT:-}" ] \
    || [ -n "${PUBLIC_PROXY_RECOVERY_COMPOSE_FILE:-}" ]; then
    echo '[ARQUITETURA] alvo produtivo da recuperação não aceita sobrescrita.' >&2
    exit 1
  fi
  compose_project="lead-portal-payments-service"
  compose_file="/root/lead-portal-payments-service/docker-compose.deploy.yml"
fi
service="proxy"

if [[ ! "${compose_project}" =~ ^[a-z0-9][a-z0-9_-]{0,62}$ ]]; then
  echo '[ARQUITETURA] projeto Compose da recuperação é inválido.' >&2
  exit 1
fi
if [[ "${compose_file}" != /* ]]; then
  echo '[ARQUITETURA] caminho do Compose da recuperação deve ser absoluto.' >&2
  exit 1
fi

proxy_containers() {
  docker ps -aq \
    --filter "label=com.docker.compose.project=${compose_project}" \
    --filter "label=com.docker.compose.service=${service}"
}

require_single_proxy() {
  local containers="$1"
  local count
  count="$(sed '/^$/d' <<<"${containers}" | wc -l | tr -d ' ')"
  if [ "${count}" -ne 1 ]; then
    echo "[ARQUITETURA] recuperação exige exatamente um proxy canônico; encontrados=${count}." >&2
    exit 1
  fi
}

containers="$(proxy_containers)"
if [ -n "${containers}" ] && [ "$(sed '/^$/d' <<<"${containers}" | wc -l | tr -d ' ')" -gt 1 ]; then
  require_single_proxy "${containers}"
fi

if [ -z "${containers}" ]; then
  if [ ! -f "${compose_file}" ]; then
    echo '[ARQUITETURA] proxy ausente e Compose canônico indisponível; recuperação bloqueada.' >&2
    exit 1
  fi
  LEAD_PORTAL_PAYMENTS_IMAGE=public-proxy-recovery-unused \
    OPENAI_API_KEY=public-proxy-recovery-unused \
    docker compose -p "${compose_project}" -f "${compose_file}" \
      up -d --no-deps --no-build "${service}"
  containers="$(proxy_containers)"
fi

require_single_proxy "${containers}"
container_id="$(sed '/^$/d' <<<"${containers}")"

docker update --restart=always "${container_id}" >/dev/null
if [ "$(docker inspect "${container_id}" --format '{{.State.Running}}')" != 'true' ]; then
  docker start "${container_id}" >/dev/null
fi

for attempt in $(seq 1 60); do
  running="$(docker inspect "${container_id}" --format '{{.State.Running}}' 2>/dev/null || true)"
  health="$(docker inspect "${container_id}" --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' 2>/dev/null || true)"
  restart_policy="$(docker inspect "${container_id}" --format '{{.HostConfig.RestartPolicy.Name}}' 2>/dev/null || true)"
  if [ "${running}" = 'true' ] \
    && [ "${restart_policy}" = 'always' ] \
    && { [ -z "${health}" ] || [ "${health}" = 'healthy' ]; } \
    && docker exec "${container_id}" nginx -t -q; then
    container_name="$(docker inspect "${container_id}" --format '{{.Name}}' | sed 's#^/##')"
    echo "PUBLIC_PROXY_RECOVERY_STATUS=recovered container=${container_name} restartPolicy=always health=${health:-nginx-config-ok}"
    exit 0
  fi
  if [ "${health}" = 'unhealthy' ]; then
    echo '[ARQUITETURA] proxy reiniciado, mas o healthcheck ficou unhealthy.' >&2
    docker logs --tail 80 "${container_id}" >&2 || true
    exit 1
  fi
  if [ "${attempt}" = '60' ]; then
    echo '[ARQUITETURA] proxy não ficou saudável dentro do limite da recuperação.' >&2
    docker inspect "${container_id}" --format 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{end}} restartPolicy={{.HostConfig.RestartPolicy.Name}}' >&2 || true
    docker logs --tail 80 "${container_id}" >&2 || true
    exit 1
  fi
  sleep 1
done
