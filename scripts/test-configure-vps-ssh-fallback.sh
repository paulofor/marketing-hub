#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
configuration_script="$test_root/scripts/configure-vps-ssh-fallback.sh"
test_tmp_directory="$(mktemp -d)"
trap 'rm -rf "$test_tmp_directory"' EXIT HUP INT TERM

mock_keygen="$test_tmp_directory/mock-ssh-keygen"
mock_keyscan="$test_tmp_directory/mock-ssh-keyscan"
mock_ssh="$test_tmp_directory/mock-ssh"

cat >"$mock_keygen" <<'MOCK_KEYGEN'
#!/usr/bin/env bash
set -euo pipefail
identity_file="${!#}"
if grep -Fq 'MALFORMED-KEY' "$identity_file"; then
  exit 1
fi
printf 'ssh-ed25519 AAAATEST\n'
MOCK_KEYGEN

cat >"$mock_keyscan" <<'MOCK_KEYSCAN'
#!/usr/bin/env bash
set -euo pipefail
known_hosts_file="$2"
install -m 700 -d "$(dirname "$known_hosts_file")"
printf '|1|host-hash|salt ssh-ed25519 AAAATESTHOSTKEY\n' >"$known_hosts_file"
chmod 600 "$known_hosts_file"
MOCK_KEYSCAN

cat >"$mock_ssh" <<'MOCK_SSH'
#!/usr/bin/env bash
set -euo pipefail
configuration_file=""
while [ "$#" -gt 0 ]; do
  if [ "$1" = "-F" ]; then
    configuration_file="$2"
    shift 2
    continue
  fi
  shift
done

test -n "$configuration_file"
while read -r directive identity_file; do
  if [ "$directive" = "IdentityFile" ] \
    && grep -Fq "$MOCK_ACCEPTED_KEY_MARKER" "$identity_file"; then
    exit 0
  fi
done <"$configuration_file"
exit 255
MOCK_SSH

chmod 700 "$mock_keygen" "$mock_ssh"
# O helper é lido por Bash, como o arquivo 100644 de um checkout limpo.
chmod 600 "$mock_keyscan"

primary_github_env="$test_tmp_directory/github-env-primary"
touch "$primary_github_env"
VPS_SSH_KEY_PRIMARY='PRIMARY-ACCEPTED' \
VPS_SSH_KEY_FALLBACK_1='FALLBACK-AVAILABLE' \
SSH_KEYGEN_BIN="$mock_keygen" \
SSH_KEYSCAN_HELPER="$mock_keyscan" \
SSH_BIN="$mock_ssh" \
MOCK_ACCEPTED_KEY_MARKER='PRIMARY-ACCEPTED' \
VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-primary" \
  bash "$configuration_script" 163.245.202.80 root "$primary_github_env" >/dev/null

primary_ssh_args="$(grep -F 'SSH_COMMON_ARGS=-F ' "$primary_github_env")"
primary_ssh_configuration="${primary_ssh_args#SSH_COMMON_ARGS=-F }"
first_identity_file="$(awk '$1 == "IdentityFile" { print $2; exit }' "$primary_ssh_configuration")"
grep -Fq 'PRIMARY-ACCEPTED' "$first_identity_file"

success_github_env="$test_tmp_directory/github-env-success"
touch "$success_github_env"
success_output="$(
  VPS_SSH_KEY_PRIMARY='PRIMARY-REJECTED' \
  VPS_SSH_KEY_FALLBACK_1='MALFORMED-KEY' \
  VPS_SSH_KEY_FALLBACK_2='FALLBACK-ACCEPTED' \
  SSH_KEYGEN_BIN="$mock_keygen" \
  SSH_KEYSCAN_HELPER="$mock_keyscan" \
  SSH_BIN="$mock_ssh" \
  MOCK_ACCEPTED_KEY_MARKER='FALLBACK-ACCEPTED' \
  VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-success" \
    bash "$configuration_script" 163.245.202.80 root "$success_github_env"
)"

ssh_common_args="$(grep -F 'SSH_COMMON_ARGS=-F ' "$success_github_env")"
ssh_configuration_file="${ssh_common_args#SSH_COMMON_ARGS=-F }"
test -f "$ssh_configuration_file"
test "$(grep -Fc 'IdentityFile ' "$ssh_configuration_file")" = "2"
grep -Fq 'BatchMode yes' "$ssh_configuration_file"
grep -Fq 'PasswordAuthentication no' "$ssh_configuration_file"
grep -Fq 'IdentitiesOnly yes' "$ssh_configuration_file"
grep -Fq 'StrictHostKeyChecking yes' "$ssh_configuration_file"
grep -Fq 'SSH_DEPLOY_READY=true' "$success_github_env"
test "$(stat -c '%a' "$ssh_configuration_file")" = "600"

if grep -Fq 'PRIMARY-REJECTED' <<<"$success_output" \
  || grep -Fq 'FALLBACK-ACCEPTED' <<<"$success_output" \
  || grep -Fq 'MALFORMED-KEY' <<<"$success_output"; then
  echo "O helper expôs conteúdo de uma credencial SSH." >&2
  exit 1
fi

failure_github_env="$test_tmp_directory/github-env-failure"
touch "$failure_github_env"
if failure_output="$(
  VPS_SSH_KEY_PRIMARY='PRIMARY-REJECTED' \
  VPS_SSH_KEY_FALLBACK_1='FALLBACK-REJECTED' \
  SSH_KEYGEN_BIN="$mock_keygen" \
  SSH_KEYSCAN_HELPER="$mock_keyscan" \
  SSH_BIN="$mock_ssh" \
  MOCK_ACCEPTED_KEY_MARKER='NO-KEY-ACCEPTED' \
  VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-failure" \
    bash "$configuration_script" 163.245.202.80 root "$failure_github_env" 2>&1
)"; then
  echo "O helper aceitou credenciais que não autenticam no host." >&2
  exit 1
fi

grep -Fq 'nenhuma das credenciais SSH válidas autenticou' <<<"$failure_output"
if grep -Fq 'PRIMARY-REJECTED' <<<"$failure_output" \
  || grep -Fq 'FALLBACK-REJECTED' <<<"$failure_output"; then
  echo "O diagnóstico de falha expôs conteúdo de uma credencial SSH." >&2
  exit 1
fi

if find "$test_tmp_directory/runtime-failure" -mindepth 1 -print -quit | grep -q .; then
  echo "O helper preservou credenciais temporárias após falha." >&2
  exit 1
fi

missing_github_env="$test_tmp_directory/github-env-missing"
touch "$missing_github_env"
if missing_output="$(
  SSH_KEYGEN_BIN="$mock_keygen" \
  SSH_KEYSCAN_HELPER="$mock_keyscan" \
  SSH_BIN="$mock_ssh" \
  VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-missing" \
    bash "$configuration_script" 163.245.202.80 root "$missing_github_env" 2>&1
)"; then
  echo "O helper aceitou ausência total de credenciais SSH." >&2
  exit 1
fi
grep -Fq 'nenhuma credencial SSH foi configurada' <<<"$missing_output"

for invalid_helper in "$test_tmp_directory/absent-helper" "$test_tmp_directory"; do
  if invalid_helper_output="$(
    SSH_KEYSCAN_HELPER="$invalid_helper" \
      bash "$configuration_script" 127.0.0.1 test "$missing_github_env" 2>&1
  )"; then
    echo "O helper aceitou dependência ausente ou diretório." >&2
    exit 1
  fi
  grep -Fq 'helper de coleta da chave do host indisponível' <<<"$invalid_helper_output"
done

invalid_github_env="$test_tmp_directory/github-env-invalid"
touch "$invalid_github_env"
if invalid_output="$(
  VPS_SSH_KEY_PRIMARY='MALFORMED-KEY' \
  SSH_KEYGEN_BIN="$mock_keygen" SSH_KEYSCAN_HELPER="$mock_keyscan" \
  VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-invalid" \
    bash "$configuration_script" 127.0.0.1 test "$invalid_github_env" 2>&1
)"; then
  echo "O helper aceitou configuração com todas as chaves inválidas." >&2
  exit 1
fi
grep -Fq 'todas as credenciais SSH configuradas são inválidas' <<<"$invalid_output"
test ! -s "$invalid_github_env"
test -z "$(find "$test_tmp_directory/runtime-invalid" -mindepth 1 -print -quit)"

scan_failure_helper="$test_tmp_directory/scan-failure.sh"
printf 'exit 1\n' >"$scan_failure_helper"
scan_github_env="$test_tmp_directory/github-env-scan-failure"
touch "$scan_github_env"
if VPS_SSH_KEY_PRIMARY='PRIMARY-ACCEPTED' \
  SSH_KEYGEN_BIN="$mock_keygen" SSH_KEYSCAN_HELPER="$scan_failure_helper" \
  VPS_SSH_RUNTIME_ROOT="$test_tmp_directory/runtime-scan-failure" \
    bash "$configuration_script" 127.0.0.1 test "$scan_github_env" >/dev/null 2>&1; then
  echo "O helper aceitou falha na coleta da identidade do servidor." >&2
  exit 1
fi
test ! -s "$scan_github_env"
test -z "$(find "$test_tmp_directory/runtime-scan-failure" -mindepth 1 -print -quit)"

echo "Fallback seguro de credenciais SSH validado."
