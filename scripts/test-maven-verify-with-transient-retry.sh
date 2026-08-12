#!/usr/bin/env bash

set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "${test_dir}"' EXIT

write_fake_maven() {
  local mode="$1"
  cat >"${test_dir}/mvn" <<EOF
#!/usr/bin/env bash
count_file="${test_dir}/count"
count=0
[[ -f "\${count_file}" ]] && count="\$(<"\${count_file}")"
count=\$((count + 1))
echo "\${count}" >"\${count_file}"
if [[ "${mode}" == "transient" && \${count} -eq 1 ]]; then
  echo "Could not transfer artifact: status code: 403"
  exit 1
fi
if [[ "${mode}" == "functional" ]]; then
  echo "COMPILATION ERROR"
  exit 1
fi
exit 0
EOF
  chmod +x "${test_dir}/mvn"
}

write_fake_maven transient
PATH="${test_dir}:${PATH}" MAVEN_RETRY_DELAY_SECONDS=0 \
  bash "${repository_root}/scripts/maven-verify-with-transient-retry.sh"
[[ "$(<"${test_dir}/count")" == "2" ]]

rm -f "${test_dir}/count"
write_fake_maven functional
if PATH="${test_dir}:${PATH}" MAVEN_RETRY_DELAY_SECONDS=0 \
  bash "${repository_root}/scripts/maven-verify-with-transient-retry.sh"; then
  echo "Falha funcional foi tratada incorretamente como sucesso." >&2
  exit 1
fi
[[ "$(<"${test_dir}/count")" == "1" ]]

echo "Contrato de retry transitório do Maven validado."
