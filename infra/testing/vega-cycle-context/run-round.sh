#!/usr/bin/env bash
# Homologa a continuidade com serviços locais; modelos e fontes comerciais são simulados.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${LEARNING_CYCLES_COMPOSE_PROJECT:?Projeto Compose exclusivo obrigatório}"
round=${1:?Rodada obrigatória}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega-cycle2/$round"
mkdir -p "$output"
api_pid=""
cleanup() { if [[ -n "$api_pid" ]]; then kill "$api_pid" 2>/dev/null || true; wait "$api_pid" 2>/dev/null || true; fi; }
trap cleanup EXIT
run() {
 local label=$1
 shift
 if "$@" > "$output/$label.log" 2>&1; then printf 'PASS %s\n' "$label"; else tail -n 30 "$output/$label.log"; return 1; fi
}
run backend env MAVEN_OPTS=-Xmx512m mvn -q -f backend/ads-service/pom.xml "-Dvega.context.output=$output/worker-input.json" test
run worker env MAVEN_OPTS=-Xmx384m mvn -q -f landing-generator-agent-worker/pom.xml test
run frontend npm --prefix frontend test -- --run
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run backend-classpath mvn -q -f backend/ads-service/pom.xml dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/cycle-classpath
run worker-classpath mvn -q -f landing-generator-agent-worker/pom.xml dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/cycle-classpath
run worker-contract java -Xmx256m -cp "landing-generator-agent-worker/target/test-classes:landing-generator-agent-worker/target/classes:$(cat landing-generator-agent-worker/target/cycle-classpath)" com.marketinghub.landinggeneratoragent.LearningCycleConstructionContractProbe "$output/worker-input.json"
LEARNING_CYCLES_DB_HOST=sandbox-docker java -Xmx512m -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/cycle-classpath)" com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleLocalApplication > "$output/api.log" 2>&1 &
api_pid=$!
ready=false
for ((i=0;i<100;i++)); do
 if curl -fsS --max-time 1 http://127.0.0.1:18091/api/products > /dev/null; then ready=true; break; fi
 sleep 0.3
done
[[ "$ready" == true ]] || { tail -n 30 "$output/api.log"; exit 1; }
run integration python3 infra/testing/vega-cycle-context/integration.py
run browser env "VEGA_CONTEXT_EVIDENCE=$output/browser" node infra/testing/vega-cycle-context/browser.cjs
run backend-format mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*LearningCycle(ConstructionContext|WorkResolver|ProcessContext|Service|ExecutionContext|LocalApplication|ConstructionContextTest|WorkResolverTest)[.]java,.*BusinessProcessActivityExecutionService[.]java,.*LearningCycleController[.]java,.*ExperimentAgentTaskTargetContextProvider(Test)?[.]java,.*AgentTaskRepository[.]java'
run worker-format mvn -q -f landing-generator-agent-worker/pom.xml spotless:check '-DspotlessFiles=.*LearningCycleConstructionContractProbe[.]java'
run frontend-format npm exec --yes --package=prettier@3.6.2 -- prettier --check frontend/src/api/learningCycle/useCycleProcessContext.ts frontend/src/pages/product/ProductLearningCycleContext.tsx frontend/src/pages/product/ProductProcessActivityExecutionsPage.tsx frontend/src/pages/product/ProductProcessActivityExecutionsPage.test.tsx frontend/src/api/businessProcess/useProductProcessActivityExecutions.ts
run diff git diff --check
python3 - "$output" <<'PY'
import json,glob,sys,xml.etree.ElementTree as ET
out={}
for module in ['backend/ads-service','landing-generator-agent-worker']:
 counts={k:0 for k in ['tests','failures','errors','skipped']}
 for f in glob.glob(module+'/target/surefire-reports/TEST-*.xml'):
  x=ET.parse(f).getroot()
  for k in counts:counts[k]+=int(x.get(k,'0'))
 assert counts['tests'] and not counts['failures'] and not counts['errors'],counts
 out[module]=counts
open(sys.argv[1]+'/counts.json','w').write(json.dumps(out))
print(json.dumps(out))
PY
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
