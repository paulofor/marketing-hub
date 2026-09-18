#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_script="${script_dir}/../frontend/docker-entrypoint.d/10-runtime-config.sh"
temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT
source_fingerprint="$(printf 'a%.0s' {1..64})"
printf '%s\n' "${source_fingerprint}" >"${temporary_dir}/frontend-source.sha256"

MUSA_RUNTIME_CONFIG_FILE="${temporary_dir}/runtime-config.js" \
  MUSA_VERSION_DIAGNOSTICS_FILE="${temporary_dir}/version-diagnostics.json" \
  MUSA_SLOT_DIAGNOSTICS_FILE="${temporary_dir}/slot-diagnostics.json" \
  PDE_HEALTH_CONTRACT_FILE="${temporary_dir}/pde-health-contract.json" \
  PDE_FRONTEND_SOURCE_FINGERPRINT_FILE="${temporary_dir}/frontend-source.sha256" \
  VITE_PDE_PRODUCT_SLUG=kit-whatsapp-pronto \
  PDE_HEALTH_REQUIRED_TEXT='Implantação personalizada' \
  PDE_HEALTH_REQUIRED_SALES_TEXT='CTA estatico obsoleto' \
  sh "${runtime_script}"

jq -e '
  .slug == "kit-whatsapp-pronto"
  and .commercialOfferPath == "/api/pde/products/kit-whatsapp-pronto/commercial-offer"
  and .integrationContractPath == "/api/pde/products/kit-whatsapp-pronto/integration-contract"
  and .requiredTexts == ["Implantação personalizada"]
' "${temporary_dir}/pde-health-contract.json" >/dev/null

jq -e --arg fingerprint "${source_fingerprint}" \
  '.frontendSourceSha256 == $fingerprint' \
  "${temporary_dir}/version-diagnostics.json" >/dev/null

if MUSA_RUNTIME_CONFIG_FILE="${temporary_dir}/invalid-runtime-config.js" \
  MUSA_VERSION_DIAGNOSTICS_FILE="${temporary_dir}/invalid-version-diagnostics.json" \
  MUSA_SLOT_DIAGNOSTICS_FILE="${temporary_dir}/invalid-slot-diagnostics.json" \
  PDE_HEALTH_CONTRACT_FILE="${temporary_dir}/invalid-pde-health-contract.json" \
  PDE_FRONTEND_SOURCE_FINGERPRINT_FILE="${temporary_dir}/missing-frontend-source.sha256" \
  VITE_PDE_PRODUCT_SLUG=kit-whatsapp-pronto \
  sh "${runtime_script}"; then
  echo '[ARQUITETURA] O runtime publicou diagnóstico sem fingerprint imutável da fonte frontend.' >&2
  exit 1
fi

if grep -Fq 'CTA estatico obsoleto' "${temporary_dir}/pde-health-contract.json"; then
  echo '[ARQUITETURA] O health publico duplicou o CTA dinamico da oferta comercial.' >&2
  exit 1
fi

echo 'Contrato comercial dinamico do health publico aprovado.'
