#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
pde_dir="$(cd "${script_dir}/.." && pwd)"
repo_dir="$(cd "${pde_dir}/.." && pwd)"
project_name="${AIHUB_COMPOSE_PROJECT_NAME:?Informe AIHUB_COMPOSE_PROJECT_NAME exclusivo da sandbox}"

if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash "${repo_dir}/scripts/run-docker-homologation.sh" bash "$0"
fi

base_compose="${pde_dir}/docker-compose.mira-commercial-validation.yml"
prebuilt_compose="${pde_dir}/docker-compose.mira-commercial-validation.prebuilt.yml"
image_namespace="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}"
export MIRA_CONTRACT_IMAGE="${image_namespace}/mira-contract:latest"
export MIRA_MYSQL_IMAGE="${image_namespace}/mira-mysql:latest"
export MIRA_PDE_BACKEND_IMAGE="${image_namespace}/mira-pde-backend:latest"
export MIRA_FRONTEND_IMAGE="${image_namespace}/mira-frontend:latest"
export MIRA_PAYMENTS_IMAGE="${image_namespace}/mira-payments:latest"
export MIRA_E2E_IMAGE="${image_namespace}/mira-e2e:latest"

compose() {
  docker compose \
    -p "${project_name}" \
    -f "${base_compose}" \
    -f "${prebuilt_compose}" \
    "$@"
}

finish() {
  status="$?"
  trap - EXIT
  set +e
  compose down --volumes --remove-orphans
  cleanup_status="$?"
  if [[ "${status}" -eq 0 && "${cleanup_status}" -ne 0 ]]; then
    status="${cleanup_status}"
  fi
  exit "${status}"
}
trap finish EXIT

docker version >/dev/null
docker buildx version >/dev/null
docker compose version >/dev/null

bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-contract \
  -f "${pde_dir}/frontend/Dockerfile.contract-server" \
  "${pde_dir}"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-mysql \
  -f "${pde_dir}/local-validation/Dockerfile.mysql57" \
  "${pde_dir}/local-validation"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-pde-backend \
  "${pde_dir}/backend"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-frontend \
  --build-arg PDE_FRONTEND_VERSION_ID=mira-commercial-v1-validation \
  -f "${pde_dir}/frontend/Dockerfile.mira-commercial" \
  "${pde_dir}/frontend"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-payments \
  "${repo_dir}/lead-portal-payments-service"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" \
  mira-e2e \
  -f "${pde_dir}/frontend/Dockerfile.assisted-service-e2e" \
  "${pde_dir}/frontend"

compose config --images >/dev/null
compose up \
  --no-build \
  --abort-on-container-exit \
  --exit-code-from pde-mira-commercial-e2e
