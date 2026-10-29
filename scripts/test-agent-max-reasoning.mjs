import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const read = (file) => readFileSync(path.join(root, file), "utf8");

export const agents = [
  ["experiment-strategist-worker/docker-compose.yml", "experiment-strategist-worker", "EXPERIMENT_STRATEGIST_REASONING_EFFORT"],
  ["financial-agent-worker/docker-compose.yml", "financial-agent-worker", "CODEX_REASONING_EFFORT"],
  ["communication-agent-worker/docker-compose.yml", "communication-agent-worker", "CODEX_REASONING_EFFORT"],
  ["meta-ad-approver-worker/docker-compose.yml", "meta-ad-approver-worker", "CODEX_REASONING_EFFORT"],
  ["growth-operator-worker/docker-compose.yml", "growth-operator-worker", "CODEX_REASONING_EFFORT"],
  ["customer-agent-worker/docker-compose.yml", "customer-agent-worker", "CUSTOMER_AGENT_REASONING_EFFORT"],
  ["product-discovery-worker/docker-compose.yml", "product-discovery-worker", "ARGOS_CODEX_REASONING_EFFORT"],
  ["product-discovery-worker/docker-compose.deploy.yml", "product-discovery-worker", "ARGOS_CODEX_PLAN_REASONING_EFFORT"],
  ["product-discovery-worker/docker-compose.deploy.yml", "product-discovery-worker", "ARGOS_CODEX_SYNTHESIS_REASONING_EFFORT"],
  ["landing-generator-agent-worker/docker-compose.yml", "landing-generator-agent-worker", "CODEX_REASONING_EFFORT"],
  ["deploy/docker-compose.video.yml", "video-management", "APOLLO_CODEX_REASONING_EFFORT"],
  ["deploy/docker-compose.video.yml", "video-management", "VIDEO_REFERENCE_ANALYSIS_REASONING_EFFORT"],
];

for (const [file, service, variable] of agents) {
  test(`${service}: Compose resolve max sem herdar credenciais da sandbox`, () => {
    const result = spawnSync("docker", ["compose", "--env-file", "/dev/null", "-p",
      process.env.ACTIONS_TEST_COMPOSE_PROJECT || "actions-reasoning-contract",
      ...(file.endsWith("docker-compose.deploy.yml") ? ["-f", path.join(root, "product-discovery-worker/docker-compose.yml")] : []),
      "-f", path.join(root, file), "config", "--format", "json"], {
      encoding: "utf8",
      timeout: 15_000,
      env: {
        PATH: process.env.PATH,
        PRODUCT_DISCOVERY_WORKER_IMAGE: "marketing-hub/argos:local-test-only",
        PDE_INTERNAL_API_TOKEN: "local-test-only",
        ...Object.fromEntries(["EXPERIMENT_STRATEGIST", "FINANCIAL_AGENT", "COMMUNICATION_AGENT", "META_AD_APPROVER", "GROWTH_OPERATOR", "CUSTOMER_AGENT"].map(key => [`${key}_CODEX_HOME`, path.join(root, "tmp/codex-test-only")])),
        LANDING_GENERATOR_CODEX_HOME: path.join(root, "tmp/codex-test-only"),
        APOLLO_CODEX_HOME_HOST_DIR: path.join(root, "tmp/apolo-test-only"),
      },
    });
    assert.equal(result.status, 0, result.stderr);
    const rendered = JSON.parse(result.stdout);
    assert.equal(rendered.services[service].environment[variable], "max");
  });
}

test("Dédalo: deploy e configuração Spring preservam max explícito", () => {
  assert.match(read(".github/workflows/landing-generator-agent-worker-ci.yml"), /CODEX_REASONING_EFFORT=max/);
  assert.match(read("landing-generator-agent-worker/src/main/resources/application.yml"), /reasoning-effort: \$\{CODEX_REASONING_EFFORT:max\}/);
});

test("Apolo: Spring preserva max no replay sem ativá-lo ou alterar os demais providers", () => {
  const config = read("video-management-service/src/main/resources/application.yml");
  assert.match(config, /reasoning-effort: \$\{APOLLO_CODEX_REASONING_EFFORT:max\}/);
  assert.match(config, /enabled: \$\{APOLLO_CODEX_SHADOW_ENABLED:false\}/);
});

for (const [module, variable] of [
  ["experiment-strategist-worker", "EXPERIMENT_STRATEGIST_REASONING_EFFORT"],
  ["financial-agent-worker", "CODEX_REASONING_EFFORT"],
  ["communication-agent-worker", "CODEX_REASONING_EFFORT"],
  ["meta-ad-approver-worker", "CODEX_REASONING_EFFORT"],
  ["growth-operator-worker", "CODEX_REASONING_EFFORT"],
]) {
  test(`${module}: Spring e publicação preservam max explícito`, () => {
    assert.ok(read(`${module}/src/main/resources/application.yml`).includes(`reasoning-effort: ${"${"}${variable}:max}`));
    const publication = module === "experiment-strategist-worker" ? read(`${module}/scripts/start-validated-runtime.sh`) : read(`.github/workflows/${module}-ci.yml`);
    assert.ok(publication.includes(`${variable}=max`));
  });
}

test("Argos: publicador fixa max nas duas fases e no padrão", () => {
  const workflow = read(".github/workflows/product-discovery-worker-ci.yml");
  for (const variable of ["ARGOS_CODEX_REASONING_EFFORT", "ARGOS_CODEX_PLAN_REASONING_EFFORT", "ARGOS_CODEX_SYNTHESIS_REASONING_EFFORT"]) {
    assert.ok(workflow.includes(`${variable}="max"`), variable);
  }
});
