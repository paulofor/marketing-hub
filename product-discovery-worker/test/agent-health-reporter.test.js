import assert from "node:assert/strict";
import test from "node:test";
import { createAgentHealthReporter } from "../src/agent-health-reporter.js";

test("publica a prova canônica de prontidão de Argos", async () => {
  let request;
  const reporter = createAgentHealthReporter({
    backendBaseUrl: "http://backend/",
    deployedVersion: 3,
    buildReference: "sha-123",
    spawnSyncFn: () => ({ status: 0 }),
    fetchFn: async (url, options) => {
      request = { url, payload: JSON.parse(options.body) };
      return { ok: true, json: async () => ({ status: "READY" }) };
    },
  });
  const result = await reporter.report();
  assert.equal(result.status, "READY");
  assert.equal(request.url, "http://backend/api/internal/agents/executor-health");
  assert.equal(request.payload.agentKey, "market-radar");
  assert.equal(request.payload.codexAuthenticated, true);
  assert.equal(request.payload.deployedVersion, 3);
});

test("reporta sessão inválida sem interromper o worker", async () => {
  let payload;
  const reporter = createAgentHealthReporter({
    spawnSyncFn: () => ({ status: 1 }),
    fetchFn: async (_url, options) => {
      payload = JSON.parse(options.body);
      return { ok: true, json: async () => ({ status: "BLOCKED" }) };
    },
  });
  await reporter.report();
  assert.equal(payload.codexAuthenticated, false);
  assert.match(payload.detail, /Reconecte/);
});
