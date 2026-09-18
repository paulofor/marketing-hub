#!/bin/sh
set -eu

CONFIG_FILE="${MUSA_RUNTIME_CONFIG_FILE:-/usr/share/nginx/html/runtime-config.js}"
DIAGNOSTICS_FILE="${MUSA_VERSION_DIAGNOSTICS_FILE:-/usr/share/nginx/html/version-diagnostics.json}"
LEGACY_DIAGNOSTICS_FILE="${MUSA_SLOT_DIAGNOSTICS_FILE:-/usr/share/nginx/html/slot-diagnostics.json}"
HEALTH_CONTRACT_FILE="${PDE_HEALTH_CONTRACT_FILE:-/usr/share/nginx/html/pde-health-contract.json}"
SOURCE_FINGERPRINT_FILE="${PDE_FRONTEND_SOURCE_FINGERPRINT_FILE:-/usr/share/nginx/html/frontend-source.sha256}"
CHECKOUT_URL="${VITE_MUSA_CHECKOUT_URL:-}"
PRODUCT_SLUG="${VITE_PDE_PRODUCT_SLUG:-metodo-musa-7-dias}"
HEALTH_REQUIRED_TEXT="${PDE_HEALTH_REQUIRED_TEXT:-Experiência assistida e manual}"
GOOGLE_CLIENT_ID="${VITE_GOOGLE_CLIENT_ID:-}"
EXPERIENCE_VERSION_OVERRIDE="${VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE:-}"
HERO_VIDEO_URL="${VITE_MUSA_HERO_VIDEO_URL:-}"
HERO_STREAM_URL="${VITE_MUSA_HERO_STREAM_URL:-}"
FRONTEND_VERSION="${PDE_FRONTEND_VERSION:-unknown}"
FRONTEND_PUBLIC_URL="${PDE_FRONTEND_PUBLIC_URL:-}"
FRONTEND_IMAGE="${PDE_FRONTEND_IMAGE:-unknown}"
FRONTEND_IMAGE_VERSION_ID="${PDE_FRONTEND_VERSION_ID:-unknown}"
DEPLOY_COMMIT_SHA="${PDE_DEPLOY_COMMIT_SHA:-unknown}"
DEPLOY_IMAGE_TAG="${PDE_DEPLOY_IMAGE_TAG:-unknown}"
DEPLOYED_AT="${PDE_DEPLOY_DEPLOYED_AT:-}"
CONTAINER_HOSTNAME="${PDE_CONTAINER_HOSTNAME:-$(hostname)}"
FRONTEND_SOURCE_SHA256=""

if [ -s "$SOURCE_FINGERPRINT_FILE" ]; then
  FRONTEND_SOURCE_SHA256="$(tr -d '\r\n' < "$SOURCE_FINGERPRINT_FILE")"
fi

case "$FRONTEND_SOURCE_SHA256" in
  *[!0-9a-f]*|'')
    echo 'Fingerprint SHA-256 da fonte frontend ausente ou inválido.' >&2
    exit 1
    ;;
esac

if [ "${#FRONTEND_SOURCE_SHA256}" -ne 64 ]; then
  echo 'Fingerprint SHA-256 da fonte frontend deve possuir 64 caracteres.' >&2
  exit 1
fi

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "$CONFIG_FILE" <<EOF
window.__MUSA_RUNTIME_CONFIG__ = {
  VITE_MUSA_CHECKOUT_URL: "$(json_escape "$CHECKOUT_URL")",
  VITE_GOOGLE_CLIENT_ID: "$(json_escape "$GOOGLE_CLIENT_ID")",
  VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE: "$(json_escape "$EXPERIENCE_VERSION_OVERRIDE")",
  VITE_MUSA_HERO_VIDEO_URL: "$(json_escape "$HERO_VIDEO_URL")",
  VITE_MUSA_HERO_STREAM_URL: "$(json_escape "$HERO_STREAM_URL")",
  VITE_PDE_PRODUCT_SLUG: "$(json_escape "$PRODUCT_SLUG")"
};
EOF

if [ "$PRODUCT_SLUG" != "metodo-musa-7-dias" ]; then
  cat > "$HEALTH_CONTRACT_FILE" <<EOF
{
  "slug": "$(json_escape "$PRODUCT_SLUG")",
  "healthPath": "/",
  "commercialOfferPath": "/api/pde/products/$(json_escape "$PRODUCT_SLUG")/commercial-offer",
  "integrationContractPath": "/api/pde/products/$(json_escape "$PRODUCT_SLUG")/integration-contract",
  "requiredTexts": ["$(json_escape "$HEALTH_REQUIRED_TEXT")"],
  "requiredHlsStreams": [],
  "forbiddenTexts": [
    "Application error",
    "Cannot find module",
    "Unexpected token",
    "Failed to fetch dynamically imported module",
    "metodo-musa-7-dias",
    "Clube MUSA"
  ]
}
EOF
elif [ "$FRONTEND_VERSION" = "v7" ] || [ "$FRONTEND_VERSION" = "v8" ]; then
  cat > "$HEALTH_CONTRACT_FILE" <<EOF
{
  "slug": "metodo-musa-7-dias",
  "healthPath": "/",
  "commercialOfferPath": "/api/pde/products/metodo-musa-7-dias/commercial-offer",
  "integrationContractPath": "/api/pde/products/metodo-musa-7-dias/integration-contract",
  "requiredTexts": ["Seu primeiro ajuste MUSA"],
  "requiredHlsStreams": [],
  "forbiddenTexts": [
    "Application error",
    "Cannot find module",
    "Unexpected token",
    "Failed to fetch dynamically imported module",
    " IA ",
    "modelo de IA",
    "algoritmo",
    "prompt",
    "schema",
    "JSON",
    "DOMÍNIOS CONHECIDOS",
    "Domínios publicados do Clube MUSA",
    "Slots versionados",
    "public-domain-strip"
  ]
}
EOF
fi

cat > "$DIAGNOSTICS_FILE" <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend",
  "version": "$(json_escape "$FRONTEND_VERSION")",
  "legacySlot": "$(json_escape "$FRONTEND_VERSION")",
  "publicUrl": "$(json_escape "$FRONTEND_PUBLIC_URL")",
  "experienceVersion": "$(json_escape "$EXPERIENCE_VERSION_OVERRIDE")",
  "productSlug": "$(json_escape "$PRODUCT_SLUG")",
  "image": "$(json_escape "$FRONTEND_IMAGE")",
  "imageVersionId": "$(json_escape "$FRONTEND_IMAGE_VERSION_ID")",
  "imageTag": "$(json_escape "$DEPLOY_IMAGE_TAG")",
  "commitSha": "$(json_escape "$DEPLOY_COMMIT_SHA")",
  "frontendSourceSha256": "$(json_escape "$FRONTEND_SOURCE_SHA256")",
  "deployedAt": "$(json_escape "$DEPLOYED_AT")",
  "containerHostname": "$(json_escape "$CONTAINER_HOSTNAME")",
  "knownPointedDomains": [
    {"host": "v1.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "legacy", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v2.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "legacy", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v5.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "pointed", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v6.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "pointed", "experienceVersion": "musa-pde-entry-v6-video-motivacional"},
    {"host": "v7.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "pointed", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"},
    {"host": "v8.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "pointed", "experienceVersion": "musa-pde-entry-v12-primeiro-ajuste-aplicavel"},
    {"host": "v9.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "reserved", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"},
    {"host": "v10.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "reserved", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"}
  ]
}
EOF

cp "$DIAGNOSTICS_FILE" "$LEGACY_DIAGNOSTICS_FILE"
