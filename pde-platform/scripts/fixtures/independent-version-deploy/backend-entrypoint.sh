#!/bin/sh
set -eu

root=/usr/share/nginx/html
mkdir -p "${root}/actuator" "${root}/api/pde/products"
printf '{"status":"UP"}\n' > "${root}/actuator/health"
printf '{"commitSha":"%s"}\n' "${PDE_DEPLOY_COMMIT_SHA}" > "${root}/api/pde/build-identity"
printf '{"slug":"metodo-musa-7-dias","status":"UP"}\n' \
  > "${root}/api/pde/products/metodo-musa-7-dias"
