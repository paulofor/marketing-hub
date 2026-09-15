#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
: "${PDE_CONTRACT_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sessão}"
: "${PDE_CONTRACT_EVIDENCE_DIR:?Informe um diretório novo para esta rodada}"
: "${AIHUB_HOMOLOGATION_SESSION:?Execute dentro de scripts/run-docker-homologation.sh}"
mkdir -p "${PDE_CONTRACT_EVIDENCE_DIR}"
evidence="$(cd "${PDE_CONTRACT_EVIDENCE_DIR}" && pwd)"
export JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2
cd "${root}"

gate() {
  local name="$1"
  shift
  if "$@" >"${evidence}/${name}.log" 2>&1; then
    printf 'OK %s\n' "${name}" | tee -a "${evidence}/gates.txt"
  else
    local code=$?
    printf 'FALHA %s (exit=%s): %s\n' "${name}" "${code}" "${evidence}/${name}.log" >&2
    tail -60 "${evidence}/${name}.log" >&2
    return "${code}"
  fi
}

gate node-version node -e 'if (process.versions.node.split(".")[0] !== "22") throw new Error("Use Node 22 como o CI dos revisores")'
gate backend-ci-contract python3 scripts/test-backend-ci-workflow.py
gate backend mvn -B -f backend/ads-service/pom.xml test
gate backend-package mvn -B -f backend/ads-service/pom.xml package -DskipTests
gate backend-package-contract python3 scripts/test-backend-packaged-resources.py
gate backend-package-integrity python3 scripts/verify-backend-packaged-resources.py
gate pde mvn -B -f pde-platform/backend/pom.xml package
gate frontend-dependencies npm --prefix pde-platform/frontend ci --include=dev
gate frontend-boundary npm --prefix pde-platform/frontend run check:api-boundary
gate frontend-build npm --prefix pde-platform/frontend run build
gate mysql env PDE_CONTRACT_FIXTURE_EXPORT="${evidence}/v5-contract.json" \
  bash infra/testing/pde-version-contract/run-mysql.sh
gate journeys python3 infra/testing/pde-version-contract/run-journies.py \
  --snapshot "${evidence}/v5-contract.json" --evidence "${evidence}/journeys"
gate commercial-evidence env EVIDENCE_COMPOSE_PROJECT="${PDE_CONTRACT_COMPOSE_PROJECT}" \
  EVIDENCE_OUTPUT_ROOT="${evidence}/commercial-evidence" \
  bash infra/testing/commercial-evidence/run-local.sh validation
gate consistency python3 scripts/test-musa-pde-public-consistency.py
gate isolation bash pde-platform/scripts/test-deploy-isolation-contract.sh
gate liquibase-static bash scripts/validate-liquibase-mysql57.sh
gate workflow bash scripts/run-actionlint.sh .github/workflows/liquibase-mysql57.yml
gate formatting mvn -B -f backend/ads-service/pom.xml \
  '-DspotlessFiles=.*PdeProductionSlotService.java,.*PdePublishedContract.*.java' spotless:check
gate diff git diff --check
