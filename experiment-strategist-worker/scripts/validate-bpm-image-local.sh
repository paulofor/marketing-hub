#!/usr/bin/env bash
# Valida a mesma imagem em dois containers sucessivos, sem rede ou credenciais externas.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ATENA_BPM_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
: "${AIHUB_HOMOLOGATION_SESSION:?Execute pelo wrapper de homologação Docker}"
case "$ATENA_BPM_COMPOSE_PROJECT" in aihub-*) ;; *) exit 2 ;; esac
export ATENA_BPM_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/atena-bpm:latest"
compose=(docker compose -p "$ATENA_BPM_COMPOSE_PROJECT" -f experiment-strategist-worker/docker-compose.bpm-local.yml)
cleanup() {
  local result=$?
  trap - EXIT
  "${compose[@]}" down --volumes --remove-orphans || result=1
  exit "$result"
}
trap cleanup EXIT
bash scripts/docker-build-temporary-image.sh atena-bpm experiment-strategist-worker
tar -C experiment-strategist-worker/target/test-classes -cf - . | "${compose[@]}" run --rm -T atena-bpm-seed
"${compose[@]}" run --rm atena-bpm-smoke produce
"${compose[@]}" run --rm atena-bpm-smoke deliver
printf 'PASS atena-image-recreation\n'
