#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
dockerfile="${repo_root}/lead-portal/backend/Dockerfile"
compose_file="${repo_root}/lead-portal/docker-compose.yml"

grep -Eq 'apt-get install .*curl|apt-get install -y .*curl' "${dockerfile}"
grep -F '"CMD",' "${compose_file}" >/dev/null
grep -F '"curl",' "${compose_file}" >/dev/null
grep -F 'http://localhost:8080/api/ops-lp-observability-v2/health' "${compose_file}" >/dev/null

echo "Contrato de healthcheck do Lead Portal aprovado."
