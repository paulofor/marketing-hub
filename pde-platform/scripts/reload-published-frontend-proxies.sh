#!/usr/bin/env bash
set -euo pipefail

# Atualiza somente o DNS dos proxies PDE já ativos, preservando imagens e versões publicadas.
network="${PDE_PLATFORM_NETWORK:?Informe a rede canônica dos frontends PDE}"
reloaded=0
for service in pde-platform-frontend-v5 pde-platform-frontend-v6 pde-platform-frontend-v7 \
  pde-platform-frontend-mira pde-platform-frontend-kit-whatsapp; do
  containers=$(docker ps --filter "network=$network" \
    --filter "label=com.docker.compose.service=$service" --format '{{.ID}}')
  while IFS= read -r container; do
    [[ -n "$container" ]] || continue
    printf 'Revalidando proxy PDE: service=%s container=%s\n' "$service" "$container"
    docker exec "$container" nginx -t
    docker exec "$container" nginx -s reload
    # A API precisa atravessar o proxy; saúde de HTML estático não comprova conexão com o backend.
    healthy=false
    for attempt in {1..15}; do
      if [[ "$service" == pde-platform-frontend-mira ]]; then
        probe=/api/pde/mira/private/v1/contract
      elif [[ "$service" == pde-platform-frontend-kit-whatsapp ]]; then
        probe=/api/pde/products/kit-whatsapp-pronto
      else
        probe=/api/pde/products/metodo-musa-7-dias
      fi
      if docker exec "$container" wget --quiet --output-document=/dev/null "http://127.0.0.1$probe"; then
        healthy=true
        break
      fi
      sleep 1
    done
    if [[ "$healthy" != true ]]; then
      printf 'Proxy PDE sem backend após reload: service=%s container=%s endpoint=%s\n' "$service" "$container" "$probe" >&2
      exit 1
    fi
    reloaded=$((reloaded + 1))
  done <<< "$containers"
done
printf 'Proxies PDE revalidados: %s. Imagens e versões preservadas.\n' "$reloaded"
