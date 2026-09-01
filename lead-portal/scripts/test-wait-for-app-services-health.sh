#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT

mkdir -p "$test_tmp_dir/bin"
cat >"$test_tmp_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >>"${DOCKER_DOUBLE_CALLS:?}"
attempt="$(wc -l <"${DOCKER_DOUBLE_CALLS}")"
container_name="${!#}"

case "${DOCKER_DOUBLE_MODE:?}" in
  recover)
    if [[ "$attempt" -le 2 ]]; then
      echo "running starting"
    else
      echo "running healthy"
    fi
    ;;
  exited)
    if [[ "$container_name" == "lead-portal-backend" ]]; then
      echo "exited unhealthy"
    else
      echo "running starting"
    fi
    ;;
  timeout)
    echo "running starting"
    ;;
  *)
    echo "modo do double inválido" >&2
    exit 2
    ;;
esac
DOCKER_DOUBLE
chmod +x "$test_tmp_dir/bin/docker"

run_health() {
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$test_tmp_dir/calls" \
    DOCKER_DOUBLE_MODE="$1" \
    bash "$script_dir/wait-for-app-services-health.sh" "$2" 1
}

: >"$test_tmp_dir/calls"
run_health recover 5
test "$(wc -l <"$test_tmp_dir/calls")" -eq 4
grep -F 'lead-portal-backend' "$test_tmp_dir/calls" >/dev/null
grep -F 'lead-portal-frontend' "$test_tmp_dir/calls" >/dev/null

: >"$test_tmp_dir/calls"
if run_health exited 5; then
  echo "Um container encerrado deveria interromper o health imediatamente." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 2

: >"$test_tmp_dir/calls"
if run_health timeout 0; then
  echo "O health deveria respeitar o prazo sem converter espera em sucesso." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 2

if bash "$script_dir/wait-for-app-services-health.sh" inválido 10; then
  echo "O health aceitou um prazo inválido." >&2
  exit 1
fi

echo "Espera temporal do health do Lead Portal validada."
