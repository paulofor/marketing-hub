#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIXTURE_DIRECTORY="$(mktemp -d)"

cleanup() {
  rm -rf -- "${FIXTURE_DIRECTORY}"
}
trap cleanup EXIT

cat >"${FIXTURE_DIRECTORY}/valid.yml" <<'YAML'
name: Valid queue
on: push
concurrency:
  group: valid-workflow
  queue: max
  cancel-in-progress: false
jobs:
  validate:
    runs-on: ubuntu-latest
    concurrency:
      group: valid-job
      queue: single
    steps:
      - run: echo valid
YAML

cat >"${FIXTURE_DIRECTORY}/invalid-value.yml" <<'YAML'
name: Invalid queue value
on: push
concurrency:
  group: invalid-value
  queue: unlimited
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - run: echo invalid
YAML

cat >"${FIXTURE_DIRECTORY}/invalid-conflict.yml" <<'YAML'
name: Invalid queue conflict
on: push
concurrency:
  group: invalid-conflict
  queue: max
  cancel-in-progress: true
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - run: echo invalid
YAML

bash "${SCRIPT_DIRECTORY}/run-actionlint.sh" "${FIXTURE_DIRECTORY}/valid.yml"

if invalid_value_output="$(bash "${SCRIPT_DIRECTORY}/run-actionlint.sh" "${FIXTURE_DIRECTORY}/invalid-value.yml" 2>&1)"; then
  echo "Actionlint aceitou um valor inválido de queue." >&2
  exit 1
fi
grep -Fq 'invalid value "unlimited" for "queue"' <<<"${invalid_value_output}"

if invalid_conflict_output="$(bash "${SCRIPT_DIRECTORY}/run-actionlint.sh" "${FIXTURE_DIRECTORY}/invalid-conflict.yml" 2>&1)"; then
  echo "Actionlint aceitou queue: max com cancel-in-progress: true." >&2
  exit 1
fi
grep -Fq '"queue: max" cannot be combined with "cancel-in-progress: true"' <<<"${invalid_conflict_output}"

echo "Contrato do Actionlint para queue validado."
