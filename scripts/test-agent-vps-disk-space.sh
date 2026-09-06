#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
mkdir -p "$test_dir/bin" "$test_dir/docker-root"

cat >"$test_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >>"${DISK_TEST_DIR:?}/calls"
case "$*" in
  'info --format {{.DockerRootDir}}')
    [[ "$DISK_TEST_MODE" != docker-failure ]] || exit 1
    printf '%s/docker-root\n' "$DISK_TEST_DIR"
    ;;
  'builder prune --force --filter until=24h --keep-storage 2GB'|'builder prune --force --filter until=1h --keep-storage 1GB')
    [[ "$DISK_TEST_MODE" != prune-failure ]] || exit 1
    if [[ "$DISK_TEST_MODE" = timeout ]]; then sleep 10; fi
    touch "$DISK_TEST_DIR/pruned"
    if [[ "$*" = *until=1h* ]]; then
      [[ "$DISK_TEST_MODE" != recent-failure ]] || exit 1
      touch "$DISK_TEST_DIR/recent-pruned"
    fi
    ;;
  *) echo "Operação Docker não permitida: $*" >&2; exit 70 ;;
esac
DOCKER_DOUBLE

cat >"$test_dir/bin/df" <<'DF_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail
[[ "$DISK_TEST_MODE" != df-failure ]] || exit 1
if [[ "$DISK_TEST_MODE" = invalid-df ]]; then echo "Filesystem invalid"; exit 0; fi
disk_test_available=0
if [[ "$DISK_TEST_MODE" = ready || "$DISK_TEST_MODE" = inode-full \
  || ( "$DISK_TEST_MODE" = recover && -f "$DISK_TEST_DIR/pruned" ) \
  || ( "$DISK_TEST_MODE" = recover-recent && -f "$DISK_TEST_DIR/recent-pruned" ) ]]; then
  disk_test_available=8388608
fi
if [[ "$1" = -Pi ]]; then
  disk_test_available=100000
  [[ "$DISK_TEST_MODE" != inode-full ]] || disk_test_available=0
fi
printf 'Filesystem 1024-blocks Used Available Capacity Mounted on\n'
printf '/dev/test 16000000 1 %s 50%% /\n' "$disk_test_available"
DF_DOUBLE
chmod +x "$test_dir/bin/"*

run_case() {
  local case_mode="$1" expected_status="$2" expected_prunes="$3"
  shift 3
  rm -f "$test_dir/pruned" "$test_dir/recent-pruned"
  : >"$test_dir/calls"
  local case_status=0
  PATH="$test_dir/bin:$PATH" DISK_TEST_DIR="$test_dir" DISK_TEST_MODE="$case_mode" \
    AGENT_VPS_DISK_LOCK_FILE="$test_dir/disk.lock" AGENT_VPS_DISK_TIMEOUT_SECONDS=1 \
    bash "$test_root/scripts/ensure-agent-vps-disk-space.sh" "$@" \
    >"$test_dir/output" 2>&1 || case_status="$?"
  if [[ "$case_status" != "$expected_status" ]]; then
    cat "$test_dir/output" >&2
    echo "Caso $case_mode: status=$case_status esperado=$expected_status" >&2
    exit 1
  fi
  local case_prunes
  case_prunes="$(grep -c '^builder prune ' "$test_dir/calls" || true)"
  [[ "$case_prunes" = "$expected_prunes" ]]
  printf 'PASS disk-case=%s mode=%s status=%s prunes=%s\n' "$case_mode" "${1:-reclaim}" "$case_status" "$case_prunes"
}

run_case ready 0 0
run_case recover 0 1
grep -q 'READY após recuperação' "$test_dir/output"
run_case recover-recent 0 2
grep -q 'sem uso há 1h, com reserva de 1GB' "$test_dir/output"
run_case recent-failure 1 2
run_case full 1 2
grep -q 'BLOCKED após coleta limitada' "$test_dir/output"
run_case full 1 0 check
run_case inode-full 1 2
run_case docker-failure 1 0
run_case df-failure 1 0
run_case invalid-df 1 0
run_case prune-failure 1 1
run_case timeout 1 1
grep -q 'coleta falhou ou excedeu' "$test_dir/output"
run_case ready 2 0 invalid-mode
AGENT_VPS_DISK_MIN_FREE_MB=0 run_case ready 2 0
AGENT_VPS_DISK_MIN_FREE_MB=invalid run_case ready 2 0

exec 8>"$test_dir/disk.lock"
flock -n 8
run_case full 1 0
grep -q 'outra verificação' "$test_dir/output"
flock -u 8

echo "16 cenários de disco, retenção, falhas e concorrência aprovados."
