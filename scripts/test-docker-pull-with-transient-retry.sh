#!/usr/bin/env bash
set -euo pipefail

TEST_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_TARGET="${TEST_SCRIPT_DIR}/docker-pull-with-transient-retry.sh"
TEST_STATE="$(mktemp)"
trap 'rm -f "${TEST_STATE}"' EXIT

docker() {
  local current_attempt
  current_attempt="$(<"${TEST_STATE}")"
  current_attempt=$((current_attempt + 1))
  printf '%s' "${current_attempt}" >"${TEST_STATE}"
  [[ "${current_attempt}" -gt "${TEST_FAILURES_BEFORE_SUCCESS}" ]]
}

sleep() {
  :
}

export -f docker sleep
export TEST_STATE TEST_FAILURES_BEFORE_SUCCESS

printf '0' >"${TEST_STATE}"
TEST_FAILURES_BEFORE_SUCCESS=2 \
  DOCKER_PULL_MAX_ATTEMPTS=3 \
  DOCKER_PULL_RETRY_DELAY_SECONDS=0 \
  bash "${TEST_TARGET}" mysql:5.7

if [[ "$(<"${TEST_STATE}")" != "3" ]]; then
  echo "O retry não realizou exatamente três tentativas antes do sucesso." >&2
  exit 1
fi

printf '0' >"${TEST_STATE}"
if TEST_FAILURES_BEFORE_SUCCESS=3 \
  DOCKER_PULL_MAX_ATTEMPTS=3 \
  DOCKER_PULL_RETRY_DELAY_SECONDS=0 \
  bash "${TEST_TARGET}" mysql:5.7; then
  echo "O retry aceitou uma falha permanente após esgotar o limite." >&2
  exit 1
fi

if [[ "$(<"${TEST_STATE}")" != "3" ]]; then
  echo "O retry permanente não respeitou o limite de três tentativas." >&2
  exit 1
fi

if DOCKER_PULL_MAX_ATTEMPTS=0 bash "${TEST_TARGET}" mysql:5.7 >/dev/null 2>&1; then
  echo "O retry aceitou um limite inválido." >&2
  exit 1
fi

echo "Contrato de retry transitório do Docker validado."
