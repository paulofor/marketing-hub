#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT

mkdir -p "$test_tmp_dir/bin" "$test_tmp_dir/proc/pressure"
cat >"$test_tmp_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >>"${DOCKER_DOUBLE_CALLS:?}"
if [[ "${DOCKER_DOUBLE_MODE:?}" == "unavailable" ]]; then
  exit 1
fi

case "$*" in
  "info"|"compose version") exit 0 ;;
  *) exit 2 ;;
esac
DOCKER_DOUBLE
chmod +x "$test_tmp_dir/bin/docker"

write_capacity() {
  printf '%s 0.00 0.00 1/1 1\n' "$1" >"$test_tmp_dir/proc/loadavg"
  printf 'MemAvailable: %s kB\n' "$2" >"$test_tmp_dir/proc/meminfo"
  printf 'some avg10=%s avg60=0.00 avg300=0.00 total=0\n' "$3" \
    >"$test_tmp_dir/proc/pressure/io"
}

run_capacity() {
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$test_tmp_dir/calls" \
    DOCKER_DOUBLE_MODE="$1" \
    SHARED_VPS_CAPACITY_PROC_ROOT="$test_tmp_dir/proc" \
    SHARED_VPS_CAPACITY_CPU_COUNT=1 \
    SHARED_VPS_CAPACITY_MAX_ATTEMPTS="${2:-2}" \
    SHARED_VPS_CAPACITY_STABLE_PROBES=2 \
    SHARED_VPS_CAPACITY_RETRY_DELAY_SECONDS=0 \
    SHARED_VPS_CAPACITY_DOCKER_TIMEOUT_SECONDS=5 \
    bash "$test_root/scripts/wait-for-shared-vps-docker-capacity.sh"
}

: >"$test_tmp_dir/calls"
write_capacity 1.25 524288 10.00
run_capacity ready
test "$(grep -Fc 'info' "$test_tmp_dir/calls")" -eq 2
test "$(grep -Fc 'compose version' "$test_tmp_dir/calls")" -eq 2

: >"$test_tmp_dir/calls"
write_capacity 8.00 524288 10.00
if run_capacity ready; then
  echo "O gate aceitou load acima do limite." >&2
  exit 1
fi

: >"$test_tmp_dir/calls"
write_capacity 1.00 65536 10.00
if run_capacity ready; then
  echo "O gate aceitou memória disponível abaixo do limite." >&2
  exit 1
fi

: >"$test_tmp_dir/calls"
write_capacity 1.00 524288 80.00
if run_capacity ready; then
  echo "O gate aceitou pressão de I/O acima do limite." >&2
  exit 1
fi

: >"$test_tmp_dir/calls"
write_capacity 1.00 524288 10.00
if run_capacity unavailable; then
  echo "O gate aceitou um daemon Docker indisponível." >&2
  exit 1
fi

if SHARED_VPS_CAPACITY_MAX_ATTEMPTS=0 \
  bash "$test_root/scripts/wait-for-shared-vps-docker-capacity.sh" \
  >/dev/null 2>&1; then
  echo "O gate aceitou uma quantidade inválida de tentativas." >&2
  exit 1
fi

echo "Gate de capacidade do VPS compartilhado validado."
