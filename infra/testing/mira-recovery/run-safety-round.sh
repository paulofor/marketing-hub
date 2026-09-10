#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${MIRA_DOCKER_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
: "${MIRA_PDE_IMAGE:?Informe a imagem PDE testada}"
: "${MIRA_FRONTEND_IMAGE:?Informe a imagem Mira testada}"
: "${MIRA_PSIQUE_IMAGE:?Informe a imagem Psique testada}"
[[ "$MIRA_DOCKER_PROJECT" =~ ^aihub-[a-z0-9-]+$ ]] || exit 2
round="${1:?Informe o diretório de evidências da rodada}"
mkdir -p "$round"
round="$(cd "$round" && pwd)"
compose=(docker compose -p "$MIRA_DOCKER_PROJECT" -f infra/testing/mira-recovery/compose-safety.yml)
docker_host=$(node -e 'const u=process.env.DOCKER_HOST||"";process.stdout.write(u.startsWith("tcp:")?new URL(u).hostname:"127.0.0.1")')

# Executa os controles sequencialmente para respeitar o limite de processos da sandbox.
step() {
  local name="$1"
  shift
  if "$@" > "$round/$name.log" 2>&1; then
    printf '%s PASS\n' "$name"
  else
    tail -40 "$round/$name.log"
    return 1
  fi
}

# Exercita o navegador e os recursos que serão publicados, com dados exclusivamente locais.
packaged() {
  "${compose[@]}" run --rm -T psique-runtime --input-type=module \
    < infra/testing/mira-recovery/packaged-safety.mjs
}

# Confirma que a persistência da integração não gerou evidência humana nem efeitos comerciais.
metrics() {
  "${compose[@]}" exec -T mysql mysql -uroot -pmira-local-root mira_safety -N -e \
    'SELECT COUNT(*) FROM pde_funnel_event WHERE product_slug="mira-private-validation" AND (JSON_UNQUOTE(JSON_EXTRACT(metadata_json,"$.trafficClass")) NOT IN ("AGENT_VALIDATION","QA_INTERNAL") OR event_type IN ("PURCHASE","PAYMENT_APPROVED"))' \
    > "$round/unexpected-metrics.txt"
  test "$(cat "$round/unexpected-metrics.txt")" = 0
}

export JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2 -Xmx768m"
step pde-java mvn -q -f pde-platform/backend/pom.xml test
step psique-java mvn -q -f customer-agent-worker/pom.xml test
step harness-contract npm --prefix customer-agent-worker test
step browser env MIRA_PRIVATE_E2E_TOKEN=mira-qa-local \
  PDE_TEST_MIRA_FRONTEND_URL="http://${docker_host}:18180" \
  npm --prefix pde-platform/frontend run test:mira-private:local -- --workers=1
step delayed-integration node infra/testing/mira-recovery/integration.mjs "$round/integration"
step packaged-integration packaged
step metrics metrics
step image-contract bash customer-agent-worker/test-dockerfile-contract.sh
step isolation-contract node pde-platform/scripts/test-product-runtime-isolation-contract.mjs
step api-boundary npm --prefix pde-platform/frontend run check:api-boundary
step diff git diff --check
printf 'Rodada completa: 11/11 controles aprovados.\n'
