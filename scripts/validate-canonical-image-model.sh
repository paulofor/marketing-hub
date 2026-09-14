#!/usr/bin/env bash

set -euo pipefail

CANONICAL_MODEL="gpt-image-2.5-sunburst"
RETIRED_MODEL_PATTERN='gpt-image-(?:1(?:\.5)?|2(?!\.5))'

if ! command -v rg >/dev/null 2>&1; then
  echo "[IMAGEM] dependência obrigatória ausente: rg (ripgrep); instale ripgrep antes de validar" >&2
  exit 2
fi

# Exige uma correspondência e distingue violação do contrato de falha técnica na leitura.
require_match() {
  local file="$1"
  local violation="$2"
  shift 2
  local status=0
  rg "$@" -- "$file" || status=$?
  case "$status" in
    0) return 0 ;;
    1)
      echo "[IMAGEM] ${violation}" >&2
      exit 1
      ;;
    *)
      echo "[IMAGEM] falha técnica ao verificar ${file} (rg: ${status}); contrato não validado" >&2
      exit 2
      ;;
  esac
}

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
  require_match "$file" "padrão canônico ausente em ${file}" \
    -q --fixed-strings "$CANONICAL_MODEL"
done

scan_status=0
rg --pcre2 -n --glob '!**/*.test.*' --glob '!**/__tests__/**' \
  "${RETIRED_MODEL_PATTERN}" "${active_sources[@]}" || scan_status=$?
case "$scan_status" in
  0)
    echo "[IMAGEM] modelo aposentado encontrado em configuração ou código de produção" >&2
    exit 1
    ;;
  1) ;; # Nenhuma ocorrência: pesquisa concluída sem modelo aposentado.
  *)
    echo "[IMAGEM] falha técnica ao pesquisar modelos aposentados (rg: ${scan_status}); contrato não validado" >&2
    exit 2
    ;;
esac

require_match frontend/src/utils/imagePricing.ts \
  "estimativa financeira do Sunburst ausente no frontend" \
  -q --fixed-strings '"GPT IMAGE 2 5 SUNBURST"'

video_contract_files=(
  "docker-compose.yml"
  "deploy/docker-compose.yml"
  "deploy/docker-compose.video.yml"
  "video-management-service/src/main/resources/application.yml"
)
for file in "${video_contract_files[@]}"; do
  require_match "$file" "modelo orquestrador de vídeo não está isolado em ${file}" \
    -q --fixed-strings "OPENAI_IMAGE_ORCHESTRATION_MODEL"
done

require_match backend/ads-service/src/main/resources/db/changelog/db.changelog-master.yaml \
  "include Liquibase canônico não é relativo ao changelog mestre" \
  -U -q --pcre2 \
  'file: changesets/2030-09-19-add-gpt-image-2-5-sunburst\.yaml\s+relativeToChangelogFile: true'

echo "[IMAGEM] Contrato gpt-image-2.5-sunburst validado nos fluxos ativos."
