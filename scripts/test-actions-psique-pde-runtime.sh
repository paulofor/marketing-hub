#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"
: "${ACTIONS_TEST_COMPOSE_PROJECT:?Informe o projeto exclusivo da sandbox}"
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash scripts/run-docker-homologation.sh bash "$0" "${1:-pde-v7}"
fi
component="${1:?Informe o componente da imagem temporária}"

image_prefix="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION}"
runtime_container="${ACTIONS_TEST_COMPOSE_PROJECT}-pde-diagnostic"
runtime_created=false

cleanup() {
  if [[ "$runtime_created" == true ]]; then
    docker rm -f "$runtime_container" >/dev/null
  fi
}
trap cleanup EXIT

case "$component" in
  psique)
    node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
    bash scripts/docker-build-temporary-image.sh psique customer-agent-worker
    ;;
  dedalo) bash scripts/docker-build-temporary-image.sh dedalo landing-generator-agent-worker ;;
  apolo) bash scripts/docker-build-temporary-image.sh apolo -f video-management-service/Dockerfile . ;;
  pde-v7) bash scripts/docker-build-temporary-image.sh pde-v7 pde-platform/frontend ;;
  *) echo "Componente inválido: $component" >&2; exit 2 ;;
esac

if [[ "$component" != pde-v7 ]]; then
  docker run --rm --network none \
    --label "com.docker.compose.project=${ACTIONS_TEST_COMPOSE_PROJECT}" \
    --entrypoint codex "$image_prefix/$component:latest" \
    --config 'model_reasoning_effort="max"' exec --help >/dev/null
fi

if [[ "$component" == psique || "$component" == dedalo ]]; then
  docker run --rm --network none \
    --label "com.docker.compose.project=${ACTIONS_TEST_COMPOSE_PROJECT}" \
    --entrypoint node "$image_prefix/$component:latest" --input-type=module -e \
    'import { chromium } from "playwright-core";
     const browser = await chromium.launch({headless: true, args: ["--no-sandbox"]});
     console.log(await browser.version()); await browser.close();'
fi

if [[ "$component" == psique ]]; then
docker run --rm --init --read-only --interactive --network none \
  --label "com.docker.compose.project=${ACTIONS_TEST_COMPOSE_PROJECT}" \
  --tmpfs /tmp:size=256m,noexec,nosuid --security-opt no-new-privileges:true \
  --env CUSTOMER_AGENT_BPM_VISUAL_SCRIPT=/app/browser/bpm-visual-evidence.mjs \
  --entrypoint node "$image_prefix/psique:latest" --input-type=module \
  < customer-agent-worker/src/test/js/bpm-visual-evidence.test.mjs
fi

if [[ "$component" == pde-v7 ]]; then
docker run --detach --name "$runtime_container" --network none \
  --label "com.docker.compose.project=${ACTIONS_TEST_COMPOSE_PROJECT}" \
  --env PDE_BACKEND_UPSTREAM=127.0.0.1:9 \
  --env PDE_FRONTEND_VERSION=v7 \
  --env VITE_PDE_PRODUCT_SLUG=metodo-musa-7-dias \
  --env VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE=musa-pde-entry-v7-espelho-antes-de-sair \
  --env PDE_FRONTEND_IMAGE="$image_prefix/pde-v7:latest" \
  --env PDE_DEPLOY_IMAGE_TAG=test-only \
  --env PDE_DEPLOY_COMMIT_SHA=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
  "$image_prefix/pde-v7:latest" >/dev/null
runtime_created=true

diagnostic=""
for attempt in {1..30}; do
  if diagnostic="$(docker exec "$runtime_container" wget -qO- http://127.0.0.1/version-diagnostics.json)"; then
    break
  fi
  printf 'Aguardando diagnóstico PDE: tentativa %s/30.\n' "$attempt" >&2
  sleep 0.2
done
printf '%s' "$diagnostic" | python3 -c '
import json,sys
diagnostic=json.load(sys.stdin)
assert diagnostic["status"] == "UP"
assert diagnostic["version"] == "v7"
assert diagnostic["productSlug"] == "metodo-musa-7-dias"
assert diagnostic["experienceVersion"] == "musa-pde-entry-v7-espelho-antes-de-sair"
assert diagnostic["commitSha"] == "a" * 40
assert "slot" not in diagnostic
print("Diagnóstico canônico aprovado na imagem PDE real.")
'
fi
printf 'Runtime %s aprovado sem chamada ao modelo.\n' "$component"
