#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"
: "${ACTIONS_TEST_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
round="${1:?Informe o identificador da rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash scripts/run-docker-homologation.sh bash "$0" "$round"
fi

result_dir="$repository_root/artifacts/actions-browser-2026-09-09/$round"
mkdir -p "$result_dir"
: > "$result_dir/results.tsv"
AGENT_BROWSER_TEST_UID="$(id -u)"
AGENT_BROWSER_TEST_GID="$(id -g)"
export AGENT_BROWSER_TEST_UID AGENT_BROWSER_TEST_GID
export ARGOS_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/argos:latest"
export PSIQUE_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/psique:latest"
export ACTIONLINT_DOCKER_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT"
compose=(docker compose -p "$ACTIONS_TEST_COMPOSE_PROJECT" -f scripts/fixtures/agent-browser-ci/compose.yml)

# Encerra somente a topologia temporária desta solicitação, inclusive após teste reprovado.
cleanup() { "${compose[@]}" --profile runtime down --volumes --remove-orphans; }
trap cleanup EXIT

# Preserva a saída completa e o código de cada controle, interrompendo na primeira falha.
step() {
  local name="$1"
  shift
  printf 'INICIANDO %s\n' "$name"
  if "$@" > "$result_dir/$name.log" 2>&1; then
    printf '%s\tPASS\n' "$name" >> "$result_dir/results.tsv"
    printf 'PASS %s\n' "$name"
  else
    local status="$?"
    printf '%s\tFAIL\n' "$name" >> "$result_dir/results.tsv"
    tail -70 "$result_dir/$name.log"
    return "$status"
  fi
}

in_module() { local module="$1"; shift; (cd "$module" && "$@"); }
# A engine isolada recebe arquivos por streaming; não depende de paths locais visíveis ao daemon.
seed_checkout() {
  tar --exclude='*/target' --exclude='*/review-evidence' -cf - \
    product-discovery-worker customer-agent-worker pesquisas scripts .github/workflows \
    docs/swagger/agent-tasks-v1-swagger.yaml \
    backend/ads-service/src/main/resources/agent-harness/agent-harness-v2.json \
    | "${compose[@]}" run --rm -T --entrypoint tar argos-ci -xf - -C /workspace
}
runtime_probe() {
  "${compose[@]}" run --rm -T "$1" --input-type=module < scripts/fixtures/agent-browser-ci/runtime-check.mjs
  "${compose[@]}" run --rm -T --entrypoint codex "$1" --version
}
psique_capture() {
  "${compose[@]}" run --rm -T --entrypoint java psique-runtime -version
  "${compose[@]}" run --rm -T psique-runtime --input-type=module \
    < customer-agent-worker/src/test/js/bpm-visual-evidence.test.mjs
}
argos_library() {
  "${compose[@]}" run --rm -T argos-runtime --input-type=module \
    < scripts/fixtures/agent-browser-ci/argos-library-check.mjs
}
missing_browser_fails() {
  local output
  if output="$("${compose[@]}" run --rm -T -e PLAYWRIGHT_BROWSERS_PATH=/browser-inexistente \
      argos-runtime --input-type=module < scripts/fixtures/agent-browser-ci/runtime-check.mjs 2>&1)"; then
    echo 'Browser ausente não pode aprovar o gate.' >&2
    return 1
  fi
  [[ "$output" == *'Executable doesn'* ]] || { printf '%s\n' "$output"; return 1; }
  echo 'Ausência do browser recusada; nenhum download ou fallback silencioso.'
}

step 01-browser-contract node --test scripts/test-agent-browser-version-contract.mjs
step 02-argos-dependencies in_module product-discovery-worker npm ci
step 02b-research-library in_module product-discovery-worker npm run build:research-library
step 03-psique-dependencies in_module customer-agent-worker npm ci
step 03b-checkout seed_checkout
step 04-argos-ci "${compose[@]}" run --rm -T argos-ci
step 05-psique-ci "${compose[@]}" run --rm -T psique-ci
step 06-psique-java in_module customer-agent-worker mvn -B '-DspotlessFiles=.*CodexProcessSupervisor.*[.]java' spotless:check test
step 07-psique-container-contract bash customer-agent-worker/test-dockerfile-contract.sh
step 08-commercial-evidence node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
step 09-argos-build bash scripts/docker-build-temporary-image.sh argos product-discovery-worker
step 10-psique-build bash scripts/docker-build-temporary-image.sh psique customer-agent-worker
step 11-argos-runtime runtime_probe argos-runtime
step 11b-argos-library argos_library
step 12-psique-runtime runtime_probe psique-runtime
step 13-psique-full-capture psique_capture
step 14-browser-failure missing_browser_fails
step 15-actionlint bash scripts/run-actionlint.sh
step 16-deployment-coordination node --test scripts/coordinate-agent-deployment.test.mjs
step 17-image-contracts node --test scripts/test-agent-image-bundle.mjs scripts/test-agent-image-workflows.mjs
step 18-ssh-contracts node scripts/test-agent-vps-ssh-workflows.mjs
step 19-deploy-queue bash scripts/test-shared-vps-deploy-queue.sh
step 20-agent-architecture bash scripts/validate-premium-agents.sh
step 21-shellcheck shellcheck scripts/test-agent-browser-ci-local.sh customer-agent-worker/test-dockerfile-contract.sh
step 22-diff git diff --check
printf 'Rodada %s: 25/25 controles aprovados.\n' "$round"
