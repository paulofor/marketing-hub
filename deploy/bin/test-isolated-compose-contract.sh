#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${DEPLOY_DIR}"

unset LEAD_PORTAL_PAYMENTS_AUTH_TOKEN

OPENAI_API_KEY=contract-test docker compose -f docker-compose.video.yml config --quiet
video_config="$(OPENAI_API_KEY=contract-test docker compose -f docker-compose.video.yml config)"
grep -Fq 'BACKEND_URL: http://191.252.181.168' <<<"${video_config}"
grep -Fq 'AGENT_HEALTH_KEY: videomaker' <<<"${video_config}"
grep -Fq 'MARKETING_HUB_REPOSITORY: /app' <<<"${video_config}"
grep -Fq 'CODEX_HOME: /root/.codex' <<<"${video_config}"
grep -Fq 'target: /root/.codex' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_ENABLED: "true"' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_MODEL: gpt-5.6' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_MAX_OUTPUT_TOKENS: "4000"' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_BUDGET_LIMIT_USD: "0.75"' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_RESERVATION_USD: "0.25"' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_INPUT_PRICE_PER_MILLION_USD: "4.00"' <<<"${video_config}"
grep -Fq 'VIDEO_REFERENCE_ANALYSIS_OUTPUT_PRICE_PER_MILLION_USD: "20.00"' <<<"${video_config}"
MYSQL_PASS=contract-test MCP_GITHUB_TOKEN=contract-test docker compose -f docker-compose.mcp.yml config --quiet

grep -Fq 'MCP_GITHUB_ENABLED: ${MCP_GITHUB_ENABLED:-true}' docker-compose.mcp.yml
grep -Fq 'MCP_GITHUB_OWNER: ${MCP_GITHUB_OWNER:-paulofor}' docker-compose.mcp.yml
grep -Fq 'MCP_GITHUB_REPO: ${MCP_GITHUB_REPO:-ai-hub}' docker-compose.mcp.yml
grep -Fq 'MCP_GITHUB_TOKEN: ${MCP_GITHUB_TOKEN:?MCP_GITHUB_TOKEN is required}' docker-compose.mcp.yml
grep -Fq 'MCP_GITHUB_TOKEN: ${{ secrets.MCP_GITHUB_TOKEN }}' ../.github/workflows/mcp-server.yml

grep -Fq 'MCP_LOG_BACKEND_PATH: ${MCP_LOG_BACKEND_PATH:-http://191.252.181.168:8099/ops-mh-observability-v2/backend-log-stream-x9k}' \
  docker-compose.mcp.yml
grep -Fq 'MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH: ${MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH:-http://191.252.120.96:18081/ops-product-discovery-observability-v1/logfile}' \
  docker-compose.mcp.yml
grep -Fq 'MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL: ${MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL:-http://191.252.120.96:18081/healthz}' \
  docker-compose.mcp.yml
grep -Fq 'MCP_BACKEND_RECOVERY_ENABLED: ${MCP_BACKEND_RECOVERY_ENABLED:-true}' docker-compose.mcp.yml
grep -Fq 'MCP_BACKEND_RECOVERY_HOST: ${MCP_BACKEND_RECOVERY_HOST:-191.252.181.168}' docker-compose.mcp.yml
grep -Fq 'MCP_BACKEND_RECOVERY_CONTAINER: ${MCP_BACKEND_RECOVERY_CONTAINER:-marketinghub-backend}' docker-compose.mcp.yml
grep -Fq 'MCP_BACKEND_RECOVERY_HEALTH_URL: ${MCP_BACKEND_RECOVERY_HEALTH_URL:-http://191.252.181.168/ops-mh-observability-v2/health}' docker-compose.mcp.yml
grep -Fq 'JAVA_OPTS: ${BACKEND_JAVA_OPTS:--XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=60 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/backend-oom.hprof}' docker-compose.yml

if ! grep -Fq 'CMD curl -fsS --connect-timeout 2 --max-time 4 http://127.0.0.1:8000/ops-mh-observability-v2/health || exit 1' \
  ../backend/ads-service/Dockerfile; then
  echo "[CONTRATO] O healthcheck do backend deve encerrar probes lentos antes do próximo intervalo." >&2
  exit 1
fi

if grep -q 'docker compose up' bin/apply-video-only.sh; then
  echo "[CONTRATO] apply-video-only.sh deve informar explicitamente docker-compose.video.yml." >&2
  exit 1
fi

if grep -q 'docker compose up' bin/apply-mcp-only.sh; then
  echo "[CONTRATO] apply-mcp-only.sh deve informar explicitamente docker-compose.mcp.yml." >&2
  exit 1
fi

grep -q 'docker compose -f docker-compose.video.yml up' bin/apply-video-only.sh

grep -q 'docker pull "${VIDEO_IMAGE}:${IMAGE_TAG}"' bin/apply-video-only.sh

if grep -q 'video-management-image.tar.*VIDEO_VPS_IP\|scp.*video-management-image.tar' ../.github/workflows/deploy-containers.yml; then
  echo "[CONTRATO] deploy de vídeo não deve transferir a imagem completa por SCP." >&2
  exit 1
fi

if ! grep -Fq 'backend-url: ${BACKEND_URL:${VIDEO_BACKEND_BASE_URL:http://backend:8000}}' ../video-management-service/src/main/resources/application.yml; then
  echo "[ARQUITETURA] A reconexão Codex de Apolo deve reutilizar VIDEO_BACKEND_BASE_URL quando BACKEND_URL não estiver disponível." >&2
  exit 1
fi

grep -q 'docker/build-push-action@v6' ../.github/workflows/deploy-containers.yml
grep -q 'cache-to: type=registry' ../.github/workflows/deploy-containers.yml
grep -q 'docker compose -f docker-compose.mcp.yml up' bin/apply-mcp-only.sh

# Os testes de inspeção audiovisual exercitam os binários reais. O job isolado
# deve instalar a mesma dependência que existe na imagem de produção.
video_build_job="$(sed -n '/^  video-management-image:/,/^  deploy-app:/p' ../.github/workflows/deploy-containers.yml)"
if ! grep -Fq 'sudo apt-get update && sudo apt-get install -y --no-install-recommends ffmpeg' <<<"${video_build_job}"; then
  echo "[CONTRATO] O job de vídeo deve instalar ffmpeg antes de executar a suíte." >&2
  exit 1
fi

# Alterar o publicador isolado exige reconstruir a imagem para que um deploy
# recuperado não termine verde mantendo código antigo no container de vídeo.
grep -Fq 'deploy/bin/apply-video-only.sh) video=true; video_deploy_descriptor=true ;;' \
  ../scripts/detect-deployment-changes.sh

# Alterar o próprio workflow pode modificar qualquer contrato de build/deploy.
# Todos os artefatos devem ser reconstruídos para validar a revisão publicada.
grep -Fq '.github/workflows/deploy-containers.yml) backend=true; frontend=true; video=true; app_deploy_descriptor=true; video_deploy_descriptor=true ;;' \
  ../scripts/detect-deployment-changes.sh

# O workflow deve consumir a fonte canônica de detecção em vez de manter uma
# segunda lista de módulos que possa divergir silenciosamente. A normalização
# preserva o contrato mesmo quando a chamada YAML é quebrada em várias linhas.
normalized_deploy_workflow="$(
  awk '{ sub(/[[:space:]]*\\[[:space:]]*$/, ""); printf "%s ", $0 }' \
    ../.github/workflows/deploy-containers.yml \
    | tr -s '[:space:]' ' '
)"
if ! grep -Fq 'bash scripts/detect-deployment-changes.sh "${base}" "${GITHUB_SHA}" "${GITHUB_OUTPUT}" "${deployed_frontend_revision}"' \
  <<<"${normalized_deploy_workflow}"; then
  echo "[CONTRATO] O workflow deve chamar o detector canônico com as revisões APP e frontend." >&2
  exit 1
fi

# Código de vídeo só pode chegar ao host quando a imagem do mesmo SHA tiver
# sido construída e publicada. Mudanças apenas de descritor continuam válidas.
grep -Fq "needs.detect-changes.outputs.video != 'true' || needs.video-management-image.result == 'success'" \
  ../.github/workflows/deploy-containers.yml
