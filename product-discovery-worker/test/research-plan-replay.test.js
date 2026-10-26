import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";
import { reusePersistedResearchPlan } from "../src/argos-codex.js";
import { processJob } from "../src/worker.js";

const plan = JSON.parse(
  await readFile(
    new URL("./fixtures/research-plan-replay.json", import.meta.url),
    "utf8",
  ),
);
function job(cycleId) {
  return {
    cycleId,
    stageCode: "research",
    executionLeaseId: `lease-${cycleId}`,
    researchMode: "VALIDATE_MARKET",
    marketType: "UNSPECIFIED",
    evidencePolicy: "PUBLIC_SOURCES_V1",
    theme: "Modelagem internacional",
    targetAudience: "Estilo pessoal",
    acquisitionChannel: "Instagram",
    researchPlanReplay: {
      cycleId,
      plan,
      rawResponse: "resposta original preservada",
      model: "gpt-6.1-sol",
    },
  };
}

for (const id of [73, 99173]) {
  test(`retoma pesquisa ${id} com plano real, sem planejamento nem repetição de custo`, async () => {
    const callbacks = [];
    let synthesisCalls = 0;
    let planningCalls = 0;
    await processJob(job(id), {
      backendBaseUrl: "http://backend.local",
      logger: { info() {}, error() {} },
      selectResearchLibraryContext: async () => ({
        evidence: [],
        coverage: [],
      }),
      planDirectedResearch: async () => {
        planningCalls++;
        throw new Error("Não pode planejar novamente");
      },
      searchInternet: async (input) => {
        assert.deepEqual(input.directedQueries, plan.publicQueries);
        return [
          {
            evidenceId: "P1",
            url: "https://www.whering.co/",
            title: "Referência pública sintética",
            snippet: "Fixture sem evidência comercial",
          },
        ];
      },
      collectMarketplaceEvidence: async () => ({
        marketplaceOffers: [],
        metaAdEvidence: [],
        metaCoverage: [
          { sourceStatus: "NO_OBSERVATIONS", publisherPlatform: "INSTAGRAM" },
        ],
      }),
      synthesizeMarketCandidates: async () => {
        synthesisCalls++;
        return {
          synthesis: {
            candidates: [],
            decisionSummary: "Evidência sintética insuficiente",
          },
          model: "gpt-6.1-sol",
          mode: "CODEX",
          usage: { inputTokens: 100, cachedInputTokens: 0, outputTokens: 20 },
          rawResponse: "síntese de teste",
          prompt: "síntese",
          agentPromptPart: "Argos",
          activityPromptPart: "Analisar evidências",
          reasoningEffort: "medium",
        };
      },
      analyzeSearchResults: () => ({
        decisionSummary: "Evidência sintética insuficiente",
        opportunities: [],
        evidenceReport: {},
      }),
      postJson: async (url, payload) => callbacks.push({ url, payload }),
      markCycleCompleted() {},
      markCycleFailed() {},
    });
    assert.equal(planningCalls, 0);
    assert.equal(synthesisCalls, 1);
    const replay = callbacks.find((c) => c.url.endsWith("/plan")).payload;
    assert.equal(replay.executionMode, "DETERMINISTIC");
    assert.equal(replay.inputTokens, undefined);
    assert.equal(replay.rawResponse, "resposta original preservada");
    assert.deepEqual(JSON.parse(replay.planJson), plan);
    const terminal = callbacks.filter(
      (c) => c.url.endsWith("/complete") || c.url.endsWith("/fail"),
    );
    assert.equal(terminal.length, 1);
    assert.ok(terminal[0].url.endsWith("/complete"), JSON.stringify(terminal));
    assert.equal(terminal[0].payload.executionLeaseId, `lease-${id}`);
    assert.equal(terminal[0].payload.analysisAudit.inputTokens, 100);
  });
}

test("plano de outro ciclo ou malformado não autoriza inferência substituta", () => {
  const input = job(73);
  input.researchPlanReplay.cycleId = 99173;
  assert.throws(() => reusePersistedResearchPlan(input), /mesma pesquisa/);
  input.researchPlanReplay.cycleId = 73;
  input.researchPlanReplay.plan = {};
  assert.throws(() => reusePersistedResearchPlan(input), /fora do contrato/);
});
