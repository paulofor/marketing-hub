import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const dockerfile = readFileSync(
  new URL("../Dockerfile", import.meta.url),
  "utf8",
);
const localCompose = readFileSync(
  new URL("../docker-compose.yml", import.meta.url),
  "utf8",
);
const deployCompose = readFileSync(
  new URL("../docker-compose.deploy.yml", import.meta.url),
  "utf8",
);
const workflow = readFileSync(
  new URL("../../.github/workflows/product-discovery-worker-ci.yml", import.meta.url),
  "utf8",
);

test("instala certificados raiz antes do cliente Codex", () => {
  const certificates = dockerfile.indexOf("ca-certificates");
  const codex = dockerfile.indexOf("npm install -g @openai/codex");

  assert.ok(
    certificates >= 0,
    "[ARQUITETURA] O runtime de Argos deve instalar certificados raiz para autenticar no Codex.",
  );
  assert.ok(
    certificates < codex,
    "[ARQUITETURA] Os certificados raiz devem ser preparados antes da instalação do cliente Codex.",
  );
});

test("reporta a versão corrente de Argos nas duas topologias", () => {
  for (const compose of [localCompose, deployCompose]) {
    assert.match(
      compose,
      /AGENT_HEALTH_VERSION: \$\{ARGOS_AGENT_VERSION:-4\}/,
      "[ARQUITETURA] Argos deve reportar por padrão a versão 4 cadastrada no backend.",
    );
  }
});

test("empacota a biblioteca factual e mantém o modelo ativo no deploy", () => {
  assert.match(
    dockerfile,
    /COPY research-library \.\/research-library/,
    "[ARQUITETURA] A imagem de Argos deve conter o índice versionado dos artigos de pesquisa.",
  );
  for (const compose of [localCompose, deployCompose]) {
    assert.match(
      compose,
      /ARGOS_CODEX_ENABLED: \$\{ARGOS_CODEX_ENABLED:-true\}/,
      "[ARQUITETURA] O planejamento e a síntese factual de Argos devem permanecer ativos por padrão.",
    );
    assert.match(
      compose,
      /PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS: \$\{PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS:-30\}/,
      "[ARQUITETURA] A coleta deve preservar a profundidade mínima homologada.",
    );
  }
  assert.match(workflow, /- "pesquisas\/\*\*"/);
  assert.match(workflow, /export ARGOS_CODEX_ENABLED='true'/);
});
