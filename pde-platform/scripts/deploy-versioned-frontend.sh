#!/usr/bin/env bash
set -Eeuo pipefail

# Promove somente uma superfície PDE: valida a candidata antes da troca e restaura a imagem
# anterior quando qualquer verificação pós-cutover falha.
target="${1:?Informe o target da superfície PDE.}"
expected_image="${2:?Informe a imagem imutável esperada.}"
expected_commit="${3:?Informe o commit esperado.}"
expected_source_sha256="${4:?Informe o fingerprint esperado da fonte.}"
release_contract="${5:?Informe o contrato que autorizou a publicação.}"
release_contract_sha256="${6:?Informe o SHA-256 do contrato.}"
receipt_file="${7:?Informe o destino do recibo de publicação.}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
inventory="${PDE_RUNTIME_INVENTORY:-${repository_root}/pde-platform/contracts/product-runtime-isolation-v1.json}"
contract_tool="${script_dir}/pde_release_contract.py"
compose_file="${PDE_DEPLOY_COMPOSE_FILE_PATH:-${repository_root}/pde-platform/docker-compose.deploy.yml}"
compose_project="${COMPOSE_PROJECT_NAME:-metodo-musa-pde-platform}"
temporary_dir="$(mktemp -d)"
candidate_name=""
candidate_id=""
expected_image_id=""
old_container_id=""
old_image_id=""
old_image_reference=""
old_diagnostics="${temporary_dir}/old-diagnostics.json"
new_diagnostics="${temporary_dir}/new-diagnostics.json"
protected_before_file="${temporary_dir}/protected-before.tsv"
cutover_started=false
rollback_in_progress=false
deployment_status="PRECHECK_FAILED"
failure_message=""
service_name="unknown"
container_name="unknown"
image_variable="unknown"
port_variable="unknown"
default_host_port=""
public_url=""
experience_version=""
product_slug=""
backend_probe_path=""
edge_reload_required="${PDE_DEPLOY_EDGE_RELOAD_REQUIRED:-false}"
public_validation_required="${PDE_DEPLOY_PUBLIC_VALIDATION_REQUIRED:-${edge_reload_required}}"
public_probe_url="${PDE_DEPLOY_PUBLIC_PROBE_URL:-}"
public_probe_container="${PDE_DEPLOY_PUBLIC_PROBE_CONTAINER:-}"
edge_proxy_candidates="${PDE_DEPLOY_EDGE_PROXY_CONTAINERS:-lead-portal-payments-service-proxy-1 lead-portal-payments-service_proxy_1 lead-portal-payments-proxy-1 lead-portal-payments_proxy_1 proxy}"

compose=(docker compose -p "${compose_project}" -f "${compose_file}")

cleanup_candidate() {
  if [[ -n "${candidate_name}" ]] && docker inspect "${candidate_name}" >/dev/null 2>&1; then
    docker rm -f "${candidate_name}" >/dev/null 2>&1 || true
  fi
}

json_field() {
  local file="$1"
  local field="$2"
  python3 - "${file}" "${field}" <<'PY'
import json
from pathlib import Path
import sys

value = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8")).get(sys.argv[2], "")
print(value if value is not None else "")
PY
}

write_receipt() {
  local status="$1"
  local message="$2"
  install -d "$(dirname "${receipt_file}")"
  PDE_RECEIPT_STATUS="${status}" \
    PDE_RECEIPT_MESSAGE="${message}" \
    PDE_RECEIPT_TARGET="${target}" \
    PDE_RECEIPT_SERVICE="${service_name}" \
    PDE_RECEIPT_CONTRACT="${release_contract}" \
    PDE_RECEIPT_CONTRACT_SHA256="${release_contract_sha256}" \
    PDE_RECEIPT_SOURCE_SHA256="${expected_source_sha256}" \
    PDE_RECEIPT_COMMIT="${expected_commit}" \
    PDE_RECEIPT_EXPECTED_IMAGE="${expected_image}" \
    PDE_RECEIPT_EXPECTED_IMAGE_ID="${expected_image_id}" \
    PDE_RECEIPT_PREVIOUS_CONTAINER_ID="${old_container_id}" \
    PDE_RECEIPT_PREVIOUS_IMAGE="${old_image_reference}" \
    PDE_RECEIPT_PREVIOUS_IMAGE_ID="${old_image_id}" \
    PDE_RECEIPT_CANDIDATE_ID="${candidate_id}" \
    PDE_RECEIPT_PROTECTED="${protected_before_file}" \
    python3 - "${receipt_file}" <<'PY'
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import sys

protected = {}
path = Path(os.environ["PDE_RECEIPT_PROTECTED"])
if path.exists():
    for line in path.read_text(encoding="utf-8").splitlines():
        if "\t" in line:
            name, identifier = line.split("\t", 1)
            protected[name] = identifier

document = {
    "schemaVersion": "pde-release-receipt.v1",
    "recordedAt": datetime.now(timezone.utc).isoformat(),
    "status": os.environ["PDE_RECEIPT_STATUS"],
    "message": os.environ["PDE_RECEIPT_MESSAGE"],
    "target": os.environ["PDE_RECEIPT_TARGET"],
    "service": os.environ["PDE_RECEIPT_SERVICE"],
    "releaseContract": os.environ["PDE_RECEIPT_CONTRACT"],
    "releaseContractSha256": os.environ["PDE_RECEIPT_CONTRACT_SHA256"],
    "frontendSourceSha256": os.environ["PDE_RECEIPT_SOURCE_SHA256"],
    "commitSha": os.environ["PDE_RECEIPT_COMMIT"],
    "image": os.environ["PDE_RECEIPT_EXPECTED_IMAGE"],
    "imageId": os.environ["PDE_RECEIPT_EXPECTED_IMAGE_ID"],
    "candidateContainerId": os.environ["PDE_RECEIPT_CANDIDATE_ID"],
    "previous": {
        "containerId": os.environ["PDE_RECEIPT_PREVIOUS_CONTAINER_ID"],
        "image": os.environ["PDE_RECEIPT_PREVIOUS_IMAGE"],
        "imageId": os.environ["PDE_RECEIPT_PREVIOUS_IMAGE_ID"],
    },
    "unchangedContainers": protected,
}
Path(sys.argv[1]).write_text(
    json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
PY
}

wait_healthy() {
  local container="$1"
  local state=""
  local health=""
  for _attempt in $(seq 1 90); do
    state="$(docker inspect --format '{{.State.Status}}' "${container}" 2>/dev/null || true)"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${container}" 2>/dev/null || true)"
    if [[ "${state}" == "running" && ("${health}" == "healthy" || "${health}" == "none") ]]; then
      return 0
    fi
    if [[ "${state}" == "exited" || "${health}" == "unhealthy" ]]; then
      docker logs --tail=120 "${container}" >&2 || true
      return 1
    fi
    sleep 1
  done
  docker inspect "${container}" --format '{{json .State}}' >&2 || true
  return 1
}

capture_diagnostics() {
  local container="$1"
  local output="$2"
  docker exec "${container}" wget --quiet --output-document=- \
    http://127.0.0.1/version-diagnostics.json >"${output}"
}

capture_public_diagnostics() {
  local output="$1"
  local separator='?'
  local url="${public_probe_url}"
  if [[ "${url}" == *'?'* ]]; then
    separator='&'
  fi
  for _attempt in $(seq 1 30); do
    if [[ -n "${public_probe_container}" ]] \
      && docker exec "${public_probe_container}" wget --quiet --output-document=- \
        "${url}${separator}release=${expected_commit}" >"${output}"; then
      return 0
    elif [[ -z "${public_probe_container}" ]] \
      && curl --fail --silent --show-error --location \
        --connect-timeout 5 --max-time 10 \
        --header 'Cache-Control: no-cache' \
        "${url}${separator}release=${expected_commit}" >"${output}"; then
      return 0
    fi
    sleep 1
  done
  echo "[ARQUITETURA] A identidade pública de ${target} não ficou disponível em ${url}." >&2
  return 1
}

validate_candidate_or_promoted() {
  local container="$1"
  local output="$2"
  wait_healthy "${container}"
  capture_diagnostics "${container}" "${output}"
  python3 "${contract_tool}" --inventory "${inventory}" validate-diagnostics \
    --target "${target}" \
    --diagnostics "${output}" \
    --expected-image "${expected_image}" \
    --expected-commit "${expected_commit}" \
    --expected-source "${expected_source_sha256}"
  docker exec "${container}" wget --quiet --output-document=/dev/null \
    "http://127.0.0.1${backend_probe_path}"
  local actual_image_id
  actual_image_id="$(docker inspect --format '{{.Image}}' "${container}")"
  [[ "${actual_image_id}" == "${expected_image_id}" ]]
}

snapshot_protected_containers() {
  : >"${protected_before_file}"
  while IFS= read -r protected_name; do
    [[ -n "${protected_name}" && "${protected_name}" != "${container_name}" ]] || continue
    if docker inspect "${protected_name}" >/dev/null 2>&1; then
      printf '%s\t%s\n' "${protected_name}" \
        "$(docker inspect --format '{{.Id}}' "${protected_name}")" >>"${protected_before_file}"
    fi
  done < <(
    python3 - "${inventory}" <<'PY'
import json
from pathlib import Path
import sys

inventory = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
for product in inventory.get("products", []):
    for surface in product.get("surfaces", []):
        print(surface["containerName"])
for shared in inventory.get("sharedInfrastructure", []):
    if shared != "public-edge-proxy":
        print(shared)
PY
  )
}

verify_protected_containers() {
  local protected_name
  local before_id
  local after_id
  while IFS=$'\t' read -r protected_name before_id; do
    [[ -n "${protected_name}" ]] || continue
    after_id="$(docker inspect --format '{{.Id}}' "${protected_name}" 2>/dev/null || true)"
    if [[ "${after_id}" != "${before_id}" ]]; then
      echo "[ARQUITETURA] A publicação de ${target} alterou ${protected_name}." >&2
      return 1
    fi
  done <"${protected_before_file}"
}

reload_public_edge_proxies() {
  local found=false
  local proxy
  local candidates
  local -a proxies
  candidates="${edge_proxy_candidates} $(docker ps --filter publish=443 --format '{{.Names}}' || true)"
  read -r -a proxies <<<"${candidates}"
  for proxy in "${proxies[@]}"; do
    [[ -n "${proxy}" ]] || continue
    if ! docker inspect "${proxy}" >/dev/null 2>&1; then
      continue
    fi
    found=true
    if [[ "$(docker inspect --format '{{.State.Running}}' "${proxy}")" != "true" ]]; then
      docker start "${proxy}" >/dev/null
    fi
    if [[ -n "${PDE_PLATFORM_NETWORK:-}" ]]; then
      docker network connect "${PDE_PLATFORM_NETWORK}" "${proxy}" >/dev/null 2>&1 || true
    fi
    docker kill -s HUP "${proxy}" >/dev/null
  done
  if [[ "${edge_reload_required}" == "true" && "${found}" != "true" ]]; then
    echo '[ARQUITETURA] Nenhum proxy público foi encontrado para concluir a troca da superfície PDE.' >&2
    return 1
  fi
}

validate_public_promotion() {
  if [[ "${public_validation_required}" != "true" ]]; then
    return 0
  fi
  local output="${temporary_dir}/public-diagnostics.json"
  capture_public_diagnostics "${output}"
  python3 "${contract_tool}" --inventory "${inventory}" validate-diagnostics \
    --target "${target}" \
    --diagnostics "${output}" \
    --expected-image "${expected_image}" \
    --expected-commit "${expected_commit}" \
    --expected-source "${expected_source_sha256}"
}

restore_previous() {
  rollback_in_progress=true
  docker rm -f "${container_name}" >/dev/null 2>&1 || true
  if [[ -z "${old_container_id}" || -z "${old_image_reference}" ]]; then
    deployment_status="ROLLBACK_UNAVAILABLE"
    return 1
  fi

  local old_commit
  local old_tag
  local old_deployed_at
  local rollback_diagnostics="${temporary_dir}/rollback-diagnostics.json"
  old_commit="$(json_field "${old_diagnostics}" commitSha)"
  old_tag="$(json_field "${old_diagnostics}" imageTag)"
  old_deployed_at="$(json_field "${old_diagnostics}" deployedAt)"

  env \
    "${image_variable}=${old_image_reference}" \
    PDE_DEPLOY_COMMIT_SHA="${old_commit}" \
    PDE_DEPLOY_IMAGE_TAG="${old_tag}" \
    PDE_DEPLOY_DEPLOYED_AT="${old_deployed_at}" \
    "${compose[@]}" up -d --no-deps --wait --wait-timeout 180 "${service_name}"
  wait_healthy "${container_name}"
  capture_diagnostics "${container_name}" "${rollback_diagnostics}"
  python3 "${contract_tool}" validate-rollback \
    --before "${old_diagnostics}" --after "${rollback_diagnostics}"
  [[ "$(docker inspect --format '{{.Image}}' "${container_name}")" == "${old_image_id}" ]]
  reload_public_edge_proxies
  if [[ "${public_validation_required}" == "true" ]]; then
    local public_rollback_diagnostics="${temporary_dir}/public-rollback-diagnostics.json"
    capture_public_diagnostics "${public_rollback_diagnostics}"
    python3 "${contract_tool}" validate-rollback \
      --before "${old_diagnostics}" --after "${public_rollback_diagnostics}"
  fi
  deployment_status="ROLLED_BACK"
}

on_error() {
  local exit_code=$?
  trap - ERR
  failure_message="Falha na promoção transacional da superfície ${target}."
  cleanup_candidate
  if [[ "${cutover_started}" == "true" && "${rollback_in_progress}" != "true" ]]; then
    if ! restore_previous; then
      deployment_status="ROLLBACK_FAILED"
      failure_message="Falha na promoção de ${target} e na restauração automática."
    fi
  fi
  write_receipt "${deployment_status}" "${failure_message}" || true
  rm -rf "${temporary_dir}"
  echo "[ARQUITETURA] ${failure_message} status=${deployment_status}" >&2
  exit "${exit_code}"
}

trap on_error ERR

python3 "${contract_tool}" --inventory "${inventory}" validate-inventory
IFS=$'\t' read -r service_name container_name image_variable port_variable default_host_port \
  public_url experience_version product_slug backend_probe_path < <(
    python3 "${contract_tool}" --inventory "${inventory}" surface --target "${target}"
  )
if [[ -z "${public_probe_url}" ]]; then
  public_probe_url="${public_url%/}/version-diagnostics.json"
fi

[[ "${expected_commit}" =~ ^[0-9a-f]{40}$ ]]
[[ "${expected_source_sha256}" =~ ^[0-9a-f]{64}$ ]]
[[ "${release_contract_sha256}" =~ ^[0-9a-f]{64}$ ]]
[[ "${expected_image}" == *":${expected_commit}" ]]
[[ -f "${release_contract}" ]]
[[ "$(sha256sum "${release_contract}" | awk '{print $1}')" == "${release_contract_sha256}" ]]
if [[ "${public_validation_required}" == "true" && -z "${public_probe_container}" ]]; then
  command -v curl >/dev/null
fi
python3 "${contract_tool}" --inventory "${inventory}" validate-release \
  --target "${target}" \
  --contract "${release_contract}" \
  --expected-source "${expected_source_sha256}"

configured_image="${!image_variable-}"
if [[ "${configured_image}" != "${expected_image}" ]]; then
  echo "[ARQUITETURA] ${image_variable} não aponta para a imagem autorizada de ${target}." >&2
  false
fi
host_port="${!port_variable-}"
host_port="${host_port:-${default_host_port}}"

if ! docker inspect pde-platform-backend >/dev/null 2>&1; then
  echo '[ARQUITETURA] Backend PDE compartilhado ausente; frontend não será promovido.' >&2
  false
fi
if [[ "$(docker inspect --format '{{.State.Status}}' pde-platform-backend)" != "running" ]]; then
  echo '[ARQUITETURA] Backend PDE compartilhado não está em execução.' >&2
  false
fi

if [[ "${PDE_DEPLOY_SKIP_PULL:-false}" != "true" ]]; then
  docker pull "${expected_image}"
fi
expected_image_id="$(docker image inspect --format '{{.Id}}' "${expected_image}")"
snapshot_protected_containers

while IFS= read -r publisher; do
  [[ -n "${publisher}" ]] || continue
  if [[ "${publisher}" != "${container_name}" && ! ("${target}" == "v5" && "${publisher}" == "pde-platform-frontend") ]]; then
    echo "[ARQUITETURA] Porta ${host_port} pertence ao container inesperado ${publisher}." >&2
    false
  fi
done < <(docker ps --filter "publish=${host_port}" --format '{{.Names}}')

candidate_name="${container_name}-candidate-${expected_commit:0:12}-$$"
"${compose[@]}" run -d --no-deps --name "${candidate_name}" "${service_name}" >/dev/null
candidate_id="$(docker inspect --format '{{.Id}}' "${candidate_name}")"
validate_candidate_or_promoted "${candidate_name}" "${temporary_dir}/candidate-diagnostics.json"
verify_protected_containers

if docker inspect "${container_name}" >/dev/null 2>&1; then
  old_container_id="$(docker inspect --format '{{.Id}}' "${container_name}")"
  old_image_id="$(docker inspect --format '{{.Image}}' "${container_name}")"
  old_image_reference="$(docker inspect --format '{{.Config.Image}}' "${container_name}")"
  capture_diagnostics "${container_name}" "${old_diagnostics}"
fi

cutover_started=true
if [[ "${target}" == "v5" ]]; then
  docker rm -f pde-platform-frontend >/dev/null 2>&1 || true
fi
docker rm -f "${container_name}" >/dev/null 2>&1 || true
"${compose[@]}" up -d --no-deps --wait --wait-timeout 180 "${service_name}"
validate_candidate_or_promoted "${container_name}" "${new_diagnostics}"
reload_public_edge_proxies
validate_public_promotion

if [[ "${PDE_DEPLOY_TEST_MODE:-false}" == "true" \
  && "${PDE_DEPLOY_TEST_FAIL_AFTER_SWITCH:-false}" == "true" ]]; then
  echo '[ARQUITETURA] Falha pós-troca injetada pela homologação local.' >&2
  false
fi

verify_protected_containers
deployment_status="PROMOTED"
cleanup_candidate
write_receipt "${deployment_status}" \
  "Candidata validada e promovida sem recriar outros runtimes PDE."
trap - ERR
rm -rf "${temporary_dir}"
printf 'Superfície PDE promovida: target=%s service=%s image=%s publicUrl=%s experienceVersion=%s product=%s\n' \
  "${target}" "${service_name}" "${expected_image}" "${public_url}" "${experience_version}" "${product_slug}"
