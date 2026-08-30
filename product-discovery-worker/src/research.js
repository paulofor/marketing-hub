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
  "tratamento",
  "diagnóstico médico",
  "diagnostico medico",
  "renda garantida",
  "lucro garantido",
  "processo judicial",
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
  const base = compactSearchBase(normalizeSearchText(
    [job.theme, job.targetAudience].filter(Boolean).join(" "),
  ));
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

  return deduplicateQueries([
    ...(Array.isArray(job.directedQueries)
      ? job.directedQueries.slice(0, 8)
      : []),
    ...referenceSourceQueries,
    ...instagramB2cQueries,
    ...domainQueries.slice(0, 2),
    ...commercialSignalQueries.slice(0, 2),
    ...scientificResearchQueries.slice(0, 3),
    ...domainQueries.slice(2),
    ...commercialSignalQueries.slice(2),
    ...scientificResearchQueries.slice(2),
    ...genericQueries,
    ...marketSignalQueries,
    ...sourceDiscoveryQueries,
  ]);
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
    if (
      attemptedQueries >= minSearchQueries &&
      uniqueResults.length >= maxSearchResults
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
  return deduplicateResults(collected).slice(0, maxSearchResults);
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
  const byDomain = new Map();
  for (const result of results || []) {
    if (!isPublicComparableOffer(result)) continue;
    const domain = safeDomain(result.url);
    if (!domain || byDomain.has(domain)) continue;
    const priceMatch = `${result.title} ${result.snippet}`.match(
      /R\$\s?\d+(?:[.,]\d{1,2})?/i,
    );
    byDomain.set(domain, {
      marketplace: "PUBLIC_WEB",
      referenceId: domain,
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
  return [...byDomain.values()];
}

function isPublicComparableOffer(result) {
  if (!hasSearchResultShape(result) || isScientificArticleCandidate(result))
    return false;
  const url = new URL(result.url);
  const domain = url.hostname.toLowerCase();
  const path = url.pathname.toLowerCase();
  if (
    /(^|\.)(reddit|youtube|facebook|instagram|tiktok|quora)\.com$/.test(
      domain,
    ) ||
    /(^|\.)(blog\.|capterra\.|getapp\.|techtudo\.|portalinsights\.|neon\.)/.test(
      domain,
    ) ||
    /\/(blog|artigo|articles|noticia|news|perguntas|perguntas-frequentes|faq|guia|recursos|directory|listas|post|comparar)(\/|$)/.test(
      path,
    ) ||
    /\.(pdf|doc|docx)$/i.test(path) ||
    domain.endsWith(".gov.br") ||
    domain.endsWith(".jus.br")
  ) {
    return false;
  }
  const text = `${result.title} ${result.snippet}`.toLowerCase();
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
    /comprar agora|assine|inscreva-se|matr[ií]cula|curso online|programa online|consultoria|mentoria|produto digital/.test(
      text,
    ) || /\/(produto|products?|curso|courses?|programa|planos?|pricing|checkout)(\/|$)/.test(path);
  return commercialSignal && (productSignal || broadOfferSignal);
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
  const highRiskHits = countHits(combined, HIGH_RISK_TERMS);
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
    (metaAdEvidence.some((item) => item.active) &&
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
  const pdeScore = highRiskHits > 0 ? 5 : 25;
  const scientificEvidenceScore = Math.min(10, scientificArticles.length * 3);
  const commercialScore = Math.min(15, commercialIntentHits * 3);
  const score = Math.min(
    100,
    scaleScore +
      unmetScore +
      pdeScore +
      scientificEvidenceScore +
      commercialScore,
  );
  const decision =
    evidence.length === 0
      ? "RESEARCH_MORE"
      : !marketplaceGatePassed
        ? "RESEARCH_MORE"
        : !instagramB2cGatePassed
          ? "RESEARCH_MORE"
          : purchaseMomentGate.required &&
              !purchaseMomentGate.finalPrioritizationEligible
            ? "RESEARCH_MORE"
            : highRiskHits > 0
              ? "HUMAN_REVIEW"
              : scientificArticles.length === 0
                ? "RESEARCH_MORE"
                : commercialIntentHits === 0
                  ? "RESEARCH_MORE"
                  : score >= 70 && independentDomains >= 2
                    ? "APPROVE"
                    : score >= 45
                      ? "RESEARCH_MORE"
                      : "REJECT";
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
      ? `A cobertura Meta/Instagram não foi comprovada (${metaCoverage.map((item) => item.sourceStatus).join(", ") || "NOT_REQUESTED"}); ausência ou indisponibilidade da fonte não significa ausência de mercado.`
      : purchaseMomentGate.required && !purchaseMomentGate.sourceQualityPassed
        ? `As fontes comerciais não passaram pelo gate de qualidade: ${purchaseMomentGate.reasons.join(" ")}`
        : purchaseMomentGate.required
          ? "A pesquisa está pronta para protótipo privado, mas ainda não possui duas leituras consistentes de microvalor, uso do resultado pronto sem montagem, preferência sobre o gratuito e avanço ao checkout."
          : highRiskHits > 0
            ? "Tema contém sinais sensíveis e exige revisão humana antes de qualquer experimento."
            : scientificArticles.length === 0
              ? "Sem sustentação científica candidata do mecanismo; nova pesquisa é obrigatória antes de campanha."
              : commercialIntentHits === 0
                ? "Não há sinal verificável de intenção de compra; pesquisar preços, concorrentes, reviews e anúncios antes de campanha."
                : "Evitar extrapolar evidência científica para promessa absoluta e validar disposição de compra em experimento controlado.";

  return {
    decisionSummary: `${options.analysisSummary ? `${options.analysisSummary} ` : ""}Ciclo pesquisado com ${evidence.length} evidências públicas, ${comparableMarketplaceOffers.length} ofertas comparáveis, ${metaAdEvidence.length} anúncios Meta/Instagram aderentes, cobertura ${metaCoverage.map((item) => item.sourceStatus).join(", ") || "NOT_REQUESTED"}, ${instagramPublicEvidence.length} evidências públicas auxiliares de Instagram, ${independentDomains} domínios independentes, ${scientificArticles.length} artigos científicos candidatos e ${commercialIntentHits} sinais de intenção comercial. Validação do momento de compra: ${purchaseMomentGate.status}. Maturidade factual: ${decision}.`,
    opportunities: opportunityBlueprints.map((blueprint) => ({
      name: blueprint.name,
      primaryAudience: blueprint.primaryAudience,
      rootPain: blueprint.rootPain,
      practicalPain: blueprint.practicalPain,
      emotionalPain: blueprint.emotionalPain,
      scaleEvidence: blueprint.scaleEvidence,
      unmetnessEvidence: blueprint.unmetnessEvidence,
      pdeExperience: `Fronteira factual para avaliação da Atena: ${blueprint.pdeValueBoundary} Base científica candidata: ${mechanismEvidence}`,
      firstCampaignAngle: null,
      commercialRisk: `${blueprint.commercialRisk} ${commercialRisk}`.trim(),
      evidenceJson: JSON.stringify({
        candidateEvidence: {
          purchaseSituation: blueprint.purchaseSituation,
          observedLanguage: blueprint.observedLanguage,
          currentAlternatives: blueprint.currentAlternatives,
          residualEffort: blueprint.residualEffort,
          instagramFitEvidence: blueprint.instagramFitEvidence,
          evidenceIds: blueprint.evidenceIds,
          maturity: blueprint.maturity,
        },
        publicEvidence: evidence,
        marketplaceOffers: comparableMarketplaceOffers,
        metaAdEvidence,
        metaCoverage,
        metaAdInterpretation:
          "Atividade e longevidade sugerem investimento sustentado, mas não comprovam vendas isoladamente.",
        instagramB2cRequired,
        instagramB2cGatePassed,
        instagramPublicEvidence,
        purchaseMomentGate,
        scientificArticles,
        commercialIntentHits,
      }),
      score,
      decision:
        blueprint.maturity === "HUMAN_REVIEW"
          ? "HUMAN_REVIEW"
          : blueprint.maturity === "REJECTED"
            ? "REJECT"
            : decision,
    })),
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
      },
      gates: {
        marketplaceGatePassed,
        instagramB2cGatePassed,
        purchaseMomentStatus: purchaseMomentGate.status,
      },
    },
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
  const words = String(value || "").split(/\s+/).filter(Boolean);
  return words.slice(0, 18).join(" ").slice(0, 140).trim();
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
