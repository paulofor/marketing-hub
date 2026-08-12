#!/usr/bin/env bash
set -euo pipefail

# Renova uma única vez a sessão OAuth canônica, bloqueando execuções dos agentes durante o login.
canonical_home=${AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home}
owner_uid=${AGENT_CODEX_OWNER_UID:-10001}
owner_gid=${AGENT_CODEX_OWNER_GID:-10001}
lock_file=${CODEX_OAUTH_LOCK_FILE:-$canonical_home/.oauth-session.lock}

if [[ ! -t 0 || ! -t 1 ]]; then
  printf '%s\n' 'A reconexão por device code exige um terminal interativo.' >&2
  exit 2
fi

install -d -m 700 -o "$owner_uid" -g "$owner_gid" "$canonical_home"
exec 9>"$lock_file"
flock 9

export CODEX_HOME="$canonical_home"
codex logout >/dev/null 2>&1 || true
printf '%s\n' 'Abra o endereço exibido pelo Codex e informe o código para autenticar a conta operacional.'
codex login --device-auth
codex login status >/dev/null
chown -R "$owner_uid:$owner_gid" "$canonical_home"
chmod 700 "$canonical_home"
chmod 600 "$canonical_home/auth.json"
printf '%s\n' 'Sessão OAuth compartilhada validada. Os agentes podem retomar as tarefas.'
