#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
DOCKERFILE="${REPOSITORY_ROOT}/ai-worker/Dockerfile"
WORKFLOW="${REPOSITORY_ROOT}/.github/workflows/ai-worker.yml"
VALIDATION="${REPOSITORY_ROOT}/.github/workflows/ai-worker-validation.yml"
DOCKERIGNORE="${REPOSITORY_ROOT}/ai-worker/.dockerignore"

grep -Fq 'COPY target/app.jar ./app.jar' "${DOCKERFILE}"
grep -Fq 'name: ai-worker-jar' "${VALIDATION}"
grep -Fq 'path: ai-worker/target/app.jar' "${VALIDATION}"
grep -Fq 'path: ai-worker/target' "${WORKFLOW}"
grep -Fq '!/target/app.jar' "${DOCKERIGNORE}"
grep -Fq 'context: ai-worker' "${WORKFLOW}"
grep -Fq 'up -d --force-recreate --remove-orphans' "${WORKFLOW}"
grep -Fq 'ARG CODEX_VERSION=0.159.3' "${DOCKERFILE}"
grep -Fq 'codex --version' "${DOCKERFILE}"
grep -Fq 'COPY codex/*.mjs /app/codex/' "${DOCKERFILE}"
grep -Fq 'npm test' "${VALIDATION}"
grep -Fq 'uses: ./.github/workflows/ai-worker-validation.yml' "${WORKFLOW}"
grep -Fq 'pull_request:' "${VALIDATION}"
grep -Fq 'workflow_call:' "${VALIDATION}"
if grep -Eq 'push:|docker login|Publish service|deploy:' "${VALIDATION}"; then
  echo 'A validação de PR não pode publicar imagens ou serviços.' >&2
  exit 1
fi
grep -Fq 'Verify worker runtime' "${WORKFLOW}"
grep -Fq 'OPENAI_TEXT_TRANSPORT:-codex-app-server' "${REPOSITORY_ROOT}/ai-worker/docker-compose.yml"
grep -Fq '/opt/marketinghub/ai-worker/codex-home' "${WORKFLOW}"

PACKAGE_LINE="$(grep -nF 'run: mvn -B -s settings.xml package -DskipTests' "${VALIDATION}" | cut -d: -f1)"
UPLOAD_LINE="$(grep -nF 'name: Preserve tested worker package' "${VALIDATION}" | cut -d: -f1)"
DOWNLOAD_LINE="$(grep -nF 'name: Restore tested worker package' "${WORKFLOW}" | cut -d: -f1)"
IMAGE_LINE="$(grep -nF 'name: Build and push image' "${WORKFLOW}" | cut -d: -f1)"

if ((PACKAGE_LINE >= UPLOAD_LINE || DOWNLOAD_LINE >= IMAGE_LINE)); then
  echo 'O JAR aprovado deve ser empacotado, preservado e restaurado antes da imagem.' >&2
  exit 1
fi

if grep -Fq 'COPY backend/ads-service' "${DOCKERFILE}"; then
  echo 'O Dockerfile do AI Worker não pode recompilar todo o backend.' >&2
  exit 1
fi

if grep -Eq '^FROM[[:space:]]+maven:' "${DOCKERFILE}"; then
  echo 'A imagem do AI Worker deve receber o JAR já aprovado pelo job de testes.' >&2
  exit 1
fi

echo 'Contrato da imagem testada do AI Worker validado.'
