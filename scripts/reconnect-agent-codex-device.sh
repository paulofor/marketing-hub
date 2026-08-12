#!/usr/bin/env bash
set -euo pipefail

# Renova uma única vez a sessão OAuth canônica pelo Codex App Server.
canonical_home=${AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home}
owner_uid=${AGENT_CODEX_OWNER_UID:-10001}
owner_gid=${AGENT_CODEX_OWNER_GID:-10001}
lock_file=${CODEX_OAUTH_LOCK_FILE:-$canonical_home/.oauth-session.lock}

install -d -m 700 -o "$owner_uid" -g "$owner_gid" "$canonical_home"
exec 9>"$lock_file"
flock 9

export CODEX_HOME="$canonical_home"
node "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/codex-app-server-device-login.mjs"
codex login status >/dev/null
chown -R "$owner_uid:$owner_gid" "$canonical_home"
chmod 700 "$canonical_home"
chmod 600 "$canonical_home/auth.json"
printf '%s\n' 'Sessão OAuth compartilhada validada pelo Codex App Server. Os agentes podem retomar as tarefas.'
