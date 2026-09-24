// Executa o worker real com somente busca, marketplace e resposta do modelo simulados.
import assert from "node:assert/strict";
import { writeFile } from "node:fs/promises";
import { processJob } from "../../../product-discovery-worker/src/worker.js";
import {
  planDirectedResearch,
  deterministicPlan,
} from "../../../product-discovery-worker/src/argos-codex.js";
import { synthesizeMarketCandidates } from "../../../product-discovery-worker/src/argos-research.js";
const job = {
  cycleId: 93007,
  executionLeaseId: "synthetic-lease",
  stageCode: "candidate-gap-deepening",
  evidencePolicy: "PUBLIC_SOURCES_V1",
  researchMode: "DISCOVER_MARKETS",
  marketType: "B2C",
  theme: "Organização de escolhas cotidianas",
  targetAudience: "Adultos com ocasião marcada",
  country: "BR",
  language: "pt-BR",
  previousCandidates: [
    { name: "Preparação de ocasião sintética" },
    { name: "Escolha de rotina sintética" },
  ],
  customerInterviews: [],
  gapResearchPolicy: {
    maximumAttempts: 2,
    maximumPublicQueriesPerAttempt: 12,
    maximumModelInvocations: 4,
    estimatedSearchCostPerRequestUsd: 0.005,
    maximumSearchCostUsd: 0.12,
    costCoverage: "ESTIMATED_SEARCH_ONLY",
    modelCostCoverage: "AGENT_TASK_AUDIT_AFTER_CALLBACK",
    pricingSource: "https://brave.com/search/api/",
    pricingObservedOn: "2026-09-23",
  },
};
const callbacks = [];
await processJob(job, {
  backendBaseUrl: "http://backend.invalid",
  maxAttempts: 1,
  logger: {
    info() {},
    error(...args) {
      console.error(...args);
    },
  },
  selectResearchLibraryContext: async () => ({ evidence: [], coverage: [] }),
  planDirectedResearch: async (input) =>
    planDirectedResearch(input, {
      enabled: true,
      execute: async (_cmd, args, prompt) => {
        assert(prompt.includes("Política de evidência: PUBLIC_SOURCES_V1"));
        await writeFile(
          args[args.indexOf("--output-last-message") + 1],
          JSON.stringify(deterministicPlan(input).plan),
        );
        return { stdout: "" };
      },
    }),
  searchInternet: async (input) => [
    {
      url: "https://community.example.org/report",
      title: "Relato sintético de uso",
      snippet:
        "Usei uma alternativa gratuita e ainda tive dificuldade para escolher.",
      sourceQuery: input.directedQueries[0],
      retrievedAt: "2026-09-24T00:00:00Z",
    },
    {
      url: "https://reviews.example.net/report",
      title: "Relato sintético de desistência",
      snippet:
        "Desisti da assinatura porque exigia muito esforço para preparar o resultado.",
      sourceQuery: input.directedQueries[1],
      retrievedAt: "2026-09-24T00:00:00Z",
    },
  ],
  collectMarketplaceEvidence: async () => ({
    marketplaceOffers: [],
    metaAdEvidence: [],
    metaCoverage: [],
  }),
  synthesizeMarketCandidates: async (context) =>
    synthesizeMarketCandidates(context, {
      enabled: true,
      execute: async (_cmd, args) => {
        const result = {
          decisionSummary:
            "Relatos sintéticos sustentados; faltam ofertas e anúncios para promoção.",
          candidates: job.previousCandidates.map((item) => ({
            ...item,
            primaryAudience: job.targetAudience,
            purchaseSituation:
              "Ocasião próxima com alternativas difíceis de comparar.",
            rootPain: "Dificuldade para decidir.",
            practicalPain: "Comparar alternativas.",
            emotionalPain: "Incerteza sobre a escolha.",
            observedLanguage: ["ainda tive dificuldade"],
            currentAlternatives: ["conteúdo gratuito"],
            residualEffort: "Preparar a decisão.",
            scaleEvidence: "Dois relatos, sem estimar mercado.",
            unmetnessEvidence: "Esforço residual relatado.",
            pdeValueBoundary: "Reduzir esforço sem definir oferta.",
            pdeDeliveryFit: {
              deliveryMode: "AI_DIGITAL_EXPERIENCE",
              minimumInput: "Ocasião e preferências.",
              aiBackstageWork: "Comparar opções.",
              readyDigitalOutcome: "Orientação pronta para usar.",
              physicalDependency: "NONE",
            },
            instagramFitEvidence: "Não investigado nesta fixture.",
            commercialRisk: "Não há oferta comprovada.",
            evidenceIds: context.publicEvidence.map((e) => e.evidenceId),
            maturity: "RESEARCHABLE",
            publicObservations: context.publicEvidence.map((e, i) => ({
              evidenceId: e.evidenceId,
              sourceRole: "PUBLIC_CUSTOMER_REPORT",
              reportedAction: i ? "ABANDONMENT_REPORTED" : "USE_REPORTED",
              supportingExcerpt: e.snippet,
              limitation: "Relato sintético de teste; sem compra conciliada.",
            })),
          })),
        };
        await writeFile(
          args[args.indexOf("--output-last-message") + 1],
          JSON.stringify(result),
        );
        return { stdout: "" };
      },
    }),
  postJson: async (url, payload) => callbacks.push({ url, payload }),
  markCycleCompleted() {},
  markCycleFailed() {},
});
assert.equal(callbacks.filter((c) => c.url.endsWith("/complete")).length, 1);
assert.equal(callbacks.filter((c) => c.url.endsWith("/fail")).length, 0);
assert(
  callbacks.every((c) => c.payload.executionLeaseId === job.executionLeaseId),
);
const plan = callbacks.find((c) => c.url.endsWith("/plan")).payload;
const result = callbacks.find((c) => c.url.endsWith("/complete")).payload;
assert.equal(result.evidenceReport.gapDeepening.customerInterviewCount, 0);
assert.equal(
  result.evidenceReport.gapDeepening.evidencePolicy,
  "PUBLIC_SOURCES_V1",
);
assert(result.opportunities.every((c) => c.decision === "RESEARCH_MORE"));
assert(
  result.opportunities.every(
    (c) => JSON.parse(c.evidenceJson).publicEvidenceAssessment.ready,
  ),
);
await writeFile(
  process.argv[2] || "/tmp/argos-public-worker.json",
  JSON.stringify({ job, plan, result }, null, 2),
);
console.log(
  JSON.stringify({
    callbacks: callbacks.length,
    candidates: result.opportunities.length,
    policy: job.evidencePolicy,
    paidCalls: 0,
  }),
);
