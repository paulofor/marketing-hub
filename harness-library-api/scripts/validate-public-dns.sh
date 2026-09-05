#!/usr/bin/env bash

set -euo pipefail

PUBLIC_DOMAIN="${1:?informe o domínio público}"
EXPECTED_IPV4="${2:?informe o IPv4 esperado}"
DIG_COMMAND="${HARNESS_LIBRARY_DIG_COMMAND:-dig}"

fail() {
  printf '[HARNESS-DNS] %s\n' "$1" >&2
  exit 1
}

command -v "${DIG_COMMAND}" >/dev/null 2>&1 \
  || fail "comando de consulta DNS indisponível: ${DIG_COMMAND}"

query_addresses() {
  local record_type="$1"

  "${DIG_COMMAND}" +noall +answer "${PUBLIC_DOMAIN}" "${record_type}" \
    | awk -v expected_type="${record_type}" \
        'toupper($4) == expected_type { value = $5; sub(/\.$/, "", value); print value }' \
    | sort -u
}

ipv4_addresses="$(query_addresses A)"
if ! grep -Fxq "${EXPECTED_IPV4}" <<<"${ipv4_addresses}"; then
  fail "DNS de ${PUBLIC_DOMAIN} ainda não aponta para ${EXPECTED_IPV4}; registros A=${ipv4_addresses:-ausente}"
fi

unexpected_ipv4="$(grep -Fvx "${EXPECTED_IPV4}" <<<"${ipv4_addresses}" || true)"
[[ -z "${unexpected_ipv4}" ]] \
  || fail "DNS também aponta para IPv4 inesperado: ${unexpected_ipv4}"

ipv6_addresses="$(query_addresses AAAA)"
[[ -z "${ipv6_addresses}" ]] \
  || fail "registro AAAA não homologado para este host: ${ipv6_addresses}"

printf '[HARNESS-DNS] %s aponta exclusivamente para %s e não possui AAAA.\n' \
  "${PUBLIC_DOMAIN}" "${EXPECTED_IPV4}"
