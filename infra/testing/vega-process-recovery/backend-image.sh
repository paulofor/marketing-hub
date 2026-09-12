#!/usr/bin/env bash
# Incorpora e confere o pacote aprovado antes da limpeza da imagem temporária.
set -euo pipefail
bash scripts/docker-build-temporary-image.sh backend -f backend/ads-service/Dockerfile .
python3 infra/testing/vega-process-recovery/verify-image-jar.py \
  "aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}/backend:${AIHUB_HOMOLOGATION_IMAGE_TAG:-latest}"
