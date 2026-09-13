#!/usr/bin/env bash
# Homologa conjuntamente contratos, interface e execução da imagem candidata na sandbox.
set -euo pipefail
cd "$(dirname "$0")/../../.."
round="${1:?Informe a rodada}"
[[ "$round" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
: "${GEN45_IMAGE:?Informe a imagem candidata construída pelo Dockerfile versionado}"
: "${GEN45_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
bash infra/testing/runway-clip-plan/run-round.sh "$round"
export GEN45_EVIDENCE_DIR="$PWD/artifacts/runway-access-recovery/$round/image"
bash infra/testing/runway-gen45/verify-image.sh
printf 'MATRIZ GEN45 COMPLETA APROVADA: %s\n' "$round"
