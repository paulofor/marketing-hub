#!/usr/bin/env bash

set -uo pipefail

max_attempts="${MAVEN_MAX_ATTEMPTS:-3}"
retry_delay_seconds="${MAVEN_RETRY_DELAY_SECONDS:-5}"
attempt=1
log_file="$(mktemp)"
trap 'rm -f "${log_file}"' EXIT

while true; do
  : >"${log_file}"
  set +e
  mvn -B verify 2>&1 | tee "${log_file}"
  status=${PIPESTATUS[0]}
  set -e

  if [[ ${status} -eq 0 ]]; then
    exit 0
  fi

  if ! grep -Eqi \
    'Could not transfer artifact|status code: (403|429|5[0-9]{2})|Connection reset|Read timed out|Connection timed out|Temporary failure in name resolution|Name or service not known' \
    "${log_file}"; then
    echo "Falha funcional do Maven; nova tentativa automática não será executada." >&2
    exit "${status}"
  fi

  if [[ ${attempt} -ge ${max_attempts} ]]; then
    echo "Falha transitória persistiu após ${max_attempts} tentativas." >&2
    exit "${status}"
  fi

  echo "Falha transitória ao acessar dependências; repetindo Maven (${attempt}/${max_attempts})." >&2
  sleep "${retry_delay_seconds}"
  attempt=$((attempt + 1))
done
