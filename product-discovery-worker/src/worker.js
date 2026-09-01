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
import {
  executeBoundedMarketResearch,
  MARKET_EXPANSION_STRATEGY_CODE,
} from "./market-expansion.js";

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
const backendCallbackMaxAttempts = Number(
  process.env.ARGOS_BACKEND_CALLBACK_MAX_ATTEMPTS || "25",
);
const backendCallbackRetryDelayMs = Number(
  process.env.ARGOS_BACKEND_CALLBACK_RETRY_DELAY_MS || "15000",
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

export async function processJob(job, dependencies = {}) {
  const logger = dependencies.logger || operationalLogger;
  const activeBackendBaseUrl = dependencies.backendBaseUrl || backendBaseUrl;
  const post = dependencies.postJson || postJson;
  const selectLibrary =
    dependencies.selectResearchLibraryContext || selectResearchLibraryContext;
  const planResearch = dependencies.planDirectedResearch || planDirectedResearch;
  const internetSearch = dependencies.searchInternet || searchInternet;
  const collectCommercialEvidence =
    dependencies.collectMarketplaceEvidence || collectMarketplaceEvidence;
  const synthesize =
    dependencies.synthesizeMarketCandidates || synthesizeMarketCandidates;
  const analyze = dependencies.analyzeSearchResults || analyzeSearchResults;
  logger.info(
    `[product-discovery-worker] processing cycle=${job.cycleId} theme=${job.theme}`,
  );
  try {
    const researchLibraryContext = await selectLibrary(job);
    const enrichedJob = { ...job, researchLibraryContext };
    const execution = await executeBoundedMarketResearch(enrichedJob, {
      maxAttempts:
        dependencies.maxAttempts ??
        process.env.ARGOS_MARKET_EXPANSION_MAX_ATTEMPTS,
      repositoryEvidence: researchLibraryContext.evidence,
      repositoryCoverage: researchLibraryContext.coverage,
      planResearch: async (researchJob) => {
        const directed = await planResearch(researchJob);
        logger.info(
          `[product-discovery-worker] directed plan cycle=${job.cycleId} lens=${directed.plan.researchLens} model=${directed.model} inputTokens=${directed.usage?.inputTokens ?? "unavailable"} cachedInputTokens=${directed.usage?.cachedInputTokens ?? "unavailable"} outputTokens=${directed.usage?.outputTokens ?? "unavailable"}`,
        );
        return directed;
      },
      persistPlan: async (directedAttempts) => {
        await post(
          `${activeBackendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/plan`,
          withExecutionLease(
            job,
            researchPlanHistoryCallbackPayload(directedAttempts),
          ),
        );
      },
      collectEvidence: async ({ job: researchJob, plan, attemptNumber }) => {
        const results = await internetSearch(
          { ...researchJob, directedQueries: plan.publicQueries },
          {
            config: dependencies.searchConfig || searchConfig,
            maxSearchResults,
            minSearchQueries,
            maxSearchQueries,
            maxResultsPerQuery,
            logger,
          },
        );
        const commercialEvidence = await collectCommercialEvidence(plan, {
          backendBaseUrl: activeBackendBaseUrl,
          logger,
          cycleId: job.cycleId,
          attemptNumber,
          executionLeaseId: job.executionLeaseId,
          researchContext: [
            job.theme,
            job.targetAudience,
            job.objective,
            plan.researchLens,
            ...(plan.metaAdRequests || []).map((request) => request.query),
          ]
            .filter(Boolean)
            .join(" "),
        });
        logger.info(
          `[product-discovery-worker] evidence collected cycle=${job.cycleId} attempt=${attemptNumber} public=${results.length} offers=${commercialEvidence.marketplaceOffers.length} metaAds=${commercialEvidence.metaAdEvidence.length}`,
        );
        return {
          publicEvidence: results,
          marketplaceOffers: deduplicateOffers([
            ...commercialEvidence.marketplaceOffers,
            ...extractPublicComparableOffers(results),
          ]),
          metaAdEvidence: commercialEvidence.metaAdEvidence,
          metaCoverage: commercialEvidence.metaCoverage,
        };
      },
      synthesize: async (context) => {
        const analysis = await synthesize(context);
        logger.info(
          `[product-discovery-worker] factual synthesis cycle=${job.cycleId} model=${analysis.model} mode=${analysis.mode} candidates=${analysis.synthesis.candidates.length} inputTokens=${analysis.usage?.inputTokens ?? "unavailable"} cachedInputTokens=${analysis.usage?.cachedInputTokens ?? "unavailable"} outputTokens=${analysis.usage?.outputTokens ?? "unavailable"}`,
        );
        return analysis;
      },
      analyze: (context) =>
        analyze(context.job, context.publicEvidence, context.marketplaceOffers, {
          minimumComparableOffers: context.plan.minimumComparableOffers,
          metaAdEvidence: context.metaAdEvidence,
          metaCoverage: context.metaCoverage,
          candidateBlueprints: context.analysis.synthesis.candidates,
          analysisSummary: context.analysis.synthesis.decisionSummary,
          analysisMode: context.analysis.mode,
          analysisModel: context.analysis.model,
          repositoryEvidence: context.repositoryEvidence,
          repositoryCoverage: context.repositoryCoverage,
        }),
    });
    execution.report.analysisAudit = analysisAuditHistoryCallbackPayload(
      execution.directedAttempts,
      execution.analysisAttempts,
    );
    await post(
      `${activeBackendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/complete`,
      withExecutionLease(job, execution.report),
    );
    (dependencies.markCycleCompleted || markCycleCompleted)(
      healthState,
      job,
      execution.report,
    );
    logger.info(
      `[product-discovery-worker] completed cycle=${job.cycleId} opportunities=${execution.report.opportunities.length} attempts=${execution.report.evidenceReport.marketExpansion.attemptsCompleted} stopReason=${execution.report.evidenceReport.marketExpansion.stopReason}`,
    );
  } catch (error) {
    (dependencies.markCycleFailed || markCycleFailed)(healthState, job, error);
    await post(
      `${activeBackendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/fail`,
      withExecutionLease(job, failureCallbackPayload(error)),
    );
    logger.error(
      `[product-discovery-worker] failed cycle=${job.cycleId}`,
      error,
    );
  }
}

/** Agrega planejamento e síntese na mesma tarefa sem perder as respostas brutas separadas. */
export function analysisAuditCallbackPayload(directed, analysis) {
  return analysisAuditHistoryCallbackPayload(
    [{ attemptNumber: 1, directed }],
    [{ attemptNumber: 1, analysis }],
  );
}

/** Consolida todas as fases adaptativas sem perder respostas brutas ou consumo por rodada. */
export function analysisAuditHistoryCallbackPayload(
  directedAttempts,
  analysisAttempts,
) {
  const directed = directedAttempts.at(-1)?.directed;
  const analysis = analysisAttempts.at(-1)?.analysis;
  if (!directed || !analysis) {
    throw new Error("Auditoria de Argos exige plano e síntese factuais");
  }
  const usage = aggregateUsage(
    ...directedAttempts.map((item) => item.directed.usage),
    ...analysisAttempts.map((item) => item.analysis.usage),
  );
  const modelExecution =
    directedAttempts.some((item) => item.directed.mode === "CODEX") ||
    analysisAttempts.some((item) => item.analysis.mode === "CODEX");
  const singleAttempt =
    directedAttempts.length === 1 && analysisAttempts.length === 1;
  return {
    rawResponse: singleAttempt
      ? analysis.rawResponse
      : JSON.stringify({
          strategyCode: MARKET_EXPANSION_STRATEGY_CODE,
          attempts: directedAttempts.map((item) => ({
            attemptNumber: item.attemptNumber,
            researchLens: item.directed.plan.researchLens,
            planRawResponse: parseAuditJson(item.directed.rawResponse),
            synthesisRawResponse: parseAuditJson(
              analysisAttempts.find(
                (analysisItem) =>
                  analysisItem.attemptNumber === item.attemptNumber,
              )?.analysis.rawResponse,
            ),
          })),
        }),
    model: analysis.model,
    executionMode: modelExecution ? "MODEL" : "DETERMINISTIC",
    promptSent: singleAttempt
      ? joinAuditParts(directed.prompt, analysis.prompt)
      : joinAttemptAuditParts(directedAttempts, analysisAttempts, "prompt"),
    agentPromptPart: modelExecution
      ? singleAttempt
        ? joinAuditParts(directed.agentPromptPart, analysis.agentPromptPart)
        : joinAttemptAuditParts(
            directedAttempts,
            analysisAttempts,
            "agentPromptPart",
          )
      : undefined,
    activityPromptPart: singleAttempt
      ? joinAuditParts(
          directed.activityPromptPart || directed.prompt,
          analysis.activityPromptPart || analysis.prompt,
        )
      : joinAttemptAuditParts(
          directedAttempts,
          analysisAttempts,
          "activityPromptPart",
        ),
    reasoningEffort: modelExecution
      ? analysis.reasoningEffort || directed.reasoningEffort
      : "NOT_APPLICABLE",
    inputTokens: usage?.inputTokens,
    cachedInputTokens: usage?.cachedInputTokens,
    outputTokens: usage?.outputTokens,
    accessedUrls: deduplicateAccessedUrls(
      analysisAttempts.flatMap((item) => item.analysis.accessedUrls || []),
    ),
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

/** Persiste o histórico cumulativo de planos e renova o mesmo lease entre as rodadas. */
export function researchPlanHistoryCallbackPayload(directedAttempts) {
  if (directedAttempts.length === 1) {
    return researchPlanCallbackPayload(directedAttempts[0].directed);
  }
  const latest = directedAttempts.at(-1).directed;
  const usage = aggregateUsage(
    ...directedAttempts.map((item) => item.directed.usage),
  );
  const modelExecution = directedAttempts.some(
    (item) => item.directed.mode === "CODEX",
  );
  return {
    planJson: JSON.stringify({
      strategyCode: MARKET_EXPANSION_STRATEGY_CODE,
      attempts: directedAttempts.map((item) => ({
        attemptNumber: item.attemptNumber,
        plan: item.directed.plan,
      })),
    }),
    rawResponse: JSON.stringify({
      strategyCode: MARKET_EXPANSION_STRATEGY_CODE,
      attempts: directedAttempts.map((item) => ({
        attemptNumber: item.attemptNumber,
        rawResponse: parseAuditJson(item.directed.rawResponse),
      })),
    }),
    model: latest.model,
    executionMode: modelExecution ? "CODEX" : "DETERMINISTIC",
    promptSent: joinDirectedAttemptParts(directedAttempts, "prompt"),
    agentPromptPart: modelExecution
      ? joinDirectedAttemptParts(directedAttempts, "agentPromptPart")
      : undefined,
    activityPromptPart: joinDirectedAttemptParts(
      directedAttempts,
      "activityPromptPart",
    ),
    reasoningEffort: modelExecution
      ? latest.reasoningEffort
      : "NOT_APPLICABLE",
    inputTokens: usage?.inputTokens,
    cachedInputTokens: usage?.cachedInputTokens,
    outputTokens: usage?.outputTokens,
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

/** Separa no documento auditável os prompts de plano e síntese de cada tentativa. */
function joinAttemptAuditParts(directedAttempts, analysisAttempts, field) {
  return directedAttempts
    .flatMap((item) => {
      const analysis = analysisAttempts.find(
        (candidate) => candidate.attemptNumber === item.attemptNumber,
      )?.analysis;
      const planPart =
        item.directed[field] ||
        (field === "activityPromptPart" ? item.directed.prompt : undefined);
      const analysisPart =
        analysis?.[field] ||
        (field === "activityPromptPart" ? analysis?.prompt : undefined);
      return [
        planPart
          ? `${attemptPhaseHeader(item.attemptNumber, "PLANEJAMENTO")}\n${planPart}`
          : null,
        analysisPart
          ? `${attemptPhaseHeader(item.attemptNumber, "SÍNTESE FACTUAL")}\n${analysisPart}`
          : null,
      ];
    })
    .filter(Boolean)
    .join("\n\n");
}

/** Separa os planos cumulativos usados para renovar o lease da mesma tarefa. */
function joinDirectedAttemptParts(directedAttempts, field) {
  return directedAttempts
    .map((item) => {
      const value =
        item.directed[field] ||
        (field === "activityPromptPart" ? item.directed.prompt : undefined);
      return value
        ? `${attemptPhaseHeader(item.attemptNumber, "PLANEJAMENTO")}\n${value}`
        : null;
    })
    .filter(Boolean)
    .join("\n\n");
}

/** Nomeia cada interação sem depender de texto livre produzido pelo modelo. */
function attemptPhaseHeader(attemptNumber, phaseName) {
  return `--- TENTATIVA ${attemptNumber} · ${phaseName} ---`;
}

/** Preserva JSON bruto como estrutura e usa nulo apenas quando a fase não foi executada. */
function parseAuditJson(value) {
  if (value == null || value === "") return null;
  try {
    return JSON.parse(value);
  } catch {
    return String(value);
  }
}

/** Deduplica URLs de todas as sínteses e respeita o limite contratual do backend. */
function deduplicateAccessedUrls(items) {
  return [
    ...new Map((items || []).map((item) => [String(item.url), item])).values(),
  ].slice(0, 50);
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

/**
 * Repete callbacks somente quando a conexão nem chegou ao backend, preservando a segurança de
 * callbacks terminais que podem ter sido aplicados antes de uma resposta interrompida.
 */
export async function postJson(url, payload, options = {}) {
  const fetchFn = options.fetchFn || fetch;
  const sleepFn = options.sleepFn || sleep;
  const logger = options.logger || operationalLogger;
  const maxAttempts = boundedInteger(
    options.maxAttempts ?? backendCallbackMaxAttempts,
    25,
    1,
    40,
  );
  const retryDelayMs = boundedInteger(
    options.retryDelayMs ?? backendCallbackRetryDelayMs,
    15000,
    0,
    30000,
  );
  const serializedPayload = JSON.stringify(payload);

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await fetchFn(url, {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: serializedPayload,
      });
      if (!response.ok) {
        throw new Error(await backendFailureMessage("POST", url, response));
      }
      return response.json();
    } catch (error) {
      const errorCode = preconnectFailureCode(error);
      if (!errorCode || attempt >= maxAttempts) throw error;
      logger.warn(
        `[product-discovery-worker] backend callback unavailable url=${url} code=${errorCode} attempt=${attempt}/${maxAttempts}; retrying`,
      );
      await sleepFn(retryDelayMs);
    }
  }
}

/** Identifica apenas falhas anteriores ao estabelecimento da conexão TCP, seguras para repetição. */
function preconnectFailureCode(error) {
  const code = error?.cause?.code || error?.code;
  return [
    "ECONNREFUSED",
    "EHOSTUNREACH",
    "ENETUNREACH",
    "EAI_AGAIN",
    "UND_ERR_CONNECT_TIMEOUT",
  ].includes(code)
    ? code
    : null;
}

/** Limita configurações operacionais para evitar espera infinita por backend indisponível. */
function boundedInteger(value, fallback, minimum, maximum) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed)) return fallback;
  return Math.min(maximum, Math.max(minimum, parsed));
}

/** Aguarda entre callbacks sem bloquear o loop de eventos do worker. */
function sleep(delayMs) {
  return new Promise((resolve) => setTimeout(resolve, delayMs));
}

/** Acrescenta ao erro HTTP somente a mensagem segura e limitada devolvida pelo backend. */
export async function backendFailureMessage(method, url, response) {
  let detail = "";
  try {
    const body = JSON.parse(await response.text());
    detail = String(body?.message || "")
      .replace(/(?:sk-|sess-|eyJ)[A-Za-z0-9._-]+/g, "[SEGREDO_REMOVIDO]")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, 500);
  } catch {
    detail = "";
  }
  return `${method} ${url} failed with status ${response.status}${detail ? `: ${detail}` : ""}`;
}

if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}
