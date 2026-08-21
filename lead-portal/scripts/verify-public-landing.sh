#!/usr/bin/env bash
set -euo pipefail

base_url="${1:?Informe a URL base pública.}"
flow_slug="${2:?Informe o slug do fluxo crítico.}"
attempts="${3:-5}"
max_seconds="${4:-4}"

if ! [[ "$flow_slug" =~ ^exp-[0-9]+-gerasalespage-v1$ ]]; then
  echo "Slug crítico inválido: ${flow_slug}" >&2
  exit 1
fi
if ! [[ "$attempts" =~ ^[1-9][0-9]*$ ]]; then
  echo "Quantidade de tentativas inválida: ${attempts}" >&2
  exit 1
fi
if ! [[ "$max_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "Tempo máximo inválido: ${max_seconds}" >&2
  exit 1
fi

probe_dir="$(mktemp -d)"
trap 'rm -rf "$probe_dir"' EXIT

for probe in $(seq 1 "$attempts"); do
  response_file="${probe_dir}/landing-${probe}.html"
  header_file="${probe_dir}/landing-${probe}.headers"
  metrics="$(curl \
    --fail \
    --silent \
    --show-error \
    --connect-timeout 2 \
    --max-time "$max_seconds" \
    --dump-header "$header_file" \
    --output "$response_file" \
    --write-out '%{http_code}|%{time_total}|%{size_download}' \
    "${base_url%/}/flows/${flow_slug}?mh_audit=deploy-${probe}")"

  http_code="${metrics%%|*}"
  remaining_metrics="${metrics#*|}"
  response_time="${remaining_metrics%%|*}"
  response_bytes="${remaining_metrics##*|}"

  if [ "$http_code" != "200" ]; then
    echo "Landing crítica respondeu HTTP ${http_code} na tentativa ${probe}." >&2
    exit 1
  fi
  grep -F 'data-mh-landing-analytics' "$response_file" >/dev/null
  grep -F 'data-mh-web-optimized' "$response_file" >/dev/null
  if ! grep -F 'data-analytics-role="primary-checkout"' "$response_file" >/dev/null \
    && ! grep -Eiq '<a[^>]+href=[^>]*(checkout|mercadopago|pagamento|pref_id)' "$response_file"; then
    echo "Landing crítica não contém CTA de checkout reconhecível na tentativa ${probe}." >&2
    exit 1
  fi
  grep -Fi 'content-type: text/html' "$header_file" >/dev/null
  echo "Landing crítica aprovada: tentativa=${probe} tempo=${response_time}s bytes=${response_bytes}"
done
