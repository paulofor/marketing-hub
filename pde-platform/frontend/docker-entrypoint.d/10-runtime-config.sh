#!/bin/sh
set -eu

CONFIG_FILE="${MUSA_RUNTIME_CONFIG_FILE:-/usr/share/nginx/html/runtime-config.js}"
CHECKOUT_URL="${VITE_MUSA_CHECKOUT_URL:-}"
GOOGLE_CLIENT_ID="${VITE_GOOGLE_CLIENT_ID:-}"

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "$CONFIG_FILE" <<EOF
window.__MUSA_RUNTIME_CONFIG__ = {
  VITE_MUSA_CHECKOUT_URL: "$(json_escape "$CHECKOUT_URL")",
  VITE_GOOGLE_CLIENT_ID: "$(json_escape "$GOOGLE_CLIENT_ID")"
};
EOF
