#!/usr/bin/env bash
set -euo pipefail

frontend_workflow=".github/workflows/frontend.yml"
deploy_workflow=".github/workflows/deploy-containers.yml"
frontend_dockerfile="frontend/Dockerfile"

if grep -Eq '^  push:' "${frontend_workflow}"; then
  echo "Frontend CI não deve repetir no push a validação já executada pelo deploy de main." >&2
  exit 1
fi

if grep -Eq '^  build-debug:' "${frontend_workflow}"; then
  echo "O build-debug duplicado e sem asserção funcional não deve existir." >&2
  exit 1
fi

for required_command in 'npm ci' 'npm run typecheck' 'npm run build'; do
  if ! grep -Fq "${required_command}" "${frontend_workflow}"; then
    echo "Frontend CI perdeu a validação obrigatória: ${required_command}." >&2
    exit 1
  fi
done

if ! grep -Fq 'COPY frontend/dist /usr/share/nginx/html' "${frontend_dockerfile}"; then
  echo "A imagem deve reutilizar exatamente o bundle validado pelo workflow." >&2
  exit 1
fi

if [[ "$(grep -Fc 'npm run build' "${deploy_workflow}")" -ne 1 ]]; then
  echo "O deploy deve gerar o bundle frontend exatamente uma vez por commit." >&2
  exit 1
fi

if ! grep -Fq 'head_sha=${GITHUB_SHA}' "${deploy_workflow}" \
  || ! grep -Fq "steps.approved-build.outputs.reused != 'true'" "${deploy_workflow}"; then
  echo "O deploy deve reutilizar somente o bundle aprovado do commit exato e manter fallback local." >&2
  exit 1
fi

echo "Contrato de eficiência do workflow frontend validado."
