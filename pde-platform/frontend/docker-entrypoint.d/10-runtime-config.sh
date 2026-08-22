#!/bin/sh
set -eu

CONFIG_FILE="${MUSA_RUNTIME_CONFIG_FILE:-/usr/share/nginx/html/runtime-config.js}"
DIAGNOSTICS_FILE="${MUSA_VERSION_DIAGNOSTICS_FILE:-/usr/share/nginx/html/version-diagnostics.json}"
LEGACY_DIAGNOSTICS_FILE="${MUSA_SLOT_DIAGNOSTICS_FILE:-/usr/share/nginx/html/slot-diagnostics.json}"
HEALTH_CONTRACT_FILE="${PDE_HEALTH_CONTRACT_FILE:-/usr/share/nginx/html/pde-health-contract.json}"
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
CONTAINER_HOSTNAME="${HOSTNAME:-unknown}"

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
  "deployedAt": "$(json_escape "$DEPLOYED_AT")",
  "containerHostname": "$(json_escape "$CONTAINER_HOSTNAME")",
  "knownPointedDomains": [
    {"host": "v1.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "legacy", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v2.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "legacy", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v5.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "active", "experienceVersion": "musa-pde-entry-v5-video-explicativo"},
    {"host": "v6.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "active", "experienceVersion": "musa-pde-entry-v6-video-motivacional"},
    {"host": "v7.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "active", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"},
    {"host": "v8.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "reserved", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"},
    {"host": "v9.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "reserved", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"},
    {"host": "v10.clubemusa.com.br", "observedAddress": "163.245.200.7", "role": "reserved", "experienceVersion": "musa-pde-entry-v7-espelho-antes-de-sair"}
  ]
}
EOF

cp "$DIAGNOSTICS_FILE" "$LEGACY_DIAGNOSTICS_FILE"
