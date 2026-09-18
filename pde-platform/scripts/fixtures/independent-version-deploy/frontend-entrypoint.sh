#!/bin/sh
set -eu

source_sha256="$(tr -d '\r\n' < /usr/share/nginx/html/frontend-source.sha256)"
cat > /usr/share/nginx/html/index.html <<EOF
<!doctype html><html lang="pt-BR"><body>${PDE_FRONTEND_VERSION_ID}</body></html>
EOF
cat > /usr/share/nginx/html/version-diagnostics.json <<EOF
{
  "status": "UP",
  "surface": "pde-platform-frontend",
  "version": "${PDE_FRONTEND_VERSION}",
  "imageVersionId": "${PDE_FRONTEND_VERSION_ID}",
  "publicUrl": "${PDE_FRONTEND_PUBLIC_URL}",
  "experienceVersion": "${VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE}",
  "productSlug": "metodo-musa-7-dias",
  "image": "${PDE_FRONTEND_IMAGE}",
  "imageTag": "${PDE_DEPLOY_IMAGE_TAG}",
  "commitSha": "${PDE_DEPLOY_COMMIT_SHA}",
  "frontendSourceSha256": "${source_sha256}",
  "deployedAt": "${PDE_DEPLOY_DEPLOYED_AT}"
}
EOF
