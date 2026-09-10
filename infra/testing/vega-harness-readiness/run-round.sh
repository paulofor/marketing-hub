#!/usr/bin/env bash
# Homologa metadados, entrada, comando, executor e cliente administrativo sem efeitos comerciais.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${VEGA377_COMPOSE_PROJECT:?Informe o projeto Compose exclusivo}"
: "${VEGA377_UI_DIR:?Informe o frontend compilado que será executado localmente}"
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega377/$round"
mkdir -p "$output"
compose=(docker compose -p "$VEGA377_COMPOSE_PROJECT" -f infra/testing/vega-harness-readiness/compose.yml)
cleanup() { "${compose[@]}" down --volumes --remove-orphans > "$output/cleanup.log" 2>&1; }
trap cleanup EXIT
run() {
  local name=$1
  shift
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    tail -n 35 "$output/$name.log"
    return 1
  fi
}
run mysql-up "${compose[@]}" up -d --wait --wait-timeout 120
export VEGA377_MYSQL=local
export VEGA377_MYSQL_HOST
VEGA377_MYSQL_HOST=$(node -e 'const h=process.env.DOCKER_HOST||"";process.stdout.write(h.startsWith("tcp:")?new URL(h).hostname:"127.0.0.1")')
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml \
  "-Dvega377.output=$output/screen.json" \
  '-Dtest=PdeTechnicalHomologation*Test,PdeHarnessResponsibilityMysql57Test,PdeAgentValidation*Test,PdeRevalidationActivityExecutionTest,BusinessProcessActivityExecution*Test,LearningCycleConstructionContextTest,LearningCycleExecutionContextTest,LearningCycleWorkResolverTest,ExperimentAgentTaskTargetContextProviderTest,AgentTaskServiceTest,AgentTaskVisualEvidenceServiceTest,ArquiteturaTest' test
run worker env MAVEN_OPTS=-Xmx512m mvn -q -f customer-agent-worker/pom.xml test
run browser-harness npm --prefix customer-agent-worker test
run admin-browser env VEGA377_SCREEN="$output/screen.json" VEGA377_BROWSER_OUTPUT="$output/browser" node infra/testing/vega-harness-readiness/browser.cjs
run liquibase-static bash scripts/validate-liquibase-mysql57.sh
run backend-format mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*(PdeTechnicalHomologation.*|PdeHarnessResponsibilityMysql57Test|BusinessProcessActivityExecutionService)\.java'
run worker-format mvn -q -f customer-agent-worker/pom.xml spotless:check '-DspotlessFiles=.*PdeAgentValidationHarnessRunner(Test)?\.java'
run workflow actionlint .github/workflows/liquibase-mysql57.yml
run diff git diff --check
python3 - "$output" <<'PY'
import json,sys,xml.etree.ElementTree as E
from pathlib import Path
report={}
for module in ['backend/ads-service','customer-agent-worker']:
    reports=[E.parse(p).getroot() for p in Path(module+'/target/surefire-reports').glob('TEST-*.xml')]
    report[module]={key:sum(int(x.get(key,0)) for x in reports) for key in ['tests','failures','errors','skipped']}
Path(sys.argv[1]+'/test-counts.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report))
PY
