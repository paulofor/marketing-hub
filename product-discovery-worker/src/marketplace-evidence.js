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
  return deduplicateMarketplaceOffers(collected);
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
