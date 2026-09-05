#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runner="${script_dir}/run-targeted-production-smokes.sh"
temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT

invocation_log="${temporary_dir}/invocations.log"
fake_npm="${temporary_dir}/npm"
fake_consistency="${temporary_dir}/consistency.sh"
fake_rigel_consistency="${temporary_dir}/rigel-consistency.sh"

cat >"${fake_npm}" <<'FAKE_NPM'
#!/usr/bin/env bash
set -euo pipefail
printf 'npm\t%s\t%s\t%s\n' \
  "${PDE_PUBLIC_HEALTH_URL:-}" \
  "${PDE_PUBLIC_HEALTH_PATH:-}" \
  "$*" >>"${PDE_SMOKE_INVOCATION_LOG}"
FAKE_NPM

cat >"${fake_consistency}" <<'FAKE_CONSISTENCY'
#!/usr/bin/env bash
set -euo pipefail
printf 'consistency\t%s\t%s\t%s\n' \
  "${PDE_PUBLIC_BASE_URL:-}" \
  "${EXPECTED_EXPERIENCE_VERSION:-}" \
  "${EXPECTED_PUBLIC_FIRST_FOLD_HEADLINE:-}" >>"${PDE_SMOKE_INVOCATION_LOG}"
FAKE_CONSISTENCY

cat >"${fake_rigel_consistency}" <<'FAKE_RIGEL_CONSISTENCY'
#!/usr/bin/env bash
set -euo pipefail
printf 'rigel-consistency\t%s\n' "${PDE_PUBLIC_BASE_URL:-}" >>"${PDE_SMOKE_INVOCATION_LOG}"
FAKE_RIGEL_CONSISTENCY

chmod +x "${fake_npm}" "${fake_consistency}" "${fake_rigel_consistency}"

run_target() {
  local target="$1"
  : >"${invocation_log}"
  PDE_SMOKE_NPM_COMMAND="${fake_npm}" \
    PDE_SMOKE_CONSISTENCY_SCRIPT="${fake_consistency}" \
    PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT="${fake_rigel_consistency}" \
    PDE_SMOKE_INVOCATION_LOG="${invocation_log}" \
    MIRA_PRIVATE_E2E_TOKEN="mira-qa-test-only" \
    bash "${runner}" "${target}"
}

run_target kit-whatsapp
grep -Fqx $'npm\thttps://kit-whatsapp-pronto.digicomdigital.com.br\t/?mh_preview=qa&pde_analytics=off\trun test:public-health' "${invocation_log}"
grep -Fqx $'rigel-consistency\thttps://kit-whatsapp-pronto.digicomdigital.com.br' "${invocation_log}"
if grep -Fq 'clubemusa.com.br' "${invocation_log}" || grep -q '^consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao Kit WhatsApp validou um produto nao publicado.' >&2
  exit 1
fi

run_target v5
grep -Fqx $'npm\thttps://v5.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v5.clubemusa.com.br\t\trun test:public-diagnostic-smoke' "${invocation_log}"
grep -Fqx $'consistency\thttps://v5.clubemusa.com.br\tmusa-pde-entry-v5-video-explicativo\t' "${invocation_log}"
if grep -Fq 'v6.clubemusa.com.br' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao v5 validou um produto nao publicado.' >&2
  exit 1
fi

run_target v7
grep -Fqx $'npm\thttps://v7.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v7.clubemusa.com.br\t\trun test:mira-private:public' "${invocation_log}"
if grep -Fq 'v5.clubemusa.com.br' "${invocation_log}" || grep -Fq 'v6.clubemusa.com.br' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao v7 validou um produto nao publicado.' >&2
  exit 1
fi

if PDE_SMOKE_NPM_COMMAND="${fake_npm}" \
  PDE_SMOKE_CONSISTENCY_SCRIPT="${fake_consistency}" \
  PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT="${fake_rigel_consistency}" \
  PDE_SMOKE_INVOCATION_LOG="${invocation_log}" \
  bash "${runner}" v7; then
  echo '[ARQUITETURA] O smoke produtivo de Mira aceitou deploy v7 sem o token exclusivo de QA.' >&2
  exit 1
fi

run_target all
test "$(grep -c $'npm\t.*\t/?mh_preview=qa&pde_analytics=off\trun test:public-health' "${invocation_log}")" -eq 4
test "$(grep -c $'npm\t.*\t\trun test:public-diagnostic-smoke' "${invocation_log}")" -eq 2
test "$(grep -c $'npm\t.*\t\trun test:mira-private:public' "${invocation_log}")" -eq 1
test "$(grep -c '^consistency' "${invocation_log}")" -eq 2
test "$(grep -c '^rigel-consistency' "${invocation_log}")" -eq 1

if PDE_SMOKE_NPM_COMMAND="${fake_npm}" \
  PDE_SMOKE_CONSISTENCY_SCRIPT="${fake_consistency}" \
  PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT="${fake_rigel_consistency}" \
  PDE_SMOKE_INVOCATION_LOG="${invocation_log}" \
  MIRA_PRIVATE_E2E_TOKEN="mira-qa-test-only" \
  bash "${runner}" desconhecida; then
  echo '[ARQUITETURA] A homologacao aceitou uma versao frontend desconhecida.' >&2
  exit 1
fi

echo 'Contrato de homologacao direcionada do PDE aprovado.'
