#!/bin/sh
set -eu

frontend_config="lead-portal/frontend/nginx.conf"
proxy_config="lead-portal/nginx.conf"
workflow=".github/workflows/lead-portal-ci.yml"

grep -F 'location = /index.html {' "$frontend_config" >/dev/null
grep -F 'Cache-Control "no-store, no-cache, must-revalidate"' "$frontend_config" >/dev/null
grep -F 'location /assets/ {' "$frontend_config" >/dev/null
grep -F 'try_files $uri =404;' "$frontend_config" >/dev/null
grep -F 'set $lead_portal_frontend_upstream http://lead-portal-frontend:80;' "$proxy_config" >/dev/null
grep -F "200:application/javascript" "$workflow" >/dev/null

if sed -n '/location \/assets\//,/^[[:space:]]*}/p' "$frontend_config" | grep -F '/index.html' >/dev/null; then
  echo "[ARQUITETURA] assets do Lead Portal não podem usar o HTML da SPA como fallback" >&2
  exit 1
fi
