#!/usr/bin/env bash
set -euo pipefail

proof_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "${proof_dir}/../../.." && pwd)"
compose_project="${PDE_LOCAL_COMPOSE_PROJECT:?PDE_LOCAL_COMPOSE_PROJECT is required}"
if [[ -z "${AIHUB_HOMOLOGATION_SESSION:-}" ]]; then
  exec bash "${repo_dir}/scripts/run-docker-homologation.sh" bash "$0"
fi
compose() {
  docker compose -p "${compose_project}" -f "${proof_dir}/compose.yml" "$@"
}
cleanup() {
  compose down --volumes --remove-orphans
}
trap cleanup EXIT

bash "${repo_dir}/scripts/docker-build-temporary-image.sh" alcyone-proof-backend \
  -f "${repo_dir}/pde-platform/backend/Dockerfile" "${repo_dir}/pde-platform/backend"
bash "${repo_dir}/scripts/docker-build-temporary-image.sh" alcyone-proof-frontend \
  -f "${repo_dir}/pde-platform/frontend/Dockerfile.alcyone" "${repo_dir}/pde-platform/frontend"
compose up -d --wait --wait-timeout 180

output_dir="${ALCYONE_PROOF_OUTPUT_DIR:-${repo_dir}/artifacts/alcyone-creative-proof}"
mkdir -p "${output_dir}"
node - "${output_dir}/input.json" <<'JS'
const fs = require("node:fs");
const dockerHost = process.env.DOCKER_HOST?.startsWith("tcp://")
  ? new URL(process.env.DOCKER_HOST).hostname : "127.0.0.1";
fs.writeFileSync(process.argv[2], JSON.stringify({
  mode: "TECHNICAL", scenarioCode: "", captureSessionId: "alcyone-local-functional-proof",
  sourceUrl: "http://" + dockerHost + ":58184",
  sourceReference: process.env.ALCYONE_PROOF_SOURCE_REFERENCE || "product:11@agent-validation-v1",
  productId: 11, productSlug: "pde-planejado-46", prototypeVersion: "alcyone-private-v3"
}));
JS
PDE_INTERNAL_API_TOKEN=alcyone-local-proof-token node \
  "${repo_dir}/customer-agent-worker/src/main/resources/browser/alcyone-agent-validation-harness.mjs" \
  "${output_dir}/input.json" "${output_dir}/report.json" "${output_dir}/screenshots"
node - "${output_dir}/report.json" <<'JS'
const assert = require("node:assert/strict"), fs = require("node:fs");
const r = JSON.parse(fs.readFileSync(process.argv[2]));
assert.equal(r.decision, "APPROVED");
assert.equal(r.scenarios.length, 9);
assert.equal(r.artifacts.length, 15);
assert.equal(r.providerCalls, 0);
assert.ok(Object.values(r.checks).every(Boolean));
for (const device of ["DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7"]) {
  const s = r.scenarios.find(s => s.scenarioCode === "ADHERENT" && s.deviceProfile === device);
  assert.deepEqual(s.screenshotEvidenceKeys, ["BRIEFING", "CONTENT", "FULL_PAGE"].map(m => "ADHERENT-" + device + "-" + m));
}
console.log("Percurso local aprovado: nove cenários, quinze capturas, nenhum provedor.");
JS
