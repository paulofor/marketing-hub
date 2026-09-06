#!/usr/bin/env bash
set -euo pipefail

# Verifica armazenamento antes do deploy e recupera somente cache antigo reconstruível.
disk_mode="${1:-reclaim}"
disk_min_free_mb="${AGENT_VPS_DISK_MIN_FREE_MB:-4096}"
disk_timeout_seconds="${AGENT_VPS_DISK_TIMEOUT_SECONDS:-120}"
disk_lock_file="${AGENT_VPS_DISK_LOCK_FILE:-/var/lock/marketinghub-agent-vps-disk.lock}"

if [[ "$#" -gt 1 || ! "$disk_mode" =~ ^(check|reclaim)$ ]]; then
  echo "Uso: $0 [check|reclaim]" >&2
  exit 2
fi
for disk_number in "$disk_min_free_mb" "$disk_timeout_seconds"; do
  if ! [[ "$disk_number" =~ ^[1-9][0-9]{0,7}$ ]]; then
    echo "Limites de disco e timeout devem ser inteiros positivos de até oito dígitos." >&2
    exit 2
  fi
done

exec 9>"$disk_lock_file"
if ! flock -n 9; then
  echo "Disco do VPS: outra verificação está em execução; deploy bloqueado." >&2
  exit 1
fi

if ! disk_docker_root="$(timeout --kill-after=5s 20s docker info --format '{{.DockerRootDir}}')" \
  || [[ "$disk_docker_root" != /* || ! -d "$disk_docker_root" ]]; then
  echo "Disco do VPS: não foi possível identificar o armazenamento do Docker; deploy bloqueado." >&2
  exit 1
fi
disk_paths=("/" "$disk_docker_root")
# Docker com image store containerd também ocupa este diretório no VPS canônico.
if [[ -d /var/lib/containerd ]]; then
  disk_paths+=("/var/lib/containerd")
fi

# Mede blocos e inodes reais; falha de leitura nunca equivale a espaço disponível.
read_disk_capacity() {
  disk_ready=true
  for disk_path in "${disk_paths[@]}"; do
    if ! disk_free_kb="$(LC_ALL=C df -Pk -- "$disk_path" | awk 'NR == 2 {print $4}')" \
      || ! disk_free_inodes="$(LC_ALL=C df -Pi -- "$disk_path" | awk 'NR == 2 {print $4}')" \
      || ! [[ "$disk_free_kb" =~ ^[0-9]{1,15}$ && "$disk_free_inodes" =~ ^[0-9]{1,15}$ ]]; then
      echo "Disco do VPS: medição inválida em ${disk_path}; deploy bloqueado." >&2
      return 1
    fi
    disk_available_mb=$((10#$disk_free_kb / 1024))
    printf 'Disco do VPS path=%s availableMb=%s minimumMb=%s freeInodes=%s minimumInodes=10000\n' \
      "$disk_path" "$disk_available_mb" "$disk_min_free_mb" "$disk_free_inodes"
    if ((disk_available_mb < disk_min_free_mb || 10#$disk_free_inodes < 10000)); then
      disk_ready=false
    fi
  done
}

read_disk_capacity
if [[ "$disk_ready" = true ]]; then
  echo "Disco do VPS: READY; nenhuma limpeza necessária."
  exit 0
fi
if [[ "$disk_mode" = check ]]; then
  echo "Disco do VPS: BLOCKED; inspeção sem limpeza." >&2
  exit 1
fi

# Sem --all, remoção de imagens, volumes ou containers; preserva cache em uso.
# A segunda faixa só é usada quando o cache antigo não devolve a reserva mínima.
for disk_policy in 24h:2GB 1h:1GB; do
  disk_unused_since="${disk_policy%:*}"
  disk_keep_storage="${disk_policy#*:}"
  printf 'Disco do VPS: recuperando somente cache sem uso há %s, com reserva de %s.\n' \
    "$disk_unused_since" "$disk_keep_storage"
  if ! timeout --kill-after=5s "${disk_timeout_seconds}s" \
    docker builder prune --force --filter "until=$disk_unused_since" --keep-storage "$disk_keep_storage"; then
    echo "Disco do VPS: coleta falhou ou excedeu o prazo; deploy bloqueado." >&2
    exit 1
  fi
  read_disk_capacity
  if [[ "$disk_ready" = true ]]; then
    echo "Disco do VPS: READY após recuperação de cache."
    exit 0
  fi
done
echo "Disco do VPS: BLOCKED após coleta limitada; preservar serviços e revisar capacidade do host." >&2
exit 1
