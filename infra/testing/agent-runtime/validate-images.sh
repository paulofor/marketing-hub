#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$repo_root"
runtime_contract="config/agents/codex-agent-health-compliance.json"
runtime_version="$(node -p "JSON.parse(require('fs').readFileSync('$runtime_contract')).codexVersion")"

node scripts/build-commercial-review-evidence.mjs . customer-agent-worker/review-evidence
node scripts/build-commercial-review-evidence.mjs . meta-ad-approver-worker/review-evidence
npm --prefix product-discovery-worker run build:research-library

while IFS= read -r module; do
  context="$module"
  if [[ "$module" == video-management-service ]]; then
    context=.
  fi
  bash scripts/docker-build-temporary-image.sh "$module" -f "$module/Dockerfile" "$context"
  reference="aihub-homologation/${AIHUB_HOMOLOGATION_SESSION:?Execute pelo wrapper de homologação}/$module:latest"
  actual="$(docker run --rm --network none --read-only \
    --security-opt no-new-privileges:true --tmpfs /tmp:size=64m,noexec,nosuid \
    --entrypoint codex "$reference" --version)"
  [[ "$actual" == "codex-cli $runtime_version" ]]
  docker run --rm --network none --read-only \
    --security-opt no-new-privileges:true --tmpfs /tmp:size=64m,noexec,nosuid \
    --entrypoint codex "$reference" debug models --bundled \
    | node -e 'let value=""; process.stdin.on("data", chunk => value+=chunk); process.stdin.on("end", () => {
      const catalog=JSON.parse(value); const models=Array.isArray(catalog)?catalog:catalog.models;
      if (!models.some(model=>model.slug==="gpt-6.1-sol")) process.exit(1);
    });'
  printf 'PASS: %s, %s, GPT 6.1 Sol no catálogo, execução confinada sem rede.\n' "$module" "$actual"
done < <(node -e 'const c=require("./config/agents/codex-agent-health-compliance.json"); for(const a of c.agents) console.log(a.module)')
