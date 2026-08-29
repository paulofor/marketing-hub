#!/usr/bin/env bash
set -euo pipefail

module_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow="${module_dir}/../.github/workflows/recover-public-proxy.yml"
recovery_script="${module_dir}/scripts/recover-public-proxy.sh"
ci_workflow="${module_dir}/../.github/workflows/lead-portal-payments-ci.yml"

for file in "${workflow}" "${recovery_script}" "${ci_workflow}"; do
  test -s "${file}" || {
    echo "[ARQUITETURA] contrato obrigatório ausente: ${file}" >&2
    exit 1
  }
done

grep -Fq 'run-name: Recover public proxy · ${{ inputs.request_id }}' "${workflow}" || {
  echo '[ARQUITETURA] run da recuperação deve ser correlacionável pelo request_id.' >&2
  exit 1
}
grep -Fq 'group: recover-public-proxy' "${workflow}" || {
  echo '[ARQUITETURA] recuperações do proxy devem ser serializadas.' >&2
  exit 1
}
grep -Fq 'DEPLOY_HOST: 163.245.200.7' "${workflow}" || {
  echo '[ARQUITETURA] host da recuperação deve permanecer fixo.' >&2
  exit 1
}
grep -Fq 'RECOVER_PUBLIC_PROXY' "${workflow}" || {
  echo '[ARQUITETURA] workflow deve exigir confirmação literal.' >&2
  exit 1
}
grep -Fq '< lead-portal-payments-service/scripts/recover-public-proxy.sh' "${workflow}" || {
  echo '[ARQUITETURA] workflow deve executar o recuperador versionado sem depender do host.' >&2
  exit 1
}
for probe in \
  'https://kit-whatsapp-pronto.digicomdigital.com.br' \
  '/healthz' \
  '/pde-health-contract.json' \
  'kit-whatsapp-pronto'; do
  grep -Fq "${probe}" "${workflow}" || {
    echo "[ARQUITETURA] sonda obrigatória ausente: ${probe}" >&2
    exit 1
  }
done

if rg -n 'inputs\.(host|target|container|service|compose|image|command)' "${workflow}"; then
  echo '[ARQUITETURA] payload público não pode escolher alvo ou comando operacional.' >&2
  exit 1
fi

recovery_step="$(awk '
  /- name: Recover fixed public proxy/ { in_step = 1 }
  in_step && /- name: Validate HTTPS health and PDE contract/ { exit }
  in_step { print }
' "${workflow}")"
if rg -n '(^|[[:space:]])(docker[[:space:]]+(compose[[:space:]]+)?(build|pull)|rsync|git[[:space:]]+(push|commit)|docker[[:space:]].*prune)' <<<"${recovery_step}"; then
  echo '[ARQUITETURA] recuperação não pode publicar código, trocar imagem ou limpar o host.' >&2
  exit 1
fi

grep -Fq 'com.docker.compose.project=${compose_project}' "${recovery_script}" || {
  echo '[ARQUITETURA] recuperador deve resolver o projeto Compose canônico por label.' >&2
  exit 1
}
grep -Fq 'com.docker.compose.service=${service}' "${recovery_script}" || {
  echo '[ARQUITETURA] recuperador deve resolver somente o serviço proxy por label.' >&2
  exit 1
}
grep -Fq 'docker update --restart=always' "${recovery_script}" || {
  echo '[ARQUITETURA] recuperador deve corrigir a política efetiva de reinício.' >&2
  exit 1
}
grep -Fq 'compose_project="lead-portal-payments-service"' "${recovery_script}" || {
  echo '[ARQUITETURA] projeto Compose produtivo deve permanecer fixo no recuperador.' >&2
  exit 1
}
grep -Fq 'compose_file="/root/lead-portal-payments-service/docker-compose.deploy.yml"' "${recovery_script}" || {
  echo '[ARQUITETURA] arquivo Compose produtivo deve permanecer fixo no recuperador.' >&2
  exit 1
}
if grep -Fq 'PUBLIC_PROXY_RECOVERY_TEST_MODE' "${workflow}"; then
  echo '[ARQUITETURA] workflow produtivo não pode habilitar sobrescritas de homologação.' >&2
  exit 1
fi
grep -Fq -- 'up -d --no-deps --no-build "${service}"' "${recovery_script}" || {
  echo '[ARQUITETURA] recriação deve usar somente a imagem local já publicada.' >&2
  exit 1
}
grep -Fq '.github/workflows/recover-public-proxy.yml' "${ci_workflow}" || {
  echo '[ARQUITETURA] mudança no workflow de recuperação deve disparar o CI proprietário.' >&2
  exit 1
}
grep -Fq './scripts/test-public-proxy-recovery-contract.sh' "${ci_workflow}" || {
  echo '[ARQUITETURA] CI deve validar o contrato da recuperação.' >&2
  exit 1
}
grep -Fq './scripts/test-public-proxy-recovery-e2e.sh' "${ci_workflow}" || {
  echo '[ARQUITETURA] CI deve homologar a recuperação Docker ponta a ponta.' >&2
  exit 1
}

echo 'Contrato da recuperação controlada do proxy público validado.'
