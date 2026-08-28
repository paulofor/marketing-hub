#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
declare -A homes=(
  [customer-agent]=CUSTOMER_AGENT_CODEX_HOME
  [financial-agent]=FINANCIAL_AGENT_CODEX_HOME
  [growth-operator]=GROWTH_OPERATOR_CODEX_HOME
  [experiment-strategist]=EXPERIMENT_STRATEGIST_CODEX_HOME
  [meta-ad-approver]=META_AD_APPROVER_CODEX_HOME
  [landing-generator]=LANDING_GENERATOR_CODEX_HOME
  [communication-director]=COMMUNICATION_AGENT_CODEX_HOME
)

for agent in "${!homes[@]}"; do
  workflow="$repo_root/.github/workflows/${agent}-worker-ci.yml"
  if [[ "$agent" == landing-generator ]]; then
    workflow="$repo_root/.github/workflows/landing-generator-agent-worker-ci.yml"
  elif [[ "$agent" == communication-director ]]; then
    workflow="$repo_root/.github/workflows/communication-agent-worker-ci.yml"
  fi
  expected_home="/opt/growth-operator/agents/$agent/codex-home"
  grep -q "${homes[$agent]}=$expected_home" "$workflow"
  grep -q "install -d -o 10001 -g 10001 $expected_home" "$workflow"
  grep -q 'cancel-in-progress: false' "$workflow"
  grep -qE -- '- ["]?scripts/codex-app-server-device-login\.mjs["]?' "$workflow"
  grep -q 'rsync -az scripts/codex-app-server-device-login.mjs' "$workflow"
  if grep -qE 'reconcile-agent-codex-auth|CODEX_HOME=/opt/growth-operator/codex-home|install .*auth\.json .*codex-home/auth\.json' "$workflow"; then
    printf '[ARQUITETURA] %s ainda compartilha ou clona a identidade Codex.\n' "$workflow" >&2
    exit 1
  fi
done

landing_workflow="$repo_root/.github/workflows/landing-generator-agent-worker-ci.yml"
grep -q 'ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=10' "$landing_workflow"
grep -q 'Aguardando saúde do executor (tentativa' "$landing_workflow"

if grep -q 'group: shared-growth-agents-repository-deploy' \
  "$repo_root/.github/workflows"/*-worker-ci.yml; then
  printf '%s\n' '[ARQUITETURA] Deploys independentes ainda compartilham fila que cancela pendências intermediárias.' >&2
  exit 1
fi

unique_count=$(printf '%s\n' "${!homes[@]}" | sed 's#^#/opt/growth-operator/agents/#; s#$#/codex-home#' | sort -u | wc -l)
[[ "$unique_count" -eq 7 ]]
printf '%s\n' '[ARQUITETURA] Sete sessões Codex isoladas, sem reconciliação ou cópia de refresh token.'
