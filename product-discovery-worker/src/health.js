import { createServer } from "node:http";

export function createHealthState(now = new Date()) {
  return {
    startedAt: now.toISOString(),
    lastPollAt: null,
    lastPollStatus: "NOT_RUN",
    lastPollError: null,
    lastCycleProcessed: null,
    lastMetaPublicBrowserCollection: null,
  };
}

export function markPollStarted(state, now = new Date()) {
  state.lastPollAt = now.toISOString();
  state.lastPollStatus = "RUNNING";
  state.lastPollError = null;
}

export function markPollCompleted(state, now = new Date()) {
  state.lastPollAt = now.toISOString();
  state.lastPollStatus = "COMPLETED";
  state.lastPollError = null;
}

export function markPollFailed(state, error, now = new Date()) {
  state.lastPollAt = now.toISOString();
  state.lastPollStatus = "FAILED";
  state.lastPollError = error?.message || "Falha desconhecida no ciclo";
}

export function markCycleCompleted(state, job, report, now = new Date()) {
  state.lastCycleProcessed = {
    cycleId: job.cycleId,
    theme: job.theme || null,
    targetAudience: job.targetAudience || null,
    status: "COMPLETED",
    processedAt: now.toISOString(),
    opportunitiesCount: Array.isArray(report?.opportunities)
      ? report.opportunities.length
      : 0,
  };
  const metaCoverage = Array.isArray(report?.evidenceReport?.metaCoverage)
    ? report.evidenceReport.metaCoverage
    : Array.isArray(report?.metaCoverage)
      ? report.metaCoverage
      : [];
  const browserCoverage = metaCoverage.find((item) =>
    ["PUBLIC_BROWSER", "SUPERVISED"].includes(item?.collectionMode),
  );
  if (browserCoverage) {
    state.lastMetaPublicBrowserCollection = {
      cycleId: job.cycleId,
      investigationId: browserCoverage.investigationId ?? null,
      sourceStatus: browserCoverage.sourceStatus || "UNKNOWN",
      collectionMode: browserCoverage.collectionMode,
      adsObserved: Number(browserCoverage.adsObserved || 0),
      activeAds: Number(browserCoverage.activeAds || 0),
      advertisersObserved: Number(browserCoverage.advertisersObserved || 0),
      processedAt: now.toISOString(),
    };
  }
}

export function markCycleFailed(state, job, error, now = new Date()) {
  state.lastCycleProcessed = {
    cycleId: job.cycleId,
    theme: job.theme || null,
    targetAudience: job.targetAudience || null,
    status: "FAILED",
    processedAt: now.toISOString(),
    errorMessage: error?.message || "Falha desconhecida na pesquisa PDE",
  };
}

export function createHealthPayload({
  searchConfig,
  state,
  pollIntervalMs,
  maxSearchResults,
  env = process.env,
}) {
  const braveKeyConfigured = Boolean(searchConfig.braveApiKey);
  const activeProviderRequiresMissingKey =
    searchConfig.provider === "brave" && !braveKeyConfigured;

  return {
    service: "product-discovery-worker",
    status:
      state.lastPollStatus === "FAILED" || activeProviderRequiresMissingKey
        ? "DEGRADED"
        : "UP",
    startedAt: state.startedAt,
    activeSearchProvider: searchConfig.provider,
    braveSearch: {
      keyStatus: braveKeyConfigured ? "CONFIGURED" : "MISSING",
      keySource: resolveBraveKeySource(env),
    },
    metaPublicBrowser: {
      enabled:
        String(env.ARGOS_META_BROWSER_ENABLED ?? "true").toLowerCase() ===
        "true",
      engine: "chromium",
      lastCollection: state.lastMetaPublicBrowserCollection,
    },
    polling: {
      intervalMs: pollIntervalMs,
      maxSearchResults,
      lastPollAt: state.lastPollAt,
      lastPollStatus: state.lastPollStatus,
      lastPollError: state.lastPollError,
    },
    lastCycleProcessed: state.lastCycleProcessed,
  };
}

export function startHealthServer({
  host = "0.0.0.0",
  port = 8080,
  searchConfig,
  state,
  pollIntervalMs,
  maxSearchResults,
  logger = console,
  env = process.env,
  logLines = () => [],
}) {
  const server = createServer((request, response) => {
    const path = new URL(request.url, "http://localhost").pathname;
    if (
      request.method === "GET" &&
      path === "/ops-product-discovery-observability-v1/logfile"
    ) {
      response.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
      response.end(`${logLines().join("\n")}\n`);
      return;
    }
    if (request.method !== "GET" || !["/healthz", "/health"].includes(path)) {
      response.writeHead(404, { "Content-Type": "application/json" });
      response.end(JSON.stringify({ error: "not_found" }));
      return;
    }

    const payload = createHealthPayload({
      searchConfig,
      state,
      pollIntervalMs,
      maxSearchResults,
      env,
    });
    response.writeHead(payload.status === "UP" ? 200 : 503, {
      "Content-Type": "application/json",
    });
    response.end(JSON.stringify(payload));
  });

  server.listen(port, host, () => {
    logger.info?.(
      "[product-discovery-worker] health server listening host=%s port=%s",
      host,
      port,
    );
  });

  return server;
}

function resolveBraveKeySource(env) {
  if (env.BRAVE_SEARCH_API_KEY || env.BRAVE_API_KEY) {
    return "env";
  }
  if (env.BRAVE_SEARCH_API_KEY_FILE) {
    return "file";
  }
  return "none";
}
