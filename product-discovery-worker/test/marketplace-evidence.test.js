import assert from "node:assert/strict";
import test from "node:test";
import { collectMarketplaceEvidence } from "../src/marketplace-evidence.js";
import { normalizeMetaAdEvidence } from "../src/marketplace-evidence.js";

test("executa pedidos dirigidos e remove ofertas duplicadas", async () => {
  const calls = [];
  const offers = await collectMarketplaceEvidence(
    {
      marketplaceRequests: [
        { marketplace: "HOTMART", query: "whatsapp", maxProducts: 10 },
        { marketplace: "CLICKBANK", query: "whatsapp", maxProducts: 10 },
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
                  title: `Oferta ${marketplace}`,
                  productUrl: `https://example.test/${marketplace}`,
                  price: "97.00",
                  tractionSignal: 85,
                  rating: 4.8,
                  reviewCount: 120,
                  observations: 3,
                  previousTractionSignal: 82,
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
  assert.equal(offers.length, 2);
  assert.equal(offers[0].collectionJobId, "job-HOTMART");
  assert.equal(offers[0].observations, 3);
  assert.equal(offers[0].previousTractionSignal, 82);
  assert.equal(offers[0].evidenceConfidence, "HIGH");
});

test("preserva longevidade Meta como sinal e não como venda comprovada", () => {
  const [ad] = normalizeMetaAdEvidence({
    items: [
      {
        metaAdId: "ad-1",
        advertiserName: "Oferta real",
        snapshotUrl: "https://www.facebook.com/ads/library/?id=ad-1",
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
