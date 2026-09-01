#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
ACTIONLINT_REVISION="644076a59742c2d1540ebd4686eab3c308f0e562"
ACTIONLINT_BINARY="${ACTIONLINT_BINARY:-${REPOSITORY_ROOT}/codex-cache/actionlint/${ACTIONLINT_REVISION}/actionlint}"

bash "${SCRIPT_DIRECTORY}/install-actionlint.sh" "${ACTIONLINT_BINARY}"
exec "${ACTIONLINT_BINARY}" "$@"
