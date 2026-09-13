#!/usr/bin/env bash
# Homologa a política de clipes usando backend/executor reais e dependências externas simuladas.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round="${1:?Informe a rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
output="$PWD/artifacts/runway-access-recovery/$round"
mkdir -p "$output"
run() {
  local step="$1"
  shift
  if "$@" > "$output/$step.log" 2>&1; then
    printf 'PASS %s\n' "$step"
  else
    tail -n 60 "$output/$step.log"
    return 1
  fi
}
rm -rf backend/ads-service/target/surefire-reports video-management-service/target/surefire-reports
run backend mvn -q -f backend/ads-service/pom.xml '-Dtest=SalesVideo*Test,Video*Test,ArquiteturaTest' test
run worker mvn -q -f video-management-service/pom.xml test
run frontend npm --prefix frontend test -- --run src/pages/audioVideoStudio/AudioVideoStudioPage.test.tsx src/api/salesVideo
run classpath mvn -q -f video-management-service/pom.xml dependency:build-classpath -Dmdep.outputFile=target/runway-classpath
run worker-contract java --class-path "video-management-service/target/classes:$(cat video-management-service/target/runway-classpath)" \
  infra/testing/runway-clip-plan/VerifyWorker.java backend/ads-service/target/runway-clip-contract.json "$output/worker-requests.json"
run config env PYTHONPATH="$PWD/artifacts/runway-access-recovery/python-libs" python3 \
  infra/testing/runway-clip-plan/validate-config.py infra/testing/runway-clip-plan/runway-request-schemas.json \
  video-management-service/config/runway/marketing-hub-campaign-final-v1.json "$output/worker-requests.json" \
  backend/ads-service/target/runway-clip-contract.json
run browser node infra/testing/runway-clip-plan/browser.cjs backend/ads-service/target/runway-clip-contract.json "$output/browser"
run spotless mvn -q -f backend/ads-service/pom.xml spotless:check '-DspotlessFiles=.*(SalesVideoProviderDurationPolicy|VideoProductionCycleService|VideoProviderFinancialPreflightService).*java'
run diff git diff --check
cp backend/ads-service/target/runway-clip-contract.json "$output/backend-contract.json"
python3 - "$output" <<'PY'
import json, pathlib, sys, xml.etree.ElementTree as E
counts = {}
for name, module in [('backend', 'backend/ads-service'), ('worker', 'video-management-service')]:
    values = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
    for path in pathlib.Path(module + '/target/surefire-reports').glob('TEST-*.xml'):
        root = E.parse(path).getroot()
        for key in values:
            values[key] += int(root.get(key, 0))
    assert values['tests'] > 0 and not values['failures'] and not values['errors'], values
    counts[name] = values
pathlib.Path(sys.argv[1], 'counts.json').write_text(json.dumps(counts, indent=2))
print(json.dumps(counts))
PY
printf 'RODADA COMPLETA APROVADA: %s\n' "$round"
