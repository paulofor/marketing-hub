import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import test from "node:test";

// Usa a configuração produzida pelo comando Java real; não herda o ambiente amplo dos testes.
test("o MCP isolado encontra o Chromium com a lista real de variáveis do Codex", () => {
  const allowed = JSON.parse(readFileSync("target/strategist-mcp-environment.json", "utf8"));
  const environment = Object.fromEntries(["PATH", "HOME", ...allowed]
    .filter(name => process.env[name] !== undefined)
    .map(name => [name, process.env[name]]));
  const result = spawnSync(process.execPath,
    ["--test", "src/test/mcp/public-research.test.mjs"],
    { env: environment, encoding: "utf8", timeout: 60000 });
  assert.equal(result.status, 0, result.stderr + result.stdout);
  assert.match(result.stdout, /# pass [1-9][0-9]*/);
  assert.match(result.stdout, /# fail 0/);
});
