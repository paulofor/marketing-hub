#!/usr/bin/env bash
set -euo pipefail

public_base_url="${PDE_PUBLIC_BASE_URL:-https://kit-whatsapp-pronto.digicomdigital.com.br}"
product_url="${public_base_url%/}/api/pde/products/kit-whatsapp-pronto"
offer_url="${public_base_url%/}/api/pde/products/kit-whatsapp-pronto/commercial-offer"
temporary_dir="$(mktemp -d)"
trap 'rm -rf "${temporary_dir}"' EXIT

curl --fail --show-error --silent --retry 6 --retry-delay 5 --retry-connrefused \
  "${product_url}" >"${temporary_dir}/product.json"
curl --fail --show-error --silent --retry 6 --retry-delay 5 --retry-connrefused \
  "${offer_url}" >"${temporary_dir}/offer.json"

jq -e '
  .slug == "kit-whatsapp-pronto"
  and .experienceVersion == "kit-whatsapp-pronto-pde-v2"
  and .layoutKey == "assisted-service-v2"
  and .commercialBinding.experimentId == 89
  and .commercialBinding.primaryCta == "Quero meu atendimento sob medida"
  and .commercialBinding.priceBrl == 349
  and .commercialBinding.billingModel == "ONE_TIME"
  and (.serviceScope.includedItems | length) == 6
  and (.publicProofs | length) == 4
  and (.commercialProcess | length) == 4
' "${temporary_dir}/product.json" >/dev/null

jq -e '
  .productSlug == "kit-whatsapp-pronto"
  and .experienceVersion == "kit-whatsapp-pronto-pde-v2"
  and .layoutKey == "assisted-service-v2"
  and .experimentId == 89
  and .primaryCta == "Quero meu atendimento sob medida"
  and .priceBrl == 349
  and (.checkoutUrl | startswith("https://"))
' "${temporary_dir}/offer.json" >/dev/null

product_promise="$(jq -r '.promise' "${temporary_dir}/product.json")"
offer_promise="$(jq -r '.promise' "${temporary_dir}/offer.json")"
if [ -z "${product_promise}" ] || [ "${product_promise}" != "${offer_promise}" ]; then
  echo '[ARQUITETURA] A promessa pública de Rigel diverge entre experiência e oferta.' >&2
  exit 1
fi

echo 'Contrato público comercial de Rigel v2 aprovado.'
