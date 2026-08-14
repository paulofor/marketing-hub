import assert from "node:assert/strict";
import test from "node:test";
import { deterministicPlan, validatePlan } from "../src/argos-codex.js";

test("plano seguro direciona Hotmart e ClickBank sem credenciais", () => {
  const result = deterministicPlan({ theme: "leads no WhatsApp", targetAudience: "nail designers" });
  validatePlan(result.plan);
  assert.equal(result.plan.minimumComparableOffers, 10);
  assert.deepEqual(
    result.plan.marketplaceRequests.map((item) => item.marketplace),
    ["HOTMART", "CLICKBANK"],
  );
  assert.equal(result.plan.metaAdRequests[0].country, "BR");
  assert.doesNotMatch(result.rawResponse, /password|senha|token|cookie/i);
});

test("plano bloqueia marketplace e volume não autorizados", () => {
  const result = deterministicPlan({ theme: "agenda", targetAudience: "manicures" });
  result.plan.marketplaceRequests[0] = {
    marketplace: "OUTRO",
    query: "agenda",
    maxProducts: 100,
  };
  assert.throws(() => validatePlan(result.plan), /Marketplace não autorizado/);
});
