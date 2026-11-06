#!/usr/bin/env bash
set -euo pipefail

spending_exec=false
if [[ "${1:-}" == --spending-exec ]]; then
  spending_exec=true
  shift
fi

# Tarefas com teto integral usam uma única chamada limitada e reserva remota antes de consumir.
# O código 125 significa exclusivamente contexto sem recibo, preservando o transporte anterior.
if [[ -n "${MH_AGENT_TASK_ID:-}" ]]; then
  bounded_script="$(dirname -- "${BASH_SOURCE[0]}")/single-video-bounded-agent.mjs"
  bounded_status=0
  node "$bounded_script" "$@" || bounded_status=$?
  if [[ "$bounded_status" -ne 125 ]]; then
    exit "$bounded_status"
  fi
fi

if [[ "$spending_exec" == true ]]; then
  unset MH_AGENT_TASK_ID
  exec "$@"
fi

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
