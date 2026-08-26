import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const scriptUrl = new URL("./test-musa-local-integration.sh", import.meta.url);
const retentionScriptUrl = new URL(
  "../local-validation/test-retention-mysql57.sh",
  import.meta.url,
);

test("a homologacao integrada combina o Compose base com a sobreposicao local", async () => {
  const script = await readFile(scriptUrl, "utf8");

  assert.match(script, /BASE_COMPOSE_FILE=.*docker-compose\.yml/);
  assert.match(
    script,
    /VALIDATION_COMPOSE_FILE=.*docker-compose\.local-validation\.yml/,
  );
  assert.match(
    script,
    /docker compose[\s\S]+-f "\$\{BASE_COMPOSE_FILE\}"[\s\S]+-f "\$\{VALIDATION_COMPOSE_FILE\}"/,
  );
  assert.match(script, /--profile local-e2e/);
  assert.match(script, /compose config --quiet/);
});

test("a homologacao executa toda a jornada dentro da rede Compose isolada", async () => {
  const script = await readFile(scriptUrl, "utf8");

  assert.match(script, /compose up -d --build --wait/);
  assert.match(script, /pde-contract-server/);
  assert.match(script, /pde-platform-backend/);
  assert.match(script, /pde-platform-frontend/);
  assert.match(script, /compose build pde-playwright-validation/);
  assert.match(
    script,
    /compose run --rm --no-deps pde-playwright-validation/,
  );
  assert.doesNotMatch(script, /npm run test:local-integration/);
  assert.doesNotMatch(script, /jdbc:mysql:\/\/127\.0\.0\.1/);
});

test("a retencao usa a mesma topologia local-e2e do backend PDE", async () => {
  const script = await readFile(retentionScriptUrl, "utf8");

  assert.match(script, /docker-compose\.yml/);
  assert.match(script, /docker-compose\.local-validation\.yml/);
  assert.match(script, /--profile local-e2e/);
});
