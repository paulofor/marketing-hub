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
attempt="$(wc -l <"${DOCKER_DOUBLE_CALLS}")"

case "${DOCKER_DOUBLE_MODE:?}" in
  recover)
    if [[ "$attempt" -lt 3 ]]; then
      echo "failed to do request: TLS handshake timeout" >&2
      exit 1
    fi
    ;;
  fatal)
    echo "unauthorized: authentication required" >&2
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

run_pull() {
  PATH="$test_tmp_dir/bin:$PATH" \
    DOCKER_DOUBLE_CALLS="$test_tmp_dir/calls" \
    DOCKER_DOUBLE_MODE="$1" \
    DOCKER_COMPOSE_PULL_RETRY_DELAY_SECONDS=0 \
    DOCKER_COMPOSE_PULL_MAX_ATTEMPTS="${2:-3}" \
    bash "$test_root/scripts/docker-compose-pull-with-transient-retry.sh" \
      -f docker-compose.yml -f docker-compose.deploy.yml
}

: >"$test_tmp_dir/calls"
run_pull recover
test "$(wc -l <"$test_tmp_dir/calls")" -eq 3
grep -Fx 'compose -f docker-compose.yml -f docker-compose.deploy.yml pull' "$test_tmp_dir/calls" >/dev/null

: >"$test_tmp_dir/calls"
if run_pull fatal; then
  echo "Uma falha de autenticação não pode ser repetida como transitória." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 1

: >"$test_tmp_dir/calls"
if run_pull exhaust 3; then
  echo "O helper deveria falhar quando o registry não recupera dentro do limite." >&2
  exit 1
fi
test "$(wc -l <"$test_tmp_dir/calls")" -eq 3

if DOCKER_COMPOSE_PULL_MAX_ATTEMPTS=0 \
  bash "$test_root/scripts/docker-compose-pull-with-transient-retry.sh" -f docker-compose.yml; then
  echo "O helper aceitou um limite inválido de tentativas." >&2
  exit 1
fi

echo "Retry transitório do Docker Compose validado."
