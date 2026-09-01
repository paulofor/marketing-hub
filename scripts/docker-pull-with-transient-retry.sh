#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 1 || -z "$1" ]]; then
  echo "Uso: $0 <imagem-docker>" >&2
  exit 2
fi

DOCKER_PULL_IMAGE="$1"
DOCKER_PULL_MAX_ATTEMPTS="${DOCKER_PULL_MAX_ATTEMPTS:-3}"
DOCKER_PULL_RETRY_DELAY_SECONDS="${DOCKER_PULL_RETRY_DELAY_SECONDS:-5}"

if ! [[ "${DOCKER_PULL_MAX_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "DOCKER_PULL_MAX_ATTEMPTS deve ser um inteiro positivo." >&2
  exit 2
fi

if ! [[ "${DOCKER_PULL_RETRY_DELAY_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "DOCKER_PULL_RETRY_DELAY_SECONDS deve ser um inteiro não negativo." >&2
  exit 2
fi

for ((attempt = 1; attempt <= DOCKER_PULL_MAX_ATTEMPTS; attempt++)); do
  if docker pull "${DOCKER_PULL_IMAGE}"; then
    exit 0
  fi

  if ((attempt == DOCKER_PULL_MAX_ATTEMPTS)); then
    echo "Falha definitiva ao baixar ${DOCKER_PULL_IMAGE} após ${attempt} tentativas." >&2
    exit 1
  fi

  echo "Falha transitória ao baixar ${DOCKER_PULL_IMAGE}; nova tentativa $((attempt + 1))/${DOCKER_PULL_MAX_ATTEMPTS}." >&2
  sleep "${DOCKER_PULL_RETRY_DELAY_SECONDS}"
done
