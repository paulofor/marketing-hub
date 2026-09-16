#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
: "${PDE_CONTRACT_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sessão}"
compose=(docker compose -p "${PDE_CONTRACT_COMPOSE_PROJECT}" -f "${root}/infra/testing/pde-version-contract/compose.yml")
cleanup() {
  "${compose[@]}" down --volumes --remove-orphans
}
trap cleanup EXIT
docker version >/dev/null
docker compose version >/dev/null
bash "${root}/scripts/docker-pull-with-transient-retry.sh" mysql:5.7
"${compose[@]}" up -d --wait --wait-timeout 180
PDE_CONTRACT_MYSQL=local mvn -B -f "${root}/backend/ads-service/pom.xml" \
  -Dtest=PdePublishedContractMysql57Test,VegaV12CandidateMysql57Test,VegaV12ContractConsistencyTest test
