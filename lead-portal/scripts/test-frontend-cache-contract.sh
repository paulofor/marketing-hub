#!/bin/sh
set -eu

frontend_config="lead-portal/frontend/nginx.conf"
proxy_config="lead-portal/nginx.conf"
workflow=".github/workflows/lead-portal-ci.yml"
flow_page="lead-portal/frontend/src/pages/FlowPage.tsx"

grep -F 'location = /index.html {' "$frontend_config" >/dev/null
grep -F 'Cache-Control "no-store, no-cache, must-revalidate"' "$frontend_config" >/dev/null
grep -F 'location /assets/ {' "$frontend_config" >/dev/null
grep -F 'try_files $uri =404;' "$frontend_config" >/dev/null
grep -F 'set $lead_portal_frontend_upstream http://lead-portal-frontend:80;' "$proxy_config" >/dev/null
grep -F "200:application/javascript" "$workflow" >/dev/null
grep -F 'docker ps --filter publish=80 -q' "$workflow" >/dev/null
grep -F 'docker ps --filter publish=443 -q' "$workflow" >/dev/null
grep -F 'com.docker.compose.project.working_dir' "$workflow" >/dev/null
grep -F 'A porta pública está ocupada por container fora do escopo do Lead Portal' "$workflow" >/dev/null
grep -F 'docker rm -f lead-portal-proxy-1 lead-portal_proxy_1' "$workflow" >/dev/null
grep -F 'up -d backend frontend' "$workflow" >/dev/null
grep -F 'up -d proxy' "$workflow" >/dev/null
grep -F 'deployment_validated=false' "$workflow" >/dev/null
grep -F 'if [ "$deployment_validated" = false ]' "$workflow" >/dev/null
grep -F 'if (!target.checkValidity()) {' "$flow_page" >/dev/null
grep -F 'target.reportValidity();' "$flow_page" >/dev/null
grep -F 'activeCustomTemplateBridges.get(doc)?.();' "$flow_page" >/dev/null
grep -F 'activeCustomTemplateBridges.set(doc, cleanup);' "$flow_page" >/dev/null
grep -F 'document.body.classList.add("flow-standalone-active");' "$flow_page" >/dev/null

validation_line="$(grep -n -F 'if (!target.checkValidity()) {' "$flow_page" | head -1 | cut -d: -f1)"
submitting_line="$(grep -n -F 'target.dataset.leadPortalSubmitting = "true";' "$flow_page" | head -1 | cut -d: -f1)"
if [ "$validation_line" -ge "$submitting_line" ]; then
  echo "[ARQUITETURA] formulário público deve validar os campos antes de iniciar a submissão" >&2
  exit 1
fi

if grep -F 'up -d --force-recreate proxy' "$workflow" >/dev/null; then
  echo "[ARQUITETURA] deploy do Lead Portal não pode recriar um segundo proxy após subir a pilha" >&2
  exit 1
fi

backend_frontend_line="$(grep -n -F 'up -d backend frontend' "$workflow" | head -1 | cut -d: -f1)"
port_owner_line="$(grep -n -F 'docker ps --filter publish=80 -q' "$workflow" | head -1 | cut -d: -f1)"
if [ "$backend_frontend_line" -ge "$port_owner_line" ]; then
  echo "[ARQUITETURA] backend e frontend saudáveis devem preceder qualquer troca do proxy público" >&2
  exit 1
fi

if grep -F 'docker rm -f lead-portal-proxy ' "$workflow" >/dev/null; then
  echo "[ARQUITETURA] o proxy canônico não pode ser removido antes de o Compose preparar a substituição" >&2
  exit 1
fi

if sed -n '/location \/assets\//,/^[[:space:]]*}/p' "$frontend_config" | grep -F '/index.html' >/dev/null; then
  echo "[ARQUITETURA] assets do Lead Portal não podem usar o HTML da SPA como fallback" >&2
  exit 1
fi
