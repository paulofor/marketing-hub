#!/bin/sh
set -eu

CONFIG_FILE="${MUSA_RUNTIME_CONFIG_FILE:-/usr/share/nginx/html/runtime-config.js}"
DIAGNOSTICS_FILE="${MUSA_SLOT_DIAGNOSTICS_FILE:-/usr/share/nginx/html/slot-diagnostics.json}"
CHECKOUT_URL="${VITE_MUSA_CHECKOUT_URL:-}"
GOOGLE_CLIENT_ID="${VITE_GOOGLE_CLIENT_ID:-}"
EXPERIENCE_VERSION_OVERRIDE="${VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE:-}"
HERO_VIDEO_URL="${VITE_MUSA_HERO_VIDEO_URL:-}"
HERO_STREAM_URL="${VITE_MUSA_HERO_STREAM_URL:-}"
FRONTEND_SLOT="${PDE_FRONTEND_SLOT:-unknown}"
FRONTEND_PUBLIC_URL="${PDE_FRONTEND_PUBLIC_URL:-}"
FRONTEND_IMAGE="${PDE_FRONTEND_IMAGE:-${PDE_PLATFORM_FRONTEND_IMAGE:-unknown}}"
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
  VITE_MUSA_HERO_STREAM_URL: "$(json_escape "$HERO_STREAM_URL")"
};
EOF

cat > "$DIAGNOSTICS_FILE" <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend",
  "slot": "$(json_escape "$FRONTEND_SLOT")",
  "publicUrl": "$(json_escape "$FRONTEND_PUBLIC_URL")",
  "experienceVersion": "$(json_escape "$EXPERIENCE_VERSION_OVERRIDE")",
  "image": "$(json_escape "$FRONTEND_IMAGE")",
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
