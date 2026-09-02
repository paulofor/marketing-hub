#!/usr/bin/env bash
set -euo pipefail

capacity_max_attempts="${SHARED_VPS_CAPACITY_MAX_ATTEMPTS:-10}"
capacity_retry_delay_seconds="${SHARED_VPS_CAPACITY_RETRY_DELAY_SECONDS:-10}"
capacity_stable_probes="${SHARED_VPS_CAPACITY_STABLE_PROBES:-2}"
capacity_max_load_per_cpu="${SHARED_VPS_CAPACITY_MAX_LOAD_PER_CPU:-4}"
capacity_max_io_pressure="${SHARED_VPS_CAPACITY_MAX_IO_PRESSURE:-50}"
capacity_min_available_mb="${SHARED_VPS_CAPACITY_MIN_AVAILABLE_MB:-128}"
capacity_docker_timeout_seconds="${SHARED_VPS_CAPACITY_DOCKER_TIMEOUT_SECONDS:-20}"
capacity_proc_root="${SHARED_VPS_CAPACITY_PROC_ROOT:-/proc}"

for positive_value in \
  "$capacity_max_attempts" \
  "$capacity_stable_probes" \
  "$capacity_max_load_per_cpu" \
  "$capacity_min_available_mb" \
  "$capacity_docker_timeout_seconds"; do
  if ! [[ "$positive_value" =~ ^[1-9][0-9]*$ ]]; then
    echo "Os limites positivos de capacidade do VPS devem ser inteiros válidos." >&2
    exit 2
  fi
done

for non_negative_value in \
  "$capacity_retry_delay_seconds" \
  "$capacity_max_io_pressure"; do
  if ! [[ "$non_negative_value" =~ ^[0-9]+$ ]]; then
    echo "Os limites não negativos de capacidade do VPS devem ser inteiros válidos." >&2
    exit 2
  fi
done

if [[ "$capacity_stable_probes" -gt "$capacity_max_attempts" ]]; then
  echo "A quantidade de probes estáveis não pode exceder o total de tentativas." >&2
  exit 2
fi

cpu_count="${SHARED_VPS_CAPACITY_CPU_COUNT:-$(getconf _NPROCESSORS_ONLN)}"
if ! [[ "$cpu_count" =~ ^[1-9][0-9]*$ ]]; then
  echo "A quantidade de CPUs do VPS deve ser um inteiro positivo." >&2
  exit 2
fi
load_limit="$((cpu_count * capacity_max_load_per_cpu))"

read_capacity_snapshot() {
  load_average="$(awk '{print $1}' "$capacity_proc_root/loadavg")"
  available_kb="$(awk '/^MemAvailable:/ {print $2; exit}' "$capacity_proc_root/meminfo")"
  available_mb="$((available_kb / 1024))"
  io_pressure="0"
  if [[ -r "$capacity_proc_root/pressure/io" ]]; then
    io_pressure="$(awk '
      /^some / {
        for (field_index = 1; field_index <= NF; field_index++) {
          if ($field_index ~ /^avg10=/) {
            split($field_index, value, "=")
            printf "%d\n", value[2]
            exit
          }
        }
      }
    ' "$capacity_proc_root/pressure/io")"
    io_pressure="${io_pressure:-0}"
  fi
}

is_numeric_snapshot() {
  [[ "$load_average" =~ ^[0-9]+([.][0-9]+)?$ ]] \
    && [[ "$available_mb" =~ ^[0-9]+$ ]] \
    && [[ "$io_pressure" =~ ^[0-9]+$ ]]
}

is_resource_capacity_available() {
  awk -v load="$load_average" -v limit="$load_limit" \
    'BEGIN { exit !(load <= limit) }' \
    && [[ "$available_mb" -ge "$capacity_min_available_mb" ]] \
    && [[ "$io_pressure" -le "$capacity_max_io_pressure" ]]
}

is_docker_capacity_available() {
  timeout --foreground --kill-after=5s "${capacity_docker_timeout_seconds}s" \
    docker info >/dev/null 2>&1 \
    && timeout --foreground --kill-after=5s "${capacity_docker_timeout_seconds}s" \
      docker compose version >/dev/null 2>&1
}

stable_probes=0
for ((attempt = 1; attempt <= capacity_max_attempts; attempt++)); do
  load_average=""
  available_mb=""
  io_pressure=""
  read_capacity_snapshot

  if ! is_numeric_snapshot; then
    echo "Não foi possível ler as métricas de capacidade do VPS." >&2
    exit 1
  fi

  docker_status="UNAVAILABLE"
  resource_status="PRESSURED"
  if is_resource_capacity_available; then
    resource_status="READY"
  fi
  if is_docker_capacity_available; then
    docker_status="READY"
  fi

  if [[ "$resource_status" == "READY" && "$docker_status" == "READY" ]]; then
    stable_probes=$((stable_probes + 1))
  else
    stable_probes=0
  fi

  printf '%s\n' \
    "Capacidade do VPS tentativa=${attempt}/${capacity_max_attempts} load=${load_average}/${load_limit} ioPressure=${io_pressure}% availableMb=${available_mb} recursos=${resource_status} docker=${docker_status} estabilidade=${stable_probes}/${capacity_stable_probes}"

  if [[ "$stable_probes" -ge "$capacity_stable_probes" ]]; then
    echo "Capacidade segura do VPS confirmada antes do deploy."
    exit 0
  fi

  if ((attempt < capacity_max_attempts)); then
    sleep "$capacity_retry_delay_seconds"
  fi
done

echo "O VPS permaneceu sob pressão; o deploy foi bloqueado antes de alterar o serviço." >&2
exit 1
