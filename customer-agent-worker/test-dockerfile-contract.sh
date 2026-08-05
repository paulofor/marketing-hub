#!/usr/bin/env bash
set -euo pipefail

dockerfile="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/Dockerfile"

grep -Fq 'getent group operator >/dev/null || groupadd --gid 10001 operator' "${dockerfile}"
grep -Fq 'id --user operator >/dev/null 2>&1 || useradd --create-home --uid 10001 --gid operator operator' "${dockerfile}"
grep -Fq 'chromium' "${dockerfile}"
grep -Fq 'npm ci --omit=dev' "${dockerfile}"
grep -Fq 'COPY --from=build /build/src/main/resources/browser /app/browser' "${dockerfile}"

if grep -Eq '&& groupadd --gid 10001 operator' "${dockerfile}"; then
  echo "[ARQUITETURA] O Dockerfile não pode recriar incondicionalmente o grupo operator." >&2
  exit 1
fi
