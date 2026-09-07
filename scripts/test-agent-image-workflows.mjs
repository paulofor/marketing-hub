import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const modules = ["agent-executor-admin-controller", "communication-agent-worker", "customer-agent-worker",
  "experiment-strategist-worker", "financial-agent-worker", "growth-operator-worker",
  "landing-generator-agent-worker", "meta-ad-approver-worker"];

function validate(workflow, module) {
  const [before, deploy] = workflow.split(/^  deploy:\s*$/m);
  assert.ok(deploy, `${module}: deploy ausente`);
  const reference = `marketing-hub/${module}:\${{ github.sha }}`;
  assert.match(before, /docker build[^\n]+/, module);
  const pack = before.match(/node scripts\/agent-image-bundle\.mjs pack [^\n]+/)?.[0];
  assert.ok(pack?.includes(reference), `${module}: pacote deve conter a imagem testada`);
  assert.match(before, /uses: actions\/upload-artifact@v4/, module);
  assert.match(before, /name: agent-images-\$\{\{ github.sha \}\}/, module);
  assert.match(before, /retention-days: 1\s+compression-level: 0\s+if-no-files-found: error/, module);
  assert.match(deploy, /needs: test(?:-build)?\s/, module);
  assert.match(deploy, /uses: actions\/download-artifact@v4\s+with:\s+name: agent-images-\$\{\{ github.sha \}\}/, module);
  assert.ok(deploy.includes(`node scripts/agent-image-bundle.mjs verify "\${{ runner.temp }}/agent-images" ${reference}`), module);
  const load = deploy.indexOf("node scripts/agent-image-bundle.mjs send");
  assert.ok(load > deploy.indexOf("< scripts/ensure-agent-vps-disk-space.sh"), `${module}: carga deve seguir gate inicial`);
  assert.ok(load < deploy.indexOf("docker compose up"), `${module}: carga deve preceder restart`);
  assert.doesNotMatch(deploy, /docker (?:build|buildx|compose build)\b|\s--build\b/, `${module}: build no VPS proibido`);
  assert.match(deploy, /docker compose up -d --no-build --pull never --remove-orphans/, module);
  assert.match(deploy, /group: deploy-vps-163-245-202-80\s+queue: max\s+cancel-in-progress: false/, module);
  assert.equal((deploy.match(/bash -s -- retention/g) ?? []).length, 2, `${module}: retenção preventiva ausente`);
}

for (const module of modules) {
  test(`${module}: a imagem aprovada chega ao Compose sem recompilar`, () => {
    validate(readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8"), module);
  });
}

test("regressões de build, revisão, ausência do pacote e ordem são bloqueadas", () => {
  const module = "customer-agent-worker";
  const source = readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8");
  for (const changed of [source.replace("--no-build --pull never", "--build"),
    source.replace("agent-image-bundle.mjs pack", "agent-image-bundle.mjs ignored"),
    source.replace(/name: agent-images-\$\{\{ github.sha \}\}/g, "name: agent-images-latest"),
    source.replace("agent-image-bundle.mjs send", "agent-image-bundle.mjs ignored")]) {
    assert.throws(() => validate(changed, module));
  }
});

test("imagens de Psique, Plutus e controlador são imutáveis e participam da retenção", () => {
  const disk = readFileSync(path.join(root, "scripts/ensure-agent-vps-disk-space.sh"), "utf8");
  for (const [module, location, variable] of [
    ["customer-agent-worker", "customer-agent-worker", "CUSTOMER_AGENT_IMAGE"],
    ["financial-agent-worker", "financial-agent-worker", "FINANCIAL_AGENT_IMAGE"],
    ["agent-executor-admin-controller", "deploy/agent-executor-admin-controller", "AGENT_EXECUTOR_ADMIN_IMAGE"],
  ]) {
    const compose = readFileSync(path.join(root, `${location}/docker-compose.yml`), "utf8");
    assert.ok(compose.includes(`image: \${${variable}:-marketing-hub/${module}:local}`));
    const workflow = readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8");
    assert.ok(workflow.includes(`${variable}=marketing-hub/${module}:\${GITHUB_SHA}`));
    assert.ok(disk.includes(`marketing-hub/${module}`));
  }
});

test("imagens oficiais da PDE participam somente da retenção segura por SHA", () => {
  const disk = readFileSync(path.join(root, "scripts/ensure-agent-vps-disk-space.sh"), "utf8");
  for (const repository of [
    "pde-platform-backend", "pde-platform-frontend-v5", "pde-platform-frontend-v6",
    "pde-platform-frontend-v7", "pde-platform-frontend-mira",
    "pde-platform-frontend-kit-whatsapp", "pde-ai-worker", "pde-retention-worker",
  ]) {
    assert.ok(disk.includes(repository), repository);
  }
  assert.match(disk, /image_tag.*\^\[0-9a-f\]\{40\}\$/s);
  assert.doesNotMatch(disk, /docker image prune --(?:all|force --all)|docker system prune/);
});

test("CI central acompanha os novos contratos e executa homologação real", () => {
  const ci = readFileSync(path.join(root, ".github/workflows/github-actions-contracts.yml"), "utf8");
  for (const file of ["agent-image-bundle.mjs", "test-agent-image-bundle.mjs", "test-agent-image-workflows.mjs", "test-agent-image-bundle-e2e.sh"]) {
    assert.ok(ci.split(`scripts/${file}`).length >= 4, file);
  }
});
