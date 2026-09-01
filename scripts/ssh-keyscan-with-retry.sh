#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "Uso: ssh-keyscan-with-retry.sh <host> <known-hosts> [porta]" >&2
  exit 2
fi

scan_host="$1"
known_hosts_file="$2"
scan_port="${3:-22}"
scan_attempts="${SSH_KEYSCAN_MAX_ATTEMPTS:-3}"
scan_timeout_seconds="${SSH_KEYSCAN_TIMEOUT_SECONDS:-30}"
scan_delay_seconds="${SSH_KEYSCAN_RETRY_DELAY_SECONDS:-5}"
scan_binary="${SSH_KEYSCAN_BIN:-ssh-keyscan}"

case "$scan_attempts:$scan_timeout_seconds:$scan_delay_seconds:$scan_port" in
  *[!0-9:]* | :* | *: | *::* )
    echo "Erro: tentativas, timeouts, atraso e porta do ssh-keyscan devem ser numéricos." >&2
    exit 2
    ;;
esac

if [ "$scan_attempts" -lt 1 ] || [ "$scan_timeout_seconds" -lt 1 ] || [ "$scan_port" -lt 1 ]; then
  echo "Erro: tentativas, timeout e porta do ssh-keyscan devem ser positivos." >&2
  exit 2
fi

if [ -z "$scan_host" ] || [ -z "$known_hosts_file" ]; then
  echo "Erro: host e arquivo known_hosts são obrigatórios." >&2
  exit 2
fi

if ! command -v "$scan_binary" >/dev/null 2>&1; then
  echo "Erro: binário ssh-keyscan indisponível: $scan_binary" >&2
  exit 2
fi

scan_tmp_dir="$(mktemp -d)"
trap 'rm -rf "$scan_tmp_dir"' EXIT HUP INT TERM
install -m 700 -d "$(dirname -- "$known_hosts_file")"
touch "$known_hosts_file"
chmod 600 "$known_hosts_file"

for scan_attempt in $(seq 1 "$scan_attempts"); do
  scan_output="$scan_tmp_dir/attempt-${scan_attempt}.known-hosts"
  if "$scan_binary" \
    -T "$scan_timeout_seconds" \
    -4 \
    -p "$scan_port" \
    -t rsa,ecdsa,ed25519 \
    -H "$scan_host" >"$scan_output" 2>/dev/null \
    && [ -s "$scan_output" ]; then
    while IFS= read -r host_key; do
      if ! grep -Fqx "$host_key" "$known_hosts_file"; then
        printf '%s\n' "$host_key" >>"$known_hosts_file"
      fi
    done <"$scan_output"
    echo "Chave SSH coletada na tentativa ${scan_attempt}/${scan_attempts}."
    exit 0
  fi

  if [ "$scan_attempt" -eq "$scan_attempts" ]; then
    echo "Erro: não foi possível coletar a chave SSH de ${scan_host} após ${scan_attempts} tentativas." >&2
    exit 1
  fi

  echo "ssh-keyscan transitório na tentativa ${scan_attempt}/${scan_attempts}; nova tentativa será feita." >&2
  sleep "$scan_delay_seconds"
done
