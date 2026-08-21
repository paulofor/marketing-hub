#!/usr/bin/env bash
set -euo pipefail

workflow=".github/workflows/lead-portal-ci.yml"
compose_file="lead-portal/docker-compose.yml"
proxy_config="lead-portal/nginx.conf"
reconcile_script="lead-portal/scripts/reconcile-stale-runtime.sh"
landing_probe_script="lead-portal/scripts/verify-public-landing.sh"
proxy_e2e_script="lead-portal/scripts/test-proxy-resilience-e2e.sh"

grep -F 'bash scripts/reconcile-stale-runtime.sh' "$workflow" >/dev/null
grep -F 'previous_backend_image=' "$workflow" >/dev/null
grep -F 'previous_frontend_image=' "$workflow" >/dev/null
grep -F 'restaurando as imagens anteriores do Lead Portal' "$workflow" >/dev/null
grep -F 'docker exec lead-portal-backend' "$workflow" >/dev/null
grep -F '/api/internal/flows/${critical_flow_slug}/optimize-assets' "$workflow" >/dev/null
grep -F 'data-mh-web-optimized' "$workflow" >/dev/null
grep -F 'bash scripts/verify-public-landing.sh' "$workflow" >/dev/null
grep -F 'LEAD_PORTAL_CRITICAL_FLOW_SLUG' "$workflow" >/dev/null
grep -F 'critical_flow_slug="${8:?critical flow slug is required}"' "$workflow" >/dev/null
grep -F 'clarity_project_id="${9:-}"' "$workflow" >/dev/null
grep -F 'mh_audit=deploy-' "$landing_probe_script" >/dev/null
grep -F -- '--max-time "$max_seconds"' "$landing_probe_script" >/dev/null
grep -F 'data-analytics-role="primary-checkout"' "$landing_probe_script" >/dev/null
grep -F 'data-mh-web-optimized' "$landing_probe_script" >/dev/null
grep -F 'checkout|mercadopago|pagamento|pref_id' "$landing_probe_script" >/dev/null
grep -F 'marketinghub-lead-portal-backend-1' "$reconcile_script" >/dev/null
grep -F 'marketinghub-lead-portal-frontend-1' "$reconcile_script" >/dev/null
grep -F '.HostConfig.PortBindings' "$reconcile_script" >/dev/null
grep -F 'lead-portal-landing-cache:/var/cache/nginx/landing' "$compose_file" >/dev/null
grep -F 'JAVA_TOOL_OPTIONS: -Xms64m -Xmx256m' "$compose_file" >/dev/null
grep -F 'proxy_cache_use_stale error timeout updating http_500 http_502 http_503 http_504;' "$proxy_config" >/dev/null
grep -F 'proxy_cache_lock on;' "$proxy_config" >/dev/null
grep -F 'proxy_read_timeout 3s;' "$proxy_config" >/dev/null
grep -F 'lead-portal/scripts/test-proxy-resilience-e2e.sh' "$workflow" >/dev/null
grep -F 'Landing-Cache: MISS' "$proxy_e2e_script" >/dev/null
grep -F 'after-proxy-restart' "$proxy_e2e_script" >/dev/null

reconcile_line="$(grep -n -F 'bash scripts/reconcile-stale-runtime.sh' "$workflow" | tail -1 | cut -d: -f1)"
public_probe_line="$(grep -n -F 'bash scripts/verify-public-landing.sh' "$workflow" | head -1 | cut -d: -f1)"
validated_line="$(grep -n -F 'deployment_validated=true' "$workflow" | head -1 | cut -d: -f1)"
if [ "$reconcile_line" -ge "$public_probe_line" ] || [ "$public_probe_line" -ge "$validated_line" ]; then
  echo "[ARQUITETURA] reconciliação, sonda pública e homologação final estão fora de ordem" >&2
  exit 1
fi

# O shell remoto elimina argumentos vazios da linha SSH. O slug obrigatório deve
# vir antes do Clarity opcional para continuar na posição 8 quando o projeto não
# estiver configurado.
validate_remote_arguments() {
  critical_flow_slug="${8:?critical flow slug is required}"
  clarity_project_id="${9:-}"
  test "$critical_flow_slug" = 'exp-88-gerasalespage-v1'
  test "$clarity_project_id" = "${EXPECTED_CLARITY_PROJECT_ID:-}"
}

validate_remote_arguments remote token registry user namespace tag sha exp-88-gerasalespage-v1
EXPECTED_CLARITY_PROJECT_ID=clarity-123 \
  validate_remote_arguments remote token registry user namespace tag sha exp-88-gerasalespage-v1 clarity-123

echo "Contrato resiliente do deploy da landing aprovado."
