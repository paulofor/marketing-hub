import {
  analyzeSearchResults,
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
  markPollStarted(healthState);
  try {
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
  }
}

async function processJob(job) {
  operationalLogger.info(
    `[product-discovery-worker] processing cycle=${job.cycleId} theme=${job.theme}`,
  );
  try {
    const directed = await planDirectedResearch(job);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/plan`,
      {
        planJson: JSON.stringify(directed.plan),
        rawResponse: directed.rawResponse,
        model: directed.model,
      },
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
    const report = analyzeSearchResults(job, results);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/complete`,
      report,
    );
    markCycleCompleted(healthState, job, report);
    operationalLogger.info(
      `[product-discovery-worker] completed cycle=${job.cycleId} opportunities=${report.opportunities.length}`,
    );
  } catch (error) {
    markCycleFailed(healthState, job, error);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/fail`,
      { errorMessage: error.message || "Falha desconhecida na pesquisa PDE" },
    );
    operationalLogger.error(
      `[product-discovery-worker] failed cycle=${job.cycleId}`,
      error,
    );
  }
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
