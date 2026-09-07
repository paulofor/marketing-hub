#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash "$test_root/scripts/run-docker-homologation.sh" bash "$0"
fi

mkdir -p "$test_root/codex-cache"
test_context="$(mktemp -d "$test_root/codex-cache/ssh-proof.XXXXXX")"
fixture="$test_root/scripts/fixtures/vps-ssh"
export VPS_SSH_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/ssh-proof:latest"
test_project="${VPS_SSH_TEST_COMPOSE_PROJECT:-ssh-proof-${AIHUB_HOMOLOGATION_SESSION}}"
compose=(docker compose -p "$test_project" -f "$fixture/compose.yml")

cleanup_test() {
  local cleanup_status="$?"
  trap - EXIT
  "${compose[@]}" down --volumes --remove-orphans || cleanup_status=1
  rm -rf -- "$test_context"
  exit "$cleanup_status"
}
trap cleanup_test EXIT

cp "$test_root/scripts/configure-vps-ssh-fallback.sh" \
  "$test_root/scripts/ssh-keyscan-with-retry.sh" "$fixture/Dockerfile" "$fixture/test.sh" "$test_context/"
bash "$test_root/scripts/docker-build-temporary-image.sh" ssh-proof "$test_context"
"${compose[@]}" run --rm -T ssh-proof
