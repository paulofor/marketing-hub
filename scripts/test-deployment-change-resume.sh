#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DETECT_SCRIPT="${REPO_ROOT}/scripts/detect-deployment-changes.sh"
TEST_REPO="$(mktemp -d)"
trap 'rm -rf "${TEST_REPO}"' EXIT

git -C "${TEST_REPO}" init -q
git -C "${TEST_REPO}" config user.email test@sandbox.local
git -C "${TEST_REPO}" config user.name "Deploy Contract Test"
mkdir -p "${TEST_REPO}/backend/ads-service" "${TEST_REPO}/frontend"
printf 'base\n' > "${TEST_REPO}/backend/ads-service/app.txt"
printf 'base\n' > "${TEST_REPO}/frontend/app.txt"
git -C "${TEST_REPO}" add .
git -C "${TEST_REPO}" commit -qm base
deployed_revision="$(git -C "${TEST_REPO}" rev-parse HEAD)"

printf 'frontend pendente\n' >> "${TEST_REPO}/frontend/app.txt"
git -C "${TEST_REPO}" commit -qam 'frontend cujo deploy falhou'

printf 'backend novo\n' >> "${TEST_REPO}/backend/ads-service/app.txt"
git -C "${TEST_REPO}" commit -qam 'correcao posterior somente no backend'
head_revision="$(git -C "${TEST_REPO}" rev-parse HEAD)"
output_file="${TEST_REPO}/outputs"

(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" "${deployed_revision}" "${head_revision}" "${output_file}"
)

grep -Fxq 'backend=true' "${output_file}"
grep -Fxq 'frontend=true' "${output_file}"
grep -Fxq 'app_deploy=true' "${output_file}"

stale_frontend_output="${TEST_REPO}/stale-frontend-output"
published_app_revision="$(git -C "${TEST_REPO}" rev-parse HEAD~1)"

(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${published_app_revision}" \
    "${head_revision}" \
    "${stale_frontend_output}" \
    "${deployed_revision}"
)

grep -Fxq 'backend=true' "${stale_frontend_output}"
grep -Fxq 'frontend=true' "${stale_frontend_output}"
grep -Fxq 'app_deploy=true' "${stale_frontend_output}"

current_frontend_output="${TEST_REPO}/current-frontend-output"
(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${published_app_revision}" \
    "${head_revision}" \
    "${current_frontend_output}" \
    "${published_app_revision}"
)

grep -Fxq 'backend=true' "${current_frontend_output}"
grep -Fxq 'frontend=false' "${current_frontend_output}"

unknown_frontend_output="${TEST_REPO}/unknown-frontend-output"
(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${published_app_revision}" \
    "${head_revision}" \
    "${unknown_frontend_output}" \
    "UNKNOWN"
)

grep -Fxq 'frontend=true' "${unknown_frontend_output}"

printf 'Contrato de retomada de modulos pendentes validado.\n'
