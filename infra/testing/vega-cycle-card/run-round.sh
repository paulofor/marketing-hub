#!/usr/bin/env bash
# Executa a matriz inteira sem consultar nem escrever em serviços produtivos.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/vega-cycle-card/$round"
mkdir -p "$output"
run() {
  local label=$1
  shift
  if "$@" > "$output/$label.log" 2>&1; then
    printf 'PASS %s\n' "$label"
  else
    tail -n 50 "$output/$label.log"
    return 1
  fi
}
run tests npm --prefix frontend test -- --run src/components/ProductValueChainCycleSummary.test.tsx src/components/ProductValueChainPosition.test.tsx src/pages/HomePage.test.tsx src/pages/product/ProductListPage.test.tsx src/pages/product/ProductProcessActivityExecutionsPage.test.tsx src/pages/product/ProductValueChainHistoryPage.test.tsx src/pages/learningCycle/LearningCyclesPage.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "VEGA_CARD_OUTPUT=$output/browser" node infra/testing/vega-cycle-card/browser.cjs
run format npm exec --yes --package=prettier@3.6.2 -- prettier --check frontend/src/components/ProductValueChainCycleSummary.tsx frontend/src/components/ProductValueChainCycleSummary.css frontend/src/components/ProductValueChainCycleSummary.test.tsx frontend/src/components/ProductValueChainPosition.tsx
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
