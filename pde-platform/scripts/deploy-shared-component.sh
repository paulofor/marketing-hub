#!/usr/bin/env bash
set -Eeuo pipefail

# Publica backend ou worker compartilhado sem recriar frontends nem os demais componentes.
component="${1:?Informe backend, ai-worker ou retention-worker.}"
expected_image="${2:?Informe a imagem imutável esperada.}"
expected_commit="${3:?Informe o commit esperado.}"
receipt_file="${4:?Informe o destino do recibo de publicação.}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
inventory="${PDE_RUNTIME_INVENTORY:-${repository_root}/pde-platform/contracts/product-runtime-isolation-v1.json}"
compose_file="${PDE_DEPLOY_COMPOSE_FILE_PATH:-${repository_root}/pde-platform/docker-compose.deploy.yml}"
compose_project="${COMPOSE_PROJECT_NAME:-metodo-musa-pde-platform}"
temporary_dir="$(mktemp -d)"
protected_before_file="${temporary_dir}/protected-before.tsv"
old_environment_file="${temporary_dir}/old-environment.txt"
old_container_id=""
old_image_id=""
old_image_reference=""
expected_image_id=""
deployment_status="PRECHECK_FAILED"
cutover_started=false
rollback_in_progress=false
service_name="unknown"
container_name="unknown"
image_variable="unknown"
proxy_reload_script="${script_dir}/reload-published-frontend-proxies.sh"

compose=(docker compose -p "${compose_project}" -f "${compose_file}")

case "${component}" in
  backend)
    service_name="pde-platform-backend"
    container_name="pde-platform-backend"
    image_variable="PDE_PLATFORM_BACKEND_IMAGE"
    ;;
  ai-worker)
    service_name="pde-ai-worker"
    container_name="pde-ai-worker"
    image_variable="PDE_AI_WORKER_IMAGE"
    ;;
  retention-worker)
    service_name="pde-retention-worker"
    container_name="pde-retention-worker"
    image_variable="PDE_RETENTION_WORKER_IMAGE"
    ;;
  *)
    echo "[ARQUITETURA] Componente PDE compartilhado inválido: ${component}" >&2
    exit 1
    ;;
esac

environment_value() {
  local name="$1"
  sed -n "s/^${name}=//p" "${old_environment_file}" | tail -n 1
}

write_receipt() {
  local status="$1"
  local message="$2"
  install -d "$(dirname "${receipt_file}")"
  PDE_RECEIPT_STATUS="${status}" \
    PDE_RECEIPT_MESSAGE="${message}" \
    PDE_RECEIPT_COMPONENT="${component}" \
    PDE_RECEIPT_SERVICE="${service_name}" \
    PDE_RECEIPT_COMMIT="${expected_commit}" \
    PDE_RECEIPT_IMAGE="${expected_image}" \
    PDE_RECEIPT_IMAGE_ID="${expected_image_id}" \
    PDE_RECEIPT_PREVIOUS_CONTAINER_ID="${old_container_id}" \
    PDE_RECEIPT_PREVIOUS_IMAGE="${old_image_reference}" \
    PDE_RECEIPT_PREVIOUS_IMAGE_ID="${old_image_id}" \
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
    "component": os.environ["PDE_RECEIPT_COMPONENT"],
    "service": os.environ["PDE_RECEIPT_SERVICE"],
    "commitSha": os.environ["PDE_RECEIPT_COMMIT"],
    "image": os.environ["PDE_RECEIPT_IMAGE"],
    "imageId": os.environ["PDE_RECEIPT_IMAGE_ID"],
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

wait_component() {
  local container="$1"
  local state=""
  local health=""
  local stable=0
  for _attempt in $(seq 1 120); do
    state="$(docker inspect --format '{{.State.Status}}' "${container}" 2>/dev/null || true)"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${container}" 2>/dev/null || true)"
    if [[ "${state}" == "exited" || "${health}" == "unhealthy" ]]; then
      docker logs --tail=160 "${container}" >&2 || true
      return 1
    fi
    if [[ "${state}" == "running" && "${health}" == "healthy" ]]; then
      return 0
    fi
    if [[ "${state}" == "running" && "${health}" == "none" ]]; then
      stable=$((stable + 1))
      if ((stable >= 10)); then
        return 0
      fi
    else
      stable=0
    fi
    sleep 1
  done
  docker inspect "${container}" --format '{{json .State}}' >&2 || true
  return 1
}

validate_component() {
  local actual_image_id
  wait_component "${container_name}"
  actual_image_id="$(docker inspect --format '{{.Image}}' "${container_name}")"
  [[ "${actual_image_id}" == "${expected_image_id}" ]]
  if [[ "${component}" == "backend" ]]; then
    docker exec "${container_name}" sh -ec '
      if command -v curl >/dev/null 2>&1; then
        curl --fail --silent http://127.0.0.1:8096/actuator/health
      else
        wget --quiet --output-document=- http://127.0.0.1:8096/actuator/health
      fi
    ' | grep -q '"status":"UP"'
    docker exec "${container_name}" sh -ec '
      if command -v curl >/dev/null 2>&1; then
        curl --fail --silent http://127.0.0.1:8096/api/pde/build-identity
      else
        wget --quiet --output-document=- http://127.0.0.1:8096/api/pde/build-identity
      fi
    ' | grep -Fq "${expected_commit}"
  fi
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
      echo "[ARQUITETURA] O deploy de ${component} alterou ${protected_name}." >&2
      return 1
    fi
  done <"${protected_before_file}"
}

reload_backend_consumers() {
  if [[ "${component}" != "backend" ]]; then
    return 0
  fi
  if [[ ! -f "${proxy_reload_script}" ]]; then
    echo '[ARQUITETURA] Recarregador dos proxies PDE não está disponível.' >&2
    return 1
  fi
  PDE_PLATFORM_NETWORK="${PDE_PLATFORM_NETWORK:?Informe a rede canônica dos frontends PDE}" \
    bash "${proxy_reload_script}"
}

preserve_running_image_references() {
  if [[ "${component}" != "backend" ]]; then
    return 0
  fi

  local image_variable_name
  local running_container
  local running_image
  while IFS=$'\t' read -r image_variable_name running_container; do
    [[ -n "${image_variable_name}" && -n "${running_container}" ]] || continue
    if ! docker inspect "${running_container}" >/dev/null 2>&1; then
      continue
    fi
    running_image="$(docker inspect --format '{{.Config.Image}}' "${running_container}")"
    printf -v "${image_variable_name}" '%s' "${running_image}"
    export "${image_variable_name?}"
  done < <(
    python3 - "${inventory}" <<'PY'
import json
from pathlib import Path
import sys

inventory = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
for product in inventory.get("products", []):
    for surface in product.get("surfaces", []):
        print(f"{surface['imageVariable']}\t{surface['containerName']}")
print("PDE_AI_WORKER_IMAGE\tpde-ai-worker")
print("PDE_RETENTION_WORKER_IMAGE\tpde-retention-worker")
PY
  )
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
  old_commit="$(environment_value PDE_DEPLOY_COMMIT_SHA)"
  old_tag="$(environment_value PDE_DEPLOY_IMAGE_TAG)"
  old_deployed_at="$(environment_value PDE_DEPLOY_DEPLOYED_AT)"
  env \
    "${image_variable}=${old_image_reference}" \
    PDE_DEPLOY_COMMIT_SHA="${old_commit}" \
    PDE_DEPLOY_IMAGE_TAG="${old_tag}" \
    PDE_DEPLOY_DEPLOYED_AT="${old_deployed_at}" \
    "${compose[@]}" up -d --no-deps "${service_name}"
  wait_component "${container_name}"
  [[ "$(docker inspect --format '{{.Image}}' "${container_name}")" == "${old_image_id}" ]]
  if [[ "${component}" == "backend" ]]; then
    docker exec "${container_name}" sh -ec '
      if command -v curl >/dev/null 2>&1; then
        curl --fail --silent http://127.0.0.1:8096/api/pde/build-identity
      else
        wget --quiet --output-document=- http://127.0.0.1:8096/api/pde/build-identity
      fi
    ' | grep -Fq "${old_commit}"
    reload_backend_consumers
  fi
  deployment_status="ROLLED_BACK"
}

on_error() {
  local exit_code=$?
  trap - ERR
  local message="Falha no deploy isolado do componente ${component}."
  if [[ "${cutover_started}" == "true" && "${rollback_in_progress}" != "true" ]]; then
    if ! restore_previous; then
      deployment_status="ROLLBACK_FAILED"
      message="Falha no deploy de ${component} e na restauração automática."
    fi
  fi
  write_receipt "${deployment_status}" "${message}" || true
  rm -rf "${temporary_dir}"
  echo "[ARQUITETURA] ${message} status=${deployment_status}" >&2
  exit "${exit_code}"
}

trap on_error ERR

[[ "${expected_commit}" =~ ^[0-9a-f]{40}$ ]]
[[ "${expected_image}" == *":${expected_commit}" ]]
configured_image="${!image_variable-}"
if [[ "${configured_image}" != "${expected_image}" ]]; then
  echo "[ARQUITETURA] ${image_variable} não aponta para a imagem autorizada." >&2
  false
fi

if [[ "${PDE_DEPLOY_SKIP_PULL:-false}" != "true" ]]; then
  docker pull "${expected_image}"
fi
expected_image_id="$(docker image inspect --format '{{.Id}}' "${expected_image}")"
preserve_running_image_references
snapshot_protected_containers

if docker inspect "${container_name}" >/dev/null 2>&1; then
  old_container_id="$(docker inspect --format '{{.Id}}' "${container_name}")"
  old_image_id="$(docker inspect --format '{{.Image}}' "${container_name}")"
  old_image_reference="$(docker inspect --format '{{.Config.Image}}' "${container_name}")"
  docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' \
    "${container_name}" >"${old_environment_file}"
fi

cutover_started=true
"${compose[@]}" up -d --no-deps "${service_name}"
validate_component
reload_backend_consumers

if [[ "${PDE_DEPLOY_TEST_MODE:-false}" == "true" \
  && "${PDE_DEPLOY_TEST_FAIL_AFTER_SWITCH:-false}" == "true" ]]; then
  echo '[ARQUITETURA] Falha compartilhada pós-troca injetada pela homologação local.' >&2
  false
fi

verify_protected_containers
deployment_status="PROMOTED"
write_receipt "${deployment_status}" \
  "Componente compartilhado promovido sem recriar outras superfícies ou workers."
trap - ERR
rm -rf "${temporary_dir}"
printf 'Componente PDE promovido: component=%s service=%s image=%s\n' \
  "${component}" "${service_name}" "${expected_image}"
