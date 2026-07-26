import { readFileSync } from "node:fs";

const PAIN_TERMS = [
  "dificuldade",
  "problema",
  "erro",
  "medo",
  "insegurança",
  "inseguranca",
  "frustração",
  "frustracao",
  "não consigo",
  "nao consigo",
  "como fazer",
  "reclamação",
  "reclamacao",
  "review",
];

const UNMET_TERMS = [
  "caro",
  "complicado",
  "demorado",
  "confuso",
  "manual",
  "consultoria",
  "não resolve",
  "nao resolve",
  "difícil",
  "dificil",
];

const HIGH_RISK_TERMS = [
  "cura",
  "tratamento",
  "diagnóstico médico",
  "diagnostico medico",
  "renda garantida",
  "lucro garantido",
  "processo judicial",
];

const DOMAIN_PAIN_QUERIES = [
  {
    match: ["roupa online", "comprar roupa", "compra online", "caimento"],
    queries: [
      "comprei roupa online e ficou ruim no corpo",
      "roupa online tamanho errado o que fazer",
      "me arrependi de comprar roupa online",
      "como saber se roupa online vai vestir bem",
      "roupa online não serviu reclamação",
    ],
  },
  {
    match: ["look", "looks", "rotina", "guardaroupa", "guarda roupa"],
    queries: [
      "não sei montar looks para trabalhar",
      "guarda roupa cheio e nada para vestir",
      "como montar looks com roupas que já tenho",
      "looks prontos para rotina corrida",
      "erro ao montar look do dia",
    ],
  },
  {
    match: ["estilo", "imagem pessoal", "consultoria", "presença", "presenca"],
    queries: [
      "consultoria de estilo é cara",
      "não sei qual é meu estilo",
      "diagnóstico de estilo online vale a pena",
      "imagem pessoal sem consultoria cara",
      "me sinto apagada com minhas roupas",
    ],
  },
];

const CONSUMER_LANGUAGE_TEMPLATES = [
  "não consigo resolver {base}",
  "tenho dificuldade com {base}",
  "como resolver {base} sem gastar muito",
  "{base} reclamação review",
  "{base} vale a pena fórum",
  "{base} antes e depois",
  "{base} erro comum",
  "{base} solução simples",
];

const MARKET_CONTEXT_TEMPLATES = [
  "{base} relato real",
  "{base} comentário de consumidor",
  "{base} dúvida comunidade",
  "{base} experiência negativa",
  "{base} alternativa barata",
];

export const SEARCH_PROVIDERS = {
  BRAVE: "brave",
  TAVILY: "tavily",
  SERPAPI: "serpapi",
  DUCKDUCKGO: "duckduckgo",
};

export function buildSearchQueries(job) {
  const base = normalizeSearchText(
    [job.theme, job.targetAudience].filter(Boolean).join(" "),
  );
  const domainQueries = inferDomainPainQueries(base);
  const genericQueries = CONSUMER_LANGUAGE_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const marketSignalQueries = MARKET_CONTEXT_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );

  return deduplicateQueries([
    ...domainQueries,
    ...genericQueries,
    ...marketSignalQueries,
  ]);
}

export function resolveSearchConfig(env = process.env) {
  const requestedProvider = normalizeProvider(env.PRODUCT_DISCOVERY_SEARCH_PROVIDER);
  const provider = requestedProvider || inferProvider(env);
  return {
    provider,
    braveApiKey: resolveSecret(env.BRAVE_SEARCH_API_KEY, env.BRAVE_SEARCH_API_KEY_FILE),
    tavilyApiKey: env.TAVILY_API_KEY || "",
    serpApiKey: env.SERPAPI_API_KEY || "",
    braveEndpoint:
      env.PRODUCT_DISCOVERY_BRAVE_SEARCH_ENDPOINT ||
      "https://api.search.brave.com/res/v1/web/search",
    tavilyEndpoint:
      env.PRODUCT_DISCOVERY_TAVILY_SEARCH_ENDPOINT ||
      "https://api.tavily.com/search",
    serpApiEndpoint:
      env.PRODUCT_DISCOVERY_SERPAPI_SEARCH_ENDPOINT ||
      "https://serpapi.com/search.json",
    country: env.PRODUCT_DISCOVERY_SEARCH_COUNTRY || "br",
    language: env.PRODUCT_DISCOVERY_SEARCH_LANGUAGE || "pt-br",
    userAgent:
      env.PRODUCT_DISCOVERY_SEARCH_USER_AGENT ||
      "MarketingHubProductDiscovery/1.0",
  };
}

function resolveSecret(value, filePath) {
  if (value) {
    return value;
  }
  if (!filePath) {
    return "";
  }
  try {
    return readFileSync(filePath, "utf8").trim();
  } catch {
    return "";
  }
}

export async function searchInternet(job, options = {}) {
  const config = options.config || resolveSearchConfig(options.env);
  const fetchFn = options.fetchFn || fetch;
  const logger = options.logger || console;
  const maxSearchResults = Number(options.maxSearchResults || 8);
  const minSearchQueries = Number(options.minSearchQueries || 4);
  const maxSearchQueries = Number(options.maxSearchQueries || 8);
  const maxResultsPerQuery = Number(options.maxResultsPerQuery || 2);
  const queries = buildSearchQueries(job).slice(0, maxSearchQueries);
  const collected = [];
  const providerErrors = [];
  let attemptedQueries = 0;
  for (const query of queries) {
    let queryResults = [];
    attemptedQueries += 1;
    try {
      queryResults = await searchQuery(query, config, fetchFn, logger);
    } catch (error) {
      if (!isSearchProviderHttpError(error)) {
        throw error;
      }
      providerErrors.push(error);
      logger.warn?.(
        "[product-discovery-worker] search query failed provider=%s query=%s status=%s error=%s",
        error.provider,
        query,
        error.status,
        error.message,
      );
      continue;
    }
    collected.push(...queryResults.slice(0, maxResultsPerQuery));
    const uniqueResults = deduplicateResults(collected);
    if (attemptedQueries >= minSearchQueries && uniqueResults.length >= maxSearchResults) {
      break;
    }
  }
  if (providerErrors.length === queries.length) {
    logger.warn?.(
      "[product-discovery-worker] all search queries failed provider=%s failures=%s",
      config.provider,
      providerErrors.length,
    );
  }
  if (collected.length === 0) {
    return fallbackResults(job, config.provider);
  }
  return deduplicateResults(collected).slice(0, maxSearchResults);
}

export function normalizeBraveResponse(payload) {
  const results = Array.isArray(payload?.web?.results) ? payload.web.results : [];
  return results
    .map((item) => ({
      title: cleanText(item.title),
      url: item.url,
      snippet: cleanText(item.description || item.extra_snippets?.join(" ") || ""),
    }))
    .filter(hasSearchResultShape);
}

export function normalizeTavilyResponse(payload) {
  const results = Array.isArray(payload?.results) ? payload.results : [];
  return results
    .map((item) => ({
      title: cleanText(item.title),
      url: item.url,
      snippet: cleanText(item.content || item.raw_content || ""),
    }))
    .filter(hasSearchResultShape);
}

export function normalizeSerpApiResponse(payload) {
  const results = Array.isArray(payload?.organic_results)
    ? payload.organic_results
    : [];
  return results
    .map((item) => ({
      title: cleanText(item.title),
      url: item.link,
      snippet: cleanText(item.snippet || item.rich_snippet?.top?.detected_extensions || ""),
    }))
    .filter(hasSearchResultShape);
}

export function analyzeSearchResults(job, results) {
  const evidence = results.slice(0, 12).map((result) => ({
    title: result.title,
    url: result.url,
    snippet: result.snippet,
  }));
  const combined = evidence
    .map((item) => `${item.title} ${item.snippet}`)
    .join(" ")
    .toLowerCase();
  const painHits = countHits(combined, PAIN_TERMS);
  const unmetHits = countHits(combined, UNMET_TERMS);
  const highRiskHits = countHits(combined, HIGH_RISK_TERMS);
  const independentDomains = new Set(
    evidence.map((item) => safeDomain(item.url)).filter(Boolean),
  ).size;
  const scaleScore = Math.min(35, independentDomains * 7 + painHits * 3);
  const unmetScore = Math.min(30, unmetHits * 5);
  const pdeScore = highRiskHits > 0 ? 5 : 25;
  const score = Math.min(100, scaleScore + unmetScore + pdeScore + 10);
  const decision =
    highRiskHits > 0
      ? "HUMAN_REVIEW"
      : score >= 70 && independentDomains >= 2
        ? "APPROVE"
        : score >= 45
          ? "RESEARCH_MORE"
          : "REJECT";

  return {
    decisionSummary: `Ciclo pesquisado com ${evidence.length} evidências públicas e ${independentDomains} domínios independentes. Principal decisão: ${decision}.`,
    opportunities: [
      {
        name: `PDE de alívio para ${job.theme}`,
        primaryAudience: job.targetAudience || job.theme,
        rootPain: `O público demonstra esforço recorrente para resolver ${job.theme} com clareza e baixo risco.`,
        practicalPain:
          "A dor aparece como excesso de tentativa, comparação, busca por orientação e dificuldade de transformar informação em ação.",
        emotionalPain:
          "A fricção tende a gerar insegurança, medo de errar e sensação de estar sozinho na decisão.",
        scaleEvidence: `${independentDomains} domínios independentes e ${painHits} sinais de dor recorrente foram encontrados nos resultados públicos.`,
        unmetnessEvidence: `${unmetHits} sinais sugerem soluções caras, confusas, demoradas ou incompletas.`,
        pdeExperience:
          "Experiência guiada em que o usuário informa sua situação, recebe diagnóstico simples, plano de ação e primeiro antes/depois aplicável.",
        firstCampaignAngle: `Pare de tentar resolver ${job.theme} no improviso: veja em poucos minutos qual é o próximo passo mais seguro.`,
        commercialRisk:
          highRiskHits > 0
            ? "Tema contém sinais sensíveis e exige revisão humana antes de qualquer experimento."
            : "Risco principal é a dor ainda estar ampla demais; validar linguagem específica antes de campanha.",
        evidenceJson: JSON.stringify(evidence),
        score,
        decision,
      },
    ],
  };
}

export function normalizeDuckDuckGoResponse(payload) {
  const related = Array.isArray(payload?.RelatedTopics)
    ? payload.RelatedTopics
    : [];
  return related.flatMap((item) => {
    if (Array.isArray(item.Topics)) {
      return item.Topics.map(toSearchResult).filter(Boolean);
    }
    const normalized = toSearchResult(item);
    return normalized ? [normalized] : [];
  });
}

function normalizeProvider(value) {
  const normalized = String(value || "").trim().toLowerCase();
  return Object.values(SEARCH_PROVIDERS).includes(normalized) ? normalized : "";
}

function inferProvider(env = process.env) {
  if (env.BRAVE_SEARCH_API_KEY || env.BRAVE_SEARCH_API_KEY_FILE) {
    return SEARCH_PROVIDERS.BRAVE;
  }
  if (env.TAVILY_API_KEY) {
    return SEARCH_PROVIDERS.TAVILY;
  }
  if (env.SERPAPI_API_KEY) {
    return SEARCH_PROVIDERS.SERPAPI;
  }
  return SEARCH_PROVIDERS.DUCKDUCKGO;
}

async function searchQuery(query, config, fetchFn, logger) {
  if (config.provider === SEARCH_PROVIDERS.BRAVE) {
    return searchBrave(query, config, fetchFn, logger);
  }
  if (config.provider === SEARCH_PROVIDERS.TAVILY) {
    return searchTavily(query, config, fetchFn, logger);
  }
  if (config.provider === SEARCH_PROVIDERS.SERPAPI) {
    return searchSerpApi(query, config, fetchFn, logger);
  }
  return searchDuckDuckGo(query, config, fetchFn, logger);
}

async function searchBrave(query, config, fetchFn, logger) {
  requireApiKey(config.braveApiKey, "BRAVE_SEARCH_API_KEY", config.provider);
  const url = new URL(config.braveEndpoint);
  url.searchParams.set("q", query);
  url.searchParams.set("country", config.country.toUpperCase());
  url.searchParams.set("search_lang", config.language.split("-")[0]);
  url.searchParams.set("count", "10");
  const payload = await getSearchJson(url.toString(), fetchFn, logger, config.provider, query, {
    Accept: "application/json",
    "Accept-Encoding": "gzip",
    "User-Agent": config.userAgent,
    "X-Subscription-Token": config.braveApiKey,
  });
  return normalizeBraveResponse(payload);
}

async function searchTavily(query, config, fetchFn, logger) {
  requireApiKey(config.tavilyApiKey, "TAVILY_API_KEY", config.provider);
  const payload = await postSearchJson(
    config.tavilyEndpoint,
    {
      query,
      search_depth: "basic",
      max_results: 10,
      include_answer: false,
      include_raw_content: false,
      topic: "general",
    },
    fetchFn,
    logger,
    config.provider,
    query,
    {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${config.tavilyApiKey}`,
    },
  );
  return normalizeTavilyResponse(payload);
}

async function searchSerpApi(query, config, fetchFn, logger) {
  requireApiKey(config.serpApiKey, "SERPAPI_API_KEY", config.provider);
  const url = new URL(config.serpApiEndpoint);
  url.searchParams.set("engine", "google");
  url.searchParams.set("q", query);
  url.searchParams.set("api_key", config.serpApiKey);
  url.searchParams.set("hl", config.language.split("-")[0]);
  url.searchParams.set("gl", config.country.toLowerCase());
  url.searchParams.set("num", "10");
  const payload = await getSearchJson(url.toString(), fetchFn, logger, config.provider, query);
  return normalizeSerpApiResponse(payload);
}

async function searchDuckDuckGo(query, config, fetchFn, logger) {
  const url = new URL("https://api.duckduckgo.com/");
  url.searchParams.set("q", query);
  url.searchParams.set("format", "json");
  url.searchParams.set("no_html", "1");
  url.searchParams.set("skip_disambig", "1");
  const payload = await getSearchJson(url.toString(), fetchFn, logger, config.provider, query);
  return normalizeDuckDuckGoResponse(payload);
}

async function getSearchJson(url, fetchFn, logger, provider, query, headers = {}) {
  const response = await fetchFn(url, {
    headers: { Accept: "application/json", ...headers },
  });
  if (!response.ok) {
    throw new SearchProviderHttpError(provider, query, response.status);
  }
  const payload = await response.json();
  logRawSearchPayload(logger, provider, query, payload);
  return payload;
}

async function postSearchJson(url, body, fetchFn, logger, provider, query, headers = {}) {
  const response = await fetchFn(url, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new SearchProviderHttpError(provider, query, response.status);
  }
  const payload = await response.json();
  logRawSearchPayload(logger, provider, query, payload);
  return payload;
}

function logRawSearchPayload(logger, provider, query, payload) {
  logger.info?.(
    "[product-discovery-worker] raw search payload received provider=%s query=%s payload=%s",
    provider,
    query,
    JSON.stringify(maskSecrets(payload)).slice(0, 12000),
  );
}

function requireApiKey(value, envName, provider) {
  if (!value) {
    throw new Error(`${provider} search requires ${envName}`);
  }
}

function fallbackResults(job, provider = SEARCH_PROVIDERS.DUCKDUCKGO) {
  return [
    {
      title: `Pesquisa inicial sobre ${job.theme}`,
      url: `https://search.brave.com/search?q=${encodeURIComponent(job.theme || "produto PDE")}`,
      snippet: `Busca ${provider} não retornou resultados estruturados suficientes para ${job.theme}; pesquisar mais em comunidades, reviews e anúncios.`,
    },
  ];
}

function toSearchResult(item) {
  if (!item?.FirstURL || !item?.Text) {
    return null;
  }
  return {
    title: item.Text.split(" - ")[0].slice(0, 160),
    url: item.FirstURL,
    snippet: item.Text,
  };
}

function deduplicateResults(results) {
  const seen = new Set();
  return results.filter((result) => {
    const key = `${safeDomain(result.url)}:${result.url}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function hasSearchResultShape(result) {
  return Boolean(result?.title && result?.url && result?.snippet);
}

function cleanText(value) {
  return String(value || "")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 500);
}

function normalizeSearchText(value) {
  return String(value || "")
    .replace(/(\d+)\+/g, "$1 anos ou mais")
    .replace(/[+]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function inferDomainPainQueries(base) {
  const normalized = normalizeForMatching(base);
  return DOMAIN_PAIN_QUERIES.filter((group) =>
    group.match.some((term) => normalized.includes(normalizeForMatching(term))),
  ).flatMap((group) => group.queries);
}

function deduplicateQueries(queries) {
  const seen = new Set();
  return queries
    .map(normalizeSearchText)
    .filter(Boolean)
    .filter((query) => {
      const key = normalizeForMatching(query);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
}

function normalizeForMatching(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

class SearchProviderHttpError extends Error {
  constructor(provider, query, status) {
    super(`${provider} search failed with status ${status}`);
    this.name = "SearchProviderHttpError";
    this.provider = provider;
    this.query = query;
    this.status = status;
  }
}

function isSearchProviderHttpError(error) {
  return error?.name === "SearchProviderHttpError";
}

function maskSecrets(value) {
  if (Array.isArray(value)) {
    return value.map(maskSecrets);
  }
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, childValue]) => [
        key,
        /token|api[_-]?key|authorization|secret/i.test(key)
          ? "[REDACTED]"
          : maskSecrets(childValue),
      ]),
    );
  }
  if (typeof value === "string") {
    return value.replace(
      /(api[_-]?key|access_token|token|secret)=([^&\s"]+)/gi,
      "$1=[REDACTED]",
    );
  }
  return value;
}

function countHits(text, terms) {
  return terms.reduce((total, term) => total + (text.includes(term) ? 1 : 0), 0);
}

function safeDomain(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}
