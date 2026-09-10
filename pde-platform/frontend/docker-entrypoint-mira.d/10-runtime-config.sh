#!/bin/sh
set -eu

ROOT=/usr/share/nginx/html
PRODUCT_ID="${PDE_PRODUCT_ID:-10}"
PRODUCT_SLUG="${VITE_PDE_PRODUCT_SLUG:-pde-planejado-36}"
EXPERIENCE_VERSION="${PDE_EXPERIENCE_VERSION:-mira-private-v3}"
FRONTEND_VERSION="${PDE_FRONTEND_VERSION:-mira-private-v3}"
FRONTEND_PUBLIC_URL="${PDE_FRONTEND_PUBLIC_URL:-https://v7.clubemusa.com.br/mira-private}"
FRONTEND_IMAGE="${PDE_FRONTEND_IMAGE:-unknown}"
FRONTEND_IMAGE_VERSION_ID="${PDE_FRONTEND_VERSION_ID:-mira-private-v3}"
DEPLOY_COMMIT_SHA="${PDE_DEPLOY_COMMIT_SHA:-unknown}"
DEPLOY_IMAGE_TAG="${PDE_DEPLOY_IMAGE_TAG:-unknown}"
DEPLOYED_AT="${PDE_DEPLOY_DEPLOYED_AT:-}"
CONTAINER_HOSTNAME="${HOSTNAME:-unknown}"

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "${ROOT}/healthz.json" <<EOF
{"status":"UP","surface":"pde-platform-frontend-mira","productId":${PRODUCT_ID},"productSlug":"$(json_escape "${PRODUCT_SLUG}")","experienceVersion":"$(json_escape "${EXPERIENCE_VERSION}")"}
EOF

cat > "${ROOT}/pde-health-contract.json" <<EOF
{
  "productId": ${PRODUCT_ID},
  "slug": "$(json_escape "${PRODUCT_SLUG}")",
  "healthPath": "/mira-private",
  "requiredTexts": ["Sua rotina, organizada com calma"],
  "requiredHlsStreams": [],
  "forbiddenTexts": ["Clube MUSA", "Método MUSA", "metodo-musa-7-dias"]
}
EOF

cat > "${ROOT}/version-diagnostics.json" <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend-mira",
  "productId": ${PRODUCT_ID},
  "productSlug": "$(json_escape "${PRODUCT_SLUG}")",
  "version": "$(json_escape "${FRONTEND_VERSION}")",
  "publicUrl": "$(json_escape "${FRONTEND_PUBLIC_URL}")",
  "experienceVersion": "$(json_escape "${EXPERIENCE_VERSION}")",
  "image": "$(json_escape "${FRONTEND_IMAGE}")",
  "imageVersionId": "$(json_escape "${FRONTEND_IMAGE_VERSION_ID}")",
  "imageTag": "$(json_escape "${DEPLOY_IMAGE_TAG}")",
  "commitSha": "$(json_escape "${DEPLOY_COMMIT_SHA}")",
  "deployedAt": "$(json_escape "${DEPLOYED_AT}")",
  "containerHostname": "$(json_escape "${CONTAINER_HOSTNAME}")"
}
EOF
