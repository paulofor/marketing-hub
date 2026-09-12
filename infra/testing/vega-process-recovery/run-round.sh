#!/usr/bin/env bash
# Homologa comunicação, coordenação, navegação e cópia com dados exclusivamente locais.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe uma identificação única para a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
: "${PROCESS_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
output="$PWD/artifacts/vega-process-recovery/$round"
mkdir -p "$output"
export VEGA_IRIS_INPUT_FILE="$output/iris-input.json"
run() {
  local name=$1
  shift
  printf 'Validando %s\n' "$name"
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    local status=$?
    tail -n 60 "$output/$name.log"
    return "$status"
  fi
}
run contrato-evidencias node --test scripts/build-commercial-review-evidence.test.mjs
run preparar-evidencias-psique node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
run preparar-evidencias-temis node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence
run processo bash infra/testing/process-automation/run-round.sh "$round"
run backend-package mvn -q -f backend/ads-service/pom.xml package -DskipTests
run backend-package-contract python3 scripts/test-backend-packaged-resources.py
run backend-image-contract python3 infra/testing/vega-process-recovery/test-image-jar.py
run backend-package-content python3 scripts/verify-backend-packaged-resources.py
run backend-image bash scripts/run-docker-homologation.sh bash infra/testing/vega-process-recovery/backend-image.sh
run iris env "VEGA_IRIS_INPUT_FILE=$VEGA_IRIS_INPUT_FILE" mvn -q -f communication-agent-worker/pom.xml test
run psique mvn -q -f customer-agent-worker/pom.xml test
run temis mvn -q -f meta-ad-approver-worker/pom.xml test
run copia bash infra/testing/process-context-copy/run-round.sh "$round"
run criativo-browser node infra/testing/vega-process-recovery/creative-browser.cjs
run imagem-iris bash scripts/run-docker-homologation.sh bash scripts/docker-build-temporary-image.sh iris-worker communication-agent-worker
run imagem-psique bash scripts/run-docker-homologation.sh bash scripts/docker-build-temporary-image.sh customer-worker customer-agent-worker
run imagem-temis bash scripts/run-docker-homologation.sh bash scripts/docker-build-temporary-image.sh meta-approver-worker meta-ad-approver-worker
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
