#!/usr/bin/env bash
# Provisiona segredos opcionais do canal somente pelo workflow versionado de publicação.
set -euo pipefail
mode=${1:?Informe backend ou worker}
destination=${2:?Informe o destino do workflow}
directory=${3:?Informe o diretório protegido}
[[ "$mode" == backend || "$mode" == worker ]] || exit 2
[[ "$directory" =~ ^/[a-zA-Z0-9_./-]+$ ]] || exit 2
[[ "$destination" =~ ^[a-zA-Z0-9_.@:-]+$ ]] || exit 2
read -r -a ssh_options <<< "${SSH_COMMON_ARGS:--o ServerAliveInterval=30 -o ServerAliveCountMax=10 -o ConnectTimeout=30}"
write_secret() {
  local filename=$1 value=$2
  [[ -n "$value" ]] || return 0
  if (( ${#value} < 32 )); then
    echo "Credencial ChatGPT Ads inválida; nenhum valor foi exibido." >&2
    return 1
  fi
  # directory e filename são caminhos validados/constantes; conteúdo segue exclusivamente por stdin.
  local remote_command
  remote_command="set -eu; umask 077; mkdir -p '$directory'; cat > '$directory/$filename.tmp'; test -s '$directory/$filename.tmp'; mv '$directory/$filename.tmp' '$directory/$filename'"
  # Caminho validado e comando construído localmente por desenho; segredo segue só em stdin.
  # shellcheck disable=SC2029
  printf '%s' "$value" | ssh "${ssh_options[@]}" "$destination" "$remote_command"
}
if [[ "$mode" == backend ]]; then
  write_secret CHATGPT_ADS_WORKER_TOKEN "${CHATGPT_ADS_WORKER_TOKEN:-}"
else
  write_secret worker-token "${CHATGPT_ADS_WORKER_TOKEN:-}"
  write_secret advertiser-key "${OPENAI_ADS_API_KEY:-}"
  permission_command="install -d -m 700 -o 10001 -g 10001 '$directory'; chown -R 10001:10001 '$directory'; chmod 700 '$directory'"
  # Os únicos caminhos aceitos foram validados antes de formar este comando remoto.
  # shellcheck disable=SC2029
  ssh "${ssh_options[@]}" "$destination" "$permission_command"
fi
