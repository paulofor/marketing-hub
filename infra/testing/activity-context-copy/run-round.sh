#!/usr/bin/env bash
# Homologa a cópia no frontend real, sem acessar ou alterar serviços de produção.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round=${1:?Informe a rodada}
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]]
output="$PWD/artifacts/activity-context-copy/$round"
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
run frontend npm --prefix frontend test -- --run \
  src/pages/product/ProductProcessActivityExecutionsPage.test.tsx \
  src/pages/product/ProductProcessActivityExecutionPanel.test.tsx \
  src/pages/product/ProductProcessTaskTracking.test.tsx \
  src/api/businessProcess/useProductProcessActivityExecutions.test.tsx
run typecheck npm --prefix frontend run typecheck
run build npm --prefix frontend run build
run browser env "ACTIVITY_COPY_OUTPUT=$output/browser" node infra/testing/activity-context-copy/browser.cjs
run format npm exec --yes --package=prettier@3.6.2 -- prettier --check \
  frontend/src/pages/product/ProductActivityContextCopyButton.tsx \
  frontend/src/pages/product/ProductActivityContextCopyButton.css \
  frontend/src/pages/product/ProductProcessActivityExecutionsPage.tsx \
  infra/testing/activity-context-copy/browser.cjs
run diff git diff --check
printf 'RODADA COMPLETA APROVADA %s\n' "$round"
