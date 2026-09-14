#!/usr/bin/env bash

set -euo pipefail

CANONICAL_MODEL="gpt-image-2.5-sunburst"
RETIRED_MODEL_PATTERN='gpt-image-(?:1(?:\.5)?|2(?!\.5))'

required_defaults=(
  ".env.example"
  "docker-compose.yml"
  "ai-worker/docker-compose.yml"
  "ai-worker/src/main/resources/application.properties"
  "deploy/docker-compose.yml"
  "deploy/docker-compose.video.yml"
  "feo/src/main/resources/application.yml"
  "lead-portal-payments-service/docker-compose.deploy.yml"
  "lead-portal-payments-service/src/main/resources/application.yml"
  "meta-ad-approver-worker/docker-compose.yml"
  "meta-ad-approver-worker/src/main/resources/application.yml"
  "video-management-service/src/main/resources/application.yml"
)

active_sources=(
  ".env.example"
  "docker-compose.yml"
  ".github/workflows/meta-ad-approver-worker-ci.yml"
  "ai-worker/docker-compose.yml"
  "ai-worker/src/main"
  "backend/ads-service/src/main/java"
  "backend/ads-service/src/main/resources/agent-harness"
  "deploy/docker-compose.yml"
  "deploy/docker-compose.video.yml"
  "deploy/local-validation/creative-production-v5/docker-compose.yml"
  "feo/src/main"
  "frontend/src"
  "landing-generator-agent-worker/src/main/resources/prompts"
  "lead-portal/backend/src/main"
  "lead-portal-payments-service/docker-compose.deploy.yml"
  "lead-portal-payments-service/scripts/agenda-cheia-photo-library.sh"
  "lead-portal-payments-service/src/main"
  "meta-ad-approver-worker/docker-compose.yml"
  "meta-ad-approver-worker/src/main"
  "video-management-service/src/main"
)

for file in "${required_defaults[@]}"; do
  if ! rg -q --fixed-strings "${CANONICAL_MODEL}" "${file}"; then
    echo "[IMAGEM] padrão canônico ausente em ${file}" >&2
    exit 1
  fi
done

if rg --pcre2 -n --glob '!**/*.test.*' --glob '!**/__tests__/**' \
  "${RETIRED_MODEL_PATTERN}" "${active_sources[@]}"; then
  echo "[IMAGEM] modelo aposentado encontrado em configuração ou código de produção" >&2
  exit 1
fi

if ! rg -q --fixed-strings '"GPT IMAGE 2 5 SUNBURST"' frontend/src/utils/imagePricing.ts; then
  echo "[IMAGEM] estimativa financeira do Sunburst ausente no frontend" >&2
  exit 1
fi

video_contract_files=(
  "docker-compose.yml"
  "deploy/docker-compose.yml"
  "deploy/docker-compose.video.yml"
  "video-management-service/src/main/resources/application.yml"
)
for file in "${video_contract_files[@]}"; do
  if ! rg -q --fixed-strings "OPENAI_IMAGE_ORCHESTRATION_MODEL" "${file}"; then
    echo "[IMAGEM] modelo orquestrador de vídeo não está isolado em ${file}" >&2
    exit 1
  fi
done

if ! rg -U -q --pcre2 \
  'file: changesets/2030-09-19-add-gpt-image-2-5-sunburst\.yaml\s+relativeToChangelogFile: true' \
  backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml; then
  echo "[IMAGEM] include Liquibase canônico não é relativo ao changelog mestre" >&2
  exit 1
fi

echo "[IMAGEM] Contrato gpt-image-2.5-sunburst validado nos fluxos ativos."
