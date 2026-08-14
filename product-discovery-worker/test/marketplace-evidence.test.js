import assert from "node:assert/strict";
import test from "node:test";
import { collectMarketplaceEvidence } from "../src/marketplace-evidence.js";

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
});
