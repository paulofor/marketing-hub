import assert from "node:assert/strict";
import test from "node:test";
import {
  executeBoundedMarketResearch,
  isNovelExpansionPlan,
  resolveMarketResearchAttempts,
} from "../src/market-expansion.js";
import { processJob } from "../src/worker.js";

test("amplia uma lente adjacente e encerra no primeiro dossiê pronto", async () => {
  const persistedPlanSizes = [];
  const synthesisContexts = [];
  const execution = await executeBoundedMarketResearch(discoveryJob(), {
    maxAttempts: 3,
    repositoryEvidence: [],
    repositoryCoverage: [],
    planResearch: async (job) =>
      directed(job.marketExpansionContext.attemptNumber),
    persistPlan: async (attempts) => persistedPlanSizes.push(attempts.length),
    collectEvidence: async ({ attemptNumber }) => evidenceBatch(attemptNumber),
    synthesize: async (context) => {
      synthesisContexts.push(context);
      return analysis(context.plan.expansionAxis);
    },
    analyze: ({ plan, publicEvidence, marketplaceOffers, metaAdEvidence }) =>
      report({
        ready: plan.expansionAxis !== "INITIAL_SCOPE",
        publicEvidence,
        marketplaceOffers,
        metaAdEvidence,
      }),
  });

  assert.deepEqual(persistedPlanSizes, [1, 2]);
  assert.equal(synthesisContexts.length, 2);
  assert.equal(synthesisContexts[1].publicEvidence.length, 2);
  assert.equal(synthesisContexts[1].marketplaceOffers.length, 10);
  assert.deepEqual(
    synthesisContexts[1].marketplaceOffers.slice(0, 2).map((item) => item.evidenceId),
    ["O1", "O2"],
  );
  assert.equal(
    execution.report.evidenceReport.marketExpansion.stopReason,
    "DOSSIER_READY_FOUND",
  );
  assert.deepEqual(
    execution.report.evidenceReport.marketExpansion.attempts.map(
      (item) => item.outcome,
    ),
    ["ADJUST_AND_CONTINUE", "DOSSIER_READY_FOUND"],
  );
});

test("interrompe sem nova síntese quando a lente não acrescenta evidência", async () => {
  let synthesisCalls = 0;
  const execution = await executeBoundedMarketResearch(discoveryJob(), {
    maxAttempts: 3,
    planResearch: async (job) =>
      directed(job.marketExpansionContext.attemptNumber),
    persistPlan: async () => {},
    collectEvidence: async ({ attemptNumber }) =>
      attemptNumber === 1
        ? evidenceBatch(1)
        : {
            ...evidenceBatch(1),
            metaCoverage: [
              {
                query: "categoria adjacente sem resultado",
                country: "BR",
                publisherPlatform: "INSTAGRAM",
                sourceStatus: "EMPTY",
              },
            ],
          },
    synthesize: async (context) => {
      synthesisCalls += 1;
      return analysis(context.plan.expansionAxis);
    },
    analyze: ({ publicEvidence, marketplaceOffers, metaAdEvidence }) =>
      report({ publicEvidence, marketplaceOffers, metaAdEvidence }),
  });

  assert.equal(synthesisCalls, 1);
  assert.equal(
    execution.report.evidenceReport.marketExpansion.stopReason,
    "NO_NEW_EVIDENCE",
  );
  assert.equal(
    execution.report.evidenceReport.marketExpansion.attempts[1]
      .newPublicEvidenceCount,
    0,
  );
  assert.equal(execution.report.evidenceReport.metaCoverage.length, 2);
});

test("preserva RESEARCH_MORE depois de três rodadas com progresso", async () => {
  const execution = await executeBoundedMarketResearch(discoveryJob(), {
    maxAttempts: 9,
    planResearch: async (job) =>
      directed(job.marketExpansionContext.attemptNumber),
    persistPlan: async () => {},
    collectEvidence: async ({ attemptNumber }) => evidenceBatch(attemptNumber),
    synthesize: async (context) => analysis(context.plan.expansionAxis),
    analyze: ({ publicEvidence, marketplaceOffers, metaAdEvidence }) =>
      report({ publicEvidence, marketplaceOffers, metaAdEvidence }),
  });

  assert.equal(execution.analysisAttempts.length, 3);
  assert.equal(
    execution.report.evidenceReport.marketExpansion.stopReason,
    "ATTEMPT_LIMIT_REACHED",
  );
  assert.equal(execution.report.opportunities[0].decision, "RESEARCH_MORE");
  assert.equal(execution.report.evidenceReport.marketExpansion.maxAttempts, 3);
});

test("não coleta outra vez quando o plano repete a lente", async () => {
  let collectionCalls = 0;
  const execution = await executeBoundedMarketResearch(discoveryJob(), {
    maxAttempts: 3,
    planResearch: async () => directed(1),
    persistPlan: async () => {},
    collectEvidence: async ({ attemptNumber }) => {
      collectionCalls += 1;
      return evidenceBatch(attemptNumber);
    },
    synthesize: async (context) => analysis(context.plan.expansionAxis),
    analyze: ({ publicEvidence, marketplaceOffers, metaAdEvidence }) =>
      report({ publicEvidence, marketplaceOffers, metaAdEvidence }),
  });

  assert.equal(collectionCalls, 1);
  assert.equal(
    execution.report.evidenceReport.marketExpansion.stopReason,
    "REPEATED_RESEARCH_LENS",
  );
});

test("não amplia ciclos que apenas validam um mercado informado", async () => {
  let planCalls = 0;
  const execution = await executeBoundedMarketResearch(
    { ...discoveryJob(), researchMode: "VALIDATE_MARKET" },
    {
      maxAttempts: 3,
      planResearch: async () => {
        planCalls += 1;
        return directed(1);
      },
      persistPlan: async () => {},
      collectEvidence: async () => evidenceBatch(1),
      synthesize: async (context) => analysis(context.plan.expansionAxis),
      analyze: ({ publicEvidence, marketplaceOffers, metaAdEvidence }) =>
        report({ publicEvidence, marketplaceOffers, metaAdEvidence }),
    },
  );

  assert.equal(planCalls, 1);
  assert.equal(execution.report.evidenceReport.marketExpansion.maxAttempts, 1);
  assert.equal(
    execution.report.evidenceReport.marketExpansion.stopReason,
    "EXPANSION_NOT_APPLICABLE",
  );
});

test("mantém ciclo, lease e callback terminal únicos no fluxo integrado", async () => {
  const callbacks = [];
  const job = { ...discoveryJob(), executionLeaseId: "lease-77" };
  await processJob(job, {
    backendBaseUrl: "http://backend.local",
    maxAttempts: 3,
    logger: { info() {}, error() {} },
    selectResearchLibraryContext: async () => ({ evidence: [], coverage: [] }),
    planDirectedResearch: async (researchJob) =>
      directed(researchJob.marketExpansionContext.attemptNumber),
    searchInternet: async (researchJob) => {
      const attempt = researchJob.marketExpansionContext.attemptNumber;
      return [publicItem(attempt)];
    },
    collectMarketplaceEvidence: async (plan) => ({
      marketplaceOffers:
        plan.expansionAxis === "INITIAL_SCOPE"
          ? [offer(1)]
          : Array.from({ length: 10 }, (_, index) => offer(index + 1)),
      metaAdEvidence: [],
      metaCoverage: [],
    }),
    synthesizeMarketCandidates: async (context) =>
      analysis(context.plan.expansionAxis),
    analyzeSearchResults: (_researchJob, publicEvidence, offers, options) =>
      report({
        ready: offers.length >= 10,
        publicEvidence,
        marketplaceOffers: offers,
        metaAdEvidence: options.metaAdEvidence,
      }),
    postJson: async (url, payload) => callbacks.push({ url, payload }),
    markCycleCompleted() {},
    markCycleFailed() {},
  });

  const planCallbacks = callbacks.filter((item) => item.url.endsWith("/plan"));
  const completeCallbacks = callbacks.filter((item) =>
    item.url.endsWith("/complete"),
  );
  assert.equal(
    planCallbacks.length,
    2,
    JSON.stringify(
      callbacks.map((item) => ({ url: item.url, error: item.payload.errorMessage })),
    ),
  );
  assert.equal(completeCallbacks.length, 1);
  assert.equal(callbacks.some((item) => item.url.endsWith("/fail")), false);
  assert.ok(
    callbacks.every((item) => item.payload.executionLeaseId === "lease-77"),
  );
  assert.equal(
    completeCallbacks[0].payload.evidenceReport.marketExpansion.stopReason,
    "DOSSIER_READY_FOUND",
  );
  assert.equal(
    JSON.parse(completeCallbacks[0].payload.analysisAudit.rawResponse).attempts
      .length,
    2,
  );
  assert.equal(completeCallbacks[0].payload.analysisAudit.inputTokens, 60);
});

test("limita configuração e exige novidade mínima na ampliação", () => {
  assert.equal(resolveMarketResearchAttempts(0), 1);
  assert.equal(resolveMarketResearchAttempts(9), 3);
  assert.equal(resolveMarketResearchAttempts("inválido"), 3);
  assert.equal(isNovelExpansionPlan(directed(2).plan, [{ attemptNumber: 1, directed: directed(1) }]), true);
  assert.equal(isNovelExpansionPlan(directed(1).plan, [{ attemptNumber: 1, directed: directed(1) }]), false);
});

function discoveryJob() {
  return {
    cycleId: 77,
    theme: "beleza e bem-estar feminino 35+",
    targetAudience: "Mulheres brasileiras entre 35 e 60 anos",
    acquisitionChannel: "Instagram",
    researchMode: "DISCOVER_MARKETS",
    marketType: "B2C",
  };
}

function directed(attemptNumber) {
  const initial = attemptNumber === 1;
  const plan = {
    researchLens: initial
      ? "Rotina de beleza antes de sair"
      : `Momento adjacente de decisão ${attemptNumber}`,
    expansionAxis: initial ? "INITIAL_SCOPE" : "ADJACENT_LIFE_MOMENT",
    expansionRationale: initial
      ? "Investigar o escopo original recebido."
      : "Buscar outra situação concreta com intenção comercial observável.",
    questions: ["Pergunta 1", "Pergunta 2", "Pergunta 3"],
    publicQueries: Array.from(
      { length: 8 },
      (_, index) => `consulta-${attemptNumber}-${index + 1}`,
    ),
    marketplaceRequests: [
      { marketplace: "HOTMART", query: `mercado ${attemptNumber}`, maxProducts: 10 },
    ],
    metaAdRequests: [
      {
        query: `categoria ${attemptNumber}`,
        country: "BR",
        publisherPlatform: "INSTAGRAM",
        maxAds: 25,
      },
    ],
    minimumComparableOffers: 10,
    stopConditions: ["Ausência de evidência independente"],
  };
  return {
    plan,
    rawResponse: JSON.stringify(plan),
    model: "modelo-teste",
    mode: "CODEX",
    prompt: `prompt do plano ${attemptNumber}`,
    agentPromptPart: "agente do plano",
    activityPromptPart: `atividade do plano ${attemptNumber}`,
    reasoningEffort: "high",
    usage: { inputTokens: 10, cachedInputTokens: 2, outputTokens: 3 },
  };
}

function analysis(expansionAxis) {
  const synthesis = {
    decisionSummary: "Síntese factual de teste.",
    candidates: [
      {
        name: "Candidata factual",
        maturity:
          expansionAxis === "INITIAL_SCOPE" ? "RESEARCHABLE" : "DOSSIER_READY",
      },
    ],
  };
  return {
    synthesis,
    rawResponse: JSON.stringify(synthesis),
    model: "modelo-teste",
    mode: "CODEX",
    prompt: `prompt da síntese ${expansionAxis}`,
    agentPromptPart: "agente da síntese",
    activityPromptPart: `atividade da síntese ${expansionAxis}`,
    reasoningEffort: "high",
    usage: { inputTokens: 20, cachedInputTokens: 4, outputTokens: 5 },
    accessedUrls: [],
  };
}

function evidenceBatch(attemptNumber) {
  return {
    publicEvidence: [publicItem(attemptNumber)],
    marketplaceOffers:
      attemptNumber === 1
        ? [offer(1)]
        : Array.from({ length: 9 }, (_, index) => offer((attemptNumber - 1) * 9 + index + 2)),
    metaAdEvidence: [],
    metaCoverage: [
      {
        query: `categoria ${attemptNumber}`,
        country: "BR",
        publisherPlatform: "INSTAGRAM",
        sourceStatus: "EMPTY",
      },
    ],
  };
}

function publicItem(index) {
  return {
    title: `Fonte pública ${index}`,
    url: `https://example${index}.test/dor`,
    snippet: "Dificuldade observada e alternativa paga.",
  };
}

function offer(index) {
  return {
    marketplace: "HOTMART",
    referenceId: `offer-${index}`,
    title: `Oferta ${index}`,
    url: `https://hotmart.test/offer-${index}`,
  };
}

function report({
  ready = false,
  publicEvidence = [],
  marketplaceOffers = [],
  metaAdEvidence = [],
}) {
  return {
    decisionSummary: ready ? "Dossiê pronto." : "Pesquisar mais.",
    opportunities: [
      {
        name: "Candidata factual",
        maturity: ready ? "DOSSIER_READY" : "RESEARCHABLE",
        decision: ready ? "APPROVE" : "RESEARCH_MORE",
      },
    ],
    evidenceReport: {
      publicEvidence,
      marketplaceOffers,
      metaAdEvidence,
      metaCoverage: [],
    },
  };
}
