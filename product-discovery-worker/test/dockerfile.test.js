import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const dockerfile = readFileSync(
  new URL("../Dockerfile", import.meta.url),
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
