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

AGENT_QUEUE_GROUP="group: deploy-vps-163-245-202-80"
AGENT_QUEUE_WORKFLOWS=(
  ".github/workflows/agent-executor-admin-controller-ci.yml"
  ".github/workflows/communication-agent-worker-ci.yml"
  ".github/workflows/customer-agent-worker-ci.yml"
  ".github/workflows/experiment-strategist-worker-ci.yml"
  ".github/workflows/financial-agent-worker-ci.yml"
  ".github/workflows/growth-operator-worker-ci.yml"
  ".github/workflows/landing-generator-agent-worker-ci.yml"
  ".github/workflows/meta-ad-approver-worker-ci.yml"
  ".github/workflows/product-discovery-worker-ci.yml"
)

for relative_workflow in "${AGENT_QUEUE_WORKFLOWS[@]}"; do
  workflow="${QUEUE_TEST_ROOT}/${relative_workflow}"
  if [[ ! -f "${workflow}" ]]; then
    echo "Workflow do VPS de agentes ausente: ${relative_workflow}" >&2
    exit 1
  fi

  deploy_block="$(awk '
    /^  deploy:/ { in_deploy = 1 }
    in_deploy && /^  [[:alnum:]_-]+:/ && !/^  deploy:/ { exit }
    in_deploy { print }
  ' "${workflow}")"
  queue_block="$(grep -F -A2 "${AGENT_QUEUE_GROUP}" <<<"${deploy_block}" || true)"
  if ! grep -Fq "${AGENT_QUEUE_GROUP}" <<<"${queue_block}" \
    || ! grep -Fq "queue: max" <<<"${queue_block}" \
    || ! grep -Fq "cancel-in-progress: false" <<<"${queue_block}"; then
    echo "Deploy fora da fila única do VPS de agentes: ${relative_workflow}" >&2
    exit 1
  fi
  if ! grep -Fq "163.245.202.80" "${workflow}" \
    || ! grep -Fq 'secrets.GROWTH_OPERATOR_VPS_SSH_KEY' "${workflow}"; then
    echo "Workflow fora do host ou da credencial canônica dos agentes: ${relative_workflow}" >&2
    exit 1
  fi
  if grep -Eq 'docker (builder|image|system) prune -af' "${workflow}"; then
    echo "Prune agressivo pode apagar imagem puxada por outro agente: ${relative_workflow}" >&2
    exit 1
  fi
done

while IFS= read -r workflow; do
  relative_workflow="${workflow#"${QUEUE_TEST_ROOT}/"}"
  if [[ ! " ${AGENT_QUEUE_WORKFLOWS[*]} " =~ " ${relative_workflow} " ]]; then
    echo "Novo workflow na fila do VPS de agentes sem registro no contrato: ${relative_workflow}" >&2
    exit 1
  fi
done < <(grep -rlF "${AGENT_QUEUE_GROUP}" "${QUEUE_TEST_ROOT}/.github/workflows" | sort)

PUBLIC_HOST_QUEUE_GROUP="group: deploy-vps-163-245-200-7"
PUBLIC_HOST_QUEUE_WORKFLOWS=(
  ".github/workflows/harness-library-api-ci.yml"
  ".github/workflows/harness-library-api-publication.yml"
  ".github/workflows/lead-portal-payments-ci.yml"
  ".github/workflows/pde-platform-metodo-musa-ci.yml"
  ".github/workflows/recover-public-proxy.yml"
)

for relative_workflow in "${PUBLIC_HOST_QUEUE_WORKFLOWS[@]}"; do
  workflow="${QUEUE_TEST_ROOT}/${relative_workflow}"
  if [[ ! -f "${workflow}" ]]; then
    echo "Workflow do VPS público ausente: ${relative_workflow}" >&2
    exit 1
  fi

  queue_block="$(grep -F -A2 "${PUBLIC_HOST_QUEUE_GROUP}" "${workflow}" || true)"
  if ! grep -Fq "${PUBLIC_HOST_QUEUE_GROUP}" <<<"${queue_block}" \
    || ! grep -Fq "queue: max" <<<"${queue_block}" \
    || ! grep -Fq "cancel-in-progress: false" <<<"${queue_block}"; then
    echo "Operação fora da fila única do VPS público: ${relative_workflow}" >&2
    exit 1
  fi

  if grep -Eq 'docker (builder|image|system) prune -af' "${workflow}"; then
    echo "Prune agressivo pode apagar imagem preparada por outra publicação: ${relative_workflow}" >&2
    exit 1
  fi
done

while IFS= read -r workflow; do
  relative_workflow="${workflow#"${QUEUE_TEST_ROOT}/"}"
  if [[ ! " ${PUBLIC_HOST_QUEUE_WORKFLOWS[*]} " =~ " ${relative_workflow} " ]]; then
    echo "Novo workflow na fila do VPS público sem registro no contrato: ${relative_workflow}" >&2
    exit 1
  fi
done < <(grep -rlF "${PUBLIC_HOST_QUEUE_GROUP}" "${QUEUE_TEST_ROOT}/.github/workflows" | sort)

ARGOS_WORKFLOW="${QUEUE_TEST_ROOT}/.github/workflows/product-discovery-worker-ci.yml"
if grep -Fq "${QUEUE_TEST_GROUP}" "${ARGOS_WORKFLOW}" \
  || grep -Fq "DEPLOY_HOST: 191.252.120.96" "${ARGOS_WORKFLOW}"; then
  echo "Argos não pode voltar ao VPS de 957 MB." >&2
  exit 1
fi

echo "Contrato da fila de deploy do host compartilhado validado."
