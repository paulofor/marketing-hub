#!/usr/bin/env bash
# Executa a matriz local com contratos reais e integrações simuladas, sem tráfego produtivo.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega-task-recovery/$round"
mkdir -p "$output"
run() {
  local name=$1
  shift
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    tail -n 45 "$output/$name.log"
    return 1
  fi
}
run backend env MAVEN_OPTS=-Xmx768m mvn -q -f backend/ads-service/pom.xml \
  "-Dvega377.output=$output/screen.json" \
  '-Dtest=PdeTechnicalHomologation*Test,PdeAgentValidation*Test,PdeRevalidationActivityExecutionTest,BusinessProcessActivityExecution*Test,ProductProcessActivityRecoveryResolverTest,LearningCycle*Test,AgentTaskServiceTest,ArquiteturaTest' test
run worker env MAVEN_OPTS=-Xmx512m mvn -q -f landing-generator-agent-worker/pom.xml test
run frontend npm --prefix frontend test -- --run \
  src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  src/pages/product/ProductValueChainHistoryPage.test.tsx \
  src/components/ProductValueChainCycleSummary.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "VEGA_RECOVERY_SCREEN=$output/screen.json.recovery.json" \
  "VEGA_RECOVERY_OUTPUT=$output/browser" node infra/testing/vega-task-recovery/browser.cjs
run backend-format mvn -q -f backend/ads-service/pom.xml spotless:check \
  '-DspotlessFiles=.*(ProductProcessActivityRecovery.*|ProductProcessActivityExecutionGroupResponse|BusinessProcessActivityExecutionService|PdeAgentValidationReworkReadinessProvider(Test)?|PdeTechnicalHomologationActivityExecutionTest|AgentTaskServiceTest)\.java'
run worker-format mvn -q -f landing-generator-agent-worker/pom.xml spotless:check \
  '-DspotlessFiles=.*PdeConstructionBpmTaskConsumer(Test)?\.java'
run frontend-format npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/api/businessProcess/types.ts \
  frontend/src/pages/product/ProductProcessActivityExecutionPanel.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionsPage.test.tsx
run diff git diff --check
python3 - "$output" <<'PY'
import json,sys,xml.etree.ElementTree as E
from pathlib import Path
counts={}
for module in ['backend/ads-service','landing-generator-agent-worker']:
    reports=[E.parse(p).getroot() for p in Path(module+'/target/surefire-reports').glob('TEST-*.xml')]
    counts[module]={key:sum(int(x.get(key,0)) for x in reports) for key in ['tests','failures','errors','skipped']}
Path(sys.argv[1]+'/test-counts.json').write_text(json.dumps(counts,indent=2))
print(json.dumps(counts))
PY
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
