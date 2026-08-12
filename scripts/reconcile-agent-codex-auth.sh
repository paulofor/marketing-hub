#!/usr/bin/env bash
set -euo pipefail

# Consolida somente a sessão mais recente para impedir cópias concorrentes do mesmo refresh token.
canonical_home=${AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home}
legacy_root=${AGENT_CODEX_LEGACY_ROOT:-/opt/marketing-hub/agents}
lock_file=${AGENT_CODEX_LOCK_FILE:-/opt/growth-operator/.codex-auth-reconcile.lock}

install -d -m 700 -o 10001 -g 10001 "$canonical_home"
exec 9>"$lock_file"
flock 9

candidates=(
  "$canonical_home/auth.json"
  "$legacy_root/customer/codex-home/auth.json"
  "$legacy_root/financial/codex-home/auth.json"
  "$legacy_root/growth-operator/codex-home/auth.json"
  "$legacy_root/strategist/codex-home/auth.json"
  "$legacy_root/meta-ad-approver/codex-home/auth.json"
  "$legacy_root/landing-generator/codex-home/auth.json"
)

latest=
latest_mtime=0
for candidate in "${candidates[@]}"; do
  if [[ -s "$candidate" ]]; then
    candidate_mtime=$(stat -c %Y "$candidate")
    if (( candidate_mtime > latest_mtime )); then
      latest=$candidate
      latest_mtime=$candidate_mtime
    fi
  fi
done

if [[ -z "$latest" ]]; then
  printf '%s\n' 'Nenhuma sessão Codex foi encontrada; reconecte a conta operacional.' >&2
  exit 1
fi

if [[ "$latest" != "$canonical_home/auth.json" ]]; then
  temporary_auth=$(mktemp "$canonical_home/.auth.json.XXXXXX")
  trap 'rm -f "$temporary_auth"' EXIT
  install -m 600 -o 10001 -g 10001 "$latest" "$temporary_auth"
  mv -f "$temporary_auth" "$canonical_home/auth.json"
  trap - EXIT
fi

chown 10001:10001 "$canonical_home/auth.json"
chmod 600 "$canonical_home/auth.json"
printf '%s\n' 'Sessão Codex compartilhada reconciliada sem expor credenciais.'
