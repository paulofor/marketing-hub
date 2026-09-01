#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
ACTIONLINT_REVISION="644076a59742c2d1540ebd4686eab3c308f0e562"
ACTIONLINT_BINARY="${ACTIONLINT_BINARY:-${REPOSITORY_ROOT}/codex-cache/actionlint/${ACTIONLINT_REVISION}/actionlint}"
SHELLCHECK_BINARY="${ACTIONLINT_SHELLCHECK_EXECUTABLE:-shellcheck}"

bash "${SCRIPT_DIRECTORY}/install-actionlint.sh" "${ACTIONLINT_BINARY}"

if command -v "${SHELLCHECK_BINARY}" >/dev/null 2>&1; then
  exec "${ACTIONLINT_BINARY}" \
    -shellcheck "${SCRIPT_DIRECTORY}/actionlint-shellcheck.sh" \
    "$@"
fi

exec "${ACTIONLINT_BINARY}" -shellcheck "" "$@"
