#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fixture_directory="$test_root/scripts/fixtures/temporary-docker-image"
session_id="cleanup-e2e-$(date -u +%Y%m%d%H%M%S)-$$"
active_session_id="${session_id}-active"
stale_session_id="${session_id}-stale"
failed_session_id="${session_id}-failed"
image_reference="aihub-homologation/${session_id}/fixture:latest"
active_image_reference="aihub-homologation/${active_session_id}/fixture:latest"
stale_image_reference="aihub-homologation/${stale_session_id}/fixture:latest"
failed_image_reference="aihub-homologation/${failed_session_id}/fixture:latest"
container_name="${active_session_id}-container"
test_tmp_dir="$(mktemp -d)"

cleanup_e2e() {
  docker rm -f "$container_name" >/dev/null 2>&1 || true
  docker image rm "$image_reference" "$active_image_reference" "$stale_image_reference" \
    "$failed_image_reference" \
    >/dev/null 2>&1 || true
  rm -rf "$test_tmp_dir"
}
trap cleanup_e2e EXIT

AIHUB_HOMOLOGATION_SESSION="$session_id" \
  AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
  AIHUB_DOCKER_CLEANUP_INTERVAL_SECONDS=1 \
  bash "$test_root/scripts/run-docker-homologation.sh" \
    bash "$test_root/scripts/docker-build-temporary-image.sh" \
      fixture "$fixture_directory" >/dev/null

if docker image inspect "$image_reference" >/dev/null 2>&1; then
  echo "A imagem da sessão encerrada não foi removida." >&2
  exit 1
fi

AIHUB_HOMOLOGATION_SESSION="$stale_session_id" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" \
    fixture "$fixture_directory" >/dev/null
AIHUB_HOMOLOGATION_SESSION="${session_id}-watcher" \
  AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
  AIHUB_DOCKER_CLEANUP_INTERVAL_SECONDS=1 \
  AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=0 \
  bash "$test_root/scripts/run-docker-homologation.sh" sleep 2 >/dev/null
if docker image inspect "$stale_image_reference" >/dev/null 2>&1; then
  echo "A passagem periódica não removeu a imagem de uma sessão encerrada." >&2
  exit 1
fi

set +e
AIHUB_HOMOLOGATION_SESSION="$failed_session_id" \
  AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
  bash "$test_root/scripts/run-docker-homologation.sh" \
    bash -c 'bash "$1" fixture "$2" >/dev/null && exit 23' \
      _ "$test_root/scripts/docker-build-temporary-image.sh" "$fixture_directory" \
    >/dev/null
failed_status="$?"
set -e
if [[ "$failed_status" -ne 23 ]]; then
  echo "O wrapper não preservou o status da homologação com falha: ${failed_status}." >&2
  exit 1
fi
if docker image inspect "$failed_image_reference" >/dev/null 2>&1; then
  echo "A imagem de uma homologação com falha não foi removida." >&2
  exit 1
fi

AIHUB_HOMOLOGATION_SESSION="$active_session_id" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" \
    fixture "$fixture_directory" >/dev/null
docker create --name "$container_name" "$active_image_reference" /noop >/dev/null

AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
  AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=0 \
  AIHUB_DOCKER_CLEANUP_SESSION="$active_session_id" \
  bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once >/dev/null
docker image inspect "$active_image_reference" >/dev/null

docker rm "$container_name" >/dev/null
AIHUB_HOMOLOGATION_SESSION_DIR="$test_tmp_dir/sessions" \
  AIHUB_DOCKER_CLEANUP_LOCK_FILE="$test_tmp_dir/cleanup.lock" \
  AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=0 \
  AIHUB_DOCKER_CLEANUP_SESSION="$active_session_id" \
  bash "$test_root/scripts/cleanup-temporary-docker-images.sh" once >/dev/null

if docker image inspect "$active_image_reference" >/dev/null 2>&1; then
  echo "A imagem deixou de estar em uso, mas não foi removida." >&2
  exit 1
fi

echo "Limpeza real de imagens temporárias validada."
