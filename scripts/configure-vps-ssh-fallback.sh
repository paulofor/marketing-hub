#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Uso: configure-vps-ssh-fallback.sh <host> <usuario> <github-env>" >&2
  exit 2
fi

deploy_host="$1"
deploy_user="$2"
github_env_file="$3"
ssh_binary="${SSH_BIN:-ssh}"
ssh_keygen_binary="${SSH_KEYGEN_BIN:-ssh-keygen}"
keyscan_helper="${SSH_KEYSCAN_HELPER:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/ssh-keyscan-with-retry.sh}"
runtime_root="${VPS_SSH_RUNTIME_ROOT:-${RUNNER_TEMP:-/tmp}}"

case "$deploy_host" in
  "" | *[!A-Za-z0-9._:-]* )
    echo "Erro: host de deploy inválido." >&2
    exit 2
    ;;
esac

case "$deploy_user" in
  "" | *[!A-Za-z0-9._-]* )
    echo "Erro: usuário de deploy inválido." >&2
    exit 2
    ;;
esac

if [ -z "$github_env_file" ] || [ ! -e "$github_env_file" ]; then
  echo "Erro: arquivo GITHUB_ENV não está disponível." >&2
  exit 2
fi

for required_binary in "$ssh_binary" "$ssh_keygen_binary"; do
  if ! command -v "$required_binary" >/dev/null 2>&1; then
    echo "Erro: dependência SSH indisponível: $required_binary" >&2
    exit 2
  fi
done

if [ ! -x "$keyscan_helper" ]; then
  echo "Erro: helper de coleta da chave do host indisponível." >&2
  exit 2
fi

install -m 700 -d "$runtime_root"
ssh_runtime_directory="$(mktemp -d "${runtime_root%/}/vps-ssh.XXXXXX")"
configuration_ready=false

cleanup_failed_configuration() {
  if [ "$configuration_ready" != "true" ]; then
    rm -rf -- "$ssh_runtime_directory"
  fi
}
trap cleanup_failed_configuration EXIT HUP INT TERM

candidate_values=(
  "${VPS_SSH_KEY_PRIMARY:-}"
  "${VPS_SSH_KEY_FALLBACK_1:-}"
  "${VPS_SSH_KEY_FALLBACK_2:-}"
  "${VPS_SSH_KEY_FALLBACK_3:-}"
)
identity_files=()
configured_candidates=0
invalid_candidates=0

for candidate_index in "${!candidate_values[@]}"; do
  candidate_value="${candidate_values[$candidate_index]}"
  if [ -z "$candidate_value" ]; then
    continue
  fi

  configured_candidates="$((configured_candidates + 1))"
  identity_file="${ssh_runtime_directory}/identity-${candidate_index}"
  printf '%s\n' "$candidate_value" | tr -d '\r' >"$identity_file"
  chmod 600 "$identity_file"

  if ! "$ssh_keygen_binary" -y -P '' -f "$identity_file" >/dev/null 2>&1; then
    invalid_candidates="$((invalid_candidates + 1))"
    rm -f -- "$identity_file"
    continue
  fi

  identity_files+=("$identity_file")
done

unset candidate_values candidate_value

if [ "$configured_candidates" -eq 0 ]; then
  echo "Erro: nenhuma credencial SSH foi configurada para o VPS de agentes." >&2
  exit 1
fi

if [ "${#identity_files[@]}" -eq 0 ]; then
  echo "Erro: todas as credenciais SSH configuradas são inválidas." >&2
  exit 1
fi

known_hosts_file="${ssh_runtime_directory}/known_hosts"
bash "$keyscan_helper" "$deploy_host" "$known_hosts_file"

ssh_config_file="${ssh_runtime_directory}/config"
{
  printf 'Host %s\n' "$deploy_host"
  printf '  HostName %s\n' "$deploy_host"
  printf '  User %s\n' "$deploy_user"
  printf '  BatchMode yes\n'
  printf '  PasswordAuthentication no\n'
  printf '  KbdInteractiveAuthentication no\n'
  printf '  IdentitiesOnly yes\n'
  printf '  StrictHostKeyChecking yes\n'
  printf '  UserKnownHostsFile %s\n' "$known_hosts_file"
  printf '  ConnectTimeout 30\n'
  printf '  ServerAliveInterval 60\n'
  printf '  ServerAliveCountMax 10\n'
  printf '  TCPKeepAlive yes\n'
  for identity_file in "${identity_files[@]}"; do
    printf '  IdentityFile %s\n' "$identity_file"
  done
} >"$ssh_config_file"
chmod 600 "$ssh_config_file"

if ! "$ssh_binary" -F "$ssh_config_file" "$deploy_user@$deploy_host" true </dev/null; then
  echo "Erro: nenhuma das credenciais SSH válidas autenticou no VPS de agentes (${deploy_host})." >&2
  exit 1
fi

printf 'SSH_COMMON_ARGS=-F %s\n' "$ssh_config_file" >>"$github_env_file"
printf 'SSH_DEPLOY_READY=true\n' >>"$github_env_file"
configuration_ready=true

echo "Autenticação SSH validada com ${#identity_files[@]} credencial(is) válida(s); ${invalid_candidates} inválida(s) ignorada(s)."
