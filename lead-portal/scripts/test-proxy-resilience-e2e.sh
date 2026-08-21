#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
suffix="$$"
network="mh-lp-resilience-${suffix}"
cache_volume="mh-lp-resilience-cache-${suffix}"
backend="mh-lp-resilience-backend-${suffix}"
frontend="mh-lp-resilience-frontend-${suffix}"
proxy="mh-lp-resilience-proxy-${suffix}"
proxy_alias="mh-lp-resilience-proxy"
test_dir="$(mktemp -d)"

stop_and_remove_container() {
  local container="$1"
  if ! docker container inspect "$container" >/dev/null 2>&1; then
    return
  fi
  if [ "$(docker inspect --format '{{.State.Running}}' "$container")" = "true" ]; then
    docker stop "$container" >/dev/null
  fi
  docker rm "$container" >/dev/null
}

cleanup() {
  stop_and_remove_container "$proxy"
  stop_and_remove_container "$backend"
  stop_and_remove_container "$frontend"
  docker network rm "$network" >/dev/null 2>&1 || true
  docker volume rm "$cache_volume" >/dev/null 2>&1 || true
  rm -r "$test_dir"
}
trap cleanup EXIT HUP INT TERM

create_proxy() {
  docker create \
    --name "$proxy" \
    --network "$network" \
    --network-alias "$proxy_alias" \
    --add-host host.docker.internal:host-gateway \
    -v "${cache_volume}:/var/cache/nginx/landing" \
    nginx:1.25-alpine >/dev/null
  docker cp "${repo_root}/lead-portal/nginx.conf" "${proxy}:/etc/nginx/conf.d/default.conf"
  docker cp "${test_dir}/letsencrypt/." "${proxy}:/etc/letsencrypt"
  docker start "$proxy" >/dev/null
  docker exec "$proxy" nginx -t >/dev/null
}

mkdir -p "${test_dir}/letsencrypt/live/oportunidadebrasil.shop"
openssl req \
  -x509 \
  -nodes \
  -newkey rsa:2048 \
  -days 1 \
  -subj '/CN=oportunidadebrasil.shop' \
  -keyout "${test_dir}/letsencrypt/live/oportunidadebrasil.shop/privkey.pem" \
  -out "${test_dir}/letsencrypt/live/oportunidadebrasil.shop/fullchain.pem" \
  >/dev/null 2>&1

docker network create "$network" >/dev/null
docker volume create "$cache_volume" >/dev/null
docker run -d \
  --name "$backend" \
  --network "$network" \
  --network-alias lead-portal-backend \
  hashicorp/http-echo:1.0.0 \
  -listen=:8080 \
  -text='<html><body data-mh-landing-analytics="true"><img data-mh-web-optimized="true"><a data-analytics-role="primary-checkout">Comprar</a></body></html>' \
  >/dev/null
docker run -d \
  --name "$frontend" \
  --network "$network" \
  --network-alias lead-portal-frontend \
  nginx:1.25-alpine \
  >/dev/null
create_proxy

client_pids=()
for client in $(seq 1 8); do
  docker exec "$frontend" sh \
    -c "timeout 4 wget --no-check-certificate --header='Host: oportunidadebrasil.shop' -S -O /tmp/landing-${client} 'https://${proxy_alias}/flows/exp-88-gerasalespage-v1?mh_audit=concurrent-${client}' 2>/tmp/headers-${client} && grep -F data-mh-landing-analytics /tmp/landing-${client} >/dev/null && grep -F data-mh-web-optimized /tmp/landing-${client} >/dev/null && grep -F 'data-analytics-role=\"primary-checkout\"' /tmp/landing-${client} >/dev/null && grep -F 'HTTP/1.1 200' /tmp/headers-${client} && grep -i 'X-Marketing-Hub-Landing-Cache:' /tmp/headers-${client}" \
    >"${test_dir}/client-${client}.out" 2>&1 &
  client_pids+=("$!")
done

for client_pid in "${client_pids[@]}"; do
  wait "$client_pid"
done

test "$(grep -h -c 'HTTP/1.1 200' "${test_dir}"/client-*.out | awk '{sum += $1} END {print sum}')" = 8
test "$(grep -h -c 'Landing-Cache: MISS' "${test_dir}"/client-*.out | awk '{sum += $1} END {print sum}')" = 1
test "$(grep -h -c 'Landing-Cache: HIT' "${test_dir}"/client-*.out | awk '{sum += $1} END {print sum}')" = 7

docker stop "$proxy" "$backend" >/dev/null
docker rm "$proxy" >/dev/null
create_proxy

docker exec "$frontend" sh \
  -c "timeout 4 wget --no-check-certificate --header='Host: oportunidadebrasil.shop' -S -O /tmp/landing-persistent 'https://${proxy_alias}/flows/exp-88-gerasalespage-v1?mh_audit=after-proxy-restart' 2>/tmp/headers-persistent && grep -F data-mh-landing-analytics /tmp/landing-persistent >/dev/null && grep -F data-mh-web-optimized /tmp/landing-persistent >/dev/null && grep -F 'data-analytics-role=\"primary-checkout\"' /tmp/landing-persistent >/dev/null && grep -F 'HTTP/1.1 200' /tmp/headers-persistent && grep -i 'X-Marketing-Hub-Landing-Cache: HIT' /tmp/headers-persistent" \
  >"${test_dir}/persistent-cache.out"

grep -F 'HTTP/1.1 200' "${test_dir}/persistent-cache.out" >/dev/null
echo "Resiliência ponta a ponta do proxy da landing aprovada."
