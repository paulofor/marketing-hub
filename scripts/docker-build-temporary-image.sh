#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -lt 2 ]]; then
  echo "Uso: $0 <nome-da-imagem> <argumentos-do-docker-build>" >&2
  exit 2
fi

homologation_session="${AIHUB_HOMOLOGATION_SESSION:-}"
image_name="$1"
shift
image_tag="${AIHUB_HOMOLOGATION_IMAGE_TAG:-latest}"

if ! [[ "$homologation_session" =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]] || [[ ${#homologation_session} -gt 128 ]]; then
  echo "AIHUB_HOMOLOGATION_SESSION deve identificar uma sessão segura e ativa." >&2
  exit 2
fi

if ! [[ "$image_name" =~ ^[a-z0-9][a-z0-9_.-]{0,127}$ ]]; then
  echo "O nome da imagem temporária deve usar apenas letras minúsculas, números, ponto, hífen ou sublinhado." >&2
  exit 2
fi

if ! [[ "$image_tag" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]]; then
  echo "AIHUB_HOMOLOGATION_IMAGE_TAG possui formato inválido." >&2
  exit 2
fi

image_reference="aihub-homologation/${homologation_session}/${image_name}:${image_tag}"

docker build \
  --label "com.marketinghub.homologation.temporary=true" \
  --label "com.marketinghub.homologation.session=${homologation_session}" \
  --label "com.marketinghub.homologation.created-at=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --tag "$image_reference" \
  "$@"

printf 'Imagem temporária criada: %s\n' "$image_reference"
