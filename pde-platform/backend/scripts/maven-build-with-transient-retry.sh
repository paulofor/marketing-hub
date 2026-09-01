#!/usr/bin/env bash
set -uo pipefail

if [[ $# -eq 0 ]]; then
  echo "Informe os argumentos do Maven que devem ser executados." >&2
  exit 2
fi

max_attempts="${MAVEN_MAX_ATTEMPTS:-3}"
retry_delay_seconds="${MAVEN_RETRY_DELAY_SECONDS:-5}"
maven_repository="${MAVEN_REPOSITORY:-${HOME}/.m2/repository}"
attempt=1
log_file="$(mktemp)"
trap 'rm -f -- "${log_file}"' EXIT

while true; do
  : >"${log_file}"
  maven_arguments=("$@")
  if [[ ${attempt} -gt 1 ]]; then
    maven_arguments=(-U "${maven_arguments[@]}")
  fi

  set +e
  mvn "${maven_arguments[@]}" 2>&1 | tee "${log_file}"
  status=${PIPESTATUS[0]}
  set -e

  if [[ ${status} -eq 0 ]]; then
    exit 0
  fi

  if ! grep -Eqi \
    'Could not transfer artifact|status code: (408|429|5[0-9]{2})|Too Many Requests|Connection reset|Read timed out|Connection timed out|Temporary failure in name resolution|Name or service not known' \
    "${log_file}"; then
    echo "Falha funcional do Maven; o build não será repetido." >&2
    exit "${status}"
  fi

  if [[ ${attempt} -ge ${max_attempts} ]]; then
    echo "Falha transitória do Maven persistiu após ${max_attempts} tentativas." >&2
    exit "${status}"
  fi

  if [[ -d "${maven_repository}" ]]; then
    find "${maven_repository}" -type f -name '*.lastUpdated' -delete
  fi
  echo "Falha transitória ao baixar dependências; repetindo Maven (${attempt}/${max_attempts})." >&2
  sleep "$((retry_delay_seconds * attempt))"
  attempt=$((attempt + 1))
done
