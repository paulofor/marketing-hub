#!/usr/bin/env bash

set -euo pipefail

image="${1:-harness-library-api:test}"
test_id="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-0}-$$"
secret_volume="harness-library-secret-runtime-${test_id}"
container="harness-library-secret-runtime-${test_id}"

cleanup() {
  docker container rm --force "${container}" >/dev/null 2>&1 || true
  docker volume rm --force "${secret_volume}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker volume create \
  --label com.marketinghub.test=harness-library-secret-runtime \
  "${secret_volume}" >/dev/null

docker run --rm \
  --user 0:0 \
  --mount "type=volume,src=${secret_volume},dst=/run/secrets" \
  --entrypoint sh \
  "${image}" \
  -c 'set -eu
      printf %s local-public-api-key-000000000000000000001 > /run/secrets/harness_library_api_key
      printf %s local-internal-signing-key-0000000000000001 > /run/secrets/harness_library_internal_signing_key
      chown 10001:10001 /run/secrets/harness_library_api_key /run/secrets/harness_library_internal_signing_key
      chmod 0400 /run/secrets/harness_library_api_key /run/secrets/harness_library_internal_signing_key'

docker run --detach \
  --name "${container}" \
  --user 10001:10001 \
  --read-only \
  --tmpfs /tmp:size=32m,mode=1777 \
  --security-opt no-new-privileges:true \
  --memory 384m \
  --env HARNESS_LIBRARY_API_KEY_FILE=/run/secrets/harness_library_api_key \
  --env HARNESS_LIBRARY_INTERNAL_SIGNING_KEY_FILE=/run/secrets/harness_library_internal_signing_key \
  --mount "type=volume,src=${secret_volume},dst=/run/secrets,readonly" \
  "${image}" >/dev/null

ready=false
for _ in $(seq 1 30); do
  if docker exec "${container}" \
    curl -fsS --max-time 3 http://127.0.0.1:9103/actuator/health/liveness \
    >/dev/null 2>&1; then
    ready=true
    break
  fi
  if [[ "$(docker inspect --format '{{.State.Status}}' "${container}")" != "running" ]]; then
    break
  fi
  sleep 1
done

if [[ "${ready}" != "true" ]]; then
  docker logs --tail=120 "${container}" >&2
  echo "Gateway não iniciou com secrets protegidos em arquivo." >&2
  exit 1
fi

configured_user="$(docker inspect --format '{{.Config.User}}' "${container}")"
[[ "${configured_user}" == "10001:10001" ]] \
  || { echo "Identidade inesperada no runtime: ${configured_user}" >&2; exit 1; }

for secret_file in harness_library_api_key harness_library_internal_signing_key; do
  permissions="$(docker exec "${container}" stat -c '%u:%g:%a' "/run/secrets/${secret_file}")"
  [[ "${permissions}" == "10001:10001:400" ]] \
    || { echo "Permissão inesperada no secret ${secret_file}: ${permissions}" >&2; exit 1; }
done

echo "Runtime não privilegiado leu secrets 0400 sem expor seu conteúdo."
