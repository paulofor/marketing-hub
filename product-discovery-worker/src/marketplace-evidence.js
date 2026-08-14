/** Executa no backend os pedidos dirigidos aos coletores sem acessar credenciais ou bancos. */
export async function collectMarketplaceEvidence(plan, options = {}) {
  const backendBaseUrl = options.backendBaseUrl;
  const fetchFn = options.fetchFn || fetch;
  const logger = options.logger || console;
  const collected = [];
  for (const request of plan.marketplaceRequests || []) {
    const url = new URL(
      "/api/internal/product-discovery/productdiscovery/v1/marketplace-offers",
      backendBaseUrl,
    );
    url.searchParams.set("marketplace", request.marketplace);
    url.searchParams.set("query", request.query);
    url.searchParams.set("limit", String(request.maxProducts));
    const response = await fetchFn(url, { headers: { Accept: "application/json" } });
    if (!response.ok) {
      logger.warn?.(
        `[product-discovery-worker] marketplace request failed marketplace=${request.marketplace} status=${response.status}`,
      );
      continue;
    }
    const payload = await response.json();
    logger.info?.(
      `[product-discovery-worker] marketplace snapshot marketplace=${request.marketplace} jobId=${payload.collectionJobId || "none"} offers=${payload.items?.length || 0}`,
    );
    collected.push(...normalizeMarketplaceOffers(payload));
  }
  for (const request of plan.metaAdRequests || []) {
    const url = new URL(
      "/api/internal/product-discovery/productdiscovery/v1/meta-ad-evidence",
      backendBaseUrl,
    );
    url.searchParams.set("query", request.query);
    url.searchParams.set("country", request.country);
    url.searchParams.set("limit", String(request.maxAds));
    const response = await fetchFn(url, { headers: { Accept: "application/json" } });
    if (!response.ok) {
      logger.warn?.(
        `[product-discovery-worker] Meta Ad Library evidence failed status=${response.status}`,
      );
      continue;
    }
    const payload = await response.json();
    logger.info?.(
      `[product-discovery-worker] Meta Ad Library evidence ads=${payload.items?.length || 0}`,
    );
    collected.push(...normalizeMetaAdEvidence(payload));
  }
  return deduplicateMarketplaceOffers(collected);
}

/** Normaliza anuncios Meta como sinais comerciais sem declara-los vendas comprovadas. */
export function normalizeMetaAdEvidence(payload) {
  return (Array.isArray(payload?.items) ? payload.items : [])
    .map((item) => ({
      marketplace: "META_AD_LIBRARY",
      referenceId: item.metaAdId,
      title: item.advertiserName || `Anúncio Meta ${item.metaAdId}`,
      url: item.snapshotUrl || item.destinationUrl,
      description: item.adText || "",
      producer: item.advertiserName || "",
      category: "PAID_AD",
      format: item.formatTypes || null,
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
    }))
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
    .filter((item) => item.marketplace && item.referenceId && item.title && item.url);
}

function deduplicateMarketplaceOffers(offers) {
  return [
    ...new Map(
      offers.map((offer) => [
        `${offer.marketplace}:${offer.referenceId}`,
        offer,
      ]),
    ).values(),
  ];
}
