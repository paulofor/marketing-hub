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
retention_repository="ghcr.io/sandbox/pde-platform-backend"
retention_references=()
retention_temporary_references=()

cleanup_test() {
  local cleanup_status="$?"
  trap - EXIT
  if ((${#retention_references[@]} > 0)); then
    docker image rm "${retention_references[@]}" >/dev/null 2>&1 || true
  fi
  if ((${#retention_temporary_references[@]} > 0)); then
    docker image rm "${retention_temporary_references[@]}" >/dev/null 2>&1 || true
  fi
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

# Prova na engine real que a retenção roda mesmo com espaço, sem remover a imagem ativa
# nem as duas versões de rollback mais recentes do repositório PDE conhecido.
for retention_entry in \
  'stale:cccccccccccccccccccccccccccccccccccccccc' \
  'rollback-2:dddddddddddddddddddddddddddddddddddddddd' \
  'rollback-1:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee'; do
  retention_name="${retention_entry%%:*}"
  retention_tag="${retention_entry#*:}"
  retention_temporary_reference="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/retention-${retention_name}:latest"
  retention_reference="${retention_repository}:${retention_tag}"
  bash "$test_root/scripts/docker-build-temporary-image.sh" "retention-${retention_name}" \
    --label "com.marketinghub.fixture.retention-version=${retention_name}" \
    --label "org.opencontainers.image.created=2026-09-07T09:00:00Z" "$fixture"
  docker image tag "$retention_temporary_reference" "$retention_reference"
  retention_temporary_references+=("$retention_temporary_reference")
  retention_references+=("$retention_reference")
done
retention_active_reference="${retention_repository}:ffffffffffffffffffffffffffffffffffffffff"
docker image tag "$AGENT_VPS_DISK_TEST_IMAGE" "$retention_active_reference"
retention_references+=("$retention_active_reference")

PATH="$test_command_path" AGENT_VPS_DISK_MIN_FREE_MB=1 AGENT_VPS_DISK_TIMEOUT_SECONDS=30 \
  AGENT_VPS_DISK_LOCK_FILE="$test_dir/retention.lock" \
  bash "$test_root/scripts/ensure-agent-vps-disk-space.sh" retention \
  >"$test_dir/retention.log" 2>&1
cat "$test_dir/retention.log"
grep -q 'READY após retenção preventiva' "$test_dir/retention.log"
if docker image inspect "${retention_repository}:cccccccccccccccccccccccccccccccccccccccc" >/dev/null 2>&1; then
  echo "A retenção preventiva preservou indevidamente a terceira versão antiga." >&2
  exit 1
fi
for retained_tag in \
  ffffffffffffffffffffffffffffffffffffffff \
  eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee \
  dddddddddddddddddddddddddddddddddddddddd; do
  docker image inspect "${retention_repository}:${retained_tag}" >/dev/null
done
[[ "$(docker image inspect --format '{{.Id}}' "$retention_active_reference")" = "$test_image_before" ]]
"${compose[@]}" exec -T proof cmp /fixture-proof.txt /data/proof.txt
echo "Engine real: imagem, tag de rollback, containers ativo/parado e volume preservados; disco insuficiente bloqueado."
