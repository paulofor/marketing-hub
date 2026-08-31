#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
READER="${REPO_ROOT}/scripts/read-frontend-build-revision.sh"
EXPECTED_REVISION="0123456789abcdef0123456789abcdef01234567"

actual_revision="$(printf '{\n  "status": "ok",\n  "commit": "%s",\n  "imageTag": "%s"\n}\n' \
  "${EXPECTED_REVISION}" "${EXPECTED_REVISION}" | bash "${READER}")"
test "${actual_revision}" = "${EXPECTED_REVISION}"

if printf '{"status":"ok","commit":"local"}\n' | bash "${READER}" >/dev/null 2>&1; then
  printf '[ARQUITETURA] leitor aceitou healthz sem revisão imutável.\n' >&2
  exit 1
fi

printf 'Leitura da revisão publicada do frontend validada.\n'
