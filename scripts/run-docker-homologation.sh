#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "Uso: $0 <comando-de-homologação> [argumentos...]" >&2
  exit 2
fi

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cleanup_script="${AIHUB_DOCKER_CLEANUP_SCRIPT:-${script_root}/cleanup-temporary-docker-images.sh}"
homologation_session="${AIHUB_HOMOLOGATION_SESSION:-session-$(date -u +%Y%m%dt%H%M%Sz)-$$-${RANDOM}}"
session_directory="${AIHUB_HOMOLOGATION_SESSION_DIR:-${TMPDIR:-/tmp}/marketinghub-docker-homologation-sessions}"

if ! [[ "$homologation_session" =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]] || [[ ${#homologation_session} -gt 128 ]]; then
  echo "AIHUB_HOMOLOGATION_SESSION possui formato inválido." >&2
  exit 2
fi

if [[ ! -x "$cleanup_script" ]]; then
  echo "Limpador Docker não executável: ${cleanup_script}" >&2
  exit 2
fi

mkdir -p "$session_directory"
session_lock_file="${session_directory}/${homologation_session}.lock"
exec 9>"$session_lock_file"
if ! flock -n 9; then
  echo "A sessão de homologação ${homologation_session} já está ativa." >&2
  exit 2
fi

export AIHUB_HOMOLOGATION_SESSION="$homologation_session"
export AIHUB_HOMOLOGATION_SESSION_DIR="$session_directory"

cleanup_pid=""
finalize_homologation() {
  incoming_status="$?"
  trap - EXIT
  set +e

  if [[ -n "$cleanup_pid" ]]; then
    kill "$cleanup_pid" >/dev/null 2>&1 || true
    wait "$cleanup_pid" >/dev/null 2>&1 || true
  fi

  flock -u 9
  AIHUB_DOCKER_CLEANUP_SESSION="$homologation_session" \
    AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS=0 \
    "$cleanup_script" once
  final_cleanup_status="$?"

  if [[ "$incoming_status" -eq 0 && "$final_cleanup_status" -ne 0 ]]; then
    incoming_status="$final_cleanup_status"
  fi
  exit "$incoming_status"
}

trap finalize_homologation EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

"$cleanup_script" watch &
cleanup_pid="$!"

printf 'Homologação Docker iniciada: sessão=%s\n' "$homologation_session"
"$@"
