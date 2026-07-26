import {
  analyzeSearchResults,
  resolveSearchConfig,
  searchInternet,
} from "./research.js";

const backendBaseUrl = process.env.BACKEND_BASE_URL || "http://191.252.181.168";
const pollIntervalMs = Number(
  process.env.PRODUCT_DISCOVERY_POLL_INTERVAL_MS || "60000",
);
const maxSearchResults = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS || "8",
);
const searchConfig = resolveSearchConfig();

async function main() {
  console.log(
    `[product-discovery-worker] started searchProvider=${searchConfig.provider}`,
  );
  await runCycle();
  setInterval(runCycle, pollIntervalMs);
}

async function runCycle() {
  try {
    const pending = await getJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/pending`,
    );
    for (const job of pending) {
      await processJob(job);
    }
  } catch (error) {
    console.error("[product-discovery-worker] cycle failed", error);
  }
}

async function processJob(job) {
  console.log(
    `[product-discovery-worker] processing cycle=${job.cycleId} theme=${job.theme}`,
  );
  try {
    const results = await searchInternet(job, {
      config: searchConfig,
      maxSearchResults,
      logger: console,
    });
    const report = analyzeSearchResults(job, results);
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/complete`,
      report,
    );
    console.log(
      `[product-discovery-worker] completed cycle=${job.cycleId} opportunities=${report.opportunities.length}`,
    );
  } catch (error) {
    await postJson(
      `${backendBaseUrl}/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${job.cycleId}/fail`,
      { errorMessage: error.message || "Falha desconhecida na pesquisa PDE" },
    );
    console.error(
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
