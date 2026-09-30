#!/bin/sh
set -eu

ROOT=/usr/share/nginx/html
PRODUCT_ID="${PDE_PRODUCT_ID:-11}"
PRODUCT_SLUG="${VITE_PDE_PRODUCT_SLUG:-pde-planejado-46}"
EXPERIENCE_VERSION="${PDE_EXPERIENCE_VERSION:-alcyone-private-v2}"
FRONTEND_VERSION="${PDE_FRONTEND_VERSION:-alcyone-private-v2}"
FRONTEND_PUBLIC_URL="${PDE_FRONTEND_PUBLIC_URL:-https://alcyone.digicomdigital.com.br}"
FRONTEND_IMAGE="${PDE_FRONTEND_IMAGE:-unknown}"
FRONTEND_IMAGE_VERSION_ID="${PDE_FRONTEND_VERSION_ID:-alcyone-private-v2}"
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
  *[!0-9a-f]*|'')
    echo 'Fingerprint SHA-256 da fonte frontend de Alcyone ausente ou inválido.' >&2
    exit 1
    ;;
esac

if [ "${#FRONTEND_SOURCE_SHA256}" -ne 64 ]; then
  echo 'Fingerprint SHA-256 da fonte frontend de Alcyone deve possuir 64 caracteres.' >&2
  exit 1
fi

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "${ROOT}/healthz.json" <<EOF
{"status":"UP","surface":"pde-platform-frontend-alcyone","productId":${PRODUCT_ID},"productSlug":"$(json_escape "${PRODUCT_SLUG}")","experienceVersion":"$(json_escape "${EXPERIENCE_VERSION}")"}
EOF

cat > "${ROOT}/pde-health-contract.json" <<EOF
{
  "productId": ${PRODUCT_ID},
  "slug": "$(json_escape "${PRODUCT_SLUG}")",
  "healthPath": "/",
  "requiredTexts": ["Três caminhos claros para a sua ocasião"],
  "requiredHlsStreams": [],
  "forbiddenTexts": ["Mira", "Clube MUSA", "metodo-musa-7-dias"]
}
EOF

cat > "${ROOT}/version-diagnostics.json" <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend-alcyone",
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
