#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT

mkdir -p "$test_tmp_dir/bin"
cat >"$test_tmp_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 5 || "$1" != "login" || "$5" != "--password-stdin" ]]; then
  echo "invocação inesperada do Docker" >&2
  exit 2
fi

password=""
IFS= read -r password || true
if [[ "$password" != "${DOCKER_DOUBLE_EXPECTED_PASSWORD:?}" ]]; then
  echo "credencial recebida pelo double é diferente da esperada" >&2
  exit 2
fi

printf '%s\n' "$*" >>"${DOCKER_DOUBLE_CALLS:?}"
attempt="$(wc -l <"$DOCKER_DOUBLE_CALLS")"

case "${DOCKER_DOUBLE_MODE:?}" in
  recover)
    if [[ "$attempt" -lt 3 ]]; then
      echo 'Error response from daemon: Get "https://ghcr.io/v2/": context deadline exceeded' >&2
      exit 1
    fi
    echo "Login Succeeded"
    ;;
  fatal)
    echo "unauthorized: authentication required" >&2
    exit 1
    ;;
  exhaust)
    echo "net/http: TLS handshake timeout" >&2
    exit 1
    ;;
  timeout)
    exit 124
    ;;
  *)
    echo "modo do double inválido" >&2
    exit 2
    ;;
esac
DOCKER_DOUBLE
chmod +x "$test_tmp_dir/bin/docker"

run_login() {
  local mode="$1"
  local stdout_file="$test_tmp_dir/stdout"
  local stderr_file="$test_tmp_dir/stderr"
  : >"$test_tmp_dir/calls"
  : >"$stdout_file"
  : >"$stderr_file"

  printf '%s' "registry-test-secret-never-log" \
    | PATH="$test_tmp_dir/bin:$PATH" \
      DOCKER_DOUBLE_CALLS="$test_tmp_dir/calls" \
      DOCKER_DOUBLE_EXPECTED_PASSWORD="registry-test-secret-never-log" \
      DOCKER_DOUBLE_MODE="$mode" \
      DOCKER_LOGIN_RETRY_DELAY_SECONDS=0 \
      DOCKER_LOGIN_TIMEOUT_SECONDS=5 \
      bash "$test_root/scripts/docker-login-with-transient-retry.sh" \
        ghcr.io test-user >"$stdout_file" 2>"$stderr_file"
}

run_login recover
test "$(wc -l <"$test_tmp_dir/calls")" -eq 3
grep -Fx 'login ghcr.io -u test-user --password-stdin' "$test_tmp_dir/calls" >/dev/null
grep -Fq 'Login Succeeded' "$test_tmp_dir/stdout"
if rg -q 'registry-test-secret-never-log' "$test_tmp_dir/stdout" "$test_tmp_dir/stderr"; then
  echo "O helper revelou a credencial do registry." >&2
  exit 1
fi

if run_login fatal; then
  echo "Uma falha de autenticação não pode ser repetida." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 1

if run_login exhaust; then
  echo "O helper deveria falhar após esgotar o retry transitório." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 3

if run_login timeout; then
  echo "O helper deveria falhar após esgotar o timeout transitório." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 3

if DOCKER_LOGIN_MAX_ATTEMPTS=0 \
  bash "$test_root/scripts/docker-login-with-transient-retry.sh" ghcr.io test-user \
  </dev/null >/dev/null 2>&1; then
  echo "O helper aceitou um limite inválido de tentativas." >&2
  exit 1
fi

if bash "$test_root/scripts/docker-login-with-transient-retry.sh" ghcr.io test-user \
  </dev/null >/dev/null 2>&1; then
  echo "O helper aceitou uma credencial ausente." >&2
  exit 1
fi

echo "Retry transitório do login Docker validado."
