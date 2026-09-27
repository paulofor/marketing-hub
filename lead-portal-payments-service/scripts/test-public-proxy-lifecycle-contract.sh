#!/usr/bin/env bash
set -euo pipefail

module_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workflow="${module_dir}/../.github/workflows/lead-portal-payments-ci.yml"

proxy_block() {
  local compose_file="$1"
  awk '
    /^  proxy:$/ { in_proxy = 1; next }
    in_proxy && /^  [[:alnum:]_-]+:$/ { exit }
    in_proxy { print }
  ' "${compose_file}"
}

for compose_file in \
  "${module_dir}/docker-compose.yml" \
  "${module_dir}/docker-compose.deploy.yml"; do
  block="$(proxy_block "${compose_file}")"
  grep -Fq 'restart: always' <<<"${block}" || {
    echo "[ARQUITETURA] proxy público deve usar restart: always em ${compose_file}" >&2
    exit 1
  }
  grep -Fq 'nginx -t -q' <<<"${block}" || {
    echo "[ARQUITETURA] proxy público deve declarar healthcheck do Nginx em ${compose_file}" >&2
    exit 1
  }
done

grep -Fq -- '--remove-orphans --wait --wait-timeout 120' "${workflow}" || {
  echo '[ARQUITETURA] deploy do proxy deve aguardar a saúde dos containers' >&2
  exit 1
}
grep -Fq 'Validate public HTTPS proxy' "${workflow}" || {
  echo '[ARQUITETURA] deploy deve validar a rota HTTPS pública depois da publicação' >&2
  exit 1
}
grep -Fq 'label=com.docker.compose.service=proxy' "${workflow}" || {
  echo '[ARQUITETURA] deploy deve localizar o proxy pela identidade do Compose' >&2
  exit 1
}
restart_policy_check="$(grep -F 'HostConfig.RestartPolicy.Name' "${workflow}" || true)"
if [[ "${restart_policy_check}" != *"= 'always'"* ]]; then
  echo '[ARQUITETURA] deploy deve confirmar a política de reinício efetiva do proxy' >&2
  exit 1
fi
grep -Fq 'https://kit-whatsapp-pronto.digicomdigital.com.br' "${workflow}" || {
  echo '[ARQUITETURA] deploy deve validar o destino público do Kit WhatsApp' >&2
  exit 1
}
for commercial_mira_contract in \
  'issue_mira_certificate' \
  'mira.digicomdigital.com.br' \
  'TLS público de Mira confirmado; a saúde funcional será exigida pelo deploy proprietário do PDE.'; do
  grep -Fq "${commercial_mira_contract}" "${workflow}" || {
    echo "[ARQUITETURA] deploy do proxy deve preparar o TLS comercial de Mira: ${commercial_mira_contract}" >&2
    exit 1
  }
done
for mira_contract in \
  'https://v7.clubemusa.com.br/mira-private/version-diagnostics.json' \
  'pde-platform-frontend-mira' \
  'pde-planejado-36'; do
  grep -Fq "${mira_contract}" "${workflow}" || {
    echo "[ARQUITETURA] deploy do proxy não valida o isolamento de Mira: ${mira_contract}" >&2
    exit 1
  }
done

for proxy_contract in \
  'mira.digicomdigital.com.br pde-platform-frontend-mira-commercial:80' \
  '/etc/nginx/certs/live/mira.digicomdigital.com.br/fullchain.pem'; do
  grep -Fq "${proxy_contract}" "${module_dir}/nginx.conf" || {
    echo "[ARQUITETURA] proxy público sem contrato comercial de Mira: ${proxy_contract}" >&2
    exit 1
  }
done

echo 'Contrato de ciclo de vida do proxy público validado.'
