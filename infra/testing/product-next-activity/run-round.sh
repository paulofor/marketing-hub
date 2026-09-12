#!/usr/bin/env bash
# Valida navegação e regressões locais, sem consultar nem escrever em produção.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/product-next-process/$round"
mkdir -p "$output"
run() {
  local label=$1
  shift
  if "$@" > "$output/$label.log" 2>&1; then
    printf 'PASS %s\n' "$label"
  else
    tail -n 60 "$output/$label.log"
    return 1
  fi
}
run tests npm --prefix frontend test -- --run src/components/ProductNextProcessSummary.test.tsx src/components/ProductValueChainCycleSummary.test.tsx src/components/ProductValueChainPosition.test.tsx src/pages/product/ProductProcessAutomationPanel.test.tsx src/pages/HomePage.test.tsx src/pages/product/ProductListPage.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/ProductValueChainHistoryPage.test.tsx src/pages/learningCycle/LearningCyclesPage.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "NEXT_PROCESS_OUTPUT=$output/browser" node infra/testing/product-next-activity/browser.cjs
run format npm exec --yes --package=prettier@3.6.2 -- prettier --check frontend/src/components/ProductNextProcess* frontend/src/components/ProductValueChainCycleSummary.tsx frontend/src/components/ProductValueChainCycleSummary.test.tsx frontend/src/components/ProductValueChainPosition.tsx frontend/src/components/ProductValueChainPosition.test.tsx frontend/src/api/businessProcess/useProductProcessActivityExecutions.ts frontend/src/api/learningCycle/useCycleProcessContext.ts infra/testing/product-next-activity/browser.cjs infra/testing/product-next-activity/rigel-activities.json
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
