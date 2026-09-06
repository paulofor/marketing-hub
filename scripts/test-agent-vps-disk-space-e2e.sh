#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash "$test_root/scripts/run-docker-homologation.sh" bash "$0"
fi

test_dir="$(mktemp -d)"
fixture="$test_root/scripts/fixtures/agent-vps-disk"
export AGENT_VPS_DISK_TEST_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/disk-proof:latest"
export AGENT_VPS_DISK_TEST_ROLLBACK_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/disk-proof:rollback"
test_project="${AGENT_VPS_DISK_TEST_COMPOSE_PROJECT:-disk-proof-${AIHUB_HOMOLOGATION_SESSION}}"
compose=(docker compose -p "$test_project" -f "$fixture/compose.yml")

cleanup_test() {
  local cleanup_status="$?"
  trap - EXIT
  "${compose[@]}" down --volumes --remove-orphans || cleanup_status=1
  rm -rf "$test_dir"
  exit "$cleanup_status"
}
trap cleanup_test EXIT

bash "$test_root/scripts/docker-build-temporary-image.sh" disk-proof "$fixture"
docker image tag "$AGENT_VPS_DISK_TEST_IMAGE" "$AGENT_VPS_DISK_TEST_ROLLBACK_IMAGE"
"${compose[@]}" up -d --no-build proof
"${compose[@]}" create --no-build rollback
for attempt in {1..20}; do
  if "${compose[@]}" exec -T proof cmp /fixture-proof.txt /data/proof.txt; then break; fi
  printf 'Aguardando fixture local: tentativa=%s\n' "$attempt"
  sleep 0.2
done
"${compose[@]}" exec -T proof cmp /fixture-proof.txt /data/proof.txt
test_containers_before="$("${compose[@]}" ps --all --quiet | sort)"
test_image_before="$(docker image inspect --format '{{.Id}}' "$AGENT_VPS_DISK_TEST_IMAGE")"

# Na sandbox a engine pode ser remota. Só o caminho da medição usa double nesse caso;
# build, coleta e inspeção de imagens, containers e volumes continuam na engine real.
test_command_path="$PATH"
test_docker_root="$(docker info --format '{{.DockerRootDir}}')"
if [[ ! -d "$test_docker_root" ]]; then
  mkdir -p "$test_dir/bin"
  export DISK_E2E_DOCKER_EXECUTABLE
  DISK_E2E_DOCKER_EXECUTABLE="$(command -v docker)"
  export DISK_E2E_MEASUREMENT_PATH="$test_dir"
  cat >"$test_dir/bin/docker" <<'REMOTE_ENGINE_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$*" = 'info --format {{.DockerRootDir}}' ]]; then
  printf '%s\n' "$DISK_E2E_MEASUREMENT_PATH"
else
  exec "$DISK_E2E_DOCKER_EXECUTABLE" "$@"
fi
REMOTE_ENGINE_DOUBLE
  chmod +x "$test_dir/bin/docker"
  test_command_path="$test_dir/bin:$PATH"
  echo "Engine remota: caminho da medição sintético; coleta e preservação testadas no Docker real."
fi

# Limite sintético impossível: executa a coleta real e exige bloqueio se o disco não atender.
test_status=0
PATH="$test_command_path" AGENT_VPS_DISK_MIN_FREE_MB=99999999 AGENT_VPS_DISK_TIMEOUT_SECONDS=30 \
  AGENT_VPS_DISK_LOCK_FILE="$test_dir/disk.lock" \
  bash "$test_root/scripts/ensure-agent-vps-disk-space.sh" >"$test_dir/result.log" 2>&1 \
  || test_status="$?"
cat "$test_dir/result.log"
[[ "$test_status" = 1 ]]
grep -q 'BLOCKED após coleta controlada' "$test_dir/result.log"
[[ "$("${compose[@]}" ps --all --quiet | sort)" = "$test_containers_before" ]]
[[ "$(docker image inspect --format '{{.Id}}' "$AGENT_VPS_DISK_TEST_IMAGE")" = "$test_image_before" ]]
[[ "$(docker image inspect --format '{{.Id}}' "$AGENT_VPS_DISK_TEST_ROLLBACK_IMAGE")" = "$test_image_before" ]]
"${compose[@]}" exec -T proof cmp /fixture-proof.txt /data/proof.txt
echo "Engine real: imagem, tag de rollback, containers ativo/parado e volume preservados; disco insuficiente bloqueado."
