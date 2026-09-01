#!/usr/bin/env bash
set -euo pipefail

compose_files=(-f docker-compose.yml -f docker-compose.deploy.yml)

if docker compose "${compose_files[@]}" up -d backend frontend; then
  exit 0
fi

echo "Recriação normal falhou; reconciliando somente os containers canônicos do Lead Portal." >&2
expected_workdir="$(pwd -P)"

for service_spec in backend:lead-portal-backend frontend:lead-portal-frontend; do
  expected_service="${service_spec%%:*}"
  container_name="${service_spec#*:}"

  if ! docker inspect "$container_name" >/dev/null 2>&1; then
    continue
  fi

  actual_service="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.service"}}' "$container_name")"
  actual_workdir="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' "$container_name")"
  published_ports="$(docker inspect --format '{{json .HostConfig.PortBindings}}' "$container_name")"

  if [ "$actual_service" != "$expected_service" ] || [ "$actual_workdir" != "$expected_workdir" ]; then
    echo "Container canônico recusado por propriedade inesperada: ${container_name}:${actual_service}:${actual_workdir}" >&2
    exit 1
  fi

  case "$published_ports" in
    null|'{}')
      ;;
    *)
      echo "Container canônico possui porta publicada e não será reconciliado automaticamente: ${container_name}" >&2
      exit 1
      ;;
  esac

  if ! timeout 90 docker stop --time 30 "$container_name" >/dev/null; then
    timeout 30 docker kill "$container_name" >/dev/null
  fi
  timeout 60 docker rm -f "$container_name" >/dev/null
done

docker compose "${compose_files[@]}" up -d backend frontend
