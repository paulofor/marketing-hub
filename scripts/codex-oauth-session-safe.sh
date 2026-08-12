#!/usr/bin/env bash
set -euo pipefail

# Serializa o uso da sessão OAuth compartilhada para impedir renovação concorrente do refresh token.
codex_home=${CODEX_HOME:?CODEX_HOME deve apontar para a sessão OAuth compartilhada}
lock_file=${CODEX_OAUTH_LOCK_FILE:-$codex_home/.oauth-session.lock}
lock_timeout=${CODEX_OAUTH_LOCK_TIMEOUT_SECONDS:-3600}

install -d -m 700 "$codex_home"
exec 9>"$lock_file"
if ! flock -w "$lock_timeout" 9; then
  printf '%s\n' 'Sessão Codex ocupada por outra execução; tente novamente após a tarefa atual.' >&2
  exit 75
fi

exec codex "$@"
