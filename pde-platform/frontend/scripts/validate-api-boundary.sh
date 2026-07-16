#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if grep -RInE \
  --include='*.ts' \
  --include='*.tsx' \
  --include='*.js' \
  --include='*.jsx' \
  --include='*.html' \
  --include='*.css' \
  --include='*.json' \
  --include='*.conf' \
  --include='Dockerfile' \
  --exclude-dir='node_modules' \
  --exclude-dir='dist' \
  '(ads-service|191\.252\.181\.168|localhost:8000|:8000\b)' \
  src public index.html vite.config.ts nginx.conf Dockerfile package.json package-lock.json; then
  echo "Erro: PDE frontend nao pode consumir diretamente o backend principal ads-service." >&2
  echo "Use somente o PDE backend, preferencialmente por /api/pde/... via pde-platform-backend." >&2
  exit 1
fi

echo "OK: PDE frontend respeita a fronteira de API do PDE backend."
