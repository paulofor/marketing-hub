#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../.."
continuation_round="${1:?Informe a identificação da rodada}"
[[ "$continuation_round" =~ ^[a-zA-Z0-9-]+$ ]] || exit 2
: "${AIHUB_HOMOLOGATION_SESSION:?Execute dentro de scripts/run-docker-homologation.sh}"
: "${MIRA_DOCKER_PROJECT:?Informe o projeto Compose exclusivo desta sandbox}"

continuation_output="$PWD/tmp/vega91-mira-${continuation_round}"
mkdir -p "$continuation_output"

bash infra/testing/mira-private-reading/run-round.sh "$continuation_round" \
  > "$continuation_output/mira-suite.log" 2>&1

node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence \
  > "$continuation_output/temis-evidence.log" 2>&1
mvn -q -f meta-ad-approver-worker/pom.xml verify \
  > "$continuation_output/temis-tests.log" 2>&1
(
  cd meta-ad-approver-worker
  npm ci --omit=dev
  node --check src/main/resources/mcp/meta-ad-approver.mjs
  node --check src/main/resources/mcp/temis-library-review.mjs
  npm run test:mcp-handshake
) > "$continuation_output/temis-mcp.log" 2>&1
bash scripts/docker-build-temporary-image.sh meta-ad-approver-worker \
  -f meta-ad-approver-worker/Dockerfile meta-ad-approver-worker \
  > "$continuation_output/temis-image.log" 2>&1

shellcheck infra/testing/vega91-mira-continuation/run-round.sh \
  infra/testing/mira-private-reading/run-round.sh \
  > "$continuation_output/shellcheck.log" 2>&1
git diff --check > "$continuation_output/diff-check.log"

printf 'Rodada %s aprovada: Mira, backend, frontend, Têmis, três dispositivos e imagens temporárias.\n' \
  "$continuation_round"
