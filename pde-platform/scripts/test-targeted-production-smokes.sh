#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/../.." && pwd)"
runner="${script_dir}/run-targeted-production-smokes.sh"
temporary_dir="$(mktemp -d "${repository_root}/.pde-smoke-test.XXXXXX")"
trap 'rm -rf "${temporary_dir}"' EXIT

invocation_log="${temporary_dir}/invocations.log"
fake_npm="${temporary_dir}/npm"
fake_consistency="${temporary_dir}/consistency.sh"
fake_rigel_consistency="${temporary_dir}/rigel-consistency.sh"

# A fixture positiva usa a fonte atual sem alterar a atestação histórica de outro produto.
frontend_source_sha256="$(node "${repository_root}/pde-platform/frontend/scripts/source-fingerprint.mjs" \
  "${repository_root}/pde-platform/frontend")"
python3 - "${repository_root}/pde-platform/contracts/vega-cycle10-preparation-v4.json" \
  "${temporary_dir}/vega-private-fixture.json" "${frontend_source_sha256}" <<'PY'
import json
from pathlib import Path
import sys

contract = json.loads(Path(sys.argv[1]).read_text())
contract["publicationContract"]["requiredFrontendSourceSha256"] = sys.argv[3]
contract["liveVisualContract"]["runtimeIdentity"]["frontendSourceSha256"] = sys.argv[3]
Path(sys.argv[2]).write_text(json.dumps(contract))
PY
private_fixture="${temporary_dir#"${repository_root}/"}/vega-private-fixture.json"

cat >"${fake_npm}" <<'FAKE_NPM'
#!/usr/bin/env bash
set -euo pipefail
printf 'npm\t%s\t%s\t%s\t%s\n' \
  "${PDE_PUBLIC_HEALTH_URL:-}" \
  "${PDE_PUBLIC_HEALTH_PATH:-}" \
  "${PDE_EXPECTED_EXPERIENCE_VERSION:-}" \
  "$*" >>"${PDE_SMOKE_INVOCATION_LOG}"
if [[ -n "${PDE_EXPECTED_PRIVATE_PROTOTYPE_VERSION:-}" ]]; then
  printf 'private-version\t%s\n' "${PDE_EXPECTED_PRIVATE_PROTOTYPE_VERSION}" >>"${PDE_SMOKE_INVOCATION_LOG}"
fi
FAKE_NPM

cat >"${fake_consistency}" <<'FAKE_CONSISTENCY'
#!/usr/bin/env bash
set -euo pipefail
printf 'consistency\t%s\t%s\t%s\n' \
  "${PDE_PUBLIC_BASE_URL:-}" \
  "${EXPECTED_EXPERIENCE_VERSION:-}" \
  "${PDE_CONTRACT_ACCESS_MODE:-published}" >>"${PDE_SMOKE_INVOCATION_LOG}"
if [[ -n "${EXPECTED_FRONTEND_SOURCE_SHA256:-}" ]]; then
  printf 'source-fingerprint\t%s\n' "${PDE_PUBLIC_BASE_URL:-}" \
    >>"${PDE_SMOKE_INVOCATION_LOG}"
fi
FAKE_CONSISTENCY

cat >"${fake_rigel_consistency}" <<'FAKE_RIGEL_CONSISTENCY'
#!/usr/bin/env bash
set -euo pipefail
printf 'rigel-consistency\t%s\n' "${PDE_PUBLIC_BASE_URL:-}" >>"${PDE_SMOKE_INVOCATION_LOG}"
FAKE_RIGEL_CONSISTENCY

chmod +x "${fake_npm}" "${fake_consistency}" "${fake_rigel_consistency}"

run_target() {
  local target="$1"
  local deployed_frontend="${2:-}"
  : >"${invocation_log}"
  PDE_SMOKE_NPM_COMMAND="${fake_npm}" \
    PDE_SMOKE_CONSISTENCY_SCRIPT="${fake_consistency}" \
    PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT="${fake_rigel_consistency}" \
    PDE_SMOKE_INVOCATION_LOG="${invocation_log}" \
    PDE_DEPLOY_FRONTEND_VERSION="${deployed_frontend}" \
    PDE_FRONTEND_CONTRACT_PATH="${3:-}" \
    IMAGE_TAG=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
    PDE_INTERNAL_API_TOKEN="test-only-token" \
    MIRA_PRIVATE_E2E_TOKEN="mira-qa-test-only" \
    bash "${runner}" "${target}"
}

run_target kit-whatsapp
grep -Fqx $'npm\thttps://kit-whatsapp-pronto.digicomdigital.com.br\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}"
grep -Fqx $'rigel-consistency\thttps://kit-whatsapp-pronto.digicomdigital.com.br' "${invocation_log}"
if grep -Fq 'clubemusa.com.br' "${invocation_log}" || grep -q '^consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao Kit WhatsApp validou um produto nao publicado.' >&2
  exit 1
fi

run_target v5
grep -Fqx $'npm\thttps://v5.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v5.clubemusa.com.br\t\tmusa-pde-entry-v5-video-explicativo\trun test:public-diagnostic-smoke' "${invocation_log}"
grep -Fqx $'consistency\thttps://v5.clubemusa.com.br\tmusa-pde-entry-v5-video-explicativo\tpublished' "${invocation_log}"
grep -Fqx $'source-fingerprint\thttps://v5.clubemusa.com.br' "${invocation_log}"
if grep -Fq 'v6.clubemusa.com.br' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao v5 validou um produto nao publicado.' >&2
  exit 1
fi

run_target v6
grep -Fqx $'npm\thttps://v6.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v6.clubemusa.com.br\t\tmusa-pde-entry-v6-video-motivacional\trun test:public-diagnostic-smoke' "${invocation_log}"
grep -Fqx $'consistency\thttps://v6.clubemusa.com.br\tmusa-pde-entry-v6-video-motivacional\tpublished' "${invocation_log}"
grep -Fqx $'source-fingerprint\thttps://v6.clubemusa.com.br' "${invocation_log}"
if grep -Fq 'Se o look parece certo' "${invocation_log}"; then
  echo '[ARQUITETURA] O smoke da v6 fixou uma copy mutável fora do contrato publicado.' >&2
  exit 1
fi

run_target v7
grep -Fqx $'npm\thttps://v7.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v7.clubemusa.com.br\t\tmusa-pde-entry-v7-espelho-antes-de-sair\trun test:public-diagnostic-smoke' "${invocation_log}"
grep -Fqx $'consistency\thttps://v7.clubemusa.com.br\tmusa-pde-entry-v7-espelho-antes-de-sair\tpublished' "${invocation_log}"
grep -Fqx $'source-fingerprint\thttps://v7.clubemusa.com.br' "${invocation_log}"
if grep -Fq 'test:mira-private:public' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao Vega v7 executou a superfície de Mira.' >&2
  exit 1
fi

run_target v8
grep -Fqx $'npm\thttps://v8.clubemusa.com.br\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}"
grep -Fqx $'npm\thttps://v8.clubemusa.com.br\t\tmusa-pde-entry-v12-primeiro-ajuste-aplicavel\trun test:public-diagnostic-smoke' "${invocation_log}"
grep -Fqx $'consistency\thttps://v8.clubemusa.com.br\tmusa-pde-entry-v12-primeiro-ajuste-aplicavel\tcandidate' "${invocation_log}"
grep -Fqx $'source-fingerprint\thttps://v8.clubemusa.com.br' "${invocation_log}"
if grep -Fq 'v7.clubemusa.com.br' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao v8 validou uma superfície não publicada.' >&2
  exit 1
fi
if grep -Fq 'v5.clubemusa.com.br' "${invocation_log}" || grep -Fq 'v6.clubemusa.com.br' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado ao v7 validou um produto nao publicado.' >&2
  exit 1
fi

if run_target v8 v8 pde-platform/contracts/vega-cycle7-preparation-v3.json; then
  echo '[ARQUITETURA] Um manifesto histórico autorizou fonte diferente da sua atestação.' >&2
  exit 1
fi

run_target v8 v8 "${private_fixture}"
grep -Fq 'exec -- playwright test tests/vega-private-public.smoke.spec.ts --config=playwright.public.config.ts' "${invocation_log}"
grep -Fqx $'private-version\tmusa-pde-entry-v13-primeiro-ajuste-aplicavel' "${invocation_log}"
if grep -Fq 'test:public-health' "${invocation_log}" || grep -q '^consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] A variante privada do sucessor disparou a oferta comercial.' >&2
  exit 1
fi

run_target mira
grep -Fqx $'npm\thttps://v7.clubemusa.com.br\t\t\trun test:mira-private:public' "${invocation_log}"
if grep -Fq 'test:public-health' "${invocation_log}" || grep -Fq 'test:public-diagnostic-smoke' "${invocation_log}" || grep -q '^consistency' "${invocation_log}" || grep -q '^rigel-consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado a Mira executou a superfície de outro produto.' >&2
  exit 1
fi

run_target mira-commercial
grep -Fqx $'npm\t\t\t\tMIRA_COMMERCIAL_PUBLIC_URL=https://mira.digicomdigital.com.br run test:mira-commercial:public' "${invocation_log}" || \
  grep -Fqx $'npm\t\t\t\trun test:mira-commercial:public' "${invocation_log}"
if grep -Fq 'test:mira-private:public' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}" || grep -q '^consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado a Mira comercial validou outra superfície.' >&2
  exit 1
fi

run_target alcyone
grep -Fqx $'npm\t\t\t\trun test:alcyone:local' "${invocation_log}"
if grep -Fq 'test:mira-private:public' "${invocation_log}" || grep -Fq 'kit-whatsapp-pronto' "${invocation_log}" || grep -q '^consistency' "${invocation_log}"; then
  echo '[ARQUITETURA] O deploy direcionado a Alcyone validou outra superfície.' >&2
  exit 1
fi

if PDE_SMOKE_NPM_COMMAND="${fake_npm}" \
  PDE_SMOKE_CONSISTENCY_SCRIPT="${fake_consistency}" \
  PDE_SMOKE_RIGEL_CONSISTENCY_SCRIPT="${fake_rigel_consistency}" \
  PDE_SMOKE_INVOCATION_LOG="${invocation_log}" \
    bash "${runner}" mira; then
  echo '[ARQUITETURA] O smoke produtivo de Mira aceitou deploy Mira sem o token exclusivo de QA.' >&2
  exit 1
fi

run_target all v8
test "$(grep -c $'npm\t.*\t/?mh_preview=qa&pde_analytics=off\t\trun test:public-health' "${invocation_log}")" -eq 5
test "$(grep -c $'npm\t.*\t\t.*\trun test:public-diagnostic-smoke' "${invocation_log}")" -eq 4
test "$(grep -c $'npm\t.*\t\t\trun test:mira-private:public' "${invocation_log}")" -eq 1
test "$(grep -c 'run test:mira-commercial:public' "${invocation_log}")" -eq 1
test "$(grep -c 'run test:alcyone:local' "${invocation_log}")" -eq 1
for expected_diagnostic in \
  $'https://v5.clubemusa.com.br\t\tmusa-pde-entry-v5-video-explicativo' \
  $'https://v6.clubemusa.com.br\t\tmusa-pde-entry-v6-video-motivacional' \
  $'https://v7.clubemusa.com.br\t\tmusa-pde-entry-v7-espelho-antes-de-sair' \
  $'https://v8.clubemusa.com.br\t\tmusa-pde-entry-v12-primeiro-ajuste-aplicavel'; do
  grep -Fq $'npm\t'"${expected_diagnostic}"$'\trun test:public-diagnostic-smoke' "${invocation_log}"
done
test "$(grep -c '^consistency' "${invocation_log}")" -eq 4
test "$(grep -c '^source-fingerprint' "${invocation_log}")" -eq 1
grep -Fqx $'source-fingerprint\thttps://v8.clubemusa.com.br' "${invocation_log}"
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
