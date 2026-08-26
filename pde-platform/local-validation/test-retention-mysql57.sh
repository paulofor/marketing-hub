#!/usr/bin/env bash
set -euo pipefail

PDE_VALIDATION_PROJECT="${PDE_VALIDATION_PROJECT:-aihub-92719989-567c-41cc-b7d5-524d61d943ea-8ed7f19b24}"
PDE_VALIDATION_INTERNAL_TOKEN="${PDE_VALIDATION_INTERNAL_TOKEN:-pde-local-internal-test}"
PDE_VALIDATION_EMAIL="teste+retention-$(date +%s%N)@sandbox.local"
PDE_VALIDATION_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PDE_VALIDATION_ROOT="$(cd "${PDE_VALIDATION_SCRIPT_DIR}/../.." && pwd)"
PDE_VALIDATION_COMPOSE=(
  docker compose
  -p "${PDE_VALIDATION_PROJECT}"
  -f "${PDE_VALIDATION_ROOT}/pde-platform/docker-compose.yml"
  -f "${PDE_VALIDATION_ROOT}/pde-platform/docker-compose.local-validation.yml"
  --profile local-e2e
)
PDE_VALIDATION_BACKEND_CONTAINER="$("${PDE_VALIDATION_COMPOSE[@]}" ps -q pde-platform-backend)"
test -n "${PDE_VALIDATION_BACKEND_CONTAINER}"

PDE_VALIDATION_ACCESS_JSON="$(docker exec "${PDE_VALIDATION_BACKEND_CONTAINER}" curl --fail --silent --show-error \
  -X POST "http://127.0.0.1:8096/api/internal/pde/test-access" \
  -H 'Content-Type: application/json' \
  -H "X-PDE-Internal-Token: ${PDE_VALIDATION_INTERNAL_TOKEN}" \
  --data "{\"productSlug\":\"metodo-musa-7-dias\",\"email\":\"${PDE_VALIDATION_EMAIL}\",\"experienceVersion\":\"musa-pde-entry-v7-espelho-antes-de-sair\"}")"
PDE_VALIDATION_ACCESS_TOKEN="$(jq -er '.token' <<<"${PDE_VALIDATION_ACCESS_JSON}")"

docker exec "${PDE_VALIDATION_BACKEND_CONTAINER}" curl --fail --silent --show-error \
  -X POST "http://127.0.0.1:8096/api/pde/access/events" \
  -H 'Content-Type: application/json' \
  --data "{\"productSlug\":\"metodo-musa-7-dias\",\"eventType\":\"MISSION_OPEN\",\"accessToken\":\"${PDE_VALIDATION_ACCESS_TOKEN}\",\"email\":\"${PDE_VALIDATION_EMAIL}\",\"provider\":\"local-mysql57\",\"source\":\"INTERNAL_QA\",\"pageUrl\":\"https://local.test/access/${PDE_VALIDATION_ACCESS_TOKEN}\",\"metadata\":{\"sessionId\":\"retention-session\",\"visitorId\":\"retention-visitor\",\"referrerUrl\":\"https://local.test/ref/${PDE_VALIDATION_ACCESS_TOKEN}\",\"clickId\":\"retention-click\",\"mh_test\":true}}" \
  >/dev/null

PDE_VALIDATION_MYSQL_CONTAINER="$("${PDE_VALIDATION_COMPOSE[@]}" ps -q pde-platform-local-mysql)"
test -n "${PDE_VALIDATION_MYSQL_CONTAINER}"
docker exec "${PDE_VALIDATION_MYSQL_CONTAINER}" mysql -N -u pde -ppde pde_local \
  --execute "UPDATE pde_access_grant SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 181 DAY) WHERE token = '${PDE_VALIDATION_ACCESS_TOKEN}'"

"${PDE_VALIDATION_COMPOSE[@]}" restart pde-platform-backend >/dev/null
for PDE_VALIDATION_ATTEMPT in $(seq 1 60); do
  if docker exec "${PDE_VALIDATION_BACKEND_CONTAINER}" curl --fail --silent \
    "http://127.0.0.1:8096/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if [ "${PDE_VALIDATION_ATTEMPT}" -eq 60 ]; then
    echo "Backend PDE não ficou saudável para o teste de retenção" >&2
    exit 1
  fi
  sleep 1
done

"${PDE_VALIDATION_COMPOSE[@]}" run --rm --no-deps \
  -e PDE_RETENTION_RUN_ONCE=true \
  -e PDE_INTERNAL_API_TOKEN="${PDE_VALIDATION_INTERNAL_TOKEN}" \
  pde-retention-worker >/dev/null

PDE_VALIDATION_OLD_GRANT_COUNT="$(docker exec "${PDE_VALIDATION_MYSQL_CONTAINER}" mysql -N -u pde -ppde pde_local \
  --execute "SELECT COUNT(*) FROM pde_access_grant WHERE token = '${PDE_VALIDATION_ACCESS_TOKEN}'")"
PDE_VALIDATION_AUDIT_COUNT="$(docker exec "${PDE_VALIDATION_MYSQL_CONTAINER}" mysql -N -u pde -ppde pde_local \
  --execute "SELECT COUNT(*) FROM pde_access_grant WHERE source = 'PRIVACY_DELETED' AND email LIKE '%@privacy.invalid'")"
PDE_VALIDATION_EVENT_CORRELATORS="$(docker exec "${PDE_VALIDATION_MYSQL_CONTAINER}" mysql -N -u pde -ppde pde_local \
  --execute "SELECT COUNT(*) FROM pde_funnel_event WHERE event_type = 'MISSION_OPEN' AND (access_token IS NOT NULL OR email IS NOT NULL OR normalized_email IS NOT NULL OR page_url IS NOT NULL OR client_ip IS NOT NULL OR user_agent IS NOT NULL OR referrer_url IS NOT NULL OR session_id IS NOT NULL OR visitor_id IS NOT NULL OR metadata_json IS NOT NULL)")"

test "${PDE_VALIDATION_OLD_GRANT_COUNT}" = "0"
test "${PDE_VALIDATION_AUDIT_COUNT}" -ge 1
test "${PDE_VALIDATION_EVENT_CORRELATORS}" = "0"

echo "Retenção MySQL 5.7 aprovada: token antigo removido, auditoria anônima preservada e correlatores apagados."
