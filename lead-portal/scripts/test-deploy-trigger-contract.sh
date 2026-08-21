#!/usr/bin/env bash
set -euo pipefail

workflow=".github/workflows/lead-portal-ci.yml"
expected="if: github.ref == 'refs/heads/main' && (github.event_name == 'push' || github.event_name == 'workflow_dispatch')"

grep -F "$expected" "$workflow" >/dev/null
grep -F 'timeout-minutes: 45' "$workflow" >/dev/null

if grep -F "if: github.ref == 'refs/heads/main' && github.event_name == 'workflow_dispatch'" "$workflow" >/dev/null; then
  echo "O deploy do Lead Portal não pode ignorar merges publicados em main." >&2
  exit 1
fi

echo "Contrato de deploy do Lead Portal aprovado: push em main e acionamento manual publicam; PR não publica."
