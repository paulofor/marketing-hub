#!/usr/bin/env bash
set -euo pipefail

test_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash "$test_root/scripts/run-docker-homologation.sh" bash "$0"
fi
test_dir="$(mktemp -d)"
fixture="$test_root/scripts/fixtures/agent-vps-disk"
test_project="${AGENT_VPS_DISK_TEST_COMPOSE_PROJECT:-image-proof-${AIHUB_HOMOLOGATION_SESSION}}"
compose=(docker compose -p "$test_project" -f "$fixture/compose.yml")
test_sha="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
test_old="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/image-old:$test_sha"
test_new="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/image-new:$test_sha"
export AGENT_VPS_DISK_TEST_IMAGE="$test_old" AGENT_VPS_DISK_TEST_ROLLBACK_IMAGE="$test_old"

cleanup_test() {
  local result="$?"
  trap - EXIT
  "${compose[@]}" down --volumes --remove-orphans || result=1
  rm -rf -- "$test_dir"
  exit "$result"
}
trap cleanup_test EXIT
AIHUB_HOMOLOGATION_IMAGE_TAG="$test_sha" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" image-old "$fixture"
cp "$fixture/Dockerfile" "$fixture/proof.txt" "$test_dir/"
printf 'nova versão sintética\n' >"$test_dir/proof.txt"
AIHUB_HOMOLOGATION_IMAGE_TAG="$test_sha" \
  bash "$test_root/scripts/docker-build-temporary-image.sh" image-new "$test_dir"
"${compose[@]}" up -d --no-build --pull never proof
"${compose[@]}" create --no-build rollback
test_old_id="$(docker image inspect --format '{{.Id}}' "$test_old")"
test_new_id="$(docker image inspect --format '{{.Id}}' "$test_new")"
test_rollback="$("${compose[@]}" ps --all --quiet rollback)"
node "$test_root/scripts/agent-image-bundle.mjs" pack "$test_dir/bundle" "$test_new"
node "$test_root/scripts/agent-image-bundle.mjs" verify "$test_dir/bundle" "$test_new"
docker image rm "$test_new"

# O transporte usa double para a engine remota da sandbox, sem expor seu socket.
# Exportação, gzip, carga, identidade, Compose, containers e volume são reais.
# OpenSSH real é validado separadamente por test-configure-vps-ssh-fallback-e2e.sh.
mkdir "$test_dir/bin"
cat >"$test_dir/bin/ssh" <<'SSH_DOUBLE'
#!/usr/bin/env bash
set -euo pipefail
test "$1" = -F
test "$3" = root@fixture.local
command_text="${*:4}"
printf '%s\n' "$command_text" >>"${IMAGE_E2E_CALLS:?}"
if [[ "$command_text" =~ ^AGENT_VPS_DISK_MIN_FREE_MB=[0-9]+\ bash\ -s$ ]]; then
  cat >/dev/null
  echo 'Capacidade sintética aprovada na engine de teste.'
elif [[ "$command_text" == docker\ image\ inspect* ]]; then
  bash -c "$command_text" | node -e '
    let payload = "";
    process.stdin.on("data", chunk => { payload += chunk; });
    process.stdin.on("end", () => {
      const inspected = JSON.parse(payload);
      inspected[0].Id = `sha256:${"c".repeat(64)}`;
      process.stdout.write(`${JSON.stringify(inspected)}\n`);
    });
  '
else
  exec bash -c "$command_text"
fi
SSH_DOUBLE
chmod 700 "$test_dir/bin/ssh"
send_output="$(PATH="$test_dir/bin:$PATH" SSH_DEPLOY_READY=true SSH_COMMON_ARGS="-F $test_dir/ssh-config" \
  IMAGE_E2E_CALLS="$test_dir/calls" \
  node "$test_root/scripts/agent-image-bundle.mjs" send "$test_dir/bundle" root@fixture.local "$test_new")"
printf '%s\n' "$send_output"
grep -Fq 'ID do store variou' <<<"$send_output"
[[ "$(docker image inspect --format '{{.Id}}' "$test_new")" = "$test_new_id" ]]
[[ "$(docker image inspect --format '{{.Id}}' "$test_old")" = "$test_old_id" ]]
export AGENT_VPS_DISK_TEST_IMAGE="$test_new"
"${compose[@]}" up -d --no-build --pull never proof
for attempt in {1..20}; do
  if "${compose[@]}" exec -T proof cmp /fixture-proof.txt /data/proof.txt; then break; fi
  printf 'Aguardando nova imagem: tentativa=%s\n' "$attempt"
  sleep 0.2
done
"${compose[@]}" exec -T proof cat /data/proof.txt | grep -Fx 'nova versão sintética'
[[ "$("${compose[@]}" ps --all --quiet rollback)" = "$test_rollback" ]]
[[ "$(docker image inspect --format '{{.Id}}' "$test_old")" = "$test_old_id" ]]
[[ "$(grep -c 'AGENT_VPS_DISK_MIN_FREE_MB=' "$test_dir/calls")" = 2 ]]
if grep -Eq 'docker (build|compose build)|--build' "$test_dir/calls"; then
  echo 'Build remoto inesperado.' >&2
  exit 1
fi
echo 'Docker real: pacote carregado, identidade íntegra, serviço atualizado sem build e rollback preservado.'
