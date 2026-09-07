#!/usr/bin/env bash
set -euo pipefail

# Verifica armazenamento e aplica retenção somente em artefatos Docker gerenciados.
disk_mode="${1:-reclaim}"
disk_min_free_mb="${AGENT_VPS_DISK_MIN_FREE_MB:-4096}"
disk_timeout_seconds="${AGENT_VPS_DISK_TIMEOUT_SECONDS:-120}"
disk_lock_file="${AGENT_VPS_DISK_LOCK_FILE:-/var/lock/marketinghub-agent-vps-disk.lock}"
disk_rollback_versions="${AGENT_VPS_DISK_ROLLBACK_VERSIONS:-2}"
disk_min_rollback_versions="${AGENT_VPS_DISK_MIN_ROLLBACK_VERSIONS:-1}"
disk_protected_tag="${AGENT_VPS_DISK_PROTECTED_TAG:-}"

if [[ "$#" -gt 1 || ! "$disk_mode" =~ ^(check|reclaim|retention)$ ]]; then
  echo "Uso: $0 [check|reclaim|retention]" >&2
  exit 2
fi
for disk_number in "$disk_min_free_mb" "$disk_timeout_seconds"; do
  if ! [[ "$disk_number" =~ ^[1-9][0-9]{0,7}$ ]]; then
    echo "Limites de disco e timeout devem ser inteiros positivos de até oito dígitos." >&2
    exit 2
  fi
done
if ! [[ "$disk_rollback_versions" =~ ^[1-9][0-9]?$ ]] \
  || ! [[ "$disk_min_rollback_versions" =~ ^[1-9][0-9]?$ ]] \
  || ((10#$disk_min_rollback_versions > 10#$disk_rollback_versions)); then
  echo "As retenções preferencial e mínima de rollback devem ser inteiros positivos, e a mínima não pode superar a preferencial." >&2
  exit 2
fi
if [[ -n "$disk_protected_tag" && ! "$disk_protected_tag" =~ ^[0-9a-f]{40}$ ]]; then
  echo "A tag protegida deve ser um SHA Git completo com 40 caracteres hexadecimais minúsculos." >&2
  exit 2
fi

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

# Reconhece somente repositórios versionados dos agentes e da PDE Platform.
is_managed_repository() {
  local image_repository="$1"
  case "$image_repository" in
    marketing-hub/agent-executor-admin-controller \
      | marketing-hub/communication-agent-worker \
      | marketing-hub/customer-agent-worker \
      | marketing-hub/experiment-strategist-worker \
      | marketing-hub/financial-agent-worker \
      | marketing-hub/growth-operator-worker \
      | marketing-hub/iris-image-studio \
      | marketing-hub/landing-generator-agent-worker \
      | marketing-hub/meta-ad-approver-worker)
      return 0
      ;;
    ghcr.io/*/product-discovery-worker)
      [[ "$image_repository" =~ ^ghcr\.io/[a-z0-9][a-z0-9._-]*/product-discovery-worker$ ]]
      return
      ;;
    ghcr.io/*/pde-ai-worker \
      | ghcr.io/*/pde-retention-worker \
      | ghcr.io/*/pde-platform-backend \
      | ghcr.io/*/pde-platform-frontend-v5 \
      | ghcr.io/*/pde-platform-frontend-v6 \
      | ghcr.io/*/pde-platform-frontend-v7 \
      | ghcr.io/*/pde-platform-frontend-mira \
      | ghcr.io/*/pde-platform-frontend-kit-whatsapp)
      [[ "$image_repository" =~ ^ghcr\.io/[a-z0-9][a-z0-9._-]*/(pde-ai-worker|pde-retention-worker|pde-platform-backend|pde-platform-frontend-v5|pde-platform-frontend-v6|pde-platform-frontend-v7|pde-platform-frontend-mira|pde-platform-frontend-kit-whatsapp)$ ]]
      return
      ;;
    *)
      return 1
      ;;
  esac
}

# Remove referências imutáveis antigas conhecidas, preservando containers, publicação e rollbacks.
reclaim_managed_history() {
  local history_label="$1" history_seconds="$2" rollback_versions="$3"
  local enforce_full_retention="${4:-false}"
  local image_listing container_listing container_id active_image_id image_row
  local image_repository image_tag image_id image_reference image_recency image_recency_epoch
  local image_last_tag_time image_created
  local previous_repository="" retained_versions=0 rollback_cutoff_epoch=""
  local current_epoch cutoff_epoch candidate
  local removal_failures=0
  local -a image_rows=() container_ids=() active_image_ids=() managed_images=() sorted_images=()
  local -a removable_images=() sorted_removable_images=()
  local -A retained_image_ids=()

  if ! image_listing="$(timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
    docker image ls --all --no-trunc --format '{{.Repository}}|{{.Tag}}|{{.ID}}')"; then
    echo "Disco do VPS: não foi possível inventariar imagens; deploy bloqueado." >&2
    return 1
  fi
  if ! container_listing="$(timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
    docker container ls --all --no-trunc --quiet)"; then
    echo "Disco do VPS: não foi possível inventariar containers; deploy bloqueado." >&2
    return 1
  fi
  if [[ -n "$image_listing" ]]; then
    mapfile -t image_rows <<<"$image_listing"
  fi
  if [[ -n "$container_listing" ]]; then
    mapfile -t container_ids <<<"$container_listing"
  fi
  for container_id in "${container_ids[@]}"; do
    if ! [[ "$container_id" =~ ^[0-9a-f]{12,64}$ ]] \
      || ! active_image_id="$(timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
        docker container inspect --format '{{.Image}}' "$container_id")" \
      || ! [[ "$active_image_id" =~ ^sha256:[0-9a-f]{64}$ ]]; then
      echo "Disco do VPS: identidade de container inválida; deploy bloqueado." >&2
      return 1
    fi
    active_image_ids+=("$active_image_id")
  done

  for image_row in "${image_rows[@]}"; do
    IFS='|' read -r image_repository image_tag image_id <<<"$image_row"
    if ! is_managed_repository "$image_repository" \
      || ! [[ "$image_tag" =~ ^[0-9a-f]{40}$ ]]; then
      continue
    fi
    if ! [[ "$image_id" =~ ^sha256:[0-9a-f]{64}$ ]]; then
      echo "Disco do VPS: identidade de imagem gerenciada inválida; deploy bloqueado." >&2
      return 1
    fi
    image_reference="${image_repository}:${image_tag}"
    image_last_tag_time="$(timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
      docker image inspect --format '{{.Metadata.LastTagTime}}' "$image_reference" 2>/dev/null || true)"
    image_recency="$image_last_tag_time"
    image_recency_epoch=""
    if [[ "$image_recency" =~ ^[0-9]{4}- ]]; then
      image_recency_epoch="$(date --date="$image_recency" +%s 2>/dev/null || true)"
    fi
    if ! [[ "$image_recency_epoch" =~ ^[0-9]{1,12}$ ]]; then
      if ! image_created="$(timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
        docker image inspect --format '{{.Created}}' "$image_reference")"; then
        echo "Disco do VPS: não foi possível obter a data de ${image_reference}; deploy bloqueado." >&2
        return 1
      fi
      image_recency="$image_created"
      image_recency_epoch="$(date --date="$image_recency" +%s 2>/dev/null || true)"
    fi
    if ! [[ "$image_recency_epoch" =~ ^[0-9]{1,12}$ ]]; then
      echo "Disco do VPS: data inválida para ${image_reference}; deploy bloqueado." >&2
      return 1
    fi
    managed_images+=("${image_repository}|${image_recency_epoch}|${image_reference}|${image_id}")
  done
  if ((${#managed_images[@]} == 0)); then
    echo "Disco do VPS: nenhuma versão imutável gerenciada elegível para coleta."
    return 0
  fi

  mapfile -t sorted_images < <(
    printf '%s\n' "${managed_images[@]}" | LC_ALL=C sort -t '|' -k1,1 -k2,2nr
  )
  current_epoch="$(date +%s)"
  cutoff_epoch=$((current_epoch - history_seconds))
  for image_row in "${sorted_images[@]}"; do
    IFS='|' read -r image_repository image_recency_epoch image_reference image_id <<<"$image_row"
    image_tag="${image_reference##*:}"
    if [[ "$image_repository" != "$previous_repository" ]]; then
      previous_repository="$image_repository"
      retained_versions=0
      rollback_cutoff_epoch=""
    fi
    active_image_id=""
    for candidate in "${active_image_ids[@]}"; do
      if [[ "$candidate" = "$image_id" ]]; then
        active_image_id="$candidate"
        break
      fi
    done
    if [[ -n "$active_image_id" ]]; then
      printf 'Disco do VPS: preservando imagem ativa %s.\n' "$image_reference"
      continue
    fi
    if [[ -n "$disk_protected_tag" && "$image_tag" = "$disk_protected_tag" ]]; then
      retained_image_ids["${image_repository}|${image_id}"]=true
      printf 'Disco do VPS: preservando imagem da publicação atual %s.\n' "$image_reference"
      continue
    fi
    if [[ -n "${retained_image_ids["${image_repository}|${image_id}"]:-}" ]]; then
      printf 'Disco do VPS: preservando tag adicional do rollback %s.\n' "$image_reference"
      continue
    fi
    if ((retained_versions < rollback_versions)); then
      retained_image_ids["${image_repository}|${image_id}"]=true
      retained_versions=$((retained_versions + 1))
      rollback_cutoff_epoch="$image_recency_epoch"
      printf 'Disco do VPS: preservando rollback %s (%s/%s).\n' \
        "$image_reference" "$retained_versions" "$rollback_versions"
      continue
    fi
    if [[ -n "$rollback_cutoff_epoch" \
      && "$image_recency_epoch" = "$rollback_cutoff_epoch" ]]; then
      retained_image_ids["${image_repository}|${image_id}"]=true
      printf 'Disco do VPS: preservando rollback %s por empate de recência no limite.\n' \
        "$image_reference"
      continue
    fi
    if ((image_recency_epoch <= cutoff_epoch)); then
      removable_images+=("${image_recency_epoch}|${image_reference}")
    fi
  done
  if ((${#removable_images[@]} == 0)); then
    printf 'Disco do VPS: nenhum histórico gerenciado além da retenção e sem uso há %s.\n' \
      "$history_label"
    return 0
  fi

  mapfile -t sorted_removable_images < <(
    printf '%s\n' "${removable_images[@]}" | LC_ALL=C sort -t '|' -k1,1n
  )
  for image_row in "${sorted_removable_images[@]}"; do
    image_reference="${image_row#*|}"
    printf 'Disco do VPS: removendo referência imutável antiga e sem container %s.\n' \
      "$image_reference"
    if ! timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
      docker image rm "$image_reference"; then
      printf 'Disco do VPS: não foi possível remover %s sem força; referência preservada.\n' \
        "$image_reference" >&2
      removal_failures=$((removal_failures + 1))
      continue
    fi
    if [[ "$enforce_full_retention" != true ]]; then
      read_disk_capacity
      if [[ "$disk_ready" = true ]]; then
        echo "Disco do VPS: READY após retenção controlada de imagens gerenciadas."
        return 0
      fi
    fi
  done
  if [[ "$enforce_full_retention" = true ]]; then
    read_disk_capacity
    if ((removal_failures > 0)); then
      printf 'Disco do VPS: retenção incompleta; falhasRemoção=%s.\n' "$removal_failures" >&2
      return 1
    fi
    echo "Disco do VPS: retenção preventiva concluída; imagem ativa e ${rollback_versions} rollback(s) preservados por repositório."
  fi
}

read_disk_capacity
if [[ "$disk_mode" = retention ]]; then
  if ! reclaim_managed_history "política preventiva" 0 "$disk_rollback_versions" true; then
    exit 1
  fi
  if [[ "$disk_ready" = true ]]; then
    echo "Disco do VPS: READY após retenção preventiva."
    exit 0
  fi
  echo "Disco do VPS: retenção preventiva aplicada; capacidade ainda insuficiente, iniciando recuperação adicional."
elif [[ "$disk_ready" = true ]]; then
  echo "Disco do VPS: READY; nenhuma limpeza necessária."
  exit 0
fi
if [[ "$disk_mode" = check ]]; then
  echo "Disco do VPS: BLOCKED; inspeção sem limpeza." >&2
  exit 1
fi

# Sem --all; preserva cache em uso. A última faixa alcança cache descartável dos builds
# recém-concluídos no VPS legado, cuja janela de uma hora impedia restaurar a reserva.
for disk_policy in 24h:2GB 1h:1GB 0s:1GB; do
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

# Imagens dangling não são rollback endereçável; o Docker preserva qualquer imagem de container.
for disk_unused_since in 24h 1h; do
  printf 'Disco do VPS: recuperando imagens sem tag e sem uso há %s.\n' "$disk_unused_since"
  if ! timeout --foreground --kill-after=5s "${disk_timeout_seconds}s" \
    docker image prune --force --filter "until=$disk_unused_since"; then
    echo "Disco do VPS: coleta de imagens sem tag falhou ou excedeu o prazo; deploy bloqueado." >&2
    exit 1
  fi
  read_disk_capacity
  if [[ "$disk_ready" = true ]]; then
    echo "Disco do VPS: READY após recuperação de imagens sem tag."
    exit 0
  fi
done

# Últimas faixas: somente tags SHA de repositórios conhecidos e nunca imagens ativas.
# A capacidade normal preserva duas versões; sob pressão comprovada, mantém ao menos uma.
for disk_history_policy in 24h:86400 1h:3600; do
  disk_history_label="${disk_history_policy%:*}"
  disk_history_seconds="${disk_history_policy#*:}"
  if ! reclaim_managed_history "$disk_history_label" "$disk_history_seconds" \
    "$disk_rollback_versions"; then
    exit 1
  fi
  if [[ "$disk_ready" = true ]]; then
    exit 0
  fi
done
if ((10#$disk_min_rollback_versions < 10#$disk_rollback_versions)); then
  printf 'Disco do VPS: capacidade insuficiente com %s rollbacks; aplicando piso seguro de %s sob pressão.\n' \
    "$disk_rollback_versions" "$disk_min_rollback_versions"
  if ! reclaim_managed_history "0s sob pressão" 0 "$disk_min_rollback_versions"; then
    exit 1
  fi
  if [[ "$disk_ready" = true ]]; then
    exit 0
  fi
fi
echo "Disco do VPS: BLOCKED após coleta controlada; preservar serviços e revisar capacidade do host." >&2
exit 1
