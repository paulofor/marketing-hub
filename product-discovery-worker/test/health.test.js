import test from "node:test";
import assert from "node:assert/strict";
import {
  createHealthPayload,
  createHealthState,
  markCycleCompleted,
  markCycleFailed,
  markPollCompleted,
  markPollFailed,
  markPollStarted,
  startHealthServer,
} from "../src/health.js";
import { resolveSearchConfig, SEARCH_PROVIDERS } from "../src/research.js";

test("createHealthPayload reports provider and Brave key status without leaking secret", () => {
  const state = createHealthState(new Date("2026-07-26T12:00:00.000Z"));
  const searchConfig = resolveSearchConfig({
    PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
    BRAVE_SEARCH_API_KEY: "brave-secret-value",
  });

  const payload = createHealthPayload({
    searchConfig,
    state,
    pollIntervalMs: 60000,
    maxSearchResults: 8,
    env: {
      PRODUCT_DISCOVERY_SEARCH_PROVIDER: "brave",
      BRAVE_SEARCH_API_KEY: "brave-secret-value",
    },
  });

  assert.equal(payload.status, "UP");
  assert.equal(payload.activeSearchProvider, SEARCH_PROVIDERS.BRAVE);
  assert.deepEqual(payload.braveSearch, {
    keyStatus: "CONFIGURED",
    keySource: "env",
  });
  assert.equal(JSON.stringify(payload).includes("brave-secret-value"), false);
});

test("createHealthPayload recognizes the sandbox Brave credential alias", () => {
  const payload = createHealthPayload({
    searchConfig: resolveSearchConfig({ BRAVE_API_KEY: "sandbox-secret" }),
    state: createHealthState(new Date("2026-08-22T04:00:00.000Z")),
    pollIntervalMs: 60000,
    maxSearchResults: 8,
    env: { BRAVE_API_KEY: "sandbox-secret" },
  });

  assert.equal(payload.activeSearchProvider, SEARCH_PROVIDERS.BRAVE);
  assert.deepEqual(payload.braveSearch, {
    keyStatus: "CONFIGURED",
    keySource: "env",
  });
  assert.equal(JSON.stringify(payload).includes("sandbox-secret"), false);
});

test("createHealthPayload keeps last processed cycle after completion and failure", () => {
  const state = createHealthState(new Date("2026-07-26T12:00:00.000Z"));
  markPollStarted(state, new Date("2026-07-26T12:01:00.000Z"));
  markCycleCompleted(
    state,
    {
      cycleId: 7,
      theme: "mulheres que compram roupa online",
      targetAudience: "mulheres 30+",
    },
    { opportunities: [{ name: "PDE MUSA" }] },
    new Date("2026-07-26T12:02:00.000Z"),
  );
  markPollCompleted(state, new Date("2026-07-26T12:03:00.000Z"));

  assert.equal(state.lastCycleProcessed.cycleId, 7);
  assert.equal(state.lastCycleProcessed.status, "COMPLETED");
  assert.equal(state.lastCycleProcessed.opportunitiesCount, 1);

  markCycleFailed(
    state,
    { cycleId: 8, theme: "moda prática" },
    new Error("Brave indisponível"),
    new Date("2026-07-26T12:04:00.000Z"),
  );
  markPollFailed(
    state,
    new Error("Falha no polling"),
    new Date("2026-07-26T12:05:00.000Z"),
  );

  const payload = createHealthPayload({
    searchConfig: resolveSearchConfig({}),
    state,
    pollIntervalMs: 60000,
    maxSearchResults: 8,
    env: {},
  });

  assert.equal(payload.status, "DEGRADED");
  assert.equal(payload.lastCycleProcessed.cycleId, 8);
  assert.equal(payload.lastCycleProcessed.status, "FAILED");
  assert.equal(payload.polling.lastPollError, "Falha no polling");
});

test("expõe o último desfecho público sem declarar anúncio como venda", () => {
  const state = createHealthState(new Date("2026-08-30T12:00:00.000Z"));
  markCycleCompleted(
    state,
    { cycleId: 144, theme: "guarda roupa cápsula" },
    {
      opportunities: [],
      evidenceReport: {
        metaCoverage: [
          {
            investigationId: 91,
            sourceStatus: "OBSERVED",
            collectionMode: "PUBLIC_BROWSER",
            adsObserved: 3,
            activeAds: 2,
            advertisersObserved: 2,
          },
        ],
      },
    },
    new Date("2026-08-30T12:02:00.000Z"),
  );

  const payload = createHealthPayload({
    searchConfig: resolveSearchConfig({}),
    state,
    pollIntervalMs: 60000,
    maxSearchResults: 30,
    env: { ARGOS_META_BROWSER_ENABLED: "true" },
  });

  assert.equal(payload.metaPublicBrowser.enabled, true);
  assert.equal(payload.metaPublicBrowser.engine, "chromium");
  assert.deepEqual(payload.metaPublicBrowser.lastCollection, {
    cycleId: 144,
    investigationId: 91,
    sourceStatus: "OBSERVED",
    collectionMode: "PUBLIC_BROWSER",
    adsObserved: 3,
    activeAds: 2,
    advertisersObserved: 2,
    processedAt: "2026-08-30T12:02:00.000Z",
  });
});

test("startHealthServer serves healthz JSON and not found responses", async () => {
  const state = createHealthState(new Date("2026-07-26T12:00:00.000Z"));
  const server = startHealthServer({
    host: "127.0.0.1",
    port: 0,
    searchConfig: resolveSearchConfig({
      PRODUCT_DISCOVERY_SEARCH_PROVIDER: "duckduckgo",
    }),
    state,
    pollIntervalMs: 60000,
    maxSearchResults: 8,
    logger: { info() {} },
    logLines: () => ["2026-08-11T12:00:00.000Z level=INFO cycle=42"],
    env: {},
  });

  await new Promise((resolve) => server.once("listening", resolve));
  const { port } = server.address();

  try {
    const healthResponse = await fetch(`http://127.0.0.1:${port}/healthz`);
    assert.equal(healthResponse.status, 200);
    const payload = await healthResponse.json();
    assert.equal(payload.service, "product-discovery-worker");
    assert.equal(payload.activeSearchProvider, SEARCH_PROVIDERS.DUCKDUCKGO);

    const logResponse = await fetch(
      `http://127.0.0.1:${port}/ops-product-discovery-observability-v1/logfile`,
    );
    assert.equal(logResponse.status, 200);
    assert.match(await logResponse.text(), /cycle=42/);

    const notFoundResponse = await fetch(`http://127.0.0.1:${port}/missing`);
    assert.equal(notFoundResponse.status, 404);
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => (error ? reject(error) : resolve()));
    });
  }
});
