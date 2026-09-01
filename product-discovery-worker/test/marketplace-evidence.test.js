import assert from "node:assert/strict";
import test from "node:test";
import {
  collectMarketplaceEvidence,
  filterRelevantOffers,
  normalizeMetaAdEvidence,
} from "../src/marketplace-evidence.js";

test("executa pedidos dirigidos e remove ofertas duplicadas", async () => {
  const calls = [];
  const evidence = await collectMarketplaceEvidence(
    {
      marketplaceRequests: [
        {
          marketplace: "HOTMART",
          query: "gerador propostas orçamentos",
          maxProducts: 10,
        },
        {
          marketplace: "CLICKBANK",
          query: "gerador propostas orçamentos",
          maxProducts: 10,
        },
      ],
    },
    {
      backendBaseUrl: "http://backend.test",
      logger: { info() {}, warn() {} },
      fetchFn: async (url) => {
        calls.push(url.toString());
        if (url.pathname.endsWith("meta-ad-evidence")) {
          return {
            ok: true,
            async json() {
              return { items: [] };
            },
          };
        }
        const marketplace = url.searchParams.get("marketplace");
        return {
          ok: true,
          async json() {
            return {
              marketplace,
              collectionJobId: `job-${marketplace}`,
              items: [
                {
                  marketplace,
                  referenceId: "1",
                  title: `Gerador de propostas ${marketplace}`,
                  productUrl: `https://example.test/${marketplace}`,
                  description:
                    "Cria propostas e orçamentos comerciais prontos.",
                  producer: "Fornecedor teste",
                  price: "97.00",
                  tractionSignal: 85,
                  rating: 4.8,
                  reviewCount: 120,
                  observations: 3,
                  previousTractionSignal: 82,
                  evidenceConfidence: "HIGH",
                },
                {
                  marketplace,
                  referenceId: "snapshot-duplicado",
                  title: `Gerador de propostas ${marketplace}`,
                  productUrl: `https://example.test/${marketplace}?snapshot=2`,
                  description:
                    "Cria propostas e orçamentos comerciais prontos.",
                  producer: "Fornecedor teste",
                  price: "97.00",
                  tractionSignal: 85,
                  evidenceConfidence: "HIGH",
                },
              ],
            };
          },
        };
      },
    },
  );
  assert.equal(calls.length, 2);
  assert.equal(evidence.marketplaceOffers.length, 2);
  assert.equal(evidence.marketplaceOffers[0].collectionJobId, "job-HOTMART");
  assert.equal(evidence.marketplaceOffers[0].observations, 3);
  assert.equal(evidence.marketplaceOffers[0].previousTractionSignal, 82);
  assert.equal(evidence.marketplaceOffers[0].evidenceConfidence, "HIGH");
  assert.deepEqual(evidence.metaAdEvidence, []);
  assert.deepEqual(evidence.metaCoverage, []);
});

test("solicita cobertura Instagram correlacionada e separa anúncios de ofertas", async () => {
  let receivedBody;
  const evidence = await collectMarketplaceEvidence(
    {
      marketplaceRequests: [],
      metaAdRequests: [
        {
          query: "treino entrevista emprego",
          country: "BR",
          publisherPlatform: "INSTAGRAM",
          maxAds: 25,
        },
      ],
    },
    {
      backendBaseUrl: "http://backend.test",
      cycleId: 81,
      executionLeaseId: "lease-81",
      researchContext: "treino entrevista emprego jovens",
      logger: { info() {}, warn() {} },
      fetchFn: async (url, init) => {
        assert.match(url.pathname, /stage-executions\/81\/meta-ad-evidence$/);
        assert.equal(init.method, "POST");
        receivedBody = JSON.parse(init.body);
        return {
          ok: true,
          async json() {
            return {
              sourceStatus: "OBSERVED",
              collectionMode: "SUPERVISED",
              investigationId: 7,
              query: "treino entrevista emprego",
              country: "BR",
              publisherPlatform: "INSTAGRAM",
              adsObserved: 1,
              activeAds: 1,
              advertisersObserved: 1,
              latestObservationAt: "2026-08-26T12:00:00Z",
              searchUrl: "https://www.facebook.com/ads/library/?q=entrevista",
              items: [
                {
                  metaAdId: "ad-1",
                  advertiserName: "Treino Entrevista",
                  adTexts: ["Treino entrevista emprego para jovens"],
                  publisherPlatforms: ["INSTAGRAM"],
                  snapshotUrl: "https://www.facebook.com/ads/library/?id=ad-1",
                  active: true,
                },
              ],
            };
          },
        };
      },
    },
  );

  assert.deepEqual(receivedBody, {
    executionLeaseId: "lease-81",
    attemptNumber: 1,
    query: "treino entrevista emprego",
    country: "BR",
    publisherPlatform: "INSTAGRAM",
    limit: 25,
  });
  assert.equal(evidence.marketplaceOffers.length, 0);
  assert.equal(evidence.metaAdEvidence.length, 1);
  assert.equal(evidence.metaCoverage[0].sourceStatus, "OBSERVED");
  assert.equal(evidence.metaCoverage[0].advertisersObserved, 1);
});

test("executa Chromium quando o backend prepara a sessão e persiste o lote antes da síntese", async () => {
  const calls = [];
  const browserCalls = [];
  const evidence = await collectMarketplaceEvidence(
    {
      marketplaceRequests: [],
      metaAdRequests: [
        {
          query: "guarda roupa cápsula climatério",
          country: "BR",
          publisherPlatform: "INSTAGRAM",
          maxAds: 12,
        },
      ],
    },
    {
      backendBaseUrl: "http://backend.test",
      cycleId: 144,
      executionLeaseId: "lease-144",
      researchContext: "mulheres 40 guarda roupa cápsula climatério",
      logger: { info() {}, warn() {} },
      collectPublicMetaAds: async (request) => {
        browserCalls.push(request);
        return {
          outcome: "OBSERVED",
          httpStatus: 403,
          platformFilterConfirmed: true,
          pageTitle: "Biblioteca de Anúncios",
          errorMessage: null,
          startedAt: "2026-08-30T12:00:00Z",
          finishedAt: "2026-08-30T12:00:02Z",
          observations: [
            {
              metaAdId: "meta-144",
              advertiserName: "Estilo Maduro",
              active: true,
              publisherPlatforms: ["INSTAGRAM"],
              formatTypes: ["VIDEO"],
              texts: ["Guarda roupa cápsula para o climatério"],
              destinationUrl: "https://estilo.example/oferta",
              snapshotUrl:
                "https://www.facebook.com/ads/library/?id=meta-144",
              pageActive: true,
              commercialSignal: true,
              rawPayload: { source: "META_AD_LIBRARY_PUBLIC_BROWSER" },
            },
          ],
        };
      },
      fetchFn: async (url, init) => {
        calls.push({ path: url.pathname, body: JSON.parse(init.body) });
        if (url.pathname.endsWith("meta-ad-evidence")) {
          return jsonResponse({
            sourceStatus: "AWAITING_PUBLIC_BROWSER",
            collectionMode: "PUBLIC_BROWSER",
            investigationId: 91,
            query: "guarda roupa cápsula climatério",
            country: "BR",
            publisherPlatform: "INSTAGRAM",
            searchUrl:
              "https://www.facebook.com/ads/library/?country=BR&q=guarda+roupa+capsula",
            items: [],
          });
        }
        return jsonResponse({
          sourceStatus: "OBSERVED",
          collectionMode: "PUBLIC_BROWSER",
          investigationId: 91,
          query: "guarda roupa cápsula climatério",
          country: "BR",
          publisherPlatform: "INSTAGRAM",
          adsObserved: 1,
          activeAds: 1,
          advertisersObserved: 1,
          searchUrl:
            "https://www.facebook.com/ads/library/?country=BR&q=guarda+roupa+capsula",
          items: [
            {
              metaAdId: "meta-144",
              advertiserName: "Estilo Maduro",
              adTexts: ["Guarda roupa cápsula para o climatério"],
              publisherPlatforms: ["INSTAGRAM"],
              snapshotUrl:
                "https://www.facebook.com/ads/library/?id=meta-144",
              active: true,
            },
          ],
        });
      },
    },
  );

  assert.equal(browserCalls.length, 1);
  assert.equal(browserCalls[0].investigationId, 91);
  assert.match(calls[1].path, /meta-ad-browser-collection$/);
  assert.equal(calls[1].body.attemptNumber, 1);
  assert.equal(calls[1].body.collectorRunId, "argos-browser-144-lease-144-1");
  assert.equal(calls[1].body.outcome, "OBSERVED");
  assert.equal(evidence.metaAdEvidence.length, 1);
  assert.equal(evidence.metaCoverage[0].collectionMode, "PUBLIC_BROWSER");
});

test("descarta ofertas que coincidem apenas com termos genéricos do público", () => {
  const offers = filterRelevantOffers(
    [
      {
        title: "Curso de investimentos",
        description: "Suporte por WhatsApp para clientes e prestadores",
      },
      {
        title: "Gerador de proposta comercial",
        description: "Monte orçamento profissional e compartilhe com o cliente",
      },
    ],
    "gerador de proposta comercial para prestadores pelo WhatsApp",
    "propostas e orçamentos para prestadores locais",
  );

  assert.deepEqual(
    offers.map((offer) => offer.title),
    ["Gerador de proposta comercial"],
  );
});

test("descarta curso incidental e preserva somente alternativa aderente ao problema", () => {
  const offers = filterRelevantOffers(
    [
      {
        title: "Mentoria Oráculo",
        description:
          "Curso com acompanhamento, cobrança de execução e suporte.",
      },
      {
        title: "InstaVistoria",
        description:
          "Compara vistoria de entrada e saída no aluguel e entrega laudo.",
      },
    ],
    "vistoria de saída aluguel cobrança danos produto curso preço",
    "inquilinos comparando vistoria, laudo e fotos do aluguel",
  );

  assert.deepEqual(
    offers.map((offer) => offer.title),
    ["InstaVistoria"],
  );
});

test("preserva longevidade Meta como sinal e não como venda comprovada", () => {
  const [ad] = normalizeMetaAdEvidence({
    items: [
      {
        metaAdId: "ad-1",
        advertiserName: "Oferta real",
        snapshotUrl: "https://www.facebook.com/ads/library/?id=ad-1",
        publisherPlatforms: ["INSTAGRAM"],
        active: true,
        commercialSignal: true,
        observations: 3,
        longevityDays: 45,
        sustainedInvestmentSignal: true,
        evidenceConfidence: "HIGH",
      },
    ],
  });
  assert.equal(ad.marketplace, "META_AD_LIBRARY");
  assert.equal(ad.sustainedInvestmentSignal, true);
  assert.match(ad.signalDisclaimer, /não venda comprovada/);
});

test("descarta anúncio observado somente fora do Instagram", () => {
  const ads = normalizeMetaAdEvidence({
    items: [
      {
        metaAdId: "ad-facebook",
        advertiserName: "Oferta real",
        snapshotUrl: "https://www.facebook.com/ads/library/?id=ad-facebook",
        publisherPlatforms: ["FACEBOOK"],
      },
    ],
  });

  assert.deepEqual(ads, []);
});

function jsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    async json() { return payload; },
    async text() { return JSON.stringify(payload); },
  };
}
