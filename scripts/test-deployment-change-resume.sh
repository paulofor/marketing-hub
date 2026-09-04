#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DETECT_SCRIPT="${REPO_ROOT}/scripts/detect-deployment-changes.sh"
TEST_REPO="$(mktemp -d)"
trap 'rm -rf "${TEST_REPO}"' EXIT

git -C "${TEST_REPO}" init -q
git -C "${TEST_REPO}" config user.email test@sandbox.local
git -C "${TEST_REPO}" config user.name "Deploy Contract Test"
mkdir -p "${TEST_REPO}/backend/ads-service" "${TEST_REPO}/frontend" "${TEST_REPO}/pesquisas/video" "${TEST_REPO}/video-management-service"
printf 'base\n' > "${TEST_REPO}/backend/ads-service/app.txt"
printf 'base\n' > "${TEST_REPO}/frontend/app.txt"
printf 'base\n' > "${TEST_REPO}/video-management-service/app.txt"
printf '# Artigo base\n' > "${TEST_REPO}/pesquisas/video/2026-09-01-base.md"
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

printf '# Novo artigo diario\n' > "${TEST_REPO}/pesquisas/video/2026-09-02-novo.md"
git -C "${TEST_REPO}" add pesquisas/video/2026-09-02-novo.md
git -C "${TEST_REPO}" commit -qm 'novo artigo de pesquisa'
research_revision="$(git -C "${TEST_REPO}" rev-parse HEAD)"
research_output="${TEST_REPO}/research-output"

(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${head_revision}" \
    "${research_revision}" \
    "${research_output}" \
    "${head_revision}"
)

grep -Fxq 'backend=true' "${research_output}"
grep -Fxq 'frontend=false' "${research_output}"
grep -Fxq 'app_deploy=true' "${research_output}"

video_deployed_revision="${research_revision}"
printf 'video pendente\n' >> "${TEST_REPO}/video-management-service/app.txt"
printf 'backend publicado no mesmo push\n' >> "${TEST_REPO}/backend/ads-service/app.txt"
git -C "${TEST_REPO}" commit -qam 'app publicado e video falhou'
partial_app_revision="$(git -C "${TEST_REPO}" rev-parse HEAD)"

printf 'backend posterior\n' >> "${TEST_REPO}/backend/ads-service/app.txt"
git -C "${TEST_REPO}" commit -qam 'backend posterior ao deploy parcial'
video_recovery_head="$(git -C "${TEST_REPO}" rev-parse HEAD)"
video_recovery_output="${TEST_REPO}/video-recovery-output"

(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${partial_app_revision}" \
    "${video_recovery_head}" \
    "${video_recovery_output}" \
    "${partial_app_revision}" \
    "${video_deployed_revision}"
)

grep -Fxq 'backend=true' "${video_recovery_output}"
grep -Fxq 'video=true' "${video_recovery_output}"
grep -Fxq 'video_deploy=true' "${video_recovery_output}"

video_current_output="${TEST_REPO}/video-current-output"
(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${partial_app_revision}" \
    "${video_recovery_head}" \
    "${video_current_output}" \
    "${partial_app_revision}" \
    "${video_recovery_head}"
)

grep -Fxq 'video=false' "${video_current_output}"

video_unknown_output="${TEST_REPO}/video-unknown-output"
(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${partial_app_revision}" \
    "${video_recovery_head}" \
    "${video_unknown_output}" \
    "${partial_app_revision}" \
    "UNKNOWN"
)

grep -Fxq 'video=true' "${video_unknown_output}"
grep -Fxq 'video_deploy=true' "${video_unknown_output}"

printf 'ajuste no reconciliador\n' > "${TEST_REPO}/deploy-placeholder"
mkdir -p "${TEST_REPO}/deploy/bin"
mv "${TEST_REPO}/deploy-placeholder" \
  "${TEST_REPO}/deploy/bin/reconcile-video-planner-secret.sh"
git -C "${TEST_REPO}" add deploy/bin/reconcile-video-planner-secret.sh
git -C "${TEST_REPO}" commit -qm 'ajusta reconciliador do segredo de video'
video_descriptor_head="$(git -C "${TEST_REPO}" rev-parse HEAD)"
video_descriptor_output="${TEST_REPO}/video-descriptor-output"

(
  cd "${TEST_REPO}"
  bash "${DETECT_SCRIPT}" \
    "${video_recovery_head}" \
    "${video_descriptor_head}" \
    "${video_descriptor_output}" \
    "${video_recovery_head}" \
    "${video_recovery_head}"
)

grep -Fxq 'video=false' "${video_descriptor_output}"
grep -Fxq 'video_deploy=true' "${video_descriptor_output}"

printf 'Contrato de retomada de modulos pendentes validado.\n'
