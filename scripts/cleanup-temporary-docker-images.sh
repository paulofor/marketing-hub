#!/usr/bin/env bash
set -euo pipefail

cleanup_mode="${1:-once}"
cleanup_interval_seconds="${AIHUB_DOCKER_CLEANUP_INTERVAL_SECONDS:-600}"
cleanup_min_age_seconds="${AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS:-3600}"
cleanup_docker_timeout_seconds="${AIHUB_DOCKER_CLEANUP_DOCKER_TIMEOUT_SECONDS:-20}"
cleanup_dry_run="${AIHUB_DOCKER_CLEANUP_DRY_RUN:-false}"
cleanup_target_session="${AIHUB_DOCKER_CLEANUP_SESSION:-}"
session_directory="${AIHUB_HOMOLOGATION_SESSION_DIR:-${TMPDIR:-/tmp}/marketinghub-docker-homologation-sessions}"
cleanup_lock_file="${AIHUB_DOCKER_CLEANUP_LOCK_FILE:-${TMPDIR:-/tmp}/marketinghub-docker-temporary-images.lock}"
temporary_label="com.marketinghub.homologation.temporary=true"

if [[ "$cleanup_mode" != "once" && "$cleanup_mode" != "watch" ]]; then
  echo "Modo inválido. Use once ou watch." >&2
  exit 2
fi

for positive_value in "$cleanup_interval_seconds" "$cleanup_docker_timeout_seconds"; do
  if ! [[ "$positive_value" =~ ^[1-9][0-9]*$ ]]; then
    echo "Intervalo e timeout da limpeza devem ser inteiros positivos." >&2
    exit 2
  fi
done

if ! [[ "$cleanup_min_age_seconds" =~ ^[0-9]+$ ]]; then
  echo "AIHUB_DOCKER_CLEANUP_MIN_AGE_SECONDS deve ser um inteiro não negativo." >&2
  exit 2
fi

if [[ "$cleanup_dry_run" != "true" && "$cleanup_dry_run" != "false" ]]; then
  echo "AIHUB_DOCKER_CLEANUP_DRY_RUN deve ser true ou false." >&2
  exit 2
fi

if [[ -n "$cleanup_target_session" ]] \
  && ! [[ "$cleanup_target_session" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$ ]]; then
  echo "AIHUB_DOCKER_CLEANUP_SESSION possui formato inválido." >&2
  exit 2
fi

mkdir -p "$session_directory" "$(dirname "$cleanup_lock_file")"
exec 8>"$cleanup_lock_file"

cleanup_once() {
  if ! flock -n 8; then
    echo "Limpeza Docker já está em execução; esta passagem foi ignorada."
    return 0
  fi

  if ! timeout --foreground --kill-after=5s "${cleanup_docker_timeout_seconds}s" \
    docker info >/dev/null 2>&1; then
    flock -u 8
    echo "Docker indisponível; nenhuma imagem foi alterada." >&2
    return 1
  fi

  local now_epoch
  local candidates=0
  local removed_references=0
  local active_sessions=0
  local recent_images=0
  local images_in_use=0
  local protected_references=0
  local invalid_images=0
  local removal_failures=0
  now_epoch="$(date -u +%s)"

  mapfile -t candidate_ids < <(
    docker image ls --all --quiet --filter "label=${temporary_label}" | sort -u
  )

  for image_id in "${candidate_ids[@]}"; do
    [[ -n "$image_id" ]] || continue
    candidates=$((candidates + 1))

    created_at="$(docker image inspect --format '{{.Created}}' "$image_id" 2>/dev/null || true)"
    created_epoch="$(date -u -d "$created_at" +%s 2>/dev/null || true)"
    image_session="$(
      docker image inspect \
        --format '{{index .Config.Labels "com.marketinghub.homologation.session"}}' \
        "$image_id" 2>/dev/null || true
    )"

    if [[ -z "$created_epoch" ]] \
      || ! [[ "$image_session" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$ ]]; then
      invalid_images=$((invalid_images + 1))
      continue
    fi

    if [[ -n "$cleanup_target_session" && "$image_session" != "$cleanup_target_session" ]]; then
      continue
    fi

    image_age_seconds=$((now_epoch - created_epoch))
    if [[ "$image_age_seconds" -lt 0 ]]; then
      image_age_seconds=0
    fi
    if [[ "$image_age_seconds" -lt "$cleanup_min_age_seconds" ]]; then
      recent_images=$((recent_images + 1))
      continue
    fi

    session_lock_file="${session_directory}/${image_session}.lock"
    exec {session_lock_fd}>"$session_lock_file"
    if ! flock -n "$session_lock_fd"; then
      active_sessions=$((active_sessions + 1))
      exec {session_lock_fd}>&-
      continue
    fi
    flock -u "$session_lock_fd"
    exec {session_lock_fd}>&-

    if docker ps --all --quiet --filter "ancestor=${image_id}" | grep -q .; then
      images_in_use=$((images_in_use + 1))
      continue
    fi

    mapfile -t image_references < <(
      docker image inspect --format '{{range .RepoTags}}{{println .}}{{end}}' \
        "$image_id" 2>/dev/null || true
    )
    temporary_references=()
    image_temporary_namespace="aihub-homologation/${image_session}/"
    for image_reference in "${image_references[@]}"; do
      if [[ "$image_reference" == "${image_temporary_namespace}"* ]]; then
        temporary_references+=("$image_reference")
      else
        protected_references=$((protected_references + 1))
      fi
    done

    if [[ "${#temporary_references[@]}" -eq 0 ]]; then
      invalid_images=$((invalid_images + 1))
      continue
    fi

    for image_reference in "${temporary_references[@]}"; do
      if [[ "$cleanup_dry_run" == "true" ]]; then
        printf 'DRY-RUN removeria %s (%s).\n' "$image_reference" "$image_id"
        removed_references=$((removed_references + 1))
        continue
      fi

      if docker image rm "$image_reference" >/dev/null; then
        printf 'Imagem temporária removida: %s\n' "$image_reference"
        removed_references=$((removed_references + 1))
      else
        echo "Imagem temporária preservada após recusa segura do Docker: ${image_reference}" >&2
        removal_failures=$((removal_failures + 1))
      fi
    done
  done

  printf '%s\n' \
    "Limpeza Docker concluída: candidatas=${candidates} referênciasRemovidas=${removed_references} sessõesAtivas=${active_sessions} recentes=${recent_images} emUso=${images_in_use} referênciasProtegidas=${protected_references} inválidas=${invalid_images} falhasRemoção=${removal_failures} dryRun=${cleanup_dry_run}."
  flock -u 8
}

if [[ "$cleanup_mode" == "once" ]]; then
  cleanup_once
  exit 0
fi

sleep_pid=""
stop_watcher() {
  if [[ -n "$sleep_pid" ]]; then
    kill "$sleep_pid" >/dev/null 2>&1 || true
  fi
  exit 0
}
trap stop_watcher INT TERM

while true; do
  cleanup_once
  sleep "$cleanup_interval_seconds" &
  sleep_pid="$!"
  wait "$sleep_pid" || true
  sleep_pid=""
done
