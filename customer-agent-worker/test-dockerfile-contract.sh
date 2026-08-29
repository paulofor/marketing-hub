#!/usr/bin/env bash
set -euo pipefail

dockerfile="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/Dockerfile"

grep -Fq 'FROM eclipse-temurin:21-jre-noble' "${dockerfile}"
grep -Fq 'getent group operator >/dev/null || groupadd --gid 10001 operator' "${dockerfile}"
grep -Fq 'id --user operator >/dev/null 2>&1 || useradd --create-home --uid 10001 --gid operator operator' "${dockerfile}"
grep -Fq 'npm ci --omit=dev' "${dockerfile}"
grep -Fq 'ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright' "${dockerfile}"
grep -Fq 'npx playwright-core install --with-deps chromium' "${dockerfile}"
grep -Fq 'chmod -R a+rX /ms-playwright' "${dockerfile}"
grep -Fq 'COPY --from=build /build/src/main/resources/browser /app/browser' "${dockerfile}"
grep -Fq 'COPY review-evidence /app/commercial-evidence' "${dockerfile}"

visual_capture="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/src/main/resources/browser/bpm-visual-evidence.mjs"
test -s "${visual_capture}"
grep -Fq 'IPHONE_15_PRO' "${visual_capture}"
grep -Fq 'fullPage: true' "${visual_capture}"
grep -Fq 'evidenceType: "FOLD"' "${visual_capture}"

compose="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/docker-compose.yml"
grep -Fq 'PLAYWRIGHT_BROWSERS_PATH: /ms-playwright' "${compose}"
grep -Fq 'CUSTOMER_AGENT_COMMERCIAL_EVIDENCE_PATH: /app/commercial-evidence' "${compose}"
if grep -Fq 'CHROMIUM_BIN: /usr/bin/chromium' "${compose}"; then
  echo "[ARQUITETURA] O worker não pode apontar para um Chromium ausente da imagem." >&2
  exit 1
fi

workflow="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.github/workflows/customer-agent-worker-ci.yml"
grep -Fq 'Validate bundled Chromium as runtime user' "${workflow}"
grep -Fq 'Validate real mobile visual capture' "${workflow}"
grep -Fq 'npm test' "${workflow}"
grep -Fq 'await chromium.launch' "${workflow}"
if grep -Fq 'channel: "chromium"' "${workflow}"; then
  echo "[ARQUITETURA] O gate do container deve usar o headless shell compatível com filesystem somente leitura." >&2
  exit 1
fi

observer="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/src/main/resources/browser/mobile-observation.mjs"
grep -Fq 'const isDirectVideo' "${observer}"
grep -Fq 'await chromium.launch' "${observer}"
grep -Fq 'video.src = source' "${observer}"
grep -Fq 'loadedmetadata' "${observer}"
if grep -Fq 'channel: "chromium"' "${observer}"; then
  echo "[ARQUITETURA] A observação mobile deve usar o headless shell compatível com filesystem somente leitura." >&2
  exit 1
fi

if grep -Eq '&& groupadd --gid 10001 operator' "${dockerfile}"; then
  echo "[ARQUITETURA] O Dockerfile não pode recriar incondicionalmente o grupo operator." >&2
  exit 1
fi
