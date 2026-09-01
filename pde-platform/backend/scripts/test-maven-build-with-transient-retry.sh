#!/usr/bin/env bash
set -euo pipefail

BACKEND_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPORARY_DIRECTORY="$(mktemp -d)"
trap 'rm -rf -- "${TEMPORARY_DIRECTORY}"' EXIT

cat >"${TEMPORARY_DIRECTORY}/mvn" <<'FAKE_MAVEN'
#!/usr/bin/env bash
set -euo pipefail
count=0
[[ -f "${FAKE_MAVEN_COUNT_FILE}" ]] && count="$(<"${FAKE_MAVEN_COUNT_FILE}")"
count=$((count + 1))
printf '%s\n' "${count}" >"${FAKE_MAVEN_COUNT_FILE}"
printf '%s\n' "$*" >>"${FAKE_MAVEN_ARGUMENTS_FILE}"

case "${FAKE_MAVEN_MODE}" in
  transient)
    if [[ ${count} -eq 1 ]]; then
      echo "Could not transfer artifact: status code: 429, reason phrase: Too Many Requests"
      exit 1
    fi
    ;;
  persistent)
    echo "Could not transfer artifact: status code: 503"
    exit 1
    ;;
  functional)
    echo "COMPILATION ERROR"
    exit 1
    ;;
esac
FAKE_MAVEN
chmod 0755 "${TEMPORARY_DIRECTORY}/mvn"

run_fake_maven() {
  local mode="$1"
  rm -f -- "${TEMPORARY_DIRECTORY}/count" "${TEMPORARY_DIRECTORY}/arguments"
  FAKE_MAVEN_MODE="${mode}" \
  FAKE_MAVEN_COUNT_FILE="${TEMPORARY_DIRECTORY}/count" \
  FAKE_MAVEN_ARGUMENTS_FILE="${TEMPORARY_DIRECTORY}/arguments" \
  PATH="${TEMPORARY_DIRECTORY}:${PATH}" \
  MAVEN_REPOSITORY="${TEMPORARY_DIRECTORY}/repository" \
  MAVEN_RETRY_DELAY_SECONDS=0 \
    bash "${BACKEND_DIRECTORY}/scripts/maven-build-with-transient-retry.sh" \
      -B -q -DskipTests package
}

run_fake_maven transient
[[ "$(<"${TEMPORARY_DIRECTORY}/count")" == "2" ]]
sed -n '2p' "${TEMPORARY_DIRECTORY}/arguments" | grep -Eq '^-U '

if run_fake_maven functional; then
  echo "Falha funcional foi tratada incorretamente como transitória." >&2
  exit 1
fi
[[ "$(<"${TEMPORARY_DIRECTORY}/count")" == "1" ]]

if MAVEN_MAX_ATTEMPTS=3 run_fake_maven persistent; then
  echo "Falha transitória persistente foi tratada incorretamente como sucesso." >&2
  exit 1
fi
[[ "$(<"${TEMPORARY_DIRECTORY}/count")" == "3" ]]

dockerfile="${BACKEND_DIRECTORY}/Dockerfile"
pom_line="$(grep -nF 'COPY pom.xml .' "${dockerfile}" | cut -d: -f1)"
offline_line="$(grep -nF 'dependency:go-offline' "${dockerfile}" | cut -d: -f1)"
source_line="$(grep -nF 'COPY src ./src' "${dockerfile}" | cut -d: -f1)"
package_line="$(grep -nF -- '-DskipTests package' "${dockerfile}" | cut -d: -f1)"

[[ ${pom_line} -lt ${offline_line} ]]
[[ ${offline_line} -lt ${source_line} ]]
[[ ${source_line} -lt ${package_line} ]]

echo "Contrato resiliente do build Maven do PDE validado."
