#!/usr/bin/env bash
set -euo pipefail

# Extrai a revisão imutável do healthz já publicado, sem depender de jq na VPS.
revision="$(sed -nE 's/^[[:space:]]*"commit"[[:space:]]*:[[:space:]]*"([0-9a-f]{40})"[[:space:]]*,?[[:space:]]*$/\1/p' | head -n 1)"

if [[ ! "${revision}" =~ ^[0-9a-f]{40}$ ]]; then
  printf '[ARQUITETURA] healthz do frontend não informou uma revisão Git válida.\n' >&2
  exit 1
fi

printf '%s\n' "${revision}"
