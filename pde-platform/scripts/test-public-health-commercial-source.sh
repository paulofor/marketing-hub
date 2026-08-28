#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_script="${script_dir}/../frontend/docker-entrypoint.d/10-runtime-config.sh"
temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT

MUSA_RUNTIME_CONFIG_FILE="${temporary_dir}/runtime-config.js" \
  MUSA_VERSION_DIAGNOSTICS_FILE="${temporary_dir}/version-diagnostics.json" \
  MUSA_SLOT_DIAGNOSTICS_FILE="${temporary_dir}/slot-diagnostics.json" \
  PDE_HEALTH_CONTRACT_FILE="${temporary_dir}/pde-health-contract.json" \
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

if grep -Fq 'CTA estatico obsoleto' "${temporary_dir}/pde-health-contract.json"; then
  echo '[ARQUITETURA] O health publico duplicou o CTA dinamico da oferta comercial.' >&2
  exit 1
fi

echo 'Contrato comercial dinamico do health publico aprovado.'
