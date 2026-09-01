#!/usr/bin/env bash
set -euo pipefail

QUEUE_TEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
QUEUE_TEST_GROUP="group: deploy-vps-191-252-120-96"
QUEUE_TEST_WORKFLOWS=(
  ".github/workflows/email-service-ci.yml"
  ".github/workflows/feo-ci.yml"
  ".github/workflows/image-watermark-ci.yml"
  ".github/workflows/image-zipper-ci.yml"
  ".github/workflows/lead-portal-ci.yml"
  ".github/workflows/mois-sales-library-worker-ci.yml"
  ".github/workflows/oprm-coletor-mei-ci.yml"
  ".github/workflows/ops-monitor-worker-ci.yml"
  ".github/workflows/pde-monitor-worker-ci.yml"
  ".github/workflows/product-ai-worker-ci.yml"
  ".github/workflows/product-discovery-worker-ci.yml"
  ".github/workflows/scientific-research-worker-ci.yml"
)

for relative_workflow in "${QUEUE_TEST_WORKFLOWS[@]}"; do
  workflow="${QUEUE_TEST_ROOT}/${relative_workflow}"
  if [[ ! -f "${workflow}" ]]; then
    echo "Workflow compartilhado ausente: ${relative_workflow}" >&2
    exit 1
  fi

  queue_block="$(grep -F -A2 "${QUEUE_TEST_GROUP}" "${workflow}" || true)"
  if ! grep -Fq "${QUEUE_TEST_GROUP}" <<<"${queue_block}"; then
    echo "Grupo de deploy compartilhado ausente: ${relative_workflow}" >&2
    exit 1
  fi
  if ! grep -Fq "queue: max" <<<"${queue_block}"; then
    echo "Fila ampliada ausente no deploy compartilhado: ${relative_workflow}" >&2
    exit 1
  fi
  if ! grep -Fq "cancel-in-progress: false" <<<"${queue_block}"; then
    echo "Proteção da execução ativa ausente: ${relative_workflow}" >&2
    exit 1
  fi

  push_block="$(awk '
    /^  push:/ { in_push = 1; next }
    in_push && /^  [[:alnum:]_-]+:/ { exit }
    in_push { print }
  ' "${workflow}")"
  if grep -Fq "${relative_workflow}" <<<"${push_block}"; then
    echo "Mudança isolada do workflow não pode disparar deploy produtivo: ${relative_workflow}" >&2
    exit 1
  fi

  pull_request_block="$(awk '
    /^  pull_request:/ { in_pull_request = 1; next }
    in_pull_request && /^  [[:alnum:]_-]+:/ { exit }
    in_pull_request { print }
  ' "${workflow}")"
  if ! grep -Fq "${relative_workflow}" <<<"${pull_request_block}"; then
    echo "Mudança do workflow deve continuar validada no pull request: ${relative_workflow}" >&2
    exit 1
  fi
  if ! grep -Fq "workflow_dispatch:" "${workflow}"; then
    echo "Rollout operacional explícito ausente: ${relative_workflow}" >&2
    exit 1
  fi
done

while IFS= read -r workflow; do
  relative_workflow="${workflow#"${QUEUE_TEST_ROOT}/"}"
  if [[ ! " ${QUEUE_TEST_WORKFLOWS[*]} " =~ " ${relative_workflow} " ]]; then
    echo "Novo workflow no host compartilhado sem registro no contrato: ${relative_workflow}" >&2
    exit 1
  fi
done < <(grep -rlF "${QUEUE_TEST_GROUP}" "${QUEUE_TEST_ROOT}/.github/workflows" | sort)

echo "Contrato da fila de deploy do host compartilhado validado."
