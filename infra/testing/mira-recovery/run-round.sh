#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${MIRA_DOCKER_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
: "${MIRA_PSIQUE_IMAGE:?Informe a imagem construída pelo Dockerfile de Psique}"
: "${MIRA_HUB_IMAGE:?Informe a imagem construída pelo Dockerfile do backend}"
round="${1:?Informe o diretório da rodada}"
mkdir -p "$round"
round="$(cd "$round" && pwd)"

# Registra cada controle e interrompe a rodada antes de qualquer publicação em falha.
step() {
  local name="$1"
  shift
  if "$@" > "$round/$name.log" 2>&1; then
    printf '%s PASS\n' "$name"
  else
    tail -60 "$round/$name.log"
    return 1
  fi
}

# Usa o harness realmente empacotado e o Chromium da imagem, sem rede externa.
runtime_contract() {
  docker compose -p "$MIRA_DOCKER_PROJECT" -f infra/testing/mira-recovery/compose.yml \
    run --rm -T psique-runtime --input-type=module \
    < customer-agent-worker/src/test/js/pde-agent-validation-harness.test.mjs
}

# Confere os catálogos com as classes da imagem final sem acesso a banco ou rede produtivos.
backend_runtime_contract() {
  javac -d "$round" scripts/BackendResearchPackageSmoke.java
  tar -C "$round" -cf - BackendResearchPackageSmoke.class |
    docker compose -p "$MIRA_DOCKER_PROJECT" -f infra/testing/mira-recovery/compose.yml \
      run --rm -T --entrypoint tar backend-runtime -xf - -C /smoke
  docker compose -p "$MIRA_DOCKER_PROJECT" -f infra/testing/mira-recovery/compose.yml \
    run --rm -T backend-runtime -Dloader.path=/smoke -Dloader.main=BackendResearchPackageSmoke \
      -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
}

step psique-java mvn -q -f customer-agent-worker/pom.xml test
step backend-java mvn -q -f backend/ads-service/pom.xml test
step pde-gates mvn -q -f pde-platform/backend/pom.xml '-Dtest=MiraPrivatePrototype*Test' test
step browser-contract npm --prefix customer-agent-worker test
step integration node infra/testing/mira-recovery/integration.mjs "$round/integration"
step packaged-harness runtime_contract
step packaged-backend backend_runtime_contract
step packaged-resources python3 scripts/verify-backend-packaged-resources.py
step image-contract bash customer-agent-worker/test-dockerfile-contract.sh
step health-contract node scripts/test-codex-agent-health-standard.mjs
step ci-contract python3 scripts/test-backend-ci-workflow.py
step deployment-detection bash scripts/test-deployment-change-resume.sh
step diff git diff --check
printf 'Rodada completa: 13/13 controles aprovados.\n'
