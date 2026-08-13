#!/usr/bin/env bash
set -euo pipefail

workflow=".github/workflows/frontend.yml"

grep -q 'retries: 3' "$workflow"
grep -Fq '[500, 502, 503, 504].includes(error.status)' "$workflow"
grep -q 'error.status === 404' "$workflow"
grep -q 'throw error' "$workflow"
grep -Fq -- "- '.github/workflows/frontend.yml'" "$workflow"
grep -Fq -- "- 'scripts/test-frontend-workflow-artifact-cleanup.sh'" "$workflow"
grep -q 'workflow_dispatch:' "$workflow"

if grep -Eq 'continue-on-error:[[:space:]]*true' "$workflow"; then
  echo "Frontend workflow must not broadly ignore cleanup failures." >&2
  exit 1
fi

echo "Frontend artifact cleanup contract is valid."
