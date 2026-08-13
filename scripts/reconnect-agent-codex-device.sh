#!/usr/bin/env bash
set -euo pipefail

# Renova uma única vez a sessão OAuth canônica pelo Codex App Server.
canonical_home=${AGENT_CODEX_CANONICAL_HOME:-/opt/growth-operator/codex-home}
owner_uid=${AGENT_CODEX_OWNER_UID:-10001}
owner_gid=${AGENT_CODEX_OWNER_GID:-10001}
lock_file=${CODEX_OAUTH_LOCK_FILE:-$canonical_home/.oauth-session.lock}
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Executa a autenticação no host quando o runtime existe; em produção, reutiliza o
# container do Dédalo, que já contém versões compatíveis de Node.js e Codex.
run_device_login() {
  if command -v node >/dev/null 2>&1 && command -v codex >/dev/null 2>&1; then
    node "$script_dir/codex-app-server-device-login.mjs"
    codex login status >/dev/null
    return
  fi

  if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' 'Node.js/Codex não existem no host e o Docker não está disponível.' >&2
    return 1
  fi

  local container_id mounted_home
  container_id=$(docker ps \
    --filter 'label=com.docker.compose.service=landing-generator-agent-worker' \
    --format '{{.ID}}' | head -n 1)
  if [[ -z "$container_id" ]]; then
    printf '%s\n' 'O container do Dédalo não está em execução; inicie-o antes da reconexão.' >&2
    return 1
  fi

  mounted_home=$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/home/landingagent/.codex"}}{{.Source}}{{end}}{{end}}' "$container_id")
  if [[ -z "$mounted_home" || "$(realpath -m "$mounted_home")" != "$(realpath -m "$canonical_home")" ]]; then
    printf '%s\n' 'O container do Dédalo não está montado na sessão OAuth canônica esperada.' >&2
    return 1
  fi

  docker exec -i "$container_id" node /workspace/marketing-hub/scripts/codex-app-server-device-login.mjs
  docker exec "$container_id" codex login status >/dev/null
}

install -d -m 700 -o "$owner_uid" -g "$owner_gid" "$canonical_home"
exec 9>"$lock_file"
flock 9

export CODEX_HOME="$canonical_home"
run_device_login
chown -R "$owner_uid:$owner_gid" "$canonical_home"
chmod 700 "$canonical_home"
chmod 600 "$canonical_home/auth.json"
printf '%s\n' 'Sessão OAuth compartilhada validada pelo Codex App Server. Os agentes podem retomar as tarefas.'
