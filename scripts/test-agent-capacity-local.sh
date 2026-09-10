#!/usr/bin/env bash
# Homologa imagens compartilhadas e a retomada de Atena sem dependências produtivas.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ACTIONS_TEST_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
: "${AIHUB_HOMOLOGATION_SESSION:?Execute pelo wrapper de homologação Docker}"
round="${1:?Informe a rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
result_dir="$PWD/artifacts/actions-capacity-2026-09-10/$round"
mkdir -p "$result_dir"
: > "$result_dir/results.tsv"
AGENT_BROWSER_TEST_UID="$(id -u)"
AGENT_BROWSER_TEST_GID="$(id -g)"
export AGENT_BROWSER_TEST_UID AGENT_BROWSER_TEST_GID
export ATENA_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/atena:latest"
export IRIS_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/iris:latest"
export ATENA_BPM_TEST_IMAGE="$ATENA_TEST_IMAGE"
export PLUTUS_BPM_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/plutus:latest"
compose=(docker compose -p "$ACTIONS_TEST_COMPOSE_PROJECT"
  -f scripts/fixtures/agent-browser-ci/compose.yml
  -f experiment-strategist-worker/docker-compose.bpm-local.yml
  -f financial-agent-worker/docker-compose.bpm-local.yml)

# Remove a topologia desta rodada mesmo quando algum controle falhar.
cleanup() {
  local status=$?
  trap - EXIT
  "${compose[@]}" --profile runtime down --volumes --remove-orphans || status=1
  exit "$status"
}
trap cleanup EXIT

# Registra o resultado de cada controle e interrompe imediatamente ao detectar defeito.
step() {
  local name="$1"
  shift
  if "$@" > "$result_dir/$name.log" 2>&1; then
    printf '%s\tPASS\n' "$name" >> "$result_dir/results.tsv"
    printf 'PASS %s\n' "$name"
  else
    local status=$?
    printf '%s\tFAIL\n' "$name" >> "$result_dir/results.tsv"
    tail -60 "$result_dir/$name.log"
    return "$status"
  fi
}
in_module() { local module="$1"; shift; (cd "$module" && "$@"); }
seed_checkout() {
  tar --exclude='*/target' --exclude='*/review-evidence' -cf - \
    experiment-strategist-worker meta-ad-approver-worker \
    | "${compose[@]}" run --rm -T --entrypoint tar atena-ci -xf - -C /workspace
}
seed_bpm() {
  tar -C experiment-strategist-worker/target/test-classes -cf - . \
    | "${compose[@]}" run --rm -T atena-bpm-seed
}
seed_plutus() {
  tar -C financial-agent-worker/target/test-classes -cf - . \
    | "${compose[@]}" run --rm -T plutus-bpm-seed
}
runtime_probe() {
  "${compose[@]}" run --rm -T "$1" --input-type=module \
    < scripts/fixtures/agent-browser-ci/runtime-check.mjs
  "${compose[@]}" run --rm -T --entrypoint codex "$1" --version
}
missing_browser_fails() {
  local output
  if output="$("${compose[@]}" run --rm -T -e PLAYWRIGHT_BROWSERS_PATH=/inexistente \
      atena-runtime --input-type=module < scripts/fixtures/agent-browser-ci/runtime-check.mjs 2>&1)"; then
    echo 'Navegador ausente não pode aprovar o runtime.' >&2
    return 1
  fi
  [[ "$output" == *'Executable doesn'* ]]
}
image_capacity() {
  docker image inspect "$ATENA_TEST_IMAGE" "$IRIS_TEST_IMAGE" \
    mcr.microsoft.com/playwright:v1.54.2-noble@sha256:18b4bcff4f8ba0ac8c44b09f09def6a4f6cb8579e5f26381c21f38b50935d5d8 \
    > "$result_dir/images.json"
  node --input-type=module - "$result_dir/images.json" <<'NODE'
import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
const [atena, iris, base] = JSON.parse(readFileSync(process.argv[2]));
for (const image of [atena, iris]) {
  assert.deepEqual(image.RootFS.Layers.slice(0, base.RootFS.Layers.length), base.RootFS.Layers);
  assert.notEqual(image.Config.User, 'root');
  console.log(JSON.stringify({ image: image.RepoTags[0], sizeBytes: image.Size,
    minimumLoadMiB: 4096 + Math.ceil(2 * image.Size / 1048576), sharedBaseLayers: base.RootFS.Layers.length }));
}
assert.equal(atena.RootFS.Layers[base.RootFS.Layers.length], iris.RootFS.Layers[base.RootFS.Layers.length]);
console.log('Base e cliente idênticos; reserva de 4 GiB preservada.');
NODE
}

step 01-browser-contract node --test scripts/test-agent-browser-version-contract.mjs
step 02-atena-java in_module experiment-strategist-worker mvn -B '-DspotlessFiles=.*CodexStrategistRunnerTest[.]java' spotless:check test
step 03-iris-java in_module meta-ad-approver-worker mvn -B '-DspotlessFiles=.*TemisContainerIsolationContractTest[.]java' spotless:check verify
step 04-atena-dependencies in_module experiment-strategist-worker npm ci --no-audit --no-fund
step 05-iris-dependencies in_module meta-ad-approver-worker npm ci --no-audit --no-fund
step 06-seed seed_checkout
step 07-atena-mcp "${compose[@]}" run --rm -T atena-ci
step 08-iris-mcp "${compose[@]}" run --rm -T iris-ci
step 09-commercial-evidence node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence
step 10-atena-image bash scripts/docker-build-temporary-image.sh atena experiment-strategist-worker
step 11-iris-image bash scripts/docker-build-temporary-image.sh iris meta-ad-approver-worker
step 12-capacity image_capacity
step 13-atena-browser runtime_probe atena-runtime
step 14-iris-browser runtime_probe iris-runtime
step 15-iris-landing-video "${compose[@]}" run --rm -T iris-runtime /app/browser-runtime-check.mjs
step 16-missing-browser missing_browser_fails
step 17-bpm-fixture seed_bpm
step 18-bpm-unavailable "${compose[@]}" run --rm -T atena-bpm-smoke produce
step 19-bpm-recreation "${compose[@]}" run --rm -T atena-bpm-smoke deliver
step 20-actionlint env ACTIONLINT_DOCKER_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT" bash scripts/run-actionlint.sh
step 21-image-contracts node --test scripts/test-agent-image-bundle.mjs scripts/test-agent-image-workflows.mjs scripts/coordinate-agent-deployment.test.mjs
step 22-disk-contracts bash scripts/test-agent-vps-disk-space.sh
step 23-ssh-contracts node scripts/test-agent-vps-ssh-workflows.mjs
step 24-deploy-queue bash scripts/test-shared-vps-deploy-queue.sh
step 25-agent-architecture bash scripts/validate-premium-agents.sh
step 26-shellcheck shellcheck scripts/test-agent-capacity-local.sh
step 27-diff git diff --check
step 28-vega-ui env FRONTEND_BASE_URL=http://127.0.0.1:4173 node frontend/e2e/vega-atena-recovery-responsive.mjs
# As próximas provas reutilizam o projeto exclusivo com seus próprios volumes descartáveis.
step 29-runtime-cleanup "${compose[@]}" --profile runtime down --volumes --remove-orphans
step 30-docker-transfer env AGENT_VPS_DISK_TEST_COMPOSE_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT" bash scripts/test-agent-image-bundle-e2e.sh
step 31-docker-retention env AGENT_VPS_DISK_TEST_COMPOSE_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT" bash scripts/test-agent-vps-disk-space-e2e.sh
step 32-ssh-real env VPS_SSH_TEST_COMPOSE_PROJECT="$ACTIONS_TEST_COMPOSE_PROJECT" bash scripts/test-configure-vps-ssh-fallback-e2e.sh
step 33-backend in_module backend/ads-service mvn -B '-DspotlessFiles=.*(BusinessProcessActivityExecutionService|AgentTaskServiceTest|Experiment[.]java|ExperimentRepository).*java' spotless:check package
step 34-backend-resources python3 scripts/verify-backend-packaged-resources.py
step 35-frontend in_module frontend npm test -- --run
step 36-backend-image bash scripts/docker-build-temporary-image.sh backend -f backend/ads-service/Dockerfile .
step 37-plutus-java in_module financial-agent-worker mvn -B '-DspotlessFiles=.*PdeEconomics.*java' spotless:check verify
step 38-plutus-image bash scripts/docker-build-temporary-image.sh plutus financial-agent-worker
step 39-plutus-fixture seed_plutus
step 40-plutus-bpm "${compose[@]}" run --rm -T plutus-bpm-smoke
printf 'Rodada %s: %s controles aprovados.\n' "$round" "$(wc -l < "$result_dir/results.tsv")"
