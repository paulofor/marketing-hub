#!/usr/bin/env bash
# Executa a matriz local da ação única com persistência, callbacks, executor simulado e navegadores.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega-one-action/$round"
mkdir -p "$output"
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
touch "$output/backend.started"
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml \
  "-Dvega377.output=$output/screen.json" \
  '-Dtest=ProductProcessExecutionProgressTest,ProductProcessRecoveryLifecycleTest,AgentTaskRecentActivityExecutionRepositoryTest,PdeTechnicalHomologation*Test,PdeAgentValidation*Test,PdeRevalidationActivityExecutionTest,BusinessProcessActivityExecution*Test,ProductProcessActivityRecoveryResolverTest,LearningCycle*Test,AgentTaskServiceTest,ArquiteturaTest' test
python3 - "$output/backend-counts.json" "$output/backend.started" <<'PY'
import json,sys,xml.etree.ElementTree as E
from pathlib import Path
reports=[E.parse(p).getroot() for p in Path('backend/ads-service/target/surefire-reports').glob('TEST-*.xml') if p.stat().st_mtime >= Path(sys.argv[2]).stat().st_mtime]
counts={key:sum(int(r.get(key,0)) for r in reports) for key in ['tests','failures','errors','skipped']}
Path(sys.argv[1]).write_text(json.dumps(counts,indent=2))
PY
run worker env MAVEN_OPTS=-Xmx512m mvn -q -f landing-generator-agent-worker/pom.xml test
run frontend npm --prefix frontend test -- --run \
  src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  src/pages/product/ProductProcessTaskTracking.test.tsx \
  src/api/businessProcess/useProductProcessActivityExecutions.test.tsx \
  src/pages/product/ProductValueChainHistoryPage.test.tsx \
  src/components/ProductValueChainCycleSummary.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "VEGA_ONE_ACTION_SCREEN=$output/screen.json.recovery.json" \
  "VEGA_ONE_ACTION_OUTPUT=$output/browser" node infra/testing/vega-one-action/browser.cjs
run backend-format mvn -q -f backend/ads-service/pom.xml spotless:check \
  '-DspotlessFiles=.*(ProductProcess.*(Progress|Recovery|Lifecycle).*|BusinessProcessActivityExecution(Service|Controller)|PdeTechnicalHomologationActivityExecutionTest|AgentTaskRepository|AgentTaskRecentActivityExecutionRepositoryTest|ExperimentRepository)\.java'
run frontend-format npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/api/businessProcess/types.ts \
  frontend/src/api/businessProcess/useProductProcessActivityExecutions.ts \
  frontend/src/api/businessProcess/useProductProcessActivityExecutions.test.tsx \
  frontend/src/pages/product/ProductProcessTaskTracking.tsx \
  frontend/src/pages/product/ProductProcessTaskTracking.test.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionPanel.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionsPage.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  frontend/src/pages/businessProcess/BusinessProcessesPage.css
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
