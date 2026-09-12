#!/usr/bin/env bash
# Valida integralmente a cópia do processo e a regressão da cópia de atividades, sem produção.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/process-context-copy/$round"
mkdir -p "$output"
run() {
  local name=$1
  shift
  if "$@" > "$output/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    tail -n 55 "$output/$name.log"
    return 1
  fi
}
run frontend npm --prefix frontend test -- --run \
  src/pages/product/productProcessContext.test.tsx \
  src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  src/pages/product/ProductProcessAutomationPanel.test.tsx \
  src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  src/pages/product/ProductProcessTaskTracking.test.tsx \
  src/api/businessProcess/useProductProcessActivityExecutions.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "PROCESS_COPY_OUTPUT=$output/browser" node infra/testing/process-context-copy/browser.cjs
run activity-copy-regression env "ACTIVITY_COPY_OUTPUT=$output/activity-browser" node infra/testing/activity-context-copy/browser.cjs
run format npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/pages/product/ProductContextCopyButton.tsx \
  frontend/src/pages/product/ProductActivityContextCopyButton.tsx \
  frontend/src/pages/product/ProductActivityContextCopyButton.css \
  frontend/src/pages/product/productActivityContext.ts \
  frontend/src/pages/product/productProcessContext.ts \
  frontend/src/pages/product/productProcessPresentation.ts \
  frontend/src/pages/product/productProcessContext.test.tsx \
  frontend/src/pages/product/ProductProcessContextCopy.tsx \
  frontend/src/pages/product/ProductProcessAutomationPanel.tsx \
  frontend/src/pages/product/ProductProcessActivityExecutionsPage.tsx \
  infra/testing/process-context-copy/browser.cjs \
  infra/testing/process-context-copy/fixture.json
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
