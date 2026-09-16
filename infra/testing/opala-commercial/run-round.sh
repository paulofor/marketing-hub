#!/usr/bin/env bash
# Homologa a recuperação Opala com banco, agentes, PDE e interfaces exclusivamente locais.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "${repo_root}"
round="${1:?Informe a identificação da rodada}"
[[ "${round}" =~ ^[a-zA-Z0-9_-]+$ ]]
compose_project="${OPALA_RECOVERY_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo}"
expected_project="aihub-2e475278-27cd-401d-91bc-05f206370c93-a6d34659ff"
[[ "${compose_project}" == "${expected_project}" ]] || {
  echo "Use o projeto Compose autorizado ${expected_project}." >&2
  exit 2
}
output="${repo_root}/artifacts/opala-commercial/${round}"
mkdir -p "${output}"
frontend_pid=""

cleanup() {
  if [[ -n "${frontend_pid}" ]]; then
    kill "${frontend_pid}" >/dev/null 2>&1 || true
    wait "${frontend_pid}" >/dev/null 2>&1 || true
  fi
  docker compose -p "${compose_project}" \
    -f pde-platform/docker-compose.product-isolation-validation.yml \
    down --volumes --remove-orphans >"${output}/cleanup.log" 2>&1 || true
}
trap cleanup EXIT

run() {
  local name="$1"
  shift
  printf 'Validando %s\n' "${name}"
  if "$@" >"${output}/${name}.log" 2>&1; then
    printf 'PASS %s\n' "${name}"
  else
    local status=$?
    tail -80 "${output}/${name}.log" >&2
    return "${status}"
  fi
}

run docker-version docker version
run docker-buildx docker buildx version
run docker-compose docker compose version
run backend mvn -B -f backend/ads-service/pom.xml \
  '-Dtest=OpalaCommercial*Test,ProcessRunRepositoryTest,ProcessRunCommercialContinuationTest,LearningCycleCommercialReadinessTest,PdeProductionSlotServiceTest,PdeVersionOverviewServiceTest' test
run landing-generator mvn -B -f landing-generator-agent-worker/pom.xml test
run plutus mvn -B -f financial-agent-worker/pom.xml test
run psique mvn -B -f customer-agent-worker/pom.xml test
run temis mvn -B -f meta-ad-approver-worker/pom.xml test
run process-worker npm --prefix process-execution-worker test
run frontend-dependencies npm --prefix frontend ci --include=dev
run frontend-tests npm --prefix frontend test -- --run \
  src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  src/pages/product/ProductProcessAutomationPanel.test.tsx \
  src/pages/learningCycle/LearningCyclesPage.test.tsx
run frontend-typecheck npm --prefix frontend run typecheck
run frontend-build npm --prefix frontend run build
run opala-mysql env OPALA_COMPOSE_PROJECT="${compose_project}" OPALA_DB_HOST=sandbox-docker \
  bash infra/testing/opala-commercial/run-mysql.sh
run pde-integration env PDE_LOCAL_COMPOSE_PROJECT="${compose_project}" \
  bash pde-platform/scripts/test-musa-local-integration.sh
run pde-runtime-isolation env PDE_LOCAL_COMPOSE_PROJECT="${compose_project}" \
  bash pde-platform/scripts/test-product-runtime-proxy-local.sh

npm --prefix frontend run dev -- --host 127.0.0.1 --port 15173 --strictPort \
  >"${output}/frontend-server.log" 2>&1 &
frontend_pid=$!
for attempt in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:15173/healthz >/dev/null; then break; fi
  if ! kill -0 "${frontend_pid}" 2>/dev/null; then
    tail -80 "${output}/frontend-server.log" >&2
    exit 1
  fi
  [[ "${attempt}" -lt 60 ]] || exit 1
  sleep 1
done
run admin-browser node infra/testing/opala-commercial/browser.mjs

run backend-format mvn -B -f backend/ads-service/pom.xml spotless:check
run shell-syntax bash -n \
  infra/testing/opala-commercial/run-round.sh \
  pde-platform/scripts/test-product-runtime-proxy-local.sh
run liquibase-static bash scripts/validate-liquibase-mysql57.sh
run diff git diff --check
printf 'RODADA OPALA APROVADA %s\n' "${round}" | tee "${output}/result.txt"
