#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${EVIDENCE_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo da sandbox}"
: "${AIHUB_HOMOLOGATION_SESSION:?Execute dentro de scripts/run-docker-homologation.sh}"
[[ "$EVIDENCE_COMPOSE_PROJECT" =~ ^aihub-[a-z0-9-]+$ ]] || exit 2
repository_root="$PWD"
export EVIDENCE_REVIEWER_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/reviewer:latest"
export EVIDENCE_MYSQL_IMAGE="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/mysql:latest"
compose=(docker compose -p "$EVIDENCE_COMPOSE_PROJECT" -f infra/testing/commercial-evidence/compose.yml)
trap '"${compose[@]}" logs --tail=60 mysql > "${round:-/tmp}/mysql.log" 2>&1; "${compose[@]}" down --volumes --remove-orphans' EXIT
export JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2 -Xmx768m"
export PDE_LOCAL_MYSQL_HOST
PDE_LOCAL_MYSQL_HOST=$(node -e 'const u=process.env.DOCKER_HOST||"";process.stdout.write(u.startsWith("tcp:")?new URL(u).hostname:"127.0.0.1")')
export PDE_LOCAL_MYSQL_PORT=33068
export PDE_INTERNAL_API_TOKEN=pde-local-internal-test

# Registra cada controle e interrompe a rodada no primeiro erro, mantendo os logs locais.
step() {
  local name="$1"
  shift
  if "$@" > "$round/$name.log" 2>&1; then
    printf '%s\tPASS\n' "$name" | tee -a "$round/results.tsv"
  else
    printf '%s\tFAIL\n' "$name" | tee -a "$round/results.tsv"
    tail -50 "$round/$name.log"
    return 1
  fi
}

# Confere o pacote real da imagem sem credenciais ou acesso a produção.
packaged() {
  "${compose[@]}" run --rm -T reviewer --input-type=module \
    < infra/testing/commercial-evidence/check-bundle.mjs
}

# Repete o contrato Java contra cada pacote entregue aos revisores.
bundles() {
  for worker in meta-ad-approver-worker customer-agent-worker; do
    node scripts/build-commercial-review-evidence.mjs . "$worker/review-evidence"
    node infra/testing/commercial-evidence/check-bundle.mjs "$worker/review-evidence"
    mvn -B -f meta-ad-approver-worker/pom.xml \
      -Dtest=PdeReviewArtifactLoaderTest \
      "-Dreview.evidence.root=$repository_root/$worker/review-evidence" test
  done
}

# Protege os fluxos de publicação afetados e os scripts de homologação.
workflows() {
  bash scripts/run-actionlint.sh .github/workflows/meta-ad-approver-worker-ci.yml \
    .github/workflows/customer-agent-worker-ci.yml .github/workflows/pde-platform-metodo-musa-ci.yml
  node --test scripts/coordinate-agent-deployment.test.mjs
  shellcheck infra/testing/commercial-evidence/run-local.sh infra/testing/mira-recovery/run-safety-round.sh \
    pde-platform/scripts/reload-published-frontend-proxies.sh pde-platform/scripts/test-backend-proxy-recovery.sh
  bash pde-platform/scripts/test-deploy-isolation-contract.sh
  bash pde-platform/scripts/test-targeted-production-smokes.sh
  node pde-platform/scripts/test-product-runtime-isolation-contract.mjs
}

for round_name in "$@"; do
  [[ "$round_name" =~ ^[a-z0-9-]+$ ]] || exit 2
  round="$repository_root/artifacts/actions-evidence-2026-09-10/$round_name"
  mkdir -p "$round"
  : > "$round/results.tsv"
  step 01-bundle-contract node --test scripts/build-commercial-review-evidence.test.mjs
  step 02-pde-java mvn -B -f pde-platform/backend/pom.xml verify
  step 03-temis-java mvn -B -f meta-ad-approver-worker/pom.xml verify
  step 04-psique-java mvn -B -f customer-agent-worker/pom.xml test
  step 05-bundles bundles
  step 06-reviewer-build bash scripts/docker-build-temporary-image.sh reviewer meta-ad-approver-worker
  step 07-mysql-build bash scripts/docker-build-temporary-image.sh mysql \
    -f pde-platform/local-validation/Dockerfile.mysql57 pde-platform/local-validation
  step 08-mysql "${compose[@]}" up -d --wait mysql
  step 09-packaged-evidence packaged
  step 10-visual-runtime "${compose[@]}" run --rm -T reviewer
  step 11-browser npm --prefix pde-platform/frontend run test:assisted-service -- \
    --workers=1 --grep 'bloqueia e orienta|publica termos|exige e-mail'
  step 12-analytics npm --prefix pde-platform/frontend run test:assisted-service:public-analytics -- --workers=1
  step 13-workflows workflows
  step 14-diff git diff --check
  "${compose[@]}" logs --tail=60 mysql > "$round/mysql.log" 2>&1
  "${compose[@]}" down --volumes --remove-orphans
  step 15-proxy-replacement bash pde-platform/scripts/test-backend-proxy-recovery.sh
  printf 'Rodada %s: 15/15 controles aprovados.\n' "$round_name"
done
