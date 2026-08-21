#!/usr/bin/env bash
set -euo pipefail

canonical_backend="lead-portal-backend"
canonical_frontend="lead-portal-frontend"

container_is_running() {
  [ "$(docker inspect --format '{{.State.Running}}' "$1" 2>/dev/null || true)" = "true" ]
}

if ! container_is_running "$canonical_backend" || ! container_is_running "$canonical_frontend"; then
  echo "Containers canônicos do Lead Portal ainda não estão ativos; reconciliação legada adiada."
  exit 0
fi

for stale_container in \
  marketinghub-lead-portal-backend-1 \
  marketinghub-lead-portal-frontend-1 \
  lead-portal-backend-1 \
  lead-portal-frontend-1 \
  lead-portal_backend_1 \
  lead-portal_frontend_1; do
  if ! docker inspect "$stale_container" >/dev/null 2>&1; then
    continue
  fi

  compose_service="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.service"}}' "$stale_container")"
  case "$compose_service" in
    backend|frontend)
      ;;
    *)
      echo "Container legado recusado por serviço inesperado: ${stale_container}:${compose_service}" >&2
      exit 1
      ;;
  esac

  published_ports="$(docker inspect --format '{{json .HostConfig.PortBindings}}' "$stale_container")"
  case "$published_ports" in
    null|'{}')
      ;;
    *)
      echo "Container legado possui porta publicada e não será removido automaticamente: ${stale_container}" >&2
      exit 1
      ;;
  esac

  docker rm -f "$stale_container" >/dev/null
  echo "Container legado removido com segurança: ${stale_container}"
done
