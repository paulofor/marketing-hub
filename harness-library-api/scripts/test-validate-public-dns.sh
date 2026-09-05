#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALIDATION_SCRIPT="${SCRIPT_DIRECTORY}/validate-public-dns.sh"
TEST_ROOT="$(mktemp -d)"
FAKE_DIG="${TEST_ROOT}/fake-dig"

cleanup() {
  rm -rf -- "${TEST_ROOT}"
}
trap cleanup EXIT

fail() {
  printf '[ARQUITETURA] %s\n' "$1" >&2
  exit 1
}

printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  'record_type="${@: -1}"' \
  'case "${record_type}" in' \
  '  A) records="${FAKE_DNS_A:-}" ;;' \
  '  AAAA) records="${FAKE_DNS_AAAA:-}" ;;' \
  '  *) exit 2 ;;' \
  'esac' \
  'while IFS= read -r address; do' \
  '  [[ -n "${address}" ]] || continue' \
  '  printf "mkthub.api.br. 3600 IN %s %s\\n" "${record_type}" "${address}"' \
  'done <<<"${records}"' \
  >"${FAKE_DIG}"
chmod 0755 "${FAKE_DIG}"

run_validation() {
  HARNESS_LIBRARY_DIG_COMMAND="${FAKE_DIG}" \
    "${VALIDATION_SCRIPT}" mkthub.api.br 163.245.200.7
}

# Caminho feliz: somente o IPv4 canônico está publicado.
FAKE_DNS_A='163.245.200.7' FAKE_DNS_AAAA='' run_validation >/dev/null

# A ausente, divergente ou acompanhado de outro endereço deve falhar fechado.
if FAKE_DNS_A='' FAKE_DNS_AAAA='' run_validation >/dev/null 2>&1; then
  fail 'validação aceitou domínio sem registro A.'
fi
if FAKE_DNS_A='203.0.113.20' FAKE_DNS_AAAA='' run_validation >/dev/null 2>&1; then
  fail 'validação aceitou IPv4 divergente.'
fi
if FAKE_DNS_A=$'163.245.200.7\n203.0.113.20' FAKE_DNS_AAAA='' \
  run_validation >/dev/null 2>&1; then
  fail 'validação aceitou IPv4 adicional.'
fi

# Somente um RR AAAA real deve bloquear; a validação não depende de getent/AF_INET6.
if FAKE_DNS_A='163.245.200.7' FAKE_DNS_AAAA='2001:db8::7' \
  run_validation >/dev/null 2>&1; then
  fail 'validação aceitou registro AAAA não homologado.'
fi

printf 'Contrato DNS público da Biblioteca do Harness validado.\n'
