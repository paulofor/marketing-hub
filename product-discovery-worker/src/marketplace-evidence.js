import { collectPublicMetaAdLibrary } from "./meta-ad-library-browser.js";

/** Executa no backend os pedidos dirigidos aos coletores sem acessar credenciais ou bancos. */
export async function collectMarketplaceEvidence(plan, options = {}) {
  const backendBaseUrl = options.backendBaseUrl;
  const fetchFn = options.fetchFn || fetch;
  const logger = options.logger || console;
  const attemptNumber = Number.isInteger(options.attemptNumber)
    ? options.attemptNumber
    : 1;
  const marketplaceOffers = [];
  const metaAdEvidence = [];
  const metaCoverage = [];
  for (const request of plan.marketplaceRequests || []) {
    const url = new URL(
      "/api/internal/product-discovery/productdiscovery/v1/marketplace-offers",
      backendBaseUrl,
    );
    url.searchParams.set("marketplace", request.marketplace);
    url.searchParams.set("query", request.query);
    url.searchParams.set("limit", String(request.maxProducts));
    const response = await fetchFn(url, {
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      logger.warn?.(
        `[product-discovery-worker] marketplace request failed marketplace=${request.marketplace} status=${response.status}`,
      );
      continue;
    }
    const payload = await response.json();
    const normalizedOffers = normalizeMarketplaceOffers(payload);
    const relevantOffers = filterRelevantOffers(
      normalizedOffers,
      request.query,
      options.researchContext,
    );
    logger.info?.(
      `[product-discovery-worker] marketplace snapshot marketplace=${request.marketplace} jobId=${payload.collectionJobId || "none"} offers=${normalizedOffers.length} relevant=${relevantOffers.length}`,
    );
    marketplaceOffers.push(...relevantOffers);
  }
  for (const request of plan.metaAdRequests || []) {
    const url = new URL(
      `/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${options.cycleId}/meta-ad-evidence`,
      backendBaseUrl,
    );
    const response = await fetchFn(url, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        executionLeaseId: options.executionLeaseId,
        attemptNumber,
        query: request.query,
        country: request.country,
        publisherPlatform: request.publisherPlatform,
        limit: request.maxAds,
      }),
    });
    if (!response.ok) {
      logger.warn?.(
        `[product-discovery-worker] Meta Ad Library evidence failed cycle=${options.cycleId} status=${response.status}`,
      );
      metaCoverage.push({
        attemptNumber,
        query: request.query,
        country: request.country,
        publisherPlatform: request.publisherPlatform,
        sourceStatus: "UNAVAILABLE",
        collectionMode: "UNKNOWN",
        adsObserved: 0,
        activeAds: 0,
        advertisersObserved: 0,
        latestObservationAt: null,
        searchUrl: null,
        interpretation:
          "A fonte Meta não respondeu; isso não comprova ausência de anúncios ou de mercado.",
      });
      continue;
    }
    let payload = await response.json();
    if (shouldUsePublicMetaBrowser(payload, options)) {
      payload = await collectAndPersistPublicMetaEvidence(
        payload,
        request,
        { ...options, attemptNumber },
      );
    }
    const normalizedAds = normalizeMetaAdEvidence(
      payload,
      request.publisherPlatform,
    );
    const relevantAds = filterRelevantOffers(
      normalizedAds,
      request.query,
      options.researchContext,
    );
    logger.info?.(
      `[product-discovery-worker] Meta Ad Library evidence cycle=${options.cycleId} platform=${request.publisherPlatform} sourceStatus=${payload.sourceStatus} ads=${normalizedAds.length} relevant=${relevantAds.length} advertisers=${payload.advertisersObserved ?? 0}`,
    );
    metaAdEvidence.push(...relevantAds);
    metaCoverage.push({
      attemptNumber,
      query: payload.query || request.query,
      country: payload.country || request.country,
      publisherPlatform: payload.publisherPlatform || request.publisherPlatform,
      sourceStatus: payload.sourceStatus || "UNKNOWN",
      collectionMode: payload.collectionMode || "UNKNOWN",
      investigationId: payload.investigationId ?? null,
      adsObserved: Number(payload.adsObserved || 0),
      activeAds: Number(payload.activeAds || 0),
      advertisersObserved: Number(payload.advertisersObserved || 0),
      latestObservationAt: payload.latestObservationAt || null,
      searchUrl: payload.searchUrl || null,
      interpretation: payload.interpretation || "Cobertura não informada.",
    });
  }
  return {
    marketplaceOffers: deduplicateMarketplaceOffers(marketplaceOffers),
    metaAdEvidence: deduplicateMarketplaceOffers(metaAdEvidence),
    metaCoverage,
  };
}

/** Decide se a investigação vigente ainda precisa da observação pública do Chromium. */
export function shouldUsePublicMetaBrowser(payload, options = {}) {
  const enabled =
    String(
      options.metaBrowserEnabled ??
        process.env.ARGOS_META_BROWSER_ENABLED ??
        "true",
    ) === "true";
  return (
    enabled &&
    payload?.sourceStatus === "AWAITING_PUBLIC_BROWSER" &&
    payload?.collectionMode === "PUBLIC_BROWSER" &&
    Number.isInteger(payload?.investigationId) &&
    /^https:\/\/((www|business)\.)?facebook\.com\/ads\/library\//.test(
      String(payload?.searchUrl || ""),
    )
  );
}

/** Executa uma sessão efêmera e persiste seu desfecho pelo controller do próprio domínio. */
async function collectAndPersistPublicMetaEvidence(payload, request, options) {
  const collect = options.collectPublicMetaAds || collectPublicMetaAdLibrary;
  const result = await collect(
    {
      cycleId: options.cycleId,
      investigationId: payload.investigationId,
      searchUrl: payload.searchUrl,
      country: payload.country || request.country,
      publisherPlatform:
        payload.publisherPlatform || request.publisherPlatform,
      maxAds: Math.min(Number(request.maxAds || 25), 25),
    },
    {
      ...(options.metaBrowserOptions || {}),
      logger: options.logger || console,
    },
  );
  const collectorRunId = `argos-browser-${options.cycleId}-${options.executionLeaseId}-${options.attemptNumber}`.slice(
    0,
    80,
  );
  const url = new URL(
    `/api/internal/product-discovery/productdiscovery/v1/research/stage-executions/${options.cycleId}/meta-ad-browser-collection`,
    options.backendBaseUrl,
  );
  const response = await (options.fetchFn || fetch)(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      executionLeaseId: options.executionLeaseId,
      attemptNumber: options.attemptNumber,
      investigationId: payload.investigationId,
      collectorRunId,
      searchUrl: payload.searchUrl,
      outcome: result.outcome,
      httpStatus: result.httpStatus,
      platformFilterConfirmed: result.platformFilterConfirmed,
      pageTitle: result.pageTitle,
      errorMessage: result.errorMessage,
      startedAt: result.startedAt,
      finishedAt: result.finishedAt,
      observations: result.observations,
    }),
  });
  if (!response.ok) {
    const detail = (await response.text()).slice(0, 1000);
    throw new Error(
      `Backend recusou a coleta pública Meta do ciclo ${options.cycleId}: HTTP ${response.status}${detail ? ` ${detail}` : ""}`,
    );
  }
  return response.json();
}

/** Normaliza anuncios Meta como sinais comerciais sem declara-los vendas comprovadas. */
export function normalizeMetaAdEvidence(
  payload,
  requestedPublisherPlatform = "INSTAGRAM",
) {
  return (Array.isArray(payload?.items) ? payload.items : [])
    .map((item) => {
      const publisherPlatforms = normalizeStringList(item.publisherPlatforms);
      return {
        marketplace: "META_AD_LIBRARY",
        referenceId: item.metaAdId,
        title: item.advertiserName || `Anúncio Meta ${item.metaAdId}`,
        url: item.snapshotUrl || item.destinationUrl,
        description: normalizeStringList(item.adTexts || item.adText).join(" "),
        producer: item.advertiserName || "",
        category: "PAID_AD",
        publisherPlatforms,
        format: normalizeStringList(item.formatTypes).join(", ") || null,
        observations: item.observations ?? 1,
        firstObservedAt: item.firstObservedAt || null,
        collectedAt: item.lastObservedAt || null,
        longevityDays: item.longevityDays ?? 0,
        active: Boolean(item.active),
        commercialSignal: Boolean(item.commercialSignal),
        sustainedInvestmentSignal: Boolean(item.sustainedInvestmentSignal),
        evidenceConfidence: item.evidenceConfidence || "LOW",
        signalDisclaimer:
          "Longevidade e atividade indicam investimento sustentado, não venda comprovada.",
      };
    })
    .filter((item) =>
      item.publisherPlatforms.includes(
        String(requestedPublisherPlatform || "INSTAGRAM").toUpperCase(),
      ),
    )
    .filter((item) => item.referenceId && item.title && item.url);
}

/** Normaliza o contrato do backend para evidencia comercial usada no dossie. */
export function normalizeMarketplaceOffers(payload) {
  return (Array.isArray(payload?.items) ? payload.items : [])
    .map((item) => ({
      marketplace: item.marketplace || payload.marketplace,
      referenceId: item.referenceId,
      title: item.title,
      url: item.productUrl,
      description: item.description || "",
      producer: item.producer || "",
      price: item.price || null,
      tractionSignal: item.tractionSignal ?? null,
      rating: item.rating ?? null,
      reviewCount: item.reviewCount ?? null,
      blueprint: item.blueprint ?? null,
      commission: item.commission || null,
      category: item.category || null,
      format: item.format || null,
      rankingPosition: item.rankingPosition ?? null,
      observations: item.observations ?? 1,
      previousTractionSignal: item.previousTractionSignal ?? null,
      firstObservedAt: item.firstObservedAt || item.collectedAt || null,
      evidenceConfidence: item.evidenceConfidence || "LOW",
      collectedAt: item.collectedAt || null,
      collectionJobId: payload.collectionJobId || null,
    }))
    .filter(
      (item) => item.marketplace && item.referenceId && item.title && item.url,
    );
}

/** Mantém somente ofertas que correspondem a pelo menos dois termos específicos da consulta. */
export function filterRelevantOffers(offers, query, researchContext = "") {
  const terms = relevantTerms(query);
  const contextTerms = relevantTerms(researchContext);
  if (terms.length === 0) return [];
  const minimumMatches = Math.min(2, terms.length);
  return offers.filter((offer) => {
    const searchableTerms = normalizedTokenSet(
      [
        offer.title,
        offer.description,
        offer.category,
        offer.format,
        offer.producer,
      ].join(" "),
    );
    const matches = terms.filter((term) => searchableTerms.has(term)).length;
    const contextMatches = contextTerms.filter((term) =>
      searchableTerms.has(term),
    ).length;
    return (
      matches >= minimumMatches &&
      (contextTerms.length === 0 || contextMatches >= 1)
    );
  });
}

function normalizedTokenSet(value) {
  return new Set(
    normalizeText(value).split(/\s+/).filter(Boolean).map(singularTerm),
  );
}

function relevantTerms(value) {
  const ignored = new Set([
    "como",
    "para",
    "pela",
    "pelo",
    "prestador",
    "prestadores",
    "servico",
    "servicos",
    "cliente",
    "clientes",
    "pequeno",
    "pequenos",
    "local",
    "locais",
    "brasil",
    "whatsapp",
    "oferta",
    "ofertas",
    "comercial",
    "comerciais",
    "preco",
    "venda",
    "vendas",
    "produto",
    "produtos",
    "curso",
    "cursos",
    "digital",
    "digitais",
    "execucao",
    "validada",
    "validado",
    "claras",
    "claro",
    "pde",
  ]);
  return [...new Set(normalizeText(value).split(/\s+/).map(singularTerm))]
    .filter((term) => term.length >= 4 && !ignored.has(term))
    .slice(0, 8);
}

function singularTerm(value) {
  return value.length > 5 && value.endsWith("s") ? value.slice(0, -1) : value;
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

function normalizeStringList(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item).toUpperCase());
  }
  try {
    const parsed = JSON.parse(String(value || "[]"));
    return Array.isArray(parsed)
      ? parsed.map((item) => String(item).toUpperCase())
      : [];
  } catch {
    return [];
  }
}

function deduplicateMarketplaceOffers(offers) {
  const uniqueOffers = new Map();
  for (const offer of offers) {
    const key = canonicalOfferKey(offer);
    if (!uniqueOffers.has(key)) uniqueOffers.set(key, offer);
  }
  return [...uniqueOffers.values()];
}

function canonicalOfferKey(offer) {
  const title = normalizeText(offer.title);
  const producer = normalizeText(offer.producer);
  return title
    ? `${offer.marketplace}:title:${title}:producer:${producer}`
    : `${offer.marketplace}:reference:${offer.referenceId}`;
}
