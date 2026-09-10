import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const root = new URL("../", import.meta.url);
const modules = ["product-discovery-worker", "customer-agent-worker",
  "experiment-strategist-worker", "meta-ad-approver-worker"];
const read = (file) => readFileSync(new URL(file, root), "utf8");

/** Confere a mesma revisão do navegador nos testes, pacote e imagem final de cada agente. */
function validate({ module, manifest, lock, dockerfile, workflow }) {
  const message = (detail) => `[ARQUITETURA] ${module}: ${detail}`;
  const version = manifest.dependencies["playwright-core"];
  assert.match(version, /^\d+\.\d+\.\d+$/, message("Playwright deve ter versão exata"));
  assert.equal(lock.packages["node_modules/playwright-core"].version, version,
    message("lock divergente do pacote"));
  assert.equal(lock.packages[""].dependencies["playwright-core"], version,
    message("raiz do lock divergente do pacote"));
  const image = dockerfile.match(/^FROM (mcr\.microsoft\.com\/playwright:v([\d.]+)-noble@sha256:[a-f0-9]{64})$/m);
  assert.ok(image, message("base Playwright precisa de versão e digest"));
  assert.equal(image[2], version, message("browser da imagem incompatível com o pacote"));
  const canonicalBase = read("product-discovery-worker/Dockerfile")
    .match(/^FROM (mcr\.microsoft\.com\/playwright:[^\s]+)$/m)?.[1];
  assert.equal(image[1], canonicalBase, message("agentes devem compartilhar a mesma base por digest"));
  assert.ok(read("scripts/fixtures/agent-browser-ci/compose.yml").includes(image[1]),
    message("matriz local deve usar a mesma imagem do CI"));
  assert.doesNotMatch(dockerfile, /\bapt(?:-get)?\b|playwright(?:-core)? install|chmod -R.*ms-playwright/,
    message("runtime não deve reinstalar browser ou bibliotecas"));
  for (const install of dockerfile.replace(/\\\r?\n/g, " ").matchAll(/^RUN .*npm (?:install -g|ci).*$/gm)) {
    assert.match(install[0], /npm cache clean --force/,
      message("cache npm deve ser removido na mesma camada da instalação"));
  }
  const testJob = workflow.split(/^  source-run:/m)[0];
  assert.ok(testJob.includes(image[1]), message("CI deve usar a mesma imagem da aplicação"));
  const browserCommand = testJob.match(/docker run --rm --init --network none[\s\S]*?npm test/)?.[0];
  assert.ok(browserCommand?.includes(image[1]), message("suíte precisa executar no browser versionado"));
  assert.ok(browserCommand.includes(`/` + module + `"`), message("diretório da suíte deve ser explícito"));
  assert.match(browserCommand, /--user "\$\(id -u\):\$\(id -g\)"/, message("preservar dono do checkout"));
  assert.doesNotMatch(testJob, /playwright(?:-core)? install|continue-on-error:\s*true|npm test[^\n]*\|\|/,
    message("instalação dinâmica ou teste ignorado no CI"));
  assert.ok(testJob.indexOf("npm test") < testJob.indexOf("docker build"),
    message("imagem publicável só deve ser construída depois dos testes"));
}

const sourceFor = (module) => ({
  module,
  manifest: JSON.parse(read(`${module}/package.json`)),
  lock: JSON.parse(read(`${module}/package-lock.json`)),
  dockerfile: read(`${module}/Dockerfile`),
  workflow: read(`.github/workflows/${module}-ci.yml`),
});

for (const module of modules) {
  test(`${module}: browser compatível e testes obrigatórios sem APT`, () => validate(sourceFor(module)));
  for (const [name, mutate] of [
    ["versão diferente no runtime", (s) => { s.dockerfile = s.dockerfile.replace("v1.54.2-noble", "v1.49.0-noble"); }],
    ["digest ausente", (s) => { s.dockerfile = s.dockerfile.replace(/(FROM mcr\.microsoft\.com\/playwright:[^@\s]+)@sha256:[a-f0-9]{64}/, "$1"); }],
    ["base divergente entre agentes", (s) => { s.dockerfile = s.dockerfile.replace(/(FROM mcr\.microsoft\.com\/playwright:[^@\s]+)@sha256:[a-f0-9]{64}/, `$1@sha256:${"a".repeat(64)}`); }],
    ["cache empacotado", (s) => { s.dockerfile = s.dockerfile.replaceAll("npm cache clean --force", "npm --version"); }],
    ["imagem diferente no CI", (s) => { s.workflow = s.workflow.replace("v1.54.2-noble", "v1.49.0-noble"); }],
    ["lock incompatível", (s) => { s.lock.packages["node_modules/playwright-core"].version = "1.49.0"; }],
    ["instalação transitória no runtime", (s) => { s.dockerfile += "\nRUN npx playwright-core install --with-deps chromium\n"; }],
    ["instalação transitória no CI", (s) => { s.workflow = s.workflow.replace("npm test", "npx playwright-core install --with-deps chromium && npm test"); }],
    ["falha ignorada", (s) => { s.workflow = s.workflow.replace("npm test", "npm test || true"); }],
    ["teste ausente", (s) => { s.workflow = s.workflow.replace("npm test", "npm --version"); }],
  ]) {
    test(`${module}: bloqueia ${name}`, () => {
      const source = sourceFor(module);
      mutate(source);
      assert.throws(() => validate(source), /\[ARQUITETURA\]/);
    });
  }
}

test("contrato central acompanha os quatro módulos e executa esta prevenção", () => {
  const workflow = read(".github/workflows/github-actions-contracts.yml");
  for (const file of modules.flatMap((module) => [
    `${module}/Dockerfile`, `${module}/package.json`, `${module}/package-lock.json`,
  ]).concat("scripts/test-agent-browser-version-contract.mjs")) {
    assert.equal(workflow.split(`- "${file}"`).length - 1, 2,
      `[ARQUITETURA] ${file} deve disparar o contrato no push e no PR`);
  }
  assert.match(workflow, /run: node --test scripts\/test-agent-browser-version-contract\.mjs/);
});
