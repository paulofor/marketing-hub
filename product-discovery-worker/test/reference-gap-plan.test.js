import test from "node:test";
import assert from "node:assert/strict";
import { writeFile } from "node:fs/promises";
import {
  deterministicPlan,
  isReferenceGapResearch,
  validatePlan,
} from "../src/argos-codex.js";
import { processJob } from "../src/worker.js";

const input = (cycleId = 73) => ({
  cycleId,
  stageCode: "candidate-gap-deepening",
  evidencePolicy: "PUBLIC_SOURCES_V1",
  researchMode: "VALIDATE_MARKET",
  marketType: "UNSPECIFIED",
  executionLeaseId: `lease-${cycleId}`,
  referenceSources: "https://www.whering.co/\nhttps://www.myindyx.com/home/",
  previousCandidates: [
    { id: 69, name: "Encontrar combinações para peças paradas no armário" },
    {
      id: 68,
      name: "Reconhecer a própria identidade ao se vestir no cotidiano",
    },
  ],
  gapResearchPolicy: {
    maximumAttempts: 2,
    maximumPublicQueriesPerAttempt: 12,
    maximumModelInvocations: 4,
    estimatedSearchCostPerRequestUsd: 0.005,
    modelCostCoverage: "AGENT_TASK_AUDIT_AFTER_CALLBACK",
    pricingSource: "https://brave.com/search/api/",
    pricingObservedOn: "2026-09-23",
  },
});

test("plano complementar de referências reutiliza candidatas e limites sem outra inferência", async () => {
  const job = input();
  assert.equal(isReferenceGapResearch(job), true);
  const directed = deterministicPlan(job);
  validatePlan(directed.plan, job);
  assert.equal(directed.mode, "DETERMINISTIC");
  assert.equal(directed.usage, null);
  assert.deepEqual(
    directed.plan.candidateGaps.map((g) => g.candidateName),
    job.previousCandidates.map((c) => c.name),
  );
  assert.ok(directed.plan.publicQueries.includes("site:whering.co"));
  assert.ok(directed.plan.publicQueries.includes("site:myindyx.com"));
  assert.equal(
    new Set(directed.plan.publicQueries).size,
    directed.plan.publicQueries.length,
  );
  assert.ok(directed.plan.publicQueries.length <= 12);
  await writeFile(
    "/tmp/vega-reference-gap-plan.json",
    JSON.stringify(directed.plan),
  );
});

test("outras identidades e cinco referências continuam no contrato por candidata", () => {
  const job = input(99173);
  job.referenceSources = Array.from(
    { length: 5 },
    (_, i) => `https://fonte${i}.example/`,
  ).join("\n");
  job.previousCandidates.push({ id: 99174, name: "Terceira situação pessoal" });
  const directed = deterministicPlan(job);
  validatePlan(directed.plan, job);
  for (let i = 0; i < 5; i++)
    assert.ok(directed.plan.publicQueries.includes(`site:fonte${i}.example`));
  assert.ok(
    directed.plan.candidateGaps.every((g) => g.publicQueries.length <= 4),
  );
  assert.equal(
    isReferenceGapResearch({
      ...job,
      evidencePolicy: "CONSENTED_INTERVIEWS_V1",
    }),
    false,
  );
  assert.equal(isReferenceGapResearch({ ...job, referenceSources: "" }), false);
});

test("retomada coleta e sintetiza uma vez, sem chamar o planejador que expirou", async () => {
  const calls = [];
  let syntheses = 0;
  await processJob(input(), {
    backendBaseUrl: "http://backend.local",
    logger: { info() {}, error() {} },
    selectResearchLibraryContext: async () => ({ evidence: [], coverage: [] }),
    planDirectedResearch: async () => {
      throw new Error("Planejador de modelo não pode ser chamado");
    },
    searchInternet: async (job) =>
      job.directedQueries.map((q, i) => ({
        evidenceId: `P${i + 1}`,
        url: `https://fonte${i}.example/`,
        sourceQuery: q,
        title: "Fonte sintética",
        snippet: "Fixture sem demanda comercial.",
      })),
    collectMarketplaceEvidence: async () => ({
      marketplaceOffers: [],
      metaAdEvidence: [],
      metaCoverage: [],
    }),
    synthesizeMarketCandidates: async () => {
      syntheses++;
      return {
        mode: "CODEX",
        model: "gpt-6.1-sol",
        prompt: "Síntese factual",
        agentPromptPart: "Argos",
        activityPromptPart: "Confrontar fontes",
        reasoningEffort: "medium",
        usage: { inputTokens: 100, cachedInputTokens: 0, outputTokens: 20 },
        synthesis: { candidates: [], decisionSummary: "Lacunas permanecem" },
      };
    },
    analyzeSearchResults: () => ({
      decisionSummary: "Lacunas permanecem",
      opportunities: input().previousCandidates.map((c) => ({
        name: c.name,
        maturity: "RESEARCHABLE",
        decision: "RESEARCH_MORE",
        evidenceJson: "{}",
      })),
      evidenceReport: {},
    }),
    postJson: async (url, payload) => calls.push({ url, payload }),
    markCycleCompleted() {},
    markCycleFailed() {},
  });
  assert.equal(syntheses, 1);
  const p = calls.find((c) => c.url.endsWith("/plan"));
  assert.ok(p);
  assert.equal(p.payload.executionMode, "DETERMINISTIC");
  const end = calls.filter(
    (c) => c.url.endsWith("/complete") || c.url.endsWith("/fail"),
  );
  assert.equal(end.length, 1);
  assert.ok(end[0].url.endsWith("/complete"), JSON.stringify(end));
  assert.equal(
    end[0].payload.evidenceReport.gapDeepening.modelInvocationCount,
    1,
  );
  assert.equal(end[0].payload.analysisAudit.inputTokens, 100);
});
