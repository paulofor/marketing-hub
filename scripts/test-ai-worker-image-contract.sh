#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
DOCKERFILE="${REPOSITORY_ROOT}/ai-worker/Dockerfile"
WORKFLOW="${REPOSITORY_ROOT}/.github/workflows/ai-worker.yml"
DOCKERIGNORE="${REPOSITORY_ROOT}/ai-worker/.dockerignore"

grep -Fq 'COPY target/app.jar ./app.jar' "${DOCKERFILE}"
grep -Fq 'name: ai-worker-jar' "${WORKFLOW}"
grep -Fq 'path: ai-worker/target/app.jar' "${WORKFLOW}"
grep -Fq 'path: ai-worker/target' "${WORKFLOW}"
grep -Fq '!/target/app.jar' "${DOCKERIGNORE}"
grep -Fq 'context: ai-worker' "${WORKFLOW}"

PACKAGE_LINE="$(grep -nF 'run: mvn -B -s settings.xml package -DskipTests' "${WORKFLOW}" | cut -d: -f1)"
UPLOAD_LINE="$(grep -nF 'name: Preserve tested worker package' "${WORKFLOW}" | cut -d: -f1)"
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
