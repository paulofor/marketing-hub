import { readFileSync } from "node:fs";
import { buildPurchaseMomentResearchGate } from "./purchase-moment-gate.js";

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
  "tratamento médico",
  "tratamento medico",
  "tratamento de saúde",
  "tratamento de saude",
  "plano de tratamento",
  "prescrição",
  "prescricao",
  "diagnóstico médico",
  "diagnostico medico",
  "aconselhamento médico",
  "aconselhamento medico",
  "terapia",
  "renda garantida",
  "lucro garantido",
  "ganho garantido",
  "retorno garantido",
  "recomendação de investimento",
  "recomendacao de investimento",
  "aconselhamento financeiro",
  "processo judicial",
  "aconselhamento jurídico",
  "aconselhamento juridico",
];

const COMMERCIAL_INTENT_TERMS = [
  "preço",
  "preco",
  "comprar",
  "contratar",
  "curso",
  "consultoria",
  "anúncio",
  "anuncio",
  "vale a pena",
  "review",
  "alternativa",
];

const DOMAIN_PAIN_QUERIES = [
  {
    match: [
      "proposta",
      "propostas",
      "orcamento",
      "orçamento",
      "cotacao",
      "cotação",
    ],
    queries: [
      "software proposta comercial preço",
      "gerador de orçamento online preço",
      "aplicativo orçamento prestador mensalidade",
      "plataforma proposta comercial teste grátis",
      "sistema de propostas para autônomos preço",
      "app orçamento profissional assinatura",
    ],
  },
  {
    match: [
      "mei",
      "autonomo",
      "autonomos",
      "autônomo",
      "autônomos",
      "whatsapp",
      "vender pelo whatsapp",
      "cliente",
      "servico",
      "serviço",
      "cobrar",
      "follow up",
    ],
    queries: [
      "MEI não sabe vender pelo WhatsApp",
      "autônomo cliente pergunta preço e some",
      "tenho vergonha de cobrar cliente no WhatsApp",
      "como responder cliente interessado no WhatsApp",
      "como fazer follow up com cliente sem ser chato",
      "como vender serviço pelo WhatsApp sendo MEI",
      "não sei colocar preço no meu serviço",
      "como transformar serviço em oferta simples",
    ],
  },
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

const SOURCE_DISCOVERY_TEMPLATES = [
  "{base} site:reddit.com",
  "{base} site:quora.com",
  "{base} site:reclameaqui.com.br",
  "{base} site:youtube.com comentários",
  "{base} site:tiktok.com",
  "{base} site:instagram.com",
  "{base} anúncio",
  "{base} curso",
  "{base} comunidade",
  "{base} fórum",
];

const COMMERCIAL_SIGNAL_TEMPLATES = [
  "{base} cliente pergunta preço e some",
  "{base} vergonha de cobrar",
  "{base} não sei vender",
  "{base} resposta pronta",
  "{base} roteiro",
  "{base} objeção preço",
  "{base} antes e depois resultado",
];

const PAID_ALTERNATIVE_TEMPLATES = [
  "{base} curso online preço",
  "{base} programa online preço",
  "{base} aplicativo assinatura preço",
  "{base} mentoria online preço",
  "{base} consultoria online preço",
  "{base} produto digital comprar",
];

const PUBLIC_OFFER_SOURCE_TEMPLATES = [
  "site:apps.apple.com/br {base} premium assinatura",
  "site:play.google.com/store/apps {base} premium assinatura",
  "site:hotmart.com/pt-br/marketplace/produtos {base}",
  "site:udemy.com/course {base}",
  "site:kiwify.com.br {base} curso online",
];

const SCIENTIFIC_RESEARCH_TEMPLATES = [
  "{base} scientific study mechanism",
  "{base} peer reviewed study mechanism",
  "{base} systematic review intervention",
  "{base} evidence based behavior change",
  "{base} ciência aplicada mecanismo",
  "{base} estudo científico revisão sistemática",
  "{base} artigo científico intervenção",
  "{base} site:pubmed.ncbi.nlm.nih.gov",
  "{base} site:scielo.org",
  "{base} site:frontiersin.org",
  "{base} site:ncbi.nlm.nih.gov/pmc",
  "{base} site:doi.org",
];

const INSTAGRAM_B2C_TEMPLATES = [
  "{base} consumidor situação real Instagram",
  "{base} aplicativo preço review pessoa física",
  "{base} anúncio Instagram Reel demonstração",
  "{base} antes e depois experiência mobile",
  "{base} comentário cliente vale a pena",
  "{base} prazo urgente tentativa frustrada quanto pagou",
  "{base} assinatura cancelamento reclamação alternativa grátis",
  "{base} comparar preço comprar decisão iminente",
  "{base} sentir valorizado reconhecido pertencimento relato",
  "{base} cansativo trabalhoso prompt IA montar sozinho reclamação",
  "{base} solução pronta resultado imediato sem configurar",
];

const SCIENTIFIC_SOURCE_DOMAINS = [
  "pubmed.ncbi.nlm.nih.gov",
  "ncbi.nlm.nih.gov",
  "pmc.ncbi.nlm.nih.gov",
  "scielo.org",
  "frontiersin.org",
  "nature.com",
  "springer.com",
  "sciencedirect.com",
  "tandfonline.com",
  "wiley.com",
  "mdpi.com",
  "plos.org",
  "sagepub.com",
  "bmj.com",
  "jamanetwork.com",
  "thelancet.com",
  "doi.org",
  "researchgate.net",
];

export const SEARCH_PROVIDERS = {
  BRAVE: "brave",
  TAVILY: "tavily",
  SERPAPI: "serpapi",
  DUCKDUCKGO: "duckduckgo",
};

export function buildSearchQueries(job) {
  const base = marketFocusSearchBase(job);
  const domainQueries = inferDomainPainQueries(base);
  const genericQueries = CONSUMER_LANGUAGE_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const marketSignalQueries = MARKET_CONTEXT_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const sourceDiscoveryQueries = SOURCE_DISCOVERY_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const commercialSignalQueries = COMMERCIAL_SIGNAL_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const paidAlternativeQueries = PAID_ALTERNATIVE_TEMPLATES.map((template) =>
    template.replace("{base}", base),
  );
  const publicOfferSourceQueries = PUBLIC_OFFER_SOURCE_TEMPLATES.map(
    (template) => template.replace("{base}", base),
  );
  const genericScientificQueries = SCIENTIFIC_RESEARCH_TEMPLATES.map(
    (template) => template.replace("{base}", base),
  );
  const instagramB2cQueries = requiresConsumerInstagramFocus(job)
    ? INSTAGRAM_B2C_TEMPLATES.map((template) =>
        template.replace("{base}", base),
      )
    : [];
  const scientificResearchQueries = /propost|or[cç]ament|cota[cç]/i.test(base)
    ? [
        "information clarity trust purchase intention online service study",
        "information overload purchase decision clarity decision support study",
        "transparent AI decision support trust purchase intention study",
        ...genericScientificQueries,
      ]
    : genericScientificQueries;
  const referenceSourceQueries = buildReferenceSourceQueries(
    job.referenceSources,
    base,
  );
  const directed = classifyDirectedQueries(job.directedQueries);

  return deduplicateQueries([
    ...directed.commercial.slice(0, 3),
    ...paidAlternativeQueries.slice(0, 4),
    ...directed.scientific.slice(0, 2),
    ...scientificResearchQueries.slice(0, 2),
    ...publicOfferSourceQueries.slice(0, 4),
    ...directed.instagram.slice(0, 2),
    ...instagramB2cQueries.slice(0, 2),
    ...directed.other.slice(0, 4),
    ...referenceSourceQueries.slice(0, 2),
    ...domainQueries.slice(0, 2),
    ...commercialSignalQueries.slice(0, 2),
    ...directed.commercial.slice(3),
    ...paidAlternativeQueries.slice(4),
    ...publicOfferSourceQueries.slice(4),
    ...directed.scientific.slice(2),
    ...scientificResearchQueries.slice(2),
    ...directed.instagram.slice(2),
    ...instagramB2cQueries.slice(2),
    ...directed.other.slice(4),
    ...referenceSourceQueries.slice(2),
    ...domainQueries.slice(2),
    ...commercialSignalQueries.slice(2),
    ...genericQueries,
    ...marketSignalQueries,
    ...sourceDiscoveryQueries,
  ]);
}

/** Distribui as consultas dirigidas por finalidade para nenhuma família factual ficar no fim da fila. */
function classifyDirectedQueries(queries) {
  const classified = {
    commercial: [],
    scientific: [],
    instagram: [],
    other: [],
  };
  for (const query of Array.isArray(queries) ? queries : []) {
    const text = String(query || "");
    if (
      /pre[cç]o|comprar|contratar|quanto custa|curso|programa|assinatura|mensalidade|mentoria|consultoria|produto digital|oferta|checkout|hotmart|clickbank|pagar/i.test(
        text,
      )
    ) {
      classified.commercial.push(text);
    } else if (
      /scientific|study|peer.?review|systematic review|artigo cient[ií]fico|estudo cient[ií]fico|revis[aã]o sistem[aá]tica|pubmed|scielo|doi\.org/i.test(
        text,
      )
    ) {
      classified.scientific.push(text);
    } else if (/instagram|reels?|meta ad|biblioteca meta/i.test(text)) {
      classified.instagram.push(text);
    } else {
      classified.other.push(text);
    }
  }
  return classified;
}

/** Exige composição comercial e científica somente na descoberta factual ampla. */
function requiresBalancedDiscoveryCoverage(job) {
  return String(job?.researchMode || "").toUpperCase() === "DISCOVER_MARKETS";
}

/** Resume a cobertura que determina se a busca já pode encerrar sem perder fontes essenciais. */
function summarizeFactualSearchCoverage(results) {
  const uniqueResults = deduplicateResults(results || []);
  return {
    publicComparableOffers: extractPublicComparableOffers(uniqueResults).length,
    scientificArticles: uniqueResults.filter(isScientificArticleCandidate)
      .length,
    instagramResults: uniqueResults.filter(isInstagramSearchResult).length,
  };
}

/** Reserva espaço no relatório para ofertas, ciência e Instagram antes de preencher com relatos gerais. */
function selectBalancedSearchResults(results, maxSearchResults) {
  const uniqueResults = deduplicateResults(results || []);
  const selected = [];
  const selectedUrls = new Set();
  const append = (items, limit) => {
    for (const item of items) {
      if (selected.length >= maxSearchResults || limit <= 0) break;
      if (selectedUrls.has(item.url)) continue;
      selected.push(item);
      selectedUrls.add(item.url);
      limit -= 1;
    }
  };

  append(
    uniqueResults.filter(isPublicComparableOffer),
    Math.min(12, maxSearchResults),
  );
  append(
    uniqueResults.filter(isScientificArticleCandidate),
    Math.min(6, maxSearchResults - selected.length),
  );
  append(
    uniqueResults.filter(isInstagramSearchResult),
    Math.min(4, maxSearchResults - selected.length),
  );
  append(uniqueResults, maxSearchResults - selected.length);
  return selected;
}

/** Reconhece menções públicas ao canal sem confundi-las com a observação da Biblioteca Meta. */
function isInstagramSearchResult(result) {
  return (
    safeDomain(result?.url).includes("instagram.com") ||
    /\binstagram\b|\breels?\b/i.test(
      `${result?.title || ""} ${result?.snippet || ""}`,
    )
  );
}

export function resolveSearchConfig(env = process.env) {
  const requestedProvider = normalizeProvider(
    env.PRODUCT_DISCOVERY_SEARCH_PROVIDER,
  );
  const provider = requestedProvider || inferProvider(env);
  return {
    provider,
    braveApiKey: resolveSecret(
      env.BRAVE_SEARCH_API_KEY || env.BRAVE_API_KEY,
      env.BRAVE_SEARCH_API_KEY_FILE,
    ),
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
  const maxSearchResults = Number(options.maxSearchResults || 12);
  const minSearchQueries = Number(options.minSearchQueries || 6);
  const maxSearchQueries = Number(options.maxSearchQueries || 14);
  const maxResultsPerQuery = Number(options.maxResultsPerQuery || 3);
  const minimumPublicComparableOffers = Number(
    options.minimumPublicComparableOffers || 10,
  );
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
    const factualCoverage = summarizeFactualSearchCoverage(uniqueResults);
    if (
      attemptedQueries >= minSearchQueries &&
      uniqueResults.length >= maxSearchResults &&
      (!requiresBalancedDiscoveryCoverage(job) ||
        (factualCoverage.publicComparableOffers >=
          minimumPublicComparableOffers &&
          factualCoverage.scientificArticles > 0))
    ) {
      break;
    }
  }
  if (providerErrors.length === attemptedQueries && attemptedQueries > 0) {
    logger.warn?.(
      "[product-discovery-worker] all search queries failed provider=%s failures=%s",
      config.provider,
      providerErrors.length,
    );
    const statuses = [
      ...new Set(providerErrors.map((error) => error.status)),
    ].join(",");
    throw new Error(
      `Todas as consultas externas falharam; provider=${config.provider}; tentativas=${attemptedQueries}; status=${statuses || "indisponivel"}`,
    );
  }
  if (collected.length === 0) return [];
  const selected = selectBalancedSearchResults(collected, maxSearchResults);
  const coverage = summarizeFactualSearchCoverage(selected);
  logger.info?.(
    "[product-discovery-worker] factual search coverage queries=%s results=%s publicOffers=%s scientific=%s instagram=%s",
    attemptedQueries,
    selected.length,
    coverage.publicComparableOffers,
    coverage.scientificArticles,
    coverage.instagramResults,
  );
  return selected;
}

export function normalizeBraveResponse(payload) {
  const results = Array.isArray(payload?.web?.results)
    ? payload.web.results
    : [];
  return results
    .map((item) => ({
      title: cleanText(item.title),
      url: item.url,
      snippet: cleanText(
        item.description || item.extra_snippets?.join(" ") || "",
      ),
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
      snippet: cleanText(
        item.snippet || item.rich_snippet?.top?.detected_extensions || "",
      ),
    }))
    .filter(hasSearchResultShape);
}

/** Converte páginas comerciais públicas em alternativas comparáveis para formatos fora de marketplaces. */
export function extractPublicComparableOffers(results) {
  const byIdentity = new Map();
  for (const result of results || []) {
    if (!isPublicComparableOffer(result)) continue;
    const domain = safeDomain(result.url);
    const identity = publicOfferIdentity(result.url);
    if (!domain || !identity || byIdentity.has(identity)) continue;
    const priceMatch = `${result.title} ${result.snippet}`.match(
      /R\$\s?\d+(?:[.,]\d{1,2})?/i,
    );
    byIdentity.set(identity, {
      marketplace: "PUBLIC_WEB",
      referenceId: identity,
      title: result.title,
      url: result.url,
      description: result.snippet,
      producer: domain,
      price: priceMatch?.[0] || null,
      tractionSignal: null,
      rating: null,
      reviewCount: null,
      category: "PUBLIC_PAID_ALTERNATIVE",
      format: null,
      observations: 1,
      firstObservedAt: null,
      evidenceConfidence: priceMatch ? "MEDIUM" : "LOW",
      collectedAt: new Date().toISOString(),
      collectionJobId: null,
      signalDisclaimer:
        "Página comercial comprova uma alternativa ofertada, não vendas, satisfação ou tração.",
    });
  }
  return [...byIdentity.values()];
}

function isPublicComparableOffer(result) {
  if (!hasSearchResultShape(result) || isScientificArticleCandidate(result))
    return false;
  const url = new URL(result.url);
  const domain = url.hostname.toLowerCase();
  const path = url.pathname.toLowerCase();
  const text = `${result.title} ${result.snippet}`.toLowerCase();
  if (isOfficialAppStoreOffer(domain, path, text)) return true;
  if (
    /(^|\.)(reddit|youtube|facebook|instagram|tiktok|quora)\.com$/.test(
      domain,
    ) ||
    /(^|\.)(blog\.|capterra\.|getapp\.|techtudo\.|portalinsights\.|neon\.)/.test(
      domain,
    ) ||
    /\/(blog|artigo|articles|noticia|noticias|news|diversos|cotidiano|materia|perguntas|perguntas-frequentes|faq|guia|recursos|directory|listas|post|comparar)(\/|$)/.test(
      path,
    ) ||
    /\.(pdf|doc|docx)$/i.test(path) ||
    domain.endsWith(".gov.br") ||
    domain.endsWith(".jus.br")
  ) {
    return false;
  }
  if (/or[cç]amento pessoal|planejador.*or[cç]amento pessoal/.test(text))
    return false;
  const commercialSignal =
    /r\$\s?\d|pre[cç]o|planos?|assinatura|mensal|teste gr[aá]tis|comece gr[aá]tis|contratar|comprar|software|plataforma|aplicativo|\bapp\b/.test(
      text,
    );
  const productSignal =
    /proposta|or[cç]amento|cota[cç][aã]o|precifica[cç][aã]o|pacote de servi[cç]os|follow.?up/.test(
      text,
    );
  const broadOfferSignal =
    /comprar agora|assine|inscreva-se|matr[ií]cula|cursos? online|cursos? (?:de|em) |programa online|consultoria|mentoria|produto digital/.test(
      text,
    ) ||
    /\/(produto|products?|cursos?|courses?|programa|planos?|pricing|checkout)(\/|$)/.test(
      path,
    );
  return commercialSignal && (productSignal || broadOfferSignal);
}

/** Reconhece cada app oficial como alternativa própria, mesmo dentro do mesmo domínio de loja. */
function isOfficialAppStoreOffer(domain, path, text) {
  const officialListing =
    (domain === "play.google.com" && path === "/store/apps/details") ||
    (domain === "apps.apple.com" && /\/app\//.test(path));
  return (
    officialListing &&
    /premium|assinatura|compras? (?:no|dentro do) app|in.?app|pago|gratuito|gr[aá]tis|r\$/.test(
      text,
    )
  );
}

/** Mantém domínio para ofertas comuns e usa o identificador do produto nas lojas compartilhadas. */
function publicOfferIdentity(value) {
  try {
    const url = new URL(value);
    const domain = url.hostname.replace(/^www\./, "").toLowerCase();
    if (domain === "play.google.com" && url.pathname === "/store/apps/details") {
      const appId = url.searchParams.get("id");
      return appId ? `${domain}:${appId}`.slice(0, 255) : domain;
    }
    if (domain === "apps.apple.com") {
      const appId = url.pathname.match(/\/id(\d+)(?:\/|$)/)?.[1];
      return appId ? `${domain}:id${appId}` : domain;
    }
    if (
      /(^|\.)(hotmart\.com|udemy\.com|kiwify\.com\.br)$/.test(domain)
    ) {
      return `${domain}:${url.pathname.replace(/\/$/, "")}`.slice(0, 255);
    }
    return domain;
  } catch {
    return "";
  }
}

export function analyzeSearchResults(
  job,
  results,
  marketplaceOffers = null,
  options = {},
) {
  const evidence = results.map((result, index) => ({
    evidenceId: result.evidenceId || `P${index + 1}`,
    title: result.title,
    url: result.url,
    snippet: result.snippet,
  }));
  const scientificArticles = extractScientificArticles(results, job).slice(
    0,
    8,
  );
  const combined = evidence
    .map((item) => `${item.title} ${item.snippet}`)
    .join(" ")
    .toLowerCase();
  const commercialCombined = evidence
    .filter((item) => !isScientificArticleCandidate(item))
    .map((item) => `${item.title} ${item.snippet}`)
    .join(" ")
    .toLowerCase();
  const painHits = countHits(combined, PAIN_TERMS);
  const unmetHits = countHits(combined, UNMET_TERMS);
  const corpusHighRiskMentions = countHits(combined, HIGH_RISK_TERMS);
  const commercialIntentHits = countHits(
    commercialCombined,
    COMMERCIAL_INTENT_TERMS,
  );
  const directedMarketplaceResearch = Array.isArray(marketplaceOffers);
  const normalizedMarketplaceOffers = marketplaceOffers || [];
  const comparableMarketplaceOffers = normalizedMarketplaceOffers.filter(
    (offer) => offer.marketplace !== "META_AD_LIBRARY",
  );
  const metaAdEvidence = Array.isArray(options.metaAdEvidence)
    ? options.metaAdEvidence
    : [];
  const metaCoverage = Array.isArray(options.metaCoverage)
    ? options.metaCoverage
    : [];
  const metaCoverageSummary = summarizeMetaCoverage(metaCoverage);
  const instagramB2cRequired = requiresConsumerInstagramFocus(job);
  const instagramPublicEvidence = evidence.filter((item) => {
    const domain = safeDomain(item.url);
    const text = `${item.title} ${item.snippet}`;
    return (
      domain.includes("instagram.com") || /\binstagram\b|\breels?\b/i.test(text)
    );
  });
  const instagramB2cGatePassed =
    !instagramB2cRequired ||
    (metaAdEvidence.some(
      (item) => item.active && metaAdIncludesInstagram(item),
    ) &&
      metaCoverage.some(
        (coverage) =>
          coverage.publisherPlatform === "INSTAGRAM" &&
          coverage.sourceStatus === "OBSERVED" &&
          Number(coverage.activeAds || 0) > 0,
      ));
  const purchaseMomentGate = buildPurchaseMomentResearchGate(
    job,
    comparableMarketplaceOffers,
    {
      evaluatedAt: options.sourceEvaluatedAt,
      maxSourceAgeDays: options.maxSourceAgeDays,
    },
  );
  const minimumComparableOffers = Number(options.minimumComparableOffers || 10);
  const marketplaceGatePassed =
    !directedMarketplaceResearch ||
    comparableMarketplaceOffers.length >= minimumComparableOffers;
  const independentDomains = new Set(
    evidence.map((item) => safeDomain(item.url)).filter(Boolean),
  ).size;
  const scaleScore = Math.min(35, independentDomains * 7 + painHits * 3);
  const unmetScore = Math.min(30, unmetHits * 5);
  const scientificEvidenceScore = Math.min(10, scientificArticles.length * 3);
  const commercialScore = Math.min(15, commercialIntentHits * 3);
  const scoreWithoutPdeFit =
    scaleScore + unmetScore + scientificEvidenceScore + commercialScore;
  const mechanismEvidence =
    scientificArticles.length > 0
      ? `${scientificArticles.length} artigo(s) científico(s) candidato(s) coletado(s) para sustentar ou limitar o mecanismo.`
      : "Nenhum artigo científico candidato foi coletado; o mecanismo não deve ser tratado como validado antes de nova pesquisa científica.";

  const opportunityBlueprints = Array.isArray(options.candidateBlueprints)
    ? options.candidateBlueprints
    : [];
  const commercialRisk = !marketplaceGatePassed
    ? `Foram encontradas ${comparableMarketplaceOffers.length} de ${minimumComparableOffers} ofertas comparáveis; o dossiê deve permanecer bloqueado para enriquecimento.`
    : !instagramB2cGatePassed
      ? `${metaCoverageSummary}; ausência ou indisponibilidade da fonte não significa ausência de mercado.`
      : purchaseMomentGate.required && !purchaseMomentGate.sourceQualityPassed
        ? `As fontes comerciais não passaram pelo gate de qualidade: ${purchaseMomentGate.reasons.join(" ")}`
        : purchaseMomentGate.required
          ? "A pesquisa está pronta para protótipo privado, mas ainda não possui duas leituras consistentes de microvalor, uso do resultado pronto sem montagem, preferência sobre o gratuito e avanço ao checkout."
          : scientificArticles.length === 0
            ? "Sem sustentação científica candidata do mecanismo; nova pesquisa é obrigatória antes de campanha."
            : commercialIntentHits === 0
              ? "Não há sinal verificável de intenção de compra; pesquisar preços, concorrentes, reviews e anúncios antes de campanha."
              : "Evitar extrapolar evidência científica para promessa absoluta e validar disposição de compra em experimento controlado.";

  const opportunities = opportunityBlueprints.map((blueprint) => {
      const evidenceIds = new Set(blueprint.evidenceIds || []);
      const referenced = (items) =>
        (items || []).filter((item) => evidenceIds.has(item.evidenceId));
      const referencedPublicEvidence = referenced(evidence);
      const referencedMarketplaceOffers = referenced(comparableMarketplaceOffers);
      const referencedMetaAdEvidence = referenced(metaAdEvidence);
      const candidatePublicDomains = new Set(
        referencedPublicEvidence
          .map((item) => safeDomain(item.url))
          .filter(Boolean),
      );
      const candidateHighRiskHits = countCandidateDeliveryRisk(blueprint);
      const score = Math.min(
        100,
        scoreWithoutPdeFit + (candidateHighRiskHits > 0 ? 5 : 25),
      );
      const candidateEvidenceReady =
        candidatePublicDomains.size >= 2 &&
        referencedMarketplaceOffers.length > 0 &&
        referencedMetaAdEvidence.some(
          (item) => item.active && metaAdIncludesInstagram(item),
        );
      const candidateDecision = factualCandidateDecision({
        evidenceCount: evidence.length,
        marketplaceGatePassed,
        instagramB2cGatePassed,
        candidateHighRiskHits,
        purchaseMomentGate,
        scientificArticleCount: scientificArticles.length,
        commercialIntentHits,
        score,
        independentDomains,
      });
      const maturity = effectiveCandidateMaturity(blueprint.maturity, {
        discoveryMode: job.researchMode === "DISCOVER_MARKETS",
        directedMarketplaceResearch,
        instagramB2cRequired,
        marketplaceGatePassed,
        instagramB2cGatePassed,
        candidateEvidenceReady,
        scientificEvidenceReady: scientificArticles.length > 0,
        commercialEvidenceReady: commercialIntentHits > 0,
        score,
        decision: candidateDecision,
      });
      const decision =
        maturity === "HUMAN_REVIEW"
          ? "HUMAN_REVIEW"
          : maturity === "REJECTED"
            ? "REJECT"
            : maturity === "DOSSIER_READY"
              ? candidateDecision
              : "RESEARCH_MORE";
      const candidateRisk =
        candidateHighRiskHits > 0
          ? "A entrega proposta contém sinal de alto risco e exige revisão humana antes de qualquer experimento."
          : "";
      return {
        name: blueprint.name,
        primaryAudience: blueprint.primaryAudience,
        rootPain: blueprint.rootPain,
        practicalPain: blueprint.practicalPain,
        emotionalPain: blueprint.emotionalPain,
        scaleEvidence: blueprint.scaleEvidence,
        unmetnessEvidence: blueprint.unmetnessEvidence,
        pdeExperience: `Fronteira factual para avaliação da Atena: ${blueprint.pdeValueBoundary} Entrada mínima: ${blueprint.pdeDeliveryFit.minimumInput} Trabalho da IA nos bastidores: ${blueprint.pdeDeliveryFit.aiBackstageWork} Resultado digital pronto: ${blueprint.pdeDeliveryFit.readyDigitalOutcome} Base científica candidata: ${mechanismEvidence}`,
        firstCampaignAngle: null,
        commercialRisk:
          `${blueprint.commercialRisk} ${candidateRisk} ${commercialRisk}`.trim(),
        evidenceJson: JSON.stringify({
          candidateEvidence: {
            purchaseSituation: blueprint.purchaseSituation,
            observedLanguage: blueprint.observedLanguage,
            currentAlternatives: blueprint.currentAlternatives,
            residualEffort: blueprint.residualEffort,
            pdeDeliveryFit: blueprint.pdeDeliveryFit,
            instagramFitEvidence: blueprint.instagramFitEvidence,
            evidenceIds: blueprint.evidenceIds,
            maturity,
          },
          referencedEvidence: {
            publicEvidence: referencedPublicEvidence,
            marketplaceOffers: referencedMarketplaceOffers,
            metaAdEvidence: referencedMetaAdEvidence,
            repositoryEvidence: referenced(options.repositoryEvidence),
          },
          publicEvidence: evidence,
          marketplaceOffers: comparableMarketplaceOffers,
          metaAdEvidence,
          repositoryEvidence: options.repositoryEvidence || [],
          metaCoverage,
          metaAdInterpretation:
            "Atividade e longevidade sugerem investimento sustentado, mas não comprovam vendas isoladamente.",
          instagramB2cRequired,
          instagramB2cGatePassed,
          instagramPublicEvidence,
          purchaseMomentGate,
          scientificArticles,
          commercialIntentHits,
          candidateReadiness: {
            independentPublicPaths: candidatePublicDomains.size,
            referencedComparableOffers: referencedMarketplaceOffers.length,
            referencedActiveInstagramAds: referencedMetaAdEvidence.filter(
              (item) => item.active && metaAdIncludesInstagram(item),
            ).length,
            highRiskHits: candidateHighRiskHits,
          },
        }),
        score,
        maturity,
        decision,
      };
    });
  const decision = aggregateOpportunityDecision(opportunities, {
    evidenceCount: evidence.length,
    marketplaceGatePassed,
    instagramB2cGatePassed,
  });
  const dossierReadyCount = opportunities.filter(
    (opportunity) => opportunity.maturity === "DOSSIER_READY",
  ).length;
  const maturitySummary =
    dossierReadyCount > 0
      ? `${dossierReadyCount} DOSSIER_READY`
      : decision;

  return {
    decisionSummary: `Ciclo pesquisado com ${evidence.length} evidências públicas, ${comparableMarketplaceOffers.length} ofertas comparáveis, ${metaAdEvidence.length} anúncios Meta/Instagram aderentes, ${metaCoverageSummary}, ${instagramPublicEvidence.length} evidências públicas auxiliares de Instagram, ${independentDomains} domínios independentes, ${scientificArticles.length} artigos científicos candidatos e ${commercialIntentHits} sinais de intenção comercial. Validação do momento de compra: ${purchaseMomentGate.status}. Maturidade factual: ${maturitySummary}.`,
    opportunities,
    evidenceReport: {
      researchMode: job.researchMode || "VALIDATE_MARKET",
      marketType: job.marketType || "UNSPECIFIED",
      publicEvidence: evidence,
      marketplaceOffers: comparableMarketplaceOffers,
      metaAdEvidence,
      metaCoverage,
      repositoryEvidence: options.repositoryEvidence || [],
      repositoryCoverage: options.repositoryCoverage || [],
      analysisMode: options.analysisMode || "DETERMINISTIC",
      analysisModel: options.analysisModel || null,
      sourceMetrics: {
        independentDomains,
        painHits,
        unmetHits,
        commercialIntentHits,
        scientificArticleCount: scientificArticles.length,
        corpusHighRiskMentions,
      },
      gates: {
        marketplaceGatePassed,
        instagramB2cGatePassed,
        purchaseMomentStatus: purchaseMomentGate.status,
      },
    },
  };
}

/** Decide a candidata sem propagar riscos encontrados apenas nas fontes das demais. */
function factualCandidateDecision({
  evidenceCount,
  marketplaceGatePassed,
  instagramB2cGatePassed,
  candidateHighRiskHits,
  purchaseMomentGate,
  scientificArticleCount,
  commercialIntentHits,
  score,
  independentDomains,
}) {
  if (evidenceCount === 0) return "RESEARCH_MORE";
  if (!marketplaceGatePassed || !instagramB2cGatePassed) {
    return "RESEARCH_MORE";
  }
  if (candidateHighRiskHits > 0) return "HUMAN_REVIEW";
  if (
    purchaseMomentGate.required &&
    !purchaseMomentGate.finalPrioritizationEligible
  ) {
    return "RESEARCH_MORE";
  }
  if (scientificArticleCount === 0 || commercialIntentHits === 0) {
    return "RESEARCH_MORE";
  }
  if (score >= 70 && independentDomains >= 2) return "APPROVE";
  return score >= 45 ? "RESEARCH_MORE" : "REJECT";
}

/** Conta risco somente na entrega proposta, não em dores, ressalvas ou corpus de mercado. */
function countCandidateDeliveryRisk(blueprint = {}) {
  const delivery = [
    blueprint.name,
    blueprint.pdeDeliveryFit?.aiBackstageWork,
    blueprint.pdeDeliveryFit?.readyDigitalOutcome,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  return countHits(delivery, HIGH_RISK_TERMS);
}

/** Resume decisões individuais sem deixar uma candidata sensível contaminar as demais. */
function aggregateOpportunityDecision(
  opportunities,
  { evidenceCount, marketplaceGatePassed, instagramB2cGatePassed },
) {
  if (
    evidenceCount === 0 ||
    !marketplaceGatePassed ||
    !instagramB2cGatePassed ||
    opportunities.length === 0
  ) {
    return "RESEARCH_MORE";
  }
  if (opportunities.some((item) => item.decision === "APPROVE")) {
    return "APPROVE";
  }
  if (opportunities.every((item) => item.decision === "HUMAN_REVIEW")) {
    return "HUMAN_REVIEW";
  }
  if (opportunities.every((item) => item.decision === "REJECT")) {
    return "REJECT";
  }
  return "RESEARCH_MORE";
}

/** Traduz os estados técnicos da Biblioteca Meta para uma conclusão comercial auditável. */
function summarizeMetaCoverage(metaCoverage) {
  const statuses = metaCoverage
    .map((item) => String(item?.sourceStatus || ""))
    .filter(Boolean);
  const attempts = statuses.length;
  if (attempts === 0) return "cobertura Meta/Instagram não solicitada";
  if (statuses.includes("UNAVAILABLE")) {
    return `cobertura Meta/Instagram não executada em ${attempts} tentativa(s) por falha de integração`;
  }
  if (statuses.some((status) => status.startsWith("AWAITING_"))) {
    return `cobertura Meta/Instagram aguardando observação em ${attempts} tentativa(s)`;
  }
  if (statuses.includes("OBSERVED")) {
    return `cobertura Meta/Instagram observada em ${attempts} tentativa(s)`;
  }
  if (
    statuses.every((status) =>
      [
        "NO_MATCHING_ACTIVE_ADS",
        "NO_ACTIVE_ADS",
        "NO_RELEVANT_PLATFORM_EVIDENCE",
      ].includes(status),
    )
  ) {
    return `cobertura Meta/Instagram executada em ${attempts} consulta(s), sem anúncio ativo aderente`;
  }
  return `cobertura Meta/Instagram sem resultado auditável em ${attempts} tentativa(s)`;
}

/** Confirma no anúncio a distribuição explícita no Instagram, sem inferir pela consulta. */
function metaAdIncludesInstagram(ad) {
  if (String(ad?.publisherPlatform || "").toUpperCase() === "INSTAGRAM") {
    return true;
  }
  return (ad?.publisherPlatforms || []).some(
    (platform) => String(platform).toUpperCase() === "INSTAGRAM",
  );
}

/** Impede que uma classificação do modelo ultrapasse os gates factuais recalculados pelo worker. */
function effectiveCandidateMaturity(
  declaredMaturity,
  {
    discoveryMode,
    directedMarketplaceResearch,
    instagramB2cRequired,
    marketplaceGatePassed,
    instagramB2cGatePassed,
    candidateEvidenceReady,
    scientificEvidenceReady,
    commercialEvidenceReady,
    score,
    decision,
  },
) {
  const declared = declaredMaturity || "SIGNAL";
  if (decision === "HUMAN_REVIEW") return "HUMAN_REVIEW";
  if (decision === "REJECT") return "REJECTED";
  if (["HUMAN_REVIEW", "REJECTED"].includes(declared)) return declared;
  const globalGatesPassed =
    (!directedMarketplaceResearch || marketplaceGatePassed) &&
    (!instagramB2cRequired || instagramB2cGatePassed) &&
    scientificEvidenceReady &&
    commercialEvidenceReady;
  const discoveryGatesPassed =
    globalGatesPassed && (!discoveryMode || candidateEvidenceReady);
  if (declared === "DOSSIER_READY" && !discoveryGatesPassed) {
    return "RESEARCHABLE";
  }
  if (
    discoveryMode &&
    declared === "RESEARCHABLE" &&
    discoveryGatesPassed &&
    score >= 45
  ) {
    return "DOSSIER_READY";
  }
  return declared;
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
  const normalized = String(value || "")
    .trim()
    .toLowerCase();
  return Object.values(SEARCH_PROVIDERS).includes(normalized) ? normalized : "";
}

function inferProvider(env = process.env) {
  if (
    env.BRAVE_SEARCH_API_KEY ||
    env.BRAVE_API_KEY ||
    env.BRAVE_SEARCH_API_KEY_FILE
  ) {
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

/** Mantém o tema central curto para que cada template acrescente uma única intenção. */
function compactSearchBase(value) {
  const words = String(value || "")
    .split(/\s+/)
    .filter(Boolean);
  return words.slice(0, 18).join(" ").slice(0, 140).trim();
}

/** Retira o recorte demográfico das consultas quando o briefing já declara um foco amplo. */
function marketFocusSearchBase(job = {}) {
  const theme = normalizeSearchText(job.theme);
  const declaredFocus = theme.match(/\bfoco em\s+(.+)$/i)?.[1];
  if (declaredFocus) {
    return compactSearchBase(declaredFocus.replace(/[.!?]+$/, ""));
  }
  return compactSearchBase(
    normalizeSearchText(
      [job.theme, job.targetAudience].filter(Boolean).join(" "),
    ),
  );
}

/** Pesquisa fontes editoriais declaradas por domínio sem acessar área autenticada. */
function buildReferenceSourceQueries(referenceSources, base) {
  return String(referenceSources || "")
    .split(/[\n,]+/)
    .map((source) => source.trim())
    .filter(Boolean)
    .flatMap((source) => {
      try {
        const domain = new URL(source).hostname.replace(/^www\./, "");
        return [
          `site:${domain} ${base}`,
          `site:${domain} ${base} desejo problema comportamento`,
        ];
      } catch {
        return [];
      }
    })
    .slice(0, 6);
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
  requireApiKey(
    config.braveApiKey,
    "BRAVE_SEARCH_API_KEY ou BRAVE_API_KEY",
    config.provider,
  );
  const normalizedQuery = normalizeBraveQuery(query);
  const url = new URL(config.braveEndpoint);
  url.searchParams.set("q", normalizedQuery);
  url.searchParams.set("country", config.country.toUpperCase());
  url.searchParams.set(
    "search_lang",
    normalizeBraveSearchLanguage(config.language),
  );
  url.searchParams.set("count", "10");
  const headers = {
    Accept: "application/json",
    "Accept-Encoding": "gzip",
    "User-Agent": config.userAgent,
    "X-Subscription-Token": config.braveApiKey,
  };
  let payload;
  try {
    payload = await getSearchJson(
      url.toString(),
      fetchFn,
      logger,
      config.provider,
      query,
      headers,
    );
  } catch (error) {
    if (!isSearchProviderHttpError(error) || error.status !== 422) {
      throw error;
    }
    const minimalUrl = new URL(config.braveEndpoint);
    minimalUrl.searchParams.set("q", normalizedQuery);
    logger.warn?.(
      "[product-discovery-worker] Brave rejected localized request; retrying minimal contract query=%s detail=%s",
      normalizedQuery,
      error.responseDetail || "indisponivel",
    );
    payload = await getSearchJson(
      minimalUrl.toString(),
      fetchFn,
      logger,
      config.provider,
      query,
      headers,
    );
  }
  return normalizeBraveResponse(payload);
}

/** Preserva os locales aceitos pela Brave, principalmente o contrato brasileiro pt-br. */
function normalizeBraveSearchLanguage(language) {
  const normalized = String(language || "pt-br")
    .trim()
    .toLowerCase();
  return normalized === "pt" ? "pt-br" : normalized;
}

export function normalizeBraveQuery(query) {
  const words = String(query || "")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  const wordLimited =
    words.length <= 50 ? words : [...words.slice(0, 36), ...words.slice(-14)];
  const normalized = wordLimited.join(" ");
  if (Array.from(normalized).length <= 400) {
    return normalized;
  }

  const intent = wordLimited.slice(-14).join(" ");
  const availablePrefixLength = Math.max(
    1,
    400 - Array.from(intent).length - 1,
  );
  const prefix = Array.from(wordLimited.slice(0, -14).join(" "))
    .slice(0, availablePrefixLength)
    .join("")
    .replace(/\s+\S*$/, "")
    .trim();
  return `${prefix} ${intent}`.trim();
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
  const payload = await getSearchJson(
    url.toString(),
    fetchFn,
    logger,
    config.provider,
    query,
  );
  return normalizeSerpApiResponse(payload);
}

async function searchDuckDuckGo(query, config, fetchFn, logger) {
  const url = new URL("https://api.duckduckgo.com/");
  url.searchParams.set("q", query);
  url.searchParams.set("format", "json");
  url.searchParams.set("no_html", "1");
  url.searchParams.set("skip_disambig", "1");
  const payload = await getSearchJson(
    url.toString(),
    fetchFn,
    logger,
    config.provider,
    query,
  );
  return normalizeDuckDuckGoResponse(payload);
}

async function getSearchJson(
  url,
  fetchFn,
  logger,
  provider,
  query,
  headers = {},
) {
  const response = await fetchFn(url, {
    headers: { Accept: "application/json", ...headers },
  });
  if (!response.ok) {
    const responseDetail = await readProviderErrorDetail(response);
    logger.warn?.(
      "[product-discovery-worker] search response rejected provider=%s url=%s status=%s detail=%s",
      provider,
      sanitizeSearchUrl(url),
      response.status,
      responseDetail || "indisponivel",
    );
    throw new SearchProviderHttpError(
      provider,
      query,
      response.status,
      responseDetail,
    );
  }
  const payload = await response.json();
  logRawSearchPayload(logger, provider, query, payload);
  return payload;
}

async function readProviderErrorDetail(response) {
  try {
    const payload = await response.text();
    return JSON.stringify(maskSecrets(JSON.parse(payload))).slice(0, 2000);
  } catch {
    return "resposta_sem_detalhe_json";
  }
}

function sanitizeSearchUrl(value) {
  const url = new URL(value);
  for (const key of url.searchParams.keys()) {
    if (/token|api[_-]?key|authorization|secret/i.test(key)) {
      url.searchParams.set(key, "[REDACTED]");
    }
  }
  return url.toString();
}

async function postSearchJson(
  url,
  body,
  fetchFn,
  logger,
  provider,
  query,
  headers = {},
) {
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

function extractScientificArticles(results, job) {
  return deduplicateResults(results)
    .filter(
      (result) =>
        isScientificArticleCandidate(result) &&
        isScientificMechanismRelevant(result, job),
    )
    .map((result) => {
      const summary =
        result.snippet || "Resumo indisponível no resultado de busca.";
      return {
        link: result.url,
        originalTitle: result.title,
        portugueseTitle: translateScientificTitleHeuristically(result.title),
        summary,
        mechanismApplication: buildMechanismApplication(result, job),
      };
    });
}

function isScientificMechanismRelevant(result, job) {
  const context = `${job?.theme || ""} ${job?.targetAudience || ""}`;
  if (!/propost|or[cç]ament|cota[cç]/i.test(context)) return true;
  const text = `${result.title} ${result.snippet}`.toLowerCase();
  const decisionMechanism =
    /purchase decision|purchase intention|decision support|information overload/.test(
      text,
    );
  const explanatoryMechanism =
    /clarity|trust|price|information processing|uncertainty|transparency|cognitive load/.test(
      text,
    );
  return decisionMechanism && explanatoryMechanism;
}

function isScientificArticleCandidate(result) {
  const domain = safeDomain(result.url);
  const text = `${result.title} ${result.snippet}`.toLowerCase();
  return (
    SCIENTIFIC_SOURCE_DOMAINS.some((sourceDomain) =>
      domain.includes(sourceDomain),
    ) ||
    /\b(pubmed|doi|systematic review|meta-analysis|clinical trial|peer reviewed|journal|estudo científico|artigo científico|revisão sistemática)\b/i.test(
      text,
    )
  );
}

function translateScientificTitleHeuristically(title) {
  const normalized = cleanText(title);
  if (
    /[áéíóúâêôãõç]/i.test(normalized) ||
    /\b(de|da|do|para|com|em|sobre)\b/i.test(normalized)
  ) {
    return normalized;
  }
  return `Título em português a revisar: ${normalized}`;
}

function buildMechanismApplication(result, job) {
  const theme = cleanText(job.theme || "o tema pesquisado");
  const finding = cleanText(result.snippet || result.title);
  return `Usar este artigo como evidência candidata para explicar qual mecanismo causal pode reduzir a dor em ${theme}. A aplicação no produto deve traduzir o achado em diagnóstico, recomendação prática e limite de promessa, sem afirmar resultado garantido. Evidência localizada: ${finding}`;
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

/** Ativa o gate de canal somente quando o ciclo declara Instagram e consumidor final. */
function requiresConsumerInstagramFocus(job) {
  return (
    /instagram/i.test(String(job?.acquisitionChannel || "")) &&
    (job?.marketType === "B2C" ||
      /\bb2c\b|consumidor|pessoa f[ií]sica/i.test(
        `${job?.commercialConstraints || ""} ${job?.targetAudience || ""}`,
      ))
  );
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
  constructor(provider, query, status, responseDetail = "") {
    super(`${provider} search failed with status ${status}`);
    this.name = "SearchProviderHttpError";
    this.provider = provider;
    this.query = query;
    this.status = status;
    this.responseDetail = responseDetail;
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
  return terms.reduce(
    (total, term) => total + (text.includes(term) ? 1 : 0),
    0,
  );
}

function safeDomain(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}
