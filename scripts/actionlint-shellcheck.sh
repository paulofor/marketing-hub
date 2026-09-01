#!/usr/bin/env bash
set -euo pipefail

SHELLCHECK_BINARY="${ACTIONLINT_SHELLCHECK_EXECUTABLE:-shellcheck}"

exec "${SHELLCHECK_BINARY}" --severity=warning "$@"
