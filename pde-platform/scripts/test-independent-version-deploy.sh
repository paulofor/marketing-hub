#!/usr/bin/env bash
set -Eeuo pipefail

# Homologa em Docker real a promoção isolada, o preflight e o rollback exato.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
fixture_dir="${script_dir}/fixtures/independent-version-deploy"
compose_file="${fixture_dir}/docker-compose.yml"
: "${PDE_LOCAL_COMPOSE_PROJECT:?PDE_LOCAL_COMPOSE_PROJECT obrigatório para isolar a homologação Docker}"
project="${PDE_LOCAL_COMPOSE_PROJECT}"
temporary_dir="$(mktemp -d)"
network_name="${project}-pde-net"

old_commit="1111111111111111111111111111111111111111"
frontend_commit="2222222222222222222222222222222222222222"
frontend_failure_commit="3333333333333333333333333333333333333333"
backend_commit="4444444444444444444444444444444444444444"
backend_failure_commit="5555555555555555555555555555555555555555"
worker_commit="6666666666666666666666666666666666666666"
old_source="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
frontend_source="09e282435e2496f1a4fc96c1fbd90dbb8c368a3b87114a9b3af8899c3f10ef42"
frontend_failure_source="cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"

old_frontend_image="local/pde-test-frontend-v8:${old_commit}"
new_frontend_image="local/pde-test-frontend-v8:${frontend_commit}"
failure_frontend_image="local/pde-test-frontend-v8:${frontend_failure_commit}"
old_backend_image="local/pde-test-backend:${old_commit}"
new_backend_image="local/pde-test-backend:${backend_commit}"
failure_backend_image="local/pde-test-backend:${backend_failure_commit}"
new_worker_image="local/pde-test-ai-worker:${worker_commit}"
edge_image="local/pde-test-edge:${old_commit}"

compose=(docker compose -p "${project}" -f "${compose_file}")

cleanup() {
  "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  rm -rf "${temporary_dir}"
}
trap cleanup EXIT

container_id() {
  docker inspect --format '{{.Id}}' "$1"
}

image_id() {
  docker inspect --format '{{.Image}}' "$1"
}

assert_same_id() {
  local name="$1"
  local expected="$2"
  local observed
  observed="$(container_id "${name}")"
  if [[ "${observed}" != "${expected}" ]]; then
    echo "[ARQUITETURA] Container ${name} foi recriado fora do escopo." >&2
    return 1
  fi
}

assert_receipt_status() {
  local receipt="$1"
  local expected="$2"
  local observed
  observed="$(python3 - "${receipt}" <<'PY'
import json
from pathlib import Path
import sys

print(json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))["status"])
PY
)"
  [[ "${observed}" == "${expected}" ]]
}

assert_container_environment() {
  local container="$1"
  local expected="$2"
  if ! docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "${container}" \
    | grep -Fxq "${expected}"; then
    echo "[ARQUITETURA] ${container} não preservou ${expected}." >&2
    return 1
  fi
}

build_frontend() {
  local image="$1"
  local source_sha256="$2"
  docker build --quiet \
    --file "${fixture_dir}/Dockerfile.frontend" \
    --build-arg "FRONTEND_SOURCE_SHA256=${source_sha256}" \
    --tag "${image}" \
    "${fixture_dir}" >/dev/null
}

build_backend() {
  local image="$1"
  local marker="$2"
  docker build --quiet \
    --file "${fixture_dir}/Dockerfile.backend" \
    --build-arg "RELEASE_MARKER=${marker}" \
    --tag "${image}" \
    "${fixture_dir}" >/dev/null
}

docker version >/dev/null
docker buildx version >/dev/null
docker compose version >/dev/null

build_frontend "${old_frontend_image}" "${old_source}"
build_frontend "${new_frontend_image}" "${frontend_source}"
build_frontend "${failure_frontend_image}" "${frontend_source}"
build_backend "${old_backend_image}" old
build_backend "${new_backend_image}" current
build_backend "${failure_backend_image}" failure
docker build --quiet \
  --file "${fixture_dir}/Dockerfile.edge" \
  --tag "${edge_image}" \
  "${fixture_dir}" >/dev/null
docker tag "${new_backend_image}" "${new_worker_image}"

for version in v5 v6 v7; do
  docker tag "${old_frontend_image}" "local/pde-test-frontend-${version}:${old_commit}"
done
docker tag "${old_backend_image}" "local/pde-test-ai-worker:${old_commit}"
docker tag "${old_backend_image}" "local/pde-test-retention-worker:${old_commit}"

export COMPOSE_PROJECT_NAME="${project}"
export PDE_DEPLOY_COMPOSE_FILE_PATH="${compose_file}"
export PDE_RUNTIME_INVENTORY="${repository_root}/pde-platform/contracts/product-runtime-isolation-v1.json"
export PDE_DEPLOY_SKIP_PULL=true
export PDE_TEST_NETWORK="${network_name}"
export PDE_PLATFORM_NETWORK="${network_name}"
export PDE_PLATFORM_FRONTEND_V5_PORT=15176
export PDE_PLATFORM_FRONTEND_V6_PORT=15177
export PDE_PLATFORM_FRONTEND_V7_PORT=15178
export PDE_PLATFORM_FRONTEND_V8_PORT=15181
export PDE_PLATFORM_FRONTEND_V5_IMAGE="local/pde-test-frontend-v5:${old_commit}"
export PDE_PLATFORM_FRONTEND_V6_IMAGE="local/pde-test-frontend-v6:${old_commit}"
export PDE_PLATFORM_FRONTEND_V7_IMAGE="local/pde-test-frontend-v7:${old_commit}"
export PDE_PLATFORM_FRONTEND_V8_IMAGE="${old_frontend_image}"
export PDE_PLATFORM_BACKEND_IMAGE="${old_backend_image}"
export PDE_AI_WORKER_IMAGE="local/pde-test-ai-worker:${old_commit}"
export PDE_RETENTION_WORKER_IMAGE="local/pde-test-retention-worker:${old_commit}"
export PDE_TEST_EDGE_IMAGE="${edge_image}"
export PDE_DEPLOY_COMMIT_SHA="${old_commit}"
export PDE_DEPLOY_IMAGE_TAG="${old_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T00:00:00Z
export PDE_DEPLOY_EDGE_RELOAD_REQUIRED=true
export PDE_DEPLOY_EDGE_PROXY_CONTAINERS=pde-test-edge-proxy
export PDE_DEPLOY_PUBLIC_VALIDATION_REQUIRED=true
export PDE_DEPLOY_PUBLIC_PROBE_CONTAINER=pde-test-edge-proxy
export PDE_DEPLOY_PUBLIC_PROBE_URL=http://127.0.0.1/version-diagnostics.json

"${compose[@]}" config --quiet
"${compose[@]}" up -d --wait

v5_id="$(container_id pde-platform-frontend-v5)"
v6_id="$(container_id pde-platform-frontend-v6)"
v7_id="$(container_id pde-platform-frontend-v7)"
v8_old_id="$(container_id pde-platform-frontend-v8)"
backend_old_id="$(container_id pde-platform-backend)"
ai_old_id="$(container_id pde-ai-worker)"
retention_old_id="$(container_id pde-retention-worker)"
edge_old_id="$(container_id pde-test-edge-proxy)"
docker exec pde-test-edge-proxy wget -qO- \
  http://127.0.0.1/version-diagnostics.json | grep -Fq "${old_commit}"

contract="$(python3 - "${repository_root}/pde-platform/contracts" <<'PY'
import json
from pathlib import Path
import re
import sys

directory = Path(sys.argv[1])
candidates = []
for path in directory.glob("musa-v12-commercial-homologation-v*.json"):
    document = json.loads(path.read_text(encoding="utf-8"))
    version = document.get("contractVersion", "")
    match = re.search(r"(?:^|[.-])v([1-9][0-9]*)$", version)
    product = document.get("product", {})
    if (
        match
        and document.get("status") == "READY_FOR_INDEPENDENT_REVIEW"
        and product.get("slug") == "metodo-musa-7-dias"
        and product.get("experienceVersion") == "musa-pde-entry-v12-primeiro-ajuste-aplicavel"
    ):
        candidates.append((int(match.group(1)), path))

if not candidates:
    raise SystemExit("Nenhum manifesto MUSA v12 pronto para a homologação transacional")
latest_revision = max(revision for revision, _ in candidates)
latest = [path for revision, path in candidates if revision == latest_revision]
if len(latest) != 1:
    raise SystemExit(
        f"Mais de um manifesto MUSA v12 vigente na revisão v{latest_revision}"
    )
print(latest[0])
PY
)"
contract_sha256="$(sha256sum "${contract}" | awk '{print $1}')"

export PDE_PLATFORM_FRONTEND_V8_IMAGE="${new_frontend_image}"
export PDE_DEPLOY_COMMIT_SHA="${frontend_commit}"
export PDE_DEPLOY_IMAGE_TAG="${frontend_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T01:00:00Z
invalid_receipt="${temporary_dir}/frontend-invalid.json"
if bash "${script_dir}/deploy-versioned-frontend.sh" \
  v8 "${new_frontend_image}" "${frontend_commit}" "${frontend_failure_source}" \
  "${contract}" "${contract_sha256}" "${invalid_receipt}"; then
  echo '[ARQUITETURA] Candidata com fingerprint divergente foi aceita.' >&2
  exit 1
fi
assert_same_id pde-platform-frontend-v8 "${v8_old_id}"
assert_same_id pde-test-edge-proxy "${edge_old_id}"
assert_receipt_status "${invalid_receipt}" PRECHECK_FAILED

promoted_receipt="${temporary_dir}/frontend-promoted.json"
bash "${script_dir}/deploy-versioned-frontend.sh" \
  v8 "${new_frontend_image}" "${frontend_commit}" "${frontend_source}" \
  "${contract}" "${contract_sha256}" "${promoted_receipt}"
v8_promoted_id="$(container_id pde-platform-frontend-v8)"
[[ "${v8_promoted_id}" != "${v8_old_id}" ]]
assert_receipt_status "${promoted_receipt}" PROMOTED
assert_same_id pde-platform-frontend-v5 "${v5_id}"
assert_same_id pde-platform-frontend-v6 "${v6_id}"
assert_same_id pde-platform-frontend-v7 "${v7_id}"
assert_same_id pde-platform-backend "${backend_old_id}"
assert_same_id pde-ai-worker "${ai_old_id}"
assert_same_id pde-retention-worker "${retention_old_id}"
assert_same_id pde-test-edge-proxy "${edge_old_id}"
docker exec pde-test-edge-proxy wget -qO- \
  http://127.0.0.1/version-diagnostics.json | grep -Fq "${frontend_commit}"

promoted_image_id="$(image_id pde-platform-frontend-v8)"
export PDE_PLATFORM_FRONTEND_V8_IMAGE="${failure_frontend_image}"
export PDE_DEPLOY_COMMIT_SHA="${frontend_failure_commit}"
export PDE_DEPLOY_IMAGE_TAG="${frontend_failure_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T02:00:00Z
rollback_receipt="${temporary_dir}/frontend-rollback.json"
if PDE_DEPLOY_TEST_MODE=true PDE_DEPLOY_TEST_FAIL_AFTER_SWITCH=true \
  bash "${script_dir}/deploy-versioned-frontend.sh" \
    v8 "${failure_frontend_image}" "${frontend_failure_commit}" \
    "${frontend_source}" "${contract}" "${contract_sha256}" \
    "${rollback_receipt}"; then
  echo '[ARQUITETURA] Falha pós-cutover injetada não interrompeu a publicação.' >&2
  exit 1
fi
[[ "$(image_id pde-platform-frontend-v8)" == "${promoted_image_id}" ]]
assert_receipt_status "${rollback_receipt}" ROLLED_BACK
docker exec pde-platform-frontend-v8 wget -qO- \
  http://127.0.0.1/version-diagnostics.json | grep -Fq "${frontend_commit}"
assert_same_id pde-test-edge-proxy "${edge_old_id}"
docker exec pde-test-edge-proxy wget -qO- \
  http://127.0.0.1/version-diagnostics.json | grep -Fq "${frontend_commit}"

frontend_ids_before_backend="${temporary_dir}/frontend-before-backend.tsv"
for version in v5 v6 v7 v8; do
  printf '%s\t%s\n' "${version}" "$(container_id "pde-platform-frontend-${version}")" \
    >> "${frontend_ids_before_backend}"
done

export PDE_PLATFORM_BACKEND_IMAGE="${new_backend_image}"
export PDE_DEPLOY_COMMIT_SHA="${backend_commit}"
export PDE_DEPLOY_IMAGE_TAG="${backend_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T03:00:00Z
backend_receipt="${temporary_dir}/backend-promoted.json"
bash "${script_dir}/deploy-shared-component.sh" \
  backend "${new_backend_image}" "${backend_commit}" "${backend_receipt}"
assert_receipt_status "${backend_receipt}" PROMOTED
assert_container_environment pde-platform-backend \
  "PDE_PLATFORM_FRONTEND_V5_IMAGE=local/pde-test-frontend-v5:${old_commit}"
assert_container_environment pde-platform-backend \
  "PDE_PLATFORM_FRONTEND_V6_IMAGE=local/pde-test-frontend-v6:${old_commit}"
assert_container_environment pde-platform-backend \
  "PDE_PLATFORM_FRONTEND_V7_IMAGE=local/pde-test-frontend-v7:${old_commit}"
assert_container_environment pde-platform-backend \
  "PDE_PLATFORM_FRONTEND_V8_IMAGE=${new_frontend_image}"
assert_container_environment pde-platform-backend \
  "PDE_AI_WORKER_IMAGE=local/pde-test-ai-worker:${old_commit}"
while IFS=$'\t' read -r version identifier; do
  assert_same_id "pde-platform-frontend-${version}" "${identifier}"
  backend_response="$(docker exec "pde-platform-frontend-${version}" wget \
    --quiet --output-document=- \
    http://127.0.0.1/api/pde/products/metodo-musa-7-dias)"
  if [[ "${backend_response}" != *'"status":"UP"'* ]]; then
    echo "[ARQUITETURA] A versão ${version} não alcançou o backend promovido: ${backend_response}" >&2
    exit 1
  fi
done < "${frontend_ids_before_backend}"

backend_promoted_image_id="$(image_id pde-platform-backend)"
export PDE_PLATFORM_BACKEND_IMAGE="${failure_backend_image}"
export PDE_DEPLOY_COMMIT_SHA="${backend_failure_commit}"
export PDE_DEPLOY_IMAGE_TAG="${backend_failure_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T04:00:00Z
backend_rollback_receipt="${temporary_dir}/backend-rollback.json"
if PDE_DEPLOY_TEST_MODE=true PDE_DEPLOY_TEST_FAIL_AFTER_SWITCH=true \
  bash "${script_dir}/deploy-shared-component.sh" \
    backend "${failure_backend_image}" "${backend_failure_commit}" \
    "${backend_rollback_receipt}"; then
  echo '[ARQUITETURA] Falha de backend injetada não acionou rollback.' >&2
  exit 1
fi
[[ "$(image_id pde-platform-backend)" == "${backend_promoted_image_id}" ]]
assert_receipt_status "${backend_rollback_receipt}" ROLLED_BACK
assert_container_environment pde-platform-backend \
  "PDE_PLATFORM_FRONTEND_V8_IMAGE=${new_frontend_image}"
for version in v5 v6 v7 v8; do
  backend_response="$(docker exec "pde-platform-frontend-${version}" wget \
    --quiet --output-document=- \
    http://127.0.0.1/api/pde/products/metodo-musa-7-dias)"
  if [[ "${backend_response}" != *'"status":"UP"'* ]]; then
    echo "[ARQUITETURA] Rollback do backend não recuperou a versão ${version}." >&2
    exit 1
  fi
done

export PDE_AI_WORKER_IMAGE="${new_worker_image}"
export PDE_DEPLOY_COMMIT_SHA="${worker_commit}"
export PDE_DEPLOY_IMAGE_TAG="${worker_commit}"
export PDE_DEPLOY_DEPLOYED_AT=2026-09-18T05:00:00Z
worker_receipt="${temporary_dir}/worker-promoted.json"
bash "${script_dir}/deploy-shared-component.sh" \
  ai-worker "${new_worker_image}" "${worker_commit}" "${worker_receipt}"
assert_receipt_status "${worker_receipt}" PROMOTED
assert_same_id pde-retention-worker "${retention_old_id}"
while IFS=$'\t' read -r version identifier; do
  assert_same_id "pde-platform-frontend-${version}" "${identifier}"
done < "${frontend_ids_before_backend}"

echo 'Homologação transacional de versões PDE aprovada.'
