#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
recreate_script="$test_root/lead-portal/scripts/recreate-app-services.sh"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT HUP INT TERM

mock_bin="$test_tmp_dir/bin"
mock_calls="$test_tmp_dir/calls"
mock_up_count="$test_tmp_dir/up-count"
mkdir -p "$mock_bin"

cat >"$mock_bin/docker" <<'MOCK_DOCKER'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"$MOCK_DOCKER_CALLS"

if [ "$1" = "compose" ] && [ "${*: -4}" = "up -d backend frontend" ]; then
  count="$(cat "$MOCK_DOCKER_UP_COUNT" 2>/dev/null || printf '0')"
  count="$((count + 1))"
  printf '%s\n' "$count" >"$MOCK_DOCKER_UP_COUNT"
  if [ "$count" -lt "${MOCK_DOCKER_UP_SUCCEED_AT:-2}" ]; then
    exit 1
  fi
  exit 0
fi

if [ "$1" = "inspect" ] && [ "$2" != "--format" ]; then
  exit 0
fi

if [ "$1" = "inspect" ] && [ "$2" = "--format" ]; then
  template="$3"
  container_name="$4"
  case "$template" in
    *compose.service*)
      case "$container_name" in
        lead-portal-backend) printf 'backend\n' ;;
        lead-portal-frontend) printf 'frontend\n' ;;
      esac
      ;;
    *project.working_dir*)
      printf '%s\n' "${MOCK_DOCKER_WORKDIR}"
      ;;
    *PortBindings*)
      printf '{}\n'
      ;;
  esac
  exit 0
fi

exit 0
MOCK_DOCKER
chmod 700 "$mock_bin/docker"

run_recreate() {
  expected_workdir="${2:-$test_root/lead-portal}"
  PATH="$mock_bin:$PATH" \
  MOCK_DOCKER_CALLS="$mock_calls" \
  MOCK_DOCKER_UP_COUNT="$mock_up_count" \
  MOCK_DOCKER_WORKDIR="$expected_workdir" \
  MOCK_DOCKER_UP_SUCCEED_AT="$1" \
    bash -c 'cd "$1" && bash scripts/recreate-app-services.sh' _ "$test_root/lead-portal"
}

run_recreate 2
test "$(cat "$mock_up_count")" = "2"
grep -F 'stop --time 30 lead-portal-backend' "$mock_calls" >/dev/null
grep -F 'rm -f lead-portal-backend' "$mock_calls" >/dev/null
grep -F 'stop --time 30 lead-portal-frontend' "$mock_calls" >/dev/null
grep -F 'rm -f lead-portal-frontend' "$mock_calls" >/dev/null

: >"$mock_calls"
: >"$mock_up_count"
run_recreate 1
if grep -Fq 'rm -f lead-portal-' "$mock_calls"; then
  echo "A recriação removeu containers mesmo após sucesso normal do Compose." >&2
  exit 1
fi

: >"$mock_calls"
: >"$mock_up_count"
if run_recreate 2 "$test_tmp_dir/outro-projeto" >/dev/null 2>&1; then
  echo "A recriação aceitou container pertencente a outro diretório Compose." >&2
  exit 1
fi
if grep -Fq 'rm -f lead-portal-' "$mock_calls"; then
  echo "A recriação removeu container pertencente a outro diretório Compose." >&2
  exit 1
fi

echo "Recuperação limitada da recriação do Lead Portal validada."
