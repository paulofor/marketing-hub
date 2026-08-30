import {
  analyzeSearchResults,
  extractPublicComparableOffers,
  resolveSearchConfig,
  searchInternet,
} from "./research.js";
import {
  createHealthState,
  markCycleCompleted,
  markCycleFailed,
  markPollCompleted,
  markPollFailed,
  markPollStarted,
  startHealthServer,
} from "./health.js";
import {
  operationalLogger,
  recentOperationalLogLines,
} from "./operational-log.js";
import { planDirectedResearch } from "./argos-codex.js";
import { synthesizeMarketCandidates } from "./argos-research.js";
import { selectResearchLibraryContext } from "./research-library.js";
import { startCodexAuthReconnectConsumer } from "./codex-auth-reconnect.js";
import { startAgentHealthReporter } from "./agent-health-reporter.js";
import { collectMarketplaceEvidence } from "./marketplace-evidence.js";
import { createAutomaticExecutionControl } from "./automatic-execution-control.js";

const backendBaseUrl = process.env.BACKEND_BASE_URL || "http://191.252.181.168";
const pollIntervalMs = Number(
  process.env.PRODUCT_DISCOVERY_POLL_INTERVAL_MS || "60000",
);
const maxSearchResults = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS || "30",
);
const minSearchQueries = Number(
  process.env.PRODUCT_DISCOVERY_MIN_SEARCH_QUERIES || "10",
);
const maxSearchQueries = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_QUERIES || "24",
);
const maxResultsPerQuery = Number(
  process.env.PRODUCT_DISCOVERY_MAX_RESULTS_PER_QUERY || "5",
);
const healthHost = process.env.PRODUCT_DISCOVERY_HEALTH_HOST || "0.0.0.0";
const healthPort = Number(process.env.PRODUCT_DISCOVERY_HEALTH_PORT || "8080");
const searchConfig = resolveSearchConfig();
const healthState = createHealthState();
const automaticExecution = createAutomaticExecutionControl({
  backendBaseUrl,
  logger: operationalLogger,
});
const pollLock = createPollLock();

async function main() {
  operationalLogger.info(
    `[product-discovery-worker] started searchProvider=${searchConfig.provider}`,
  );
  startHealthServer({
    host: healthHost,
    port: healthPort,
    searchConfig,
    state: healthState,
    pollIntervalMs,
    maxSearchResults,
    logger: operationalLogger,
    logLines: recentOperationalLogLines,
  });
  startCodexAuthReconnectConsumer({
    backendBaseUrl,
    logger: operationalLogger,
  });
  startAgentHealthReporter({ backendBaseUrl, logger: operationalLogger });
  await runCycle();
  setInterval(runCycle, pollIntervalMs);
}

async function runCycle() {
  if (!pollLock.tryAcquire()) {
    operationalLogger.info(
      "[product-discovery-worker] polling anterior ainda está em execução; nova rodada ignorada",
    );
    return;
  }
  markPollStarted(healthState);
  try {
    if (!(await automaticExecution.allowsAutomaticExecution())) {
      markPollCompleted(healthState);
      return;
    }
    const pending = await getJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending`,
    );
    for (const job of pending) {
      await processJob(job);
    }
    markPollCompleted(healthState);
  } catch (error) {
    markPollFailed(healthState, error);
    operationalLogger.error("[product-discovery-worker] cycle failed", error);
  } finally {
    pollLock.release();
  }
}

async function processJob(job) {
  operationalLogger.info(
    `[product-discovery-worker] processing cycle=${job.cycleId} theme=${job.theme}`,
  );
  try {
    const researchLibraryContext = await selectResearchLibraryContext(job);
    const enrichedJob = { ...job, researchLibraryContext };
    const directed = await planDirectedResearch(enrichedJob);
    operationalLogger.info(
      `[product-discovery-worker] directed plan cycle=${job.cycleId} model=${directed.model} inputTokens=${directed.usage?.inputTokens ?? "unavailable"} cachedInputTokens=${directed.usage?.cachedInputTokens ?? "unavailable"} outputTokens=${directed.usage?.outputTokens ?? "unavailable"}`,
    );
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/plan`,
      withExecutionLease(job, researchPlanCallbackPayload(directed)),
    );
    const results = await searchInternet(
      { ...enrichedJob, directedQueries: directed.plan.publicQueries },
      {
        config: searchConfig,
        maxSearchResults,
        minSearchQueries,
        maxSearchQueries,
        maxResultsPerQuery,
        logger: operationalLogger,
      },
    );
    const commercialEvidence = await collectMarketplaceEvidence(directed.plan, {
      backendBaseUrl,
      logger: operationalLogger,
      cycleId: job.cycleId,
      executionLeaseId: job.executionLeaseId,
      researchContext: [job.theme, job.targetAudience, job.objective]
        .filter(Boolean)
        .join(" "),
    });
    const comparableOffers = deduplicateOffers([
      ...commercialEvidence.marketplaceOffers,
      ...extractPublicComparableOffers(results),
    ]);
    const publicEvidence = identifyEvidence(results, "P", "PUBLIC_SEARCH");
    const identifiedOffers = identifyEvidence(
      comparableOffers,
      "O",
      "COMMERCIAL_OFFER",
    );
    const identifiedMetaAds = identifyEvidence(
      commercialEvidence.metaAdEvidence,
      "M",
      "META_AD_LIBRARY",
    );
    const analysis = await synthesizeMarketCandidates({
      job: enrichedJob,
      plan: directed.plan,
      publicEvidence,
      repositoryEvidence: researchLibraryContext.evidence,
      repositoryCoverage: researchLibraryContext.coverage,
      marketplaceOffers: identifiedOffers,
      metaAdEvidence: identifiedMetaAds,
      metaCoverage: commercialEvidence.metaCoverage,
    });
    operationalLogger.info(
      `[product-discovery-worker] factual synthesis cycle=${job.cycleId} model=${analysis.model} mode=${analysis.mode} candidates=${analysis.synthesis.candidates.length} inputTokens=${analysis.usage?.inputTokens ?? "unavailable"} cachedInputTokens=${analysis.usage?.cachedInputTokens ?? "unavailable"} outputTokens=${analysis.usage?.outputTokens ?? "unavailable"}`,
    );
    const report = analyzeSearchResults(enrichedJob, publicEvidence, identifiedOffers, {
      minimumComparableOffers: directed.plan.minimumComparableOffers,
      metaAdEvidence: identifiedMetaAds,
      metaCoverage: commercialEvidence.metaCoverage,
      candidateBlueprints: analysis.synthesis.candidates,
      analysisSummary: analysis.synthesis.decisionSummary,
      analysisMode: analysis.mode,
      analysisModel: analysis.model,
      repositoryEvidence: researchLibraryContext.evidence,
      repositoryCoverage: researchLibraryContext.coverage,
    });
    report.analysisAudit = analysisAuditCallbackPayload(directed, analysis);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/complete`,
      withExecutionLease(job, report),
    );
    markCycleCompleted(healthState, job, report);
    operationalLogger.info(
      `[product-discovery-worker] completed cycle=${job.cycleId} opportunities=${report.opportunities.length}`,
    );
  } catch (error) {
    markCycleFailed(healthState, job, error);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/fail`,
      withExecutionLease(job, failureCallbackPayload(error)),
    );
    operationalLogger.error(
      `[product-discovery-worker] failed cycle=${job.cycleId}`,
      error,
    );
  }
}

/** Agrega planejamento e síntese na mesma tarefa sem perder as respostas brutas separadas. */
export function analysisAuditCallbackPayload(directed, analysis) {
  const usage = aggregateUsage(directed.usage, analysis.usage);
  const modelExecution = directed.mode === "CODEX" || analysis.mode === "CODEX";
  return {
    rawResponse: analysis.rawResponse,
    model: analysis.model,
    executionMode: modelExecution ? "MODEL" : "DETERMINISTIC",
    promptSent: joinAuditParts(directed.prompt, analysis.prompt),
    agentPromptPart: modelExecution
      ? joinAuditParts(directed.agentPromptPart, analysis.agentPromptPart)
      : undefined,
    activityPromptPart: joinAuditParts(
      directed.activityPromptPart || directed.prompt,
      analysis.activityPromptPart || analysis.prompt,
    ),
    reasoningEffort: modelExecution
      ? analysis.reasoningEffort || directed.reasoningEffort
      : "NOT_APPLICABLE",
    inputTokens: usage?.inputTokens,
    cachedInputTokens: usage?.cachedInputTokens,
    outputTokens: usage?.outputTokens,
    accessedUrls: analysis.accessedUrls,
  };
}

/** Preserva no bloqueio o prompt e o raciocínio já preparados para a tentativa de Argos. */
export function failureCallbackPayload(error) {
  return {
    errorMessage: error?.message || "Falha desconhecida na pesquisa PDE",
    ...(error?.executionAudit ? { executionAudit: error.executionAudit } : {}),
  };
}

/** Vincula cada callback ao lease entregue pelo backend sem alterar o resultado funcional. */
export function withExecutionLease(job, payload) {
  return { executionLeaseId: job.executionLeaseId, ...payload };
}

/** Monta a auditoria disponível do plano sem inventar consumo ou configuração ausente. */
export function researchPlanCallbackPayload(directed) {
  return {
    planJson: JSON.stringify(directed.plan),
    rawResponse: directed.rawResponse,
    model: directed.model,
    executionMode: directed.mode,
    promptSent: directed.prompt,
    agentPromptPart: directed.agentPromptPart,
    activityPromptPart:
      directed.activityPromptPart ||
      (directed.mode === "DETERMINISTIC" ? directed.prompt : undefined),
    reasoningEffort: directed.reasoningEffort,
    inputTokens: directed.usage?.inputTokens,
    cachedInputTokens: directed.usage?.cachedInputTokens,
    outputTokens: directed.usage?.outputTokens,
  };
}

/** Impede polls sobrepostos sem deslocar a decisão de fila para o executor. */
export function createPollLock() {
  let running = false;
  return {
    tryAcquire() {
      if (running) return false;
      running = true;
      return true;
    },
    release() {
      running = false;
    },
  };
}

/** Remove a mesma alternativa observada por consultas diferentes sem inflar o gate. */
function deduplicateOffers(offers) {
  return [
    ...new Map(
      offers.map((offer) => [
        `${offer.marketplace}:${offer.referenceId || offer.url}`,
        offer,
      ]),
    ).values(),
  ];
}

/** Atribui identidades estáveis depois da deduplicação para impedir citação inventada pelo modelo. */
function identifyEvidence(items, prefix, sourceType) {
  return (items || []).map((item, index) => ({
    ...item,
    evidenceId: `${prefix}${index + 1}`,
    sourceType,
  }));
}

/** Soma tokens somente quando todas as chamadas reais informaram seus contadores. */
function aggregateUsage(...usages) {
  const reported = usages.filter(Boolean);
  if (reported.length !== usages.length) return null;
  return reported.reduce(
    (total, usage) => ({
      inputTokens: total.inputTokens + Number(usage.inputTokens || 0),
      cachedInputTokens:
        total.cachedInputTokens + Number(usage.cachedInputTokens || 0),
      outputTokens: total.outputTokens + Number(usage.outputTokens || 0),
    }),
    { inputTokens: 0, cachedInputTokens: 0, outputTokens: 0 },
  );
}

/** Separa as duas fases no prompt auditável sem alterar o conteúdo enviado a cada chamada. */
function joinAuditParts(planPart, analysisPart) {
  return [
    planPart ? `--- PLANEJAMENTO ---\n${planPart}` : null,
    analysisPart ? `--- SÍNTESE FACTUAL ---\n${analysisPart}` : null,
  ]
    .filter(Boolean)
    .join("\n\n");
}

async function getJson(url) {
  const response = await fetch(url, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`GET ${url} failed with status ${response.status}`);
  }
  return response.json();
}

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(`POST ${url} failed with status ${response.status}`);
  }
  return response.json();
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}
