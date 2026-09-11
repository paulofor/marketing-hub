#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${VEGA_LOCAL_HOST:?Informe IP da sandbox acessível pela engine}"
: "${VEGA_TEST_DOCKER_HOST:?Informe endereço da engine isolada}"
: "${VEGA_COMPOSE_PROJECT:?Informe projeto exclusivo}"
compose=(docker compose -p "$VEGA_COMPOSE_PROJECT" -f infra/testing/vega-private-prototype/compose.yml -f infra/testing/vega-private-prototype/images.compose.yml)
node infra/testing/vega-private-prototype/prepare-images.mjs
"${compose[@]}" create --force-recreate proxy
"${compose[@]}" cp lead-portal-payments-service/nginx.conf proxy:/etc/nginx/conf.d/default.conf
"${compose[@]}" cp artifacts/vega380/proxy-certs proxy:/etc/nginx/certs
"${compose[@]}" up -d vega-frontend vega-worker admin-image
"${compose[@]}" start proxy
"${compose[@]}" exec -T proxy nginx -t
"${compose[@]}" run --rm -T backend-image
"${compose[@]}" run --rm -T --entrypoint sh image-harness -c 'set -e; cat > /evidence/input.json; node /app/browser/vega-agent-validation-harness.mjs /evidence/input.json /evidence/output.json /evidence/browser; tar -C /evidence -cf - .' < artifacts/vega380/image-harness/input.json | tar -xf - -C artifacts/vega380/image-harness
curl --retry 5 --retry-connrefused --retry-delay 1 --noproxy '*' --fail --silent --show-error --cacert artifacts/vega380/proxy-certs/test.crt --resolve "v7.clubemusa.com.br:18319:$VEGA_TEST_DOCKER_HOST" https://v7.clubemusa.com.br:18319/vega-private/version-diagnostics.json > artifacts/vega380/image-diagnostics.json
curl --noproxy '*' --fail --silent --show-error --cacert artifacts/vega380/proxy-certs/test.crt --resolve "v7.clubemusa.com.br:18319:$VEGA_TEST_DOCKER_HOST" https://v7.clubemusa.com.br:18319/api/pde/vega/private/v1/contract > artifacts/vega380/image-contract.json
curl --fail --silent --show-error "http://$VEGA_TEST_DOCKER_HOST:18320/healthz"
node - <<'JS'
const assert=require('node:assert/strict');const fs=require('fs');
const load=name=>JSON.parse(fs.readFileSync('artifacts/vega380/'+name,'utf8'));
assert.equal(load('image-harness/output.json').decision,'APPROVED');
assert.ok(load('image-harness/output.json').artifacts.every(a=>a.evidenceType==='FULL_PAGE' && a.foldNumber===null));
assert.equal(load('image-diagnostics.json').experienceVersion,load('image-contract.json').prototypeVersion);
assert.equal(load('image-contract.json').paymentEnabled,false);
console.log('IMAGENS, TLS, PROXY E CINCO CENÁRIOS APROVADOS');
JS
