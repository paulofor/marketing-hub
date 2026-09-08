import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const read = (file) => readFileSync(path.join(root, file), "utf8");

for (const [file, service, variable] of [
  ["landing-generator-agent-worker/docker-compose.yml", "landing-generator-agent-worker", "CODEX_REASONING_EFFORT"],
  ["deploy/docker-compose.video.yml", "video-management", "APOLLO_CODEX_REASONING_EFFORT"],
]) {
  test(`${service}: Compose resolve max sem herdar credenciais da sandbox`, () => {
    const result = spawnSync("docker", ["compose", "--env-file", "/dev/null", "-p",
      process.env.ACTIONS_TEST_COMPOSE_PROJECT || "actions-reasoning-contract",
      "-f", path.join(root, file), "config", "--format", "json"], {
      encoding: "utf8",
      timeout: 15_000,
      env: {
        PATH: process.env.PATH,
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
