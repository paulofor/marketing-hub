#!/bin/sh
set -eu

ROOT=/usr/share/nginx/html
PRODUCT_ID="${PDE_PRODUCT_ID:-10}"
PRODUCT_SLUG="${VITE_PDE_PRODUCT_SLUG:-pde-planejado-36}"
EXPERIENCE_VERSION="${PDE_EXPERIENCE_VERSION:-mira-commercial-v1}"
FRONTEND_VERSION="${PDE_FRONTEND_VERSION:-mira-commercial-v1}"
FRONTEND_PUBLIC_URL="${PDE_FRONTEND_PUBLIC_URL:-https://mira.digicomdigital.com.br}"
FRONTEND_IMAGE="${PDE_FRONTEND_IMAGE:-unknown}"
FRONTEND_IMAGE_VERSION_ID="${PDE_FRONTEND_VERSION_ID:-mira-commercial-v1}"
DEPLOY_COMMIT_SHA="${PDE_DEPLOY_COMMIT_SHA:-unknown}"
DEPLOY_IMAGE_TAG="${PDE_DEPLOY_IMAGE_TAG:-unknown}"
DEPLOYED_AT="${PDE_DEPLOY_DEPLOYED_AT:-}"
CONTAINER_HOSTNAME="${PDE_CONTAINER_HOSTNAME:-$(hostname)}"
SOURCE_FINGERPRINT_FILE="${PDE_FRONTEND_SOURCE_FINGERPRINT_FILE:-${ROOT}/frontend-source.sha256}"
FRONTEND_SOURCE_SHA256=""

if [ -s "${SOURCE_FINGERPRINT_FILE}" ]; then
  FRONTEND_SOURCE_SHA256="$(tr -d '\r\n' < "${SOURCE_FINGERPRINT_FILE}")"
fi

case "${FRONTEND_SOURCE_SHA256}" in
  *[!0-9a-f]*|'') echo 'Fingerprint SHA-256 da fonte comercial de Mira ausente ou inválido.' >&2; exit 1 ;;
esac
if [ "${#FRONTEND_SOURCE_SHA256}" -ne 64 ]; then
  echo 'Fingerprint SHA-256 da fonte comercial de Mira deve possuir 64 caracteres.' >&2
  exit 1
fi

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "${ROOT}/healthz.json" <<EOF
{"status":"UP","surface":"pde-platform-frontend-mira-commercial","productId":${PRODUCT_ID},"productSlug":"$(json_escape "${PRODUCT_SLUG}")","experienceVersion":"$(json_escape "${EXPERIENCE_VERSION}")"}
EOF

cat > "${ROOT}/pde-health-contract.json" <<EOF
{
  "productId": ${PRODUCT_ID},
  "slug": "$(json_escape "${PRODUCT_SLUG}")",
  "healthPath": "/",
  "commercialOfferPath": "/api/pde/products/$(json_escape "${PRODUCT_SLUG}")/commercial-offer?slotCode=v1",
  "integrationContractPath": "/api/pde/products/$(json_escape "${PRODUCT_SLUG}")/integration-contract?slotCode=v1&experienceVersion=$(json_escape "${EXPERIENCE_VERSION}")",
  "requiredTexts": ["Cuide de você com mais clareza", "duas organizações incluídas por R$ 49"],
  "requiredHlsStreams": ["/media/mira-commercial-demo-v2-hls/index.m3u8"],
  "requiredAssets": ["/media/mira-commercial-demo-v2.mp4", "/media/mira-commercial-demo-v2-poster.jpg", "/media/mira-commercial-demo-v2-hls/index.m3u8", "/media/mira-commercial-demo-v2-hls/segment-000.ts", "/media/mira-commercial-control-v2.png"],
  "forbiddenTexts": ["acesso privado", "SIMULATED_NO_CHARGE", "Clube MUSA", "Método MUSA", "Homologação interna", "evidência sintética", "Voz gerada por IA"]
}
EOF

cat > "${ROOT}/version-diagnostics.json" <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend-mira-commercial",
  "productId": ${PRODUCT_ID},
  "productSlug": "$(json_escape "${PRODUCT_SLUG}")",
  "version": "$(json_escape "${FRONTEND_VERSION}")",
  "publicUrl": "$(json_escape "${FRONTEND_PUBLIC_URL}")",
  "experienceVersion": "$(json_escape "${EXPERIENCE_VERSION}")",
  "image": "$(json_escape "${FRONTEND_IMAGE}")",
  "imageVersionId": "$(json_escape "${FRONTEND_IMAGE_VERSION_ID}")",
  "imageTag": "$(json_escape "${DEPLOY_IMAGE_TAG}")",
  "commitSha": "$(json_escape "${DEPLOY_COMMIT_SHA}")",
  "frontendSourceSha256": "$(json_escape "${FRONTEND_SOURCE_SHA256}")",
  "deployedAt": "$(json_escape "${DEPLOYED_AT}")",
  "containerHostname": "$(json_escape "${CONTAINER_HOSTNAME}")"
}
EOF
