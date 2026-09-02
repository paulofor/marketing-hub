#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$test_tmp_dir"' EXIT

mkdir -p "$test_tmp_dir/bin"
cat >"$test_tmp_dir/bin/docker" <<'DOCKER_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >>"${DOCKER_DOUBLE_CALLS:?}"
attempt="$(wc -l <"$DOCKER_DOUBLE_CALLS")"

case "${DOCKER_DOUBLE_MODE:?}" in
  recover)
    if [[ "$attempt" -eq 1 ]]; then
      exit 124
    fi
    ;;
  fatal)
    echo "invalid compose project" >&2
    exit 1
    ;;
  exhaust)
    echo "context deadline exceeded" >&2
    exit 1
    ;;
  *)
    echo "modo do double inválido" >&2
    exit 2
    ;;
esac
DOCKER_DOUBLE
chmod +x "$test_tmp_dir/bin/docker"

run_compose_up() {
  : >"$test_tmp_dir/calls"
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$test_tmp_dir/calls" \
    DOCKER_DOUBLE_MODE="$1" \
    DOCKER_COMPOSE_UP_MAX_ATTEMPTS="${2:-2}" \
    DOCKER_COMPOSE_UP_RETRY_DELAY_SECONDS=0 \
    DOCKER_COMPOSE_UP_TIMEOUT_SECONDS=5 \
    DOCKER_COMPOSE_UP_FORCE_RECREATE=true \
    bash "$test_root/scripts/docker-compose-up-with-transient-retry.sh" \
      -f docker-compose.yml -f docker-compose.deploy.yml
}

run_compose_up recover
test "$(wc -l <"$test_tmp_dir/calls")" -eq 2
sed -n '1p' "$test_tmp_dir/calls" \
  | grep -Fx 'compose -f docker-compose.yml -f docker-compose.deploy.yml up -d --force-recreate --remove-orphans' >/dev/null
sed -n '2p' "$test_tmp_dir/calls" \
  | grep -Fx 'compose -f docker-compose.yml -f docker-compose.deploy.yml up -d --remove-orphans' >/dev/null

if run_compose_up fatal; then
  echo "Uma falha funcional do Compose não pode ser repetida." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 1

if run_compose_up exhaust 2; then
  echo "O helper deveria falhar após esgotar a reconciliação." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 2

if DOCKER_COMPOSE_UP_MAX_ATTEMPTS=0 \
  bash "$test_root/scripts/docker-compose-up-with-transient-retry.sh" \
    -f docker-compose.yml >/dev/null 2>&1; then
  echo "O helper aceitou um limite inválido de tentativas." >&2
  exit 1
fi

echo "Reconciliação transitória do Docker Compose validada."
