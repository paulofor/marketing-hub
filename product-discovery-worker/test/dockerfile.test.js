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
      /AGENT_HEALTH_VERSION: \$\{ARGOS_AGENT_VERSION:-3\}/,
      "[ARQUITETURA] Argos deve reportar por padrão a versão 3 cadastrada no backend.",
    );
  }
});
