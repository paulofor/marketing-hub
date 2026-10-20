#!/usr/bin/env bash
# Remove somente o cron legado identificado, depois da homologação TLS do workflow.
set -euo pipefail
DEPLOY_DIR="${DEPLOY_DIR:-/opt/marketinghub/containers}"
MCP_TLS_LOCK="${MCP_TLS_LOCK:-/tmp/mcp-certbot-renew.lock}"
umask 077
exec 9>"${MCP_TLS_LOCK}"
flock -w 30 9
current="$(crontab -l)"
marker='# mcpserverdigi.shop certbot renew'
if ! grep -Fq "${marker}" <<<"${current}"; then
  echo '[MCP TLS] Cron legado já ausente; nenhuma alteração.'
  exit 0
fi
backup="${DEPLOY_DIR}/volumes/mcp/certbot/legacy-crontab-before-tls-workflow.txt"
if [[ ! -e "${backup}" ]]; then
  printf '%s\n' "${current}" > "${backup}"
fi
# Não remove tarefas de outros domínios nem uma linha diferente com o mesmo comentário.
filtered="$(awk '
  /# mcpserverdigi[.]shop certbot renew$/ && /docker run/ && /certbot/ && /marketinghub-mcp-nginx/ { next }
  { print }
' <<<"${current}")"
if grep -Fq "${marker}" <<<"${filtered}"; then
  echo '[MCP TLS] Cron legado divergente; revisão necessária, sem remover entradas.' >&2
  exit 1
fi
printf '%s\n' "${filtered}" | crontab -
echo '[MCP TLS] Cron legado substituído pela agenda auditada do workflow; demais tarefas preservadas.'
