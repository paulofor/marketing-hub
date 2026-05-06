#!/usr/bin/env bash
set -euo pipefail

# Deploy do módulo mois-hotmart-collector no mesmo host do MOIS principal.
# Pré-requisitos:
# - Docker + Docker Compose plugin no host remoto
# - Acesso SSH configurado
# - Registry acessível para pull da imagem
#
# Variáveis obrigatórias:
#   DEPLOY_HOST=usuario@host
#   IMAGE_TAG=2026.05.06-1
#
# Variáveis opcionais:
#   IMAGE_REPO=marketinghub/mois-hotmart-collector
#   REMOTE_DIR=/opt/marketinghub/mois-hotmart-collector
#   SSH_OPTS='-o StrictHostKeyChecking=no'

: "${DEPLOY_HOST:?DEPLOY_HOST é obrigatório (ex.: ubuntu@191.252.120.96)}"
: "${IMAGE_TAG:?IMAGE_TAG é obrigatório (ex.: 2026.05.06-1)}"

IMAGE_REPO="${IMAGE_REPO:-marketinghub/mois-hotmart-collector}"
REMOTE_DIR="${REMOTE_DIR:-/opt/marketinghub/mois-hotmart-collector}"
SSH_OPTS="${SSH_OPTS:-}"
IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"

printf '==> Build da imagem %s\n' "$IMAGE"
docker build -t "$IMAGE" ./mois-hotmart-collector

printf '==> Push da imagem %s\n' "$IMAGE"
docker push "$IMAGE"

printf '==> Preparando diretório remoto %s:%s\n' "$DEPLOY_HOST" "$REMOTE_DIR"
ssh $SSH_OPTS "$DEPLOY_HOST" "mkdir -p '$REMOTE_DIR'"

printf '==> Enviando compose de deploy\n'
scp $SSH_OPTS ./mois-hotmart-collector/docker-compose.deploy.yml "$DEPLOY_HOST:$REMOTE_DIR/docker-compose.yml"

printf '==> Subindo container remoto com nova imagem\n'
ssh $SSH_OPTS "$DEPLOY_HOST" \
  "cd '$REMOTE_DIR' && MOIS_HOTMART_COLLECTOR_IMAGE='$IMAGE' docker compose pull && MOIS_HOTMART_COLLECTOR_IMAGE='$IMAGE' docker compose up -d"

printf '==> Deploy concluído em %s com imagem %s\n' "$DEPLOY_HOST" "$IMAGE"
