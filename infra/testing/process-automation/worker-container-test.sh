#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_root"
project="${PROCESS_COMPOSE_PROJECT:?Projeto exclusivo obrigatório}"
session="${AIHUB_HOMOLOGATION_SESSION:?Execute por scripts/run-docker-homologation.sh}"
base_image="aihub-homologation/${session}/process-worker:${AIHUB_HOMOLOGATION_IMAGE_TAG:-latest}"
export PROCESS_TEST_WORKER_IMAGE="aihub-homologation/${session}/process-worker-fixture:${AIHUB_HOMOLOGATION_IMAGE_TAG:-latest}"
compose() { docker compose -p "$project" -f infra/testing/process-automation/worker-container.yml "$@"; }
cleanup() { compose logs --no-color; compose rm --stop --force process-worker-fixture process-backend-double; }
trap cleanup EXIT
bash scripts/docker-build-temporary-image.sh process-worker process-execution-worker
bash scripts/docker-build-temporary-image.sh process-worker-fixture --build-arg "PROCESS_TEST_BASE_IMAGE=$base_image" -f infra/testing/process-automation/Dockerfile.worker-fixture infra/testing/process-automation
compose up -d --wait --wait-timeout 100
compose exec -T process-worker-fixture node --input-type=module -e '
import assert from "node:assert/strict";
assert.equal(process.getuid(), 1000);
assert.equal(process.versions.node.split(".")[0], "22");
const state = await (await fetch("http://process-backend-double:8000/health")).json();
assert.equal(state.dispatched, true);
assert(state.polls > 0);
console.log("PASS imagem Node 22, usuário restrito, token em arquivo, polling e conciliação autenticados");'
compose stop -t 60 process-worker-fixture
container_id="$(compose ps -a -q process-worker-fixture)"
test "$(docker inspect --format '{{.State.ExitCode}}' "$container_id")" = 0
printf 'PASS parada graciosa da imagem do conciliador\n'
