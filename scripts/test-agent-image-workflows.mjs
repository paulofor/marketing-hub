import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const modules = ["agent-executor-admin-controller", "communication-agent-worker", "customer-agent-worker",
  "experiment-strategist-worker", "financial-agent-worker", "growth-operator-worker",
  "landing-generator-agent-worker", "meta-ad-approver-worker"];

function validate(workflow, module, startup = null) {
  const [before, deploy] = workflow.split(/^  deploy:\s*$/m);
  assert.ok(deploy, `${module}: deploy ausente`);
  const eventDriven = module !== "agent-executor-admin-controller";
  const reference = `marketing-hub/${module}:\${{ github.sha }}`;
  const deployedReference = eventDriven
    ? `marketing-hub/${module}:\${DEPLOY_SOURCE_SHA}`
    : reference;
  assert.match(before, /docker build[^\n]+/, module);
  const pack = before.match(/node scripts\/agent-image-bundle\.mjs pack [^\n]+/)?.[0];
  assert.ok(pack?.includes(reference), `${module}: pacote deve conter a imagem testada`);
  assert.match(before, /uses: actions\/upload-artifact@v4/, module);
  assert.match(before, /name: agent-images-\$\{\{ github.sha \}\}/, module);
  assert.match(
    before,
    new RegExp(`retention-days: ${eventDriven ? 7 : 1}\\s+compression-level: 0\\s+if-no-files-found: error`),
    module,
  );
  assert.match(deploy, /(?:^    needs: test(?:-build)?$|^      - test(?:-build)?$)/m, module);
  if (eventDriven) {
    assert.match(
      deploy,
      /uses: actions\/download-artifact@v4\s+with:\s+name: agent-images-\$\{\{ env\.DEPLOY_SOURCE_SHA \}\}[\s\S]+github-token: \$\{\{ github\.token \}\}[\s\S]+run-id:.*needs\.source-run\.outputs\.source_run_id/,
      module,
    );
    assert.match(before, /workflow_run:[\s\S]+workflows: \["Build & Deploy containers"\]/, module);
    assert.match(deploy, /ref: \$\{\{ env\.DEPLOY_SOURCE_SHA \}\}/, module);
  } else {
    assert.match(deploy, /uses: actions\/download-artifact@v4\s+with:\s+name: agent-images-\$\{\{ github.sha \}\}/, module);
  }
  const verification = deploy.match(/node scripts\/agent-image-bundle\.mjs verify [^\n]+/)?.[0];
  assert.ok(verification?.includes(deployedReference), module);
  const load = deploy.indexOf("node scripts/agent-image-bundle.mjs send");
  assert.ok(load > deploy.indexOf("< scripts/ensure-agent-vps-disk-space.sh"), `${module}: carga deve seguir gate inicial`);
  const guardedRuntime = module === "experiment-strategist-worker";
  const restart = guardedRuntime
    ? deploy.indexOf("-- bash scripts/start-validated-runtime.sh")
    : deploy.indexOf("docker compose up");
  assert.ok(load < restart, `${module}: carga deve preceder restart`);
  if (guardedRuntime) {
    assert.ok(startup, `${module}: comando versionado ausente`);
    assert.match(deploy, /with-agent-consumer-lock\.py[^\n]+--revision '\$\{DEPLOY_SOURCE_SHA\}' -- bash scripts\/start-validated-runtime\.sh/, module);
    assert.match(startup, /EXPERIMENT_STRATEGIST_IMAGE="marketing-hub\/experiment-strategist-worker:\$validated_revision"/, module);
    assert.doesNotMatch(startup, /docker (?:build|buildx|compose build)\b|\s--build\b/, `${module}: build no VPS proibido`);
  }
  assert.doesNotMatch(deploy, /docker (?:build|buildx|compose build)\b|\s--build\b/, `${module}: build no VPS proibido`);
  assert.match(guardedRuntime ? startup : deploy, /docker compose up -d --no-build --pull never --remove-orphans/, module);
  assert.match(deploy, /group: deploy-vps-163-245-202-80\s+queue: max\s+cancel-in-progress: false/, module);
  assert.equal((deploy.match(/bash -s -- retention/g) ?? []).length, 2, `${module}: retenção preventiva ausente`);
}

for (const module of modules) {
  test(`${module}: a imagem aprovada chega ao Compose sem recompilar`, () => {
    const startup = module === "experiment-strategist-worker"
      ? readFileSync(path.join(root, `${module}/scripts/start-validated-runtime.sh`), "utf8")
      : null;
    validate(readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8"), module, startup);
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

test("Atena preserva imagem imutável e lock ao extrair o comando versionado", () => {
  const module = "experiment-strategist-worker";
  const workflow = readFileSync(path.join(root, `.github/workflows/${module}-ci.yml`), "utf8");
  const startup = readFileSync(path.join(root, `${module}/scripts/start-validated-runtime.sh`), "utf8");
  assert.throws(() => validate(workflow.replace("with-agent-consumer-lock.py' --container", "unprotected.py' --container"), module, startup));
  assert.throws(() => validate(workflow, module, startup.replace("--no-build --pull never", "--build")));
  assert.throws(() => validate(workflow, module, startup.replace("marketing-hub/experiment-strategist-worker:$validated_revision", "marketing-hub/experiment-strategist-worker:latest")));
});

// Impede somar os picos de extração de duas imagens que podem ser carregadas em sequência.
function validateSequentialReviewImages(workflow) {
  const deploy = workflow.split(/^  deploy:\s*$/m)[1];
  const beforeSsh = deploy.split('Configure authenticated agent VPS SSH')[0];
  for (const [bundle, module] of [["reviewer", "meta-ad-approver-worker"], ["studio", "iris-image-studio"]]) {
    for (const operation of ["pack", "verify", "send"]) {
      const commands = workflow.split("\n").filter((line) => line.includes(`agent-image-bundle.mjs ${operation} `));
      const command = commands.find((line) => line.includes(`/agent-images/${bundle}"`));
      assert.ok(command?.includes(`marketing-hub/${module}:`), `[ARQUITETURA] ${bundle}: ${operation} separado e imutável obrigatório`);
      assert.equal((command.match(/marketing-hub\//g) ?? []).length, 1, `[ARQUITETURA] ${bundle}: carregar uma imagem por vez`);
    }
    assert.ok(beforeSsh.includes(`/agent-images/${bundle}"`), `[ARQUITETURA] ${bundle}: verificar ambos os pacotes antes de acessar o host`);
  }
  const restart = deploy.indexOf("docker compose up");
  const transfers = [...deploy.matchAll(/agent-image-bundle\.mjs send /g)];
  assert.equal(transfers.length, 3);
  assert.equal((deploy.match(/if \[ -f "\$\{\{ runner.temp \}\}\/agent-images\/manifest.json" \]; then/g) ?? []).length, 2,
    '[ARQUITETURA] continuação de revisão antiga preserva verificação e carga do pacote original');
  assert.ok(transfers.every((match) => match.index < restart), '[ARQUITETURA] ambas as cargas precedem o restart');
}

test("Íris verifica os dois artefatos antes do SSH e carrega um de cada vez antes do restart", () => {
  const source = readFileSync(path.join(root, ".github/workflows/meta-ad-approver-worker-ci.yml"), "utf8");
  validateSequentialReviewImages(source);
  for (const broken of [source.replaceAll("/agent-images/studio", "/agent-images/reviewer"),
    source.replace('agent-image-bundle.mjs verify "${{ runner.temp }}/agent-images/studio"', 'agent-image-bundle.mjs ignored "${{ runner.temp }}/agent-images/studio"'),
    source.replace('agent-image-bundle.mjs send "${{ runner.temp }}/agent-images/studio"', 'agent-image-bundle.mjs ignored "${{ runner.temp }}/agent-images/studio"')]) {
    assert.throws(() => validateSequentialReviewImages(broken));
  }
});

for (const scenario of ["sequential", "legacy", "missing-studio", "reviewer-failure", "studio-failure"]) {
  test(`comandos reais do workflow de Íris: ${scenario}`, (t) => {
    const directory = mkdtempSync(path.join(os.tmpdir(), "iris-transfer-"));
    t.after(() => rmSync(directory, { recursive: true, force: true }));
    const artifact = path.join(directory, "agent-images");
    const bin = path.join(directory, "bin");
    mkdirSync(bin);
    for (const part of scenario === "legacy" ? [""] : ["reviewer", "studio"]) {
      mkdirSync(path.join(artifact, part), { recursive: true });
      if (scenario !== "missing-studio" || part !== "studio") {
        writeFileSync(path.join(artifact, part, "manifest.json"), "{}");
      }
    }
    writeFileSync(path.join(bin, "node"), `#!${process.execPath}
const fs = require('node:fs');
const args = process.argv.slice(2);
fs.appendFileSync(process.env.TRANSFER_CALLS, JSON.stringify(args) + '\\n');
if (!fs.existsSync(args[2] + '/manifest.json')) process.exit(2);
if (args[1] === 'send' && args[2].endsWith('/' + process.env.FAIL_PART)) process.exit(3);
`, { mode: 0o700 });
    const workflow = readFileSync(path.join(root, ".github/workflows/meta-ad-approver-worker-ci.yml"), "utf8");
    const env = { ...process.env, PATH: `${bin}:${process.env.PATH}`, TRANSFER_CALLS: path.join(directory, "calls"),
      FAIL_PART: scenario.endsWith("-failure") ? scenario.split("-")[0] : "unused",
      DEPLOY_SOURCE_SHA: "a".repeat(40), VPS_USER: "root", VPS_IP: "fixture.local" };
    const runStep = (name) => {
      const step = workflow.split(`- name: ${name}\n`)[1].split(/\n      - /)[0];
      const command = step.split("        run: |\n")[1].split("\n").map((line) => line.slice(10)).join("\n")
        .replaceAll("${{ runner.temp }}", directory);
      return spawnSync("bash", ["-euo", "pipefail", "-c", command], { env, encoding: "utf8", timeout: 10000 });
    };
    const verified = runStep("Verify tested images");
    if (scenario === "missing-studio") assert.notEqual(verified.status, 0);
    else {
      assert.equal(verified.status, 0, verified.stderr);
      const sent = runStep("Load tested images without rebuilding");
      assert.equal(sent.status, scenario.endsWith("-failure") ? 3 : 0, sent.stderr);
    }
    const calls = readFileSync(env.TRANSFER_CALLS, "utf8").trim().split("\n").map(JSON.parse);
    const sends = calls.filter((call) => call[1] === "send");
    const verifications = calls.filter((call) => call[1] === "verify");
    assert.equal(verifications.length, scenario === "legacy" ? 1 : 2);
    assert.equal(sends.length, scenario === "missing-studio" ? 0 : ["legacy", "reviewer-failure"].includes(scenario) ? 1 : 2);
    assert.ok(calls.every((call) => call.filter((arg) => arg.startsWith("marketing-hub/")).every((ref) => ref.endsWith(env.DEPLOY_SOURCE_SHA))));
  });
}

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
    const revisionVariable = module === "agent-executor-admin-controller" ? "GITHUB_SHA" : "DEPLOY_SOURCE_SHA";
    assert.ok(workflow.includes(`${variable}=marketing-hub/${module}:\${${revisionVariable}}`));
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

test("contrato isolado de vídeo e MCP acompanha o modelo canônico e bloqueia divergência no PR", () => {
  const ci = readFileSync(path.join(root, ".github/workflows/github-actions-contracts.yml"), "utf8");
  const contract = readFileSync(path.join(root, "deploy/bin/test-isolated-compose-contract.sh"), "utf8");
  assert.ok(contract.includes("config/agents/codex-agent-health-compliance.json"));
  for (const variable of ["APOLLO_PLANNER_MODEL", "APOLLO_CODEX_MODEL", "VIDEO_REFERENCE_ANALYSIS_MODEL", "OPENAI_IMAGE_ORCHESTRATION_MODEL"]) {
    assert.ok(contract.includes(`${variable}: \${runtime_model}`), variable);
  }
  const pullRequest = ci.split("  pull_request:")[1]?.split("  workflow_dispatch:")[0];
  for (const source of ["deploy/docker-compose.video.yml", "deploy/docker-compose.mcp.yml", "deploy/bin/test-isolated-compose-contract.sh"]) {
    assert.ok(pullRequest?.includes(source), `${source}: gate deve executar antes do merge`);
  }
  assert.ok(ci.includes("run: bash deploy/bin/test-isolated-compose-contract.sh"));
});
