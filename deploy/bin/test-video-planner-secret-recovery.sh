#!/usr/bin/env bash
set -euo pipefail

TEST_DIRECTORY="$(mktemp -d)"
trap 'rm -rf "${TEST_DIRECTORY}"' EXIT

SECRET_FILE="${TEST_DIRECTORY}/openai-token/openai_api_key"
mkdir -p "${SECRET_FILE}"
printf 'conteudo legado\n' > "${SECRET_FILE}/preservado.txt"

printf 'nova-credencial' \
  | OPENAI_API_KEY_HOST_FILE="${SECRET_FILE}" bash "$(dirname "${BASH_SOURCE[0]}")/reconcile-video-planner-secret.sh"

[[ -f "${SECRET_FILE}" ]]
[[ "$(<"${SECRET_FILE}")" == "nova-credencial" ]]
[[ "$(stat -c '%a' "${SECRET_FILE}")" == "600" ]]
[[ -f "${SECRET_FILE}.legacy-directory/preservado.txt" ]]

printf 'credencial-renovada' \
  | OPENAI_API_KEY_HOST_FILE="${SECRET_FILE}" bash "$(dirname "${BASH_SOURCE[0]}")/reconcile-video-planner-secret.sh"

[[ "$(<"${SECRET_FILE}")" == "credencial-renovada" ]]
[[ -f "${SECRET_FILE}.legacy-directory/preservado.txt" ]]

if printf '' \
  | OPENAI_API_KEY_HOST_FILE="${SECRET_FILE}" bash "$(dirname "${BASH_SOURCE[0]}")/reconcile-video-planner-secret.sh"; then
  echo "[CONTRATO] Credencial vazia não pode substituir o arquivo válido." >&2
  exit 1
fi

[[ "$(<"${SECRET_FILE}")" == "credencial-renovada" ]]
printf 'Recuperação atômica da credencial do planejador validada.\n'
