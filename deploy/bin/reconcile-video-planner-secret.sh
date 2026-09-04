#!/usr/bin/env bash
set -euo pipefail

SECRET_FILE="${OPENAI_API_KEY_HOST_FILE:-/root/infra/openai-token/openai_api_key}"
SECRET_DIRECTORY="$(dirname "${SECRET_FILE}")"
LEGACY_DIRECTORY="${SECRET_FILE}.legacy-directory"
TEMPORARY_FILE="${SECRET_FILE}.tmp.$$"

cleanup() {
  rm -f "${TEMPORARY_FILE}"
}
trap cleanup EXIT

install -d -m 700 "${SECRET_DIRECTORY}"

if [[ -d "${SECRET_FILE}" ]]; then
  if [[ -e "${LEGACY_DIRECTORY}" ]]; then
    echo "Erro: o backup recuperável ${LEGACY_DIRECTORY} já existe; o diretório inválido não será sobrescrito." >&2
    exit 1
  fi
  mv "${SECRET_FILE}" "${LEGACY_DIRECTORY}"
fi

umask 077
dd of="${TEMPORARY_FILE}" status=none
if [[ ! -s "${TEMPORARY_FILE}" ]]; then
  echo "Erro: a credencial recebida para o planejador está vazia." >&2
  exit 1
fi
chmod 600 "${TEMPORARY_FILE}"
mv "${TEMPORARY_FILE}" "${SECRET_FILE}"
trap - EXIT
