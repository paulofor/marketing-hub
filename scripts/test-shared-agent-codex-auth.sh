#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
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

grep -q 'AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home' "$repo_root/scripts/reconcile-agent-codex-auth.sh"
grep -q 'flock 9' "$repo_root/scripts/reconcile-agent-codex-auth.sh"

test_root=$(mktemp -d)
trap 'rm -rf "$test_root"' EXIT
mkdir -p "$test_root/canonical" "$test_root/legacy/financial/codex-home"
printf '%s\n' '{"session":"old"}' > "$test_root/canonical/auth.json"
sleep 1
printf '%s\n' '{"session":"latest"}' > "$test_root/legacy/financial/codex-home/auth.json"
AGENT_CODEX_CANONICAL_HOME="$test_root/canonical" \
AGENT_CODEX_LEGACY_ROOT="$test_root/legacy" \
AGENT_CODEX_LOCK_FILE="$test_root/reconcile.lock" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null
grep -q '"latest"' "$test_root/canonical/auth.json"

sleep 1
printf '%s\n' '{"session":"canonical-current"}' > "$test_root/canonical/auth.json"
AGENT_CODEX_CANONICAL_HOME="$test_root/canonical" \
AGENT_CODEX_LEGACY_ROOT="$test_root/legacy" \
AGENT_CODEX_LOCK_FILE="$test_root/reconcile.lock" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null
grep -q '"canonical-current"' "$test_root/canonical/auth.json"

mkdir -p "$test_root/empty-canonical" "$test_root/empty-legacy"
if AGENT_CODEX_CANONICAL_HOME="$test_root/empty-canonical" \
  AGENT_CODEX_LEGACY_ROOT="$test_root/empty-legacy" \
  AGENT_CODEX_LOCK_FILE="$test_root/empty.lock" \
  bash "$repo_root/scripts/reconcile-agent-codex-auth.sh" >/dev/null 2>&1; then
  printf '%s\n' '[ARQUITETURA] Reconciliação aceitou ausência de sessão Codex.' >&2
  exit 1
fi
printf '%s\n' '[ARQUITETURA] Sessão Codex única protegida contra clones de refresh token.'
