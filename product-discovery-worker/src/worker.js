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
import { startCodexAuthReconnectConsumer } from "./codex-auth-reconnect.js";
import { startAgentHealthReporter } from "./agent-health-reporter.js";
import { collectMarketplaceEvidence } from "./marketplace-evidence.js";
import { createAutomaticExecutionControl } from "./automatic-execution-control.js";

const backendBaseUrl = process.env.BACKEND_BASE_URL || "http://191.252.181.168";
const pollIntervalMs = Number(
  process.env.PRODUCT_DISCOVERY_POLL_INTERVAL_MS || "60000",
);
const maxSearchResults = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS || "12",
);
const minSearchQueries = Number(
  process.env.PRODUCT_DISCOVERY_MIN_SEARCH_QUERIES || "6",
);
const maxSearchQueries = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_QUERIES || "14",
);
const maxResultsPerQuery = Number(
  process.env.PRODUCT_DISCOVERY_MAX_RESULTS_PER_QUERY || "3",
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
    const directed = await planDirectedResearch(job);
    operationalLogger.info(
      `[product-discovery-worker] directed plan cycle=${job.cycleId} model=${directed.model} inputTokens=${directed.usage?.inputTokens ?? "unavailable"} cachedInputTokens=${directed.usage?.cachedInputTokens ?? "unavailable"} outputTokens=${directed.usage?.outputTokens ?? "unavailable"}`,
    );
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/plan`,
      withExecutionLease(job, {
        planJson: JSON.stringify(directed.plan),
        rawResponse: directed.rawResponse,
        model: directed.model,
      }),
    );
    const results = await searchInternet(
      { ...job, directedQueries: directed.plan.publicQueries },
      {
        config: searchConfig,
        maxSearchResults,
        minSearchQueries,
        maxSearchQueries,
        maxResultsPerQuery,
        logger: operationalLogger,
      },
    );
    const marketplaceOffers = await collectMarketplaceEvidence(directed.plan, {
      backendBaseUrl,
      logger: operationalLogger,
      researchContext: [job.theme, job.targetAudience, job.objective]
        .filter(Boolean)
        .join(" "),
    });
    const comparableOffers = deduplicateOffers([
      ...marketplaceOffers,
      ...extractPublicComparableOffers(results),
    ]);
    const report = analyzeSearchResults(job, results, comparableOffers, {
      minimumComparableOffers: directed.plan.minimumComparableOffers,
    });
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
      withExecutionLease(job, {
        errorMessage: error.message || "Falha desconhecida na pesquisa PDE",
      }),
    );
    operationalLogger.error(
      `[product-discovery-worker] failed cycle=${job.cycleId}`,
      error,
    );
  }
}

/** Vincula cada callback ao lease entregue pelo backend sem alterar o resultado funcional. */
export function withExecutionLease(job, payload) {
  return { executionLeaseId: job.executionLeaseId, ...payload };
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
