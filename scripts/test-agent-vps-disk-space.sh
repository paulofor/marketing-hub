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
  'builder prune --force --filter until=24h --keep-storage 2GB'|'builder prune --force --filter until=1h --keep-storage 1GB'|'builder prune --force --filter until=0s --keep-storage 1GB')
    [[ "$DISK_TEST_MODE" != prune-failure ]] || exit 1
    if [[ "$DISK_TEST_MODE" = timeout ]]; then sleep 10; fi
    if [[ "$*" = *until=0s* ]]; then
      [[ "$DISK_TEST_MODE" != fresh-failure ]] || exit 1
      touch "$DISK_TEST_DIR/fresh-pruned"
    fi
    touch "$DISK_TEST_DIR/pruned"
    if [[ "$*" = *until=1h* ]]; then
      [[ "$DISK_TEST_MODE" != recent-failure ]] || exit 1
      touch "$DISK_TEST_DIR/recent-pruned"
    fi
    ;;
  'image prune --force --filter until=24h'|'image prune --force --filter until=1h')
    [[ "$DISK_TEST_MODE" != image-prune-failure ]] || exit 1
    touch "$DISK_TEST_DIR/image-pruned"
    ;;
  'image ls --all --no-trunc --format {{.Repository}}|{{.Tag}}|{{.ID}}')
    [[ "$DISK_TEST_MODE" != image-list-failure ]] || exit 1
    if [[ "$DISK_TEST_MODE" =~ ^(recover-managed|recover-managed-alias|recover-managed-pressure|managed-rm-failure|retention-ready)$ ]]; then
      printf '%s\n' \
        'marketing-hub/meta-ad-approver-worker|ffffffffffffffffffffffffffffffffffffffff|sha256:1111111111111111111111111111111111111111111111111111111111111111' \
        'marketing-hub/meta-ad-approver-worker|eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee|sha256:2222222222222222222222222222222222222222222222222222222222222222' \
        'marketing-hub/meta-ad-approver-worker|abababababababababababababababababababab|sha256:2222222222222222222222222222222222222222222222222222222222222222'
      if [[ ! -f "$DISK_TEST_DIR/second-rollback-removed" ]]; then
        printf '%s\n' 'marketing-hub/meta-ad-approver-worker|dddddddddddddddddddddddddddddddddddddddd|sha256:3333333333333333333333333333333333333333333333333333333333333333'
      fi
      if [[ ! -f "$DISK_TEST_DIR/managed-removed" ]]; then
        printf '%s\n' 'marketing-hub/meta-ad-approver-worker|cccccccccccccccccccccccccccccccccccccccc|sha256:4444444444444444444444444444444444444444444444444444444444444444'
      fi
      printf '%s\n' \
        'marketing-hub/meta-ad-approver-worker|latest|sha256:6666666666666666666666666666666666666666666666666666666666666666' \
        'unrelated/system|bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|sha256:5555555555555555555555555555555555555555555555555555555555555555' \
        'ghcr.io/paulofor/product-discovery-worker|aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa|sha256:7777777777777777777777777777777777777777777777777777777777777777'
      if [[ "$DISK_TEST_MODE" = recover-managed-alias ]]; then
        printf '%s\n' 'marketing-hub/meta-ad-approver-worker|bcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbc|sha256:4444444444444444444444444444444444444444444444444444444444444444'
      fi
    elif [[ "$DISK_TEST_MODE" =~ ^retention-pde(-tie|-subsecond|-legacy-tie)?$ ]]; then
      printf '%s\n' \
        'ghcr.io/paulofor/pde-platform-backend|ffffffffffffffffffffffffffffffffffffffff|sha256:1111111111111111111111111111111111111111111111111111111111111111' \
        'ghcr.io/paulofor/pde-platform-backend|eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee|sha256:2222222222222222222222222222222222222222222222222222222222222222' \
        'ghcr.io/paulofor/pde-platform-backend|dddddddddddddddddddddddddddddddddddddddd|sha256:3333333333333333333333333333333333333333333333333333333333333333' \
        'ghcr.io/paulofor/pde-platform-backend|cccccccccccccccccccccccccccccccccccccccc|sha256:4444444444444444444444444444444444444444444444444444444444444444' \
        'ghcr.io/paulofor/pde-platform-backend|latest|sha256:6666666666666666666666666666666666666666666666666666666666666666' \
        'ghcr.io/paulofor/not-pde|bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|sha256:5555555555555555555555555555555555555555555555555555555555555555'
    fi
    ;;
  'container ls --all --no-trunc --quiet')
    [[ "$DISK_TEST_MODE" != container-list-failure ]] || exit 1
    if [[ "$DISK_TEST_MODE" =~ ^(recover-managed|recover-managed-alias|recover-managed-pressure|managed-rm-failure|retention-ready|retention-pde|retention-pde-tie|retention-pde-subsecond|retention-pde-legacy-tie)$ ]]; then
      printf '%s\n' '9999999999999999999999999999999999999999999999999999999999999999'
    fi
    ;;
  'container inspect --format {{.Image}} 9999999999999999999999999999999999999999999999999999999999999999')
    printf '%s\n' 'sha256:1111111111111111111111111111111111111111111111111111111111111111'
    ;;
  "image inspect --format {{ index .Config.Labels \"org.opencontainers.image.created\" }} "*)
    image_reference="${*:5}"
    if [[ "$DISK_TEST_MODE" = retention-pde ]]; then
      case "$image_reference" in
        *:ffffffffffffffffffffffffffffffffffffffff) printf '%s\n' '2026-09-07T10:00:00Z' ;;
        *:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee) printf '%s\n' '2026-09-07T09:00:00Z' ;;
        *:dddddddddddddddddddddddddddddddddddddddd) printf '%s\n' '2026-09-07T08:00:00Z' ;;
        *:cccccccccccccccccccccccccccccccccccccccc) printf '%s\n' '2026-09-04T12:00:00Z' ;;
        *) printf '\n' ;;
      esac
    elif [[ "$DISK_TEST_MODE" = retention-pde-tie ]]; then
      case "$image_reference" in
        *:ffffffffffffffffffffffffffffffffffffffff) printf '%s\n' '2026-09-07T10:00:00Z' ;;
        *) printf '%s\n' '2026-09-07T09:00:00Z' ;;
      esac
    else
      printf '\n'
    fi
    ;;
  image\ inspect\ --format\ \{\{.Metadata.LastTagTime\}\}\ *)
    image_reference="${*:5}"
    case "$image_reference" in
      *:ffffffffffffffffffffffffffffffffffffffff) printf '%s\n' '2026-09-07T10:00:00Z' ;;
      *:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee|*:abababababababababababababababababababab)
        if [[ "$DISK_TEST_MODE" = retention-pde-subsecond ]]; then
          printf '%s\n' '2026-09-07T09:00:00.300000000Z'
        elif [[ "$DISK_TEST_MODE" = retention-pde-legacy-tie ]]; then
          printf '%s\n' '0001-01-01T00:00:00Z'
        else
          printf '%s\n' '2026-09-07T09:00:00Z'
        fi
        ;;
      *:dddddddddddddddddddddddddddddddddddddddd)
        if [[ "$DISK_TEST_MODE" = retention-pde-subsecond ]]; then
          printf '%s\n' '2026-09-07T09:00:00.200000000Z'
        elif [[ "$DISK_TEST_MODE" = retention-pde-legacy-tie ]]; then
          printf '%s\n' '0001-01-01T00:00:00Z'
        elif [[ "$DISK_TEST_MODE" = retention-pde-tie ]]; then
          printf '%s\n' '2026-09-07T09:00:00Z'
        else
          printf '%s\n' '2026-09-07T08:00:00Z'
        fi
        ;;
      *:cccccccccccccccccccccccccccccccccccccccc|*:bcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbc)
        if [[ "$DISK_TEST_MODE" = retention-pde-subsecond ]]; then
          printf '%s\n' '2026-09-07T09:00:00.100000000Z'
        elif [[ "$DISK_TEST_MODE" = retention-pde-legacy-tie ]]; then
          printf '%s\n' '0001-01-01T00:00:00Z'
        elif [[ "$DISK_TEST_MODE" = retention-pde-tie ]]; then
          printf '%s\n' '2026-09-07T09:00:00Z'
        else
          printf '%s\n' '2026-09-04T12:00:00Z'
        fi
        ;;
      *:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa) printf '%s\n' '2026-09-05T12:00:00Z' ;;
      *) exit 71 ;;
    esac
    ;;
  image\ inspect\ --format\ \{\{.Created\}\}\ *)
    image_reference="${*:5}"
    case "$image_reference" in
      *:ffffffffffffffffffffffffffffffffffffffff) printf '%s\n' '2026-09-06T17:00:00Z' ;;
      *:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee) printf '%s\n' '2026-09-06T16:00:00Z' ;;
      *:abababababababababababababababababababab) printf '%s\n' '2026-09-06T16:00:00Z' ;;
      *:dddddddddddddddddddddddddddddddddddddddd)
        if [[ "$DISK_TEST_MODE" = retention-pde-legacy-tie ]]; then
          printf '%s\n' '2026-09-06T16:00:00Z'
        else
          printf '%s\n' '2026-09-06T15:00:00Z'
        fi
        ;;
      *:cccccccccccccccccccccccccccccccccccccccc|*:bcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbc)
        if [[ "$DISK_TEST_MODE" = retention-pde-legacy-tie ]]; then
          printf '%s\n' '2026-09-06T16:00:00Z'
        else
          printf '%s\n' '2026-09-04T12:00:00Z'
        fi
        ;;
      *:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa) printf '%s\n' '2026-09-05T12:00:00Z' ;;
      *) exit 71 ;;
    esac
    ;;
  image\ rm\ marketing-hub/meta-ad-approver-worker:cccccccccccccccccccccccccccccccccccccccc)
    [[ "$DISK_TEST_MODE" != managed-rm-failure ]] || exit 1
    touch "$DISK_TEST_DIR/managed-removed"
    ;;
  image\ rm\ marketing-hub/meta-ad-approver-worker:bcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbcbc)
    touch "$DISK_TEST_DIR/alias-removed"
    ;;
  image\ rm\ marketing-hub/meta-ad-approver-worker:dddddddddddddddddddddddddddddddddddddddd)
    touch "$DISK_TEST_DIR/second-rollback-removed"
    ;;
  image\ rm\ ghcr.io/paulofor/pde-platform-backend:cccccccccccccccccccccccccccccccccccccccc)
    touch "$DISK_TEST_DIR/managed-removed"
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
  || "$DISK_TEST_MODE" = retention-ready || "$DISK_TEST_MODE" = retention-pde \
  || "$DISK_TEST_MODE" = retention-pde-tie \
  || "$DISK_TEST_MODE" = retention-pde-subsecond \
  || "$DISK_TEST_MODE" = retention-pde-legacy-tie \
  || ( "$DISK_TEST_MODE" = recover && -f "$DISK_TEST_DIR/pruned" ) \
  || ( "$DISK_TEST_MODE" = recover-recent && -f "$DISK_TEST_DIR/recent-pruned" ) \
  || ( "$DISK_TEST_MODE" = recover-fresh && -f "$DISK_TEST_DIR/fresh-pruned" ) \
  || ( "$DISK_TEST_MODE" = recover-dangling && -f "$DISK_TEST_DIR/image-pruned" ) \
  || ( "$DISK_TEST_MODE" = recover-managed && -f "$DISK_TEST_DIR/managed-removed" ) \
  || ( "$DISK_TEST_MODE" = recover-managed-pressure && -f "$DISK_TEST_DIR/second-rollback-removed" ) \
  || ( "$DISK_TEST_MODE" = recover-managed-alias && -f "$DISK_TEST_DIR/managed-removed" && -f "$DISK_TEST_DIR/alias-removed" ) ]]; then
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
  local case_mode="$1" expected_status="$2" expected_builder_prunes="$3"
  local expected_image_prunes="$4" expected_image_removals="$5"
  shift 5
  rm -f "$test_dir/pruned" "$test_dir/recent-pruned" "$test_dir/image-pruned" \
    "$test_dir/managed-removed" "$test_dir/fresh-pruned" "$test_dir/alias-removed" \
    "$test_dir/second-rollback-removed"
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
  local case_builder_prunes case_image_prunes case_image_removals
  case_builder_prunes="$(grep -c '^builder prune ' "$test_dir/calls" || true)"
  case_image_prunes="$(grep -c '^image prune ' "$test_dir/calls" || true)"
  case_image_removals="$(grep -c '^image rm ' "$test_dir/calls" || true)"
  [[ "$case_builder_prunes" = "$expected_builder_prunes" ]]
  [[ "$case_image_prunes" = "$expected_image_prunes" ]]
  [[ "$case_image_removals" = "$expected_image_removals" ]]
  printf 'PASS disk-case=%s mode=%s status=%s builderPrunes=%s imagePrunes=%s imageRemovals=%s\n' \
    "$case_mode" "${1:-reclaim}" "$case_status" "$case_builder_prunes" \
    "$case_image_prunes" "$case_image_removals"
}

run_case ready 0 0 0 0
run_case recover 0 1 0 0
grep -q 'READY após recuperação' "$test_dir/output"
run_case recover-recent 0 2 0 0
grep -q 'sem uso há 1h, com reserva de 1GB' "$test_dir/output"
run_case recover-fresh 0 3 0 0
grep -q 'sem uso há 0s, com reserva de 1GB' "$test_dir/output"
run_case fresh-failure 1 3 0 0
run_case recover-dangling 0 3 1 0
grep -q 'READY após recuperação de imagens sem tag' "$test_dir/output"
run_case recover-managed 0 3 2 1
grep -q 'READY após retenção controlada' "$test_dir/output"
grep -Fxq 'image rm marketing-hub/meta-ad-approver-worker:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
if grep -Eq '^image rm .*(ffffffff|eeeeeeee|abababab|dddddddd|latest|unrelated|product-discovery)' \
  "$test_dir/calls"; then
  echo "A coleta tentou remover imagem ativa, rollback ou referência fora do escopo." >&2
  exit 1
fi
run_case recover-managed-alias 0 3 2 2
run_case recover-managed-pressure 0 3 2 2
grep -q 'aplicando piso seguro de 1 sob pressão' "$test_dir/output"
grep -Fxq 'image rm marketing-hub/meta-ad-approver-worker:dddddddddddddddddddddddddddddddddddddddd' "$test_dir/calls"
if grep -Eq '^image rm .*(ffffffff|eeeeeeee|abababab|latest|unrelated|product-discovery)' \
  "$test_dir/calls"; then
  echo "A pressão de capacidade tentou remover imagem ativa, rollback mínimo ou referência fora do escopo." >&2
  exit 1
fi
AGENT_VPS_DISK_MIN_ROLLBACK_VERSIONS=2 run_case managed-rm-failure 1 3 2 2
grep -q 'referência preservada' "$test_dir/output"
run_case recent-failure 1 2 0 0
run_case full 1 3 2 0
grep -q 'BLOCKED após coleta controlada' "$test_dir/output"
run_case full 1 0 0 0 check
run_case inode-full 1 3 2 0
run_case docker-failure 1 0 0 0
run_case df-failure 1 0 0 0
run_case invalid-df 1 0 0 0
run_case prune-failure 1 1 0 0
run_case image-prune-failure 1 3 1 0
run_case image-list-failure 1 3 2 0
run_case container-list-failure 1 3 2 0
run_case timeout 1 1 0 0
grep -q 'coleta falhou ou excedeu' "$test_dir/output"
run_case ready 2 0 0 0 invalid-mode
run_case retention-ready 0 0 0 1 retention
grep -q 'retenção preventiva concluída' "$test_dir/output"
grep -Fxq 'image rm marketing-hub/meta-ad-approver-worker:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
AGENT_VPS_DISK_PROTECTED_TAG=cccccccccccccccccccccccccccccccccccccccc \
  run_case retention-ready 0 0 0 0 retention
grep -q 'preservando imagem da publicação atual' "$test_dir/output"
run_case retention-pde 0 0 0 1 retention
grep -Fxq 'image rm ghcr.io/paulofor/pde-platform-backend:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
run_case retention-pde-tie 0 0 0 1 retention
grep -Fxq 'image rm ghcr.io/paulofor/pde-platform-backend:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
run_case retention-pde-subsecond 0 0 0 1 retention
grep -Fxq 'image rm ghcr.io/paulofor/pde-platform-backend:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
run_case retention-pde-legacy-tie 0 0 0 1 retention
grep -Fxq 'image rm ghcr.io/paulofor/pde-platform-backend:cccccccccccccccccccccccccccccccccccccccc' "$test_dir/calls"
AGENT_VPS_DISK_MIN_FREE_MB=0 run_case ready 2 0 0 0
AGENT_VPS_DISK_MIN_FREE_MB=invalid run_case ready 2 0 0 0
AGENT_VPS_DISK_ROLLBACK_VERSIONS=0 run_case ready 2 0 0 0
AGENT_VPS_DISK_MIN_ROLLBACK_VERSIONS=0 run_case ready 2 0 0 0
AGENT_VPS_DISK_MIN_ROLLBACK_VERSIONS=3 run_case ready 2 0 0 0
AGENT_VPS_DISK_PROTECTED_TAG=latest run_case ready 2 0 0 0

exec 8>"$test_dir/disk.lock"
flock -n 8
run_case full 1 0 0 0
grep -q 'outra verificação' "$test_dir/output"
flock -u 8

echo "36 cenários de disco, retenção preventiva/adaptativa, falhas e concorrência aprovados."
