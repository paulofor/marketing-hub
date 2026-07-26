import {
  analyzeSearchResults,
  buildSearchQueries,
  normalizeDuckDuckGoResponse,
} from "./research.js";

const backendBaseUrl = process.env.BACKEND_BASE_URL || "http://191.252.181.168";
const pollIntervalMs = Number(
  process.env.PRODUCT_DISCOVERY_POLL_INTERVAL_MS || "60000",
);
const maxSearchResults = Number(
  process.env.PRODUCT_DISCOVERY_MAX_SEARCH_RESULTS || "8",
);

async function main() {
  console.log("[product-discovery-worker] started");
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
    const results = await searchInternet(job);
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

async function searchInternet(job) {
  const queries = buildSearchQueries(job);
  const collected = [];
  for (const query of queries) {
    const url = new URL("https://api.duckduckgo.com/");
    url.searchParams.set("q", query);
    url.searchParams.set("format", "json");
    url.searchParams.set("no_html", "1");
    url.searchParams.set("skip_disambig", "1");
    const payload = await getJson(url.toString());
    collected.push(...normalizeDuckDuckGoResponse(payload));
    if (collected.length >= maxSearchResults) {
      break;
    }
  }
  if (collected.length === 0) {
    return fallbackResults(job);
  }
  return collected.slice(0, maxSearchResults);
}

function fallbackResults(job) {
  return [
    {
      title: `Pesquisa inicial sobre ${job.theme}`,
      url: "https://duckduckgo.com/",
      snippet: `Busca pública não retornou tópicos estruturados suficientes para ${job.theme}; pesquisar mais em comunidades, reviews e anúncios.`,
    },
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
