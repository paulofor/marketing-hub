#!/usr/bin/env bash
# Executa a migração real em banco sintético e remove somente a topologia desta validação.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${VEGA377_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo}"
compose=(docker compose -p "$VEGA377_COMPOSE_PROJECT" -f infra/testing/vega-harness-readiness/compose.yml)
cleanup() { "${compose[@]}" down --volumes --remove-orphans; }
trap cleanup EXIT
bash scripts/docker-pull-with-transient-retry.sh mysql:5.7
"${compose[@]}" up -d --wait --wait-timeout 120
export VEGA377_MYSQL=local
export VEGA377_MYSQL_HOST
VEGA377_MYSQL_HOST=$(node -e 'const h=process.env.DOCKER_HOST||"";process.stdout.write(h.startsWith("tcp:")?new URL(h).hostname:"127.0.0.1")')
MAVEN_OPTS=-Xmx768m mvn -B -q -f backend/ads-service/pom.xml -Dtest=PdeHarnessResponsibilityMysql57Test test
