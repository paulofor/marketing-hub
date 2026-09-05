#!/usr/bin/env bash
set -euo pipefail

target_frontend="${1:-}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
frontend_dir="${repository_root}/pde-platform/frontend"
npm_command="${PDE_SMOKE_NPM_COMMAND:-npm}"
consistency_script="${PDE_SMOKE_CONSISTENCY_SCRIPT:-${repository_root}/scripts/check-musa-pde-public-consistency.sh}"
rigel_consistency_script="${PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT:-${script_dir}/check-rigel-pde-public-consistency.sh}"

run_public_health() {
  local public_url="$1"
  (
    cd "${frontend_dir}"
    PDE_PUBLIC_HEALTH_URL="${public_url}" \
      PDE_PUBLIC_HEALTH_PATH='/?mh_preview=qa&pde_analytics=off' \
      "${npm_command}" run test:public-health
  )
}

run_public_diagnostic() {
  local public_url="$1"
  (
    cd "${frontend_dir}"
    PDE_PUBLIC_HEALTH_URL="${public_url}" "${npm_command}" run test:public-diagnostic-smoke
  )
}

run_mira_private() {
  local public_url="$1"
  : "${MIRA_PRIVATE_E2E_TOKEN:?MIRA_PRIVATE_E2E_TOKEN is required for Mira private production validation}"
  (
    cd "${frontend_dir}"
    PDE_PUBLIC_HEALTH_URL="${public_url}" \
      "${npm_command}" run test:mira-private:public
  )
}

run_musa_consistency() {
  local public_url="$1"
  local experience_version="$2"
  local first_fold_headline="${3:-}"

  PRODUCT_SLUG=metodo-musa-7-dias \
    PDE_PUBLIC_BASE_URL="${public_url}" \
    EXPECTED_EXPERIENCE_VERSION="${experience_version}" \
    EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE="${first_fold_headline}" \
    bash "${consistency_script}"
}

validate_v5() {
  run_public_health https://v5.clubemusa.com.br
  run_public_diagnostic https://v5.clubemusa.com.br
  run_musa_consistency \
    https://v5.clubemusa.com.br \
    musa-pde-entry-v5-video-explicativo
}

validate_v6() {
  run_public_health https://v6.clubemusa.com.br
  run_public_diagnostic https://v6.clubemusa.com.br
  run_musa_consistency \
    https://v6.clubemusa.com.br \
    musa-pde-entry-v6-video-motivacional \
    "Se o look parece certo, por que você ainda sente que falta presença?"
}

validate_v7() {
  run_public_health https://v7.clubemusa.com.br
  run_mira_private https://v7.clubemusa.com.br
}

validate_kit_whatsapp() {
  run_public_health https://kit-whatsapp-pronto.digicomdigital.com.br
  PDE_PUBLIC_BASE_URL=https://kit-whatsapp-pronto.digicomdigital.com.br \
    bash "${rigel_consistency_script}"
}

case "${target_frontend}" in
  v5)
    validate_v5
    ;;
  v6)
    validate_v6
    ;;
  v7)
    validate_v7
    ;;
  kit-whatsapp)
    validate_kit_whatsapp
    ;;
  all)
    validate_v5
    validate_v6
    validate_v7
    validate_kit_whatsapp
    ;;
  none)
    ;;
  *)
    echo "Versao frontend PDE invalida para homologacao: ${target_frontend}" >&2
    exit 1
    ;;
esac
