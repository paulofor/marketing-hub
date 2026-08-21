#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
dockerfile="${repo_root}/lead-portal/backend/Dockerfile"
compose_file="${repo_root}/lead-portal/docker-compose.yml"

grep -Eq 'apt-get install .*curl|apt-get install -y .*curl' "${dockerfile}"
grep -F '"CMD-SHELL",' "${compose_file}" >/dev/null
grep -F 'http://localhost:8080/api/ops-lp-observability-v2/health' "${compose_file}" >/dev/null
grep -F 'http://localhost:8080/api/flows/$${LEAD_PORTAL_CRITICAL_FLOW_SLUG}/page?mh_test=1' "${compose_file}" >/dev/null
grep -F -- '--max-time 4' "${compose_file}" >/dev/null

echo "Contrato de healthcheck do Lead Portal aprovado."
