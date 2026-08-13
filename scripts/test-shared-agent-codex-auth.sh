#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_root=$(mktemp -d)
trap 'rm -rf "$test_root"' EXIT
workflows=(
  customer-agent-worker-ci.yml
  financial-agent-worker-ci.yml
  growth-operator-worker-ci.yml
  experiment-strategist-worker-ci.yml
  meta-ad-approver-worker-ci.yml
  landing-generator-agent-worker-ci.yml
)

for workflow in "${workflows[@]}"; do
  path="$repo_root/.github/workflows/$workflow"
  grep -q "scripts/reconcile-agent-codex-auth.sh" "$path"
  grep -q "CODEX_HOME=/opt/growth-operator/codex-home" "$path"
  if grep -Eq 'install .*auth\.json .*agents/.*/codex-home/auth\.json' "$path"; then
    printf '[ARQUITETURA] %s ainda clona refresh token entre agentes.\n' "$workflow" >&2
    exit 1
  fi
done

for compose in \
  financial-agent-worker/docker-compose.yml \
  growth-operator-worker/docker-compose.yml \
  experiment-strategist-worker/docker-compose.yml \
  meta-ad-approver-worker/docker-compose.yml \
  landing-generator-agent-worker/docker-compose.yml; do
  grep -q 'CODEX_COMMAND: /workspace/marketing-hub/scripts/codex-oauth-session-safe.sh' "$repo_root/$compose"
done
grep -q 'CUSTOMER_AGENT_CODEX_EXECUTABLE: /workspace/scripts/codex-oauth-session-safe.sh' \
  "$repo_root/customer-agent-worker/docker-compose.yml"

grep -q 'AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home' "$repo_root/scripts/reconcile-agent-codex-auth.sh"
grep -q 'flock 9' "$repo_root/scripts/reconcile-agent-codex-auth.sh"
grep -q 'flock -w' "$repo_root/scripts/codex-oauth-session-safe.sh"
grep -q 'codex-app-server-device-login.mjs' "$repo_root/scripts/reconnect-agent-codex-device.sh"
grep -q "account/login/start.*chatgptDeviceCode" "$repo_root/scripts/codex-app-server-device-login.mjs"
grep -q "account/login/completed" "$repo_root/scripts/codex-app-server-device-login.mjs"
if grep -q 'codex logout' "$repo_root/scripts/reconnect-agent-codex-device.sh"; then
  printf '%s\n' '[ARQUITETURA] Reconexão apaga a sessão válida antes de confirmar a nova.' >&2
  exit 1
fi

mkdir -p "$test_root/bin" "$test_root/shared-home"
cat > "$test_root/bin/codex" <<'EOF'
#!/usr/bin/env bash
printf 'start:%s\n' "$1" >> "$CODEX_SERIALIZATION_PROBE"
sleep 1
printf 'finish:%s\n' "$1" >> "$CODEX_SERIALIZATION_PROBE"
EOF
chmod +x "$test_root/bin/codex"
export CODEX_SERIALIZATION_PROBE="$test_root/serialization.log"
PATH="$test_root/bin:$PATH" CODEX_HOME="$test_root/shared-home" \
  bash "$repo_root/scripts/codex-oauth-session-safe.sh" first &
first_pid=$!
PATH="$test_root/bin:$PATH" CODEX_HOME="$test_root/shared-home" \
  bash "$repo_root/scripts/codex-oauth-session-safe.sh" second &
second_pid=$!
wait "$first_pid" "$second_pid"
python3 - "$CODEX_SERIALIZATION_PROBE" <<'PY'
import pathlib, sys
events = pathlib.Path(sys.argv[1]).read_text().splitlines()
if len(events) != 4 or events[0].split(':')[0] != 'start' or events[1].split(':')[0] != 'finish' or events[2].split(':')[0] != 'start' or events[3].split(':')[0] != 'finish':
    raise SystemExit('[ARQUITETURA] Execuções Codex compartilharam a sessão OAuth simultaneamente.')
PY

cat > "$test_root/bin/fake-codex-app-server" <<'EOF'
#!/usr/bin/env node
const readline = require('node:readline');
const rl = readline.createInterface({ input: process.stdin });
const send = (value) => process.stdout.write(`${JSON.stringify(value)}\n`);
rl.on('line', (line) => {
  const message = JSON.parse(line);
  if (message.method === 'initialize') send({ id: message.id, result: { userAgent: 'fake' } });
  if (message.method === 'account/login/start') {
    send({ id: message.id, result: { loginId: 'login-1', verificationUrl: 'https://example.test/device', userCode: 'SAFE-CODE' } });
    setTimeout(() => send({ method: 'account/login/completed', params: { loginId: 'login-1', success: true } }), 10);
  }
  if (message.method === 'account/read') send({ id: message.id, result: { authMode: 'chatgpt', planType: 'plus' } });
});
EOF
chmod +x "$test_root/bin/fake-codex-app-server"
CODEX_APP_SERVER_COMMAND="$test_root/bin/fake-codex-app-server" \
  node "$repo_root/scripts/codex-app-server-device-login.mjs" > "$test_root/device-login.log"
grep -q 'SAFE-CODE' "$test_root/device-login.log"
grep -q 'modo chatgpt' "$test_root/device-login.log"

mkdir -p "$test_root/canonical" "$test_root/legacy/financial/codex-home"
printf '%s\n' '{"session":"old"}' > "$test_root/canonical/auth.json"
sleep 1
printf '%s\n' '{"session":"latest"}' > "$test_root/legacy/financial/codex-home/auth.json"
AGENT_CODEX_CANONICAL_HOME="$test_root/canonical" \
AGENT_CODEX_LEGACY_ROOT="$test_root/legacy" \
AGENT_CODEX_LOCK_FILE="$test_root/reconcile.lock" \
AGENT_CODEX_OWNER_UID="$(id -u)" \
AGENT_CODEX_OWNER_GID="$(id -g)" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null
grep -q '"old"' "$test_root/canonical/auth.json"

sleep 1
printf '%s\n' '{"session":"canonical-current"}' > "$test_root/canonical/auth.json"
AGENT_CODEX_CANONICAL_HOME="$test_root/canonical" \
AGENT_CODEX_LEGACY_ROOT="$test_root/legacy" \
AGENT_CODEX_LOCK_FILE="$test_root/reconcile.lock" \
AGENT_CODEX_OWNER_UID="$(id -u)" \
AGENT_CODEX_OWNER_GID="$(id -g)" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null
grep -q '"canonical-current"' "$test_root/canonical/auth.json"

mkdir -p "$test_root/empty-canonical" "$test_root/empty-legacy"
if AGENT_CODEX_CANONICAL_HOME="$test_root/empty-canonical" \
  AGENT_CODEX_LEGACY_ROOT="$test_root/empty-legacy" \
  AGENT_CODEX_LOCK_FILE="$test_root/empty.lock" \
  AGENT_CODEX_OWNER_UID="$(id -u)" \
  AGENT_CODEX_OWNER_GID="$(id -g)" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null 2>&1; then
  printf '%s\n' '[ARQUITETURA] Reconciliação aceitou ausência de sessão Codex.' >&2
  exit 1
fi
printf '%s\n' '[ARQUITETURA] Sessão Codex única protegida contra clones de refresh token.'
