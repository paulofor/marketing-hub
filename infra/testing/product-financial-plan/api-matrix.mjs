import assert from "node:assert/strict";
import fs from "node:fs/promises";
const base = "http://127.0.0.1:18095";
const api = "/api/financial-plans/v1";
const seed = JSON.parse(
  await fs.readFile(
    "backend/ads-service/src/test/resources/financial-plan/assumptions.json",
    "utf8",
  ),
);
seed.validUntil = new Date(Date.now() + 30 * 86400000)
  .toISOString()
  .slice(0, 10);
const assumptions = () => structuredClone(seed);
let checks = 0;
export async function request(path, body, status = 200) {
  const r = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const raw = await r.text();
  assert.equal(r.status, status, `${path}: ${raw}`);
  checks++;
  return raw ? JSON.parse(raw) : null;
}
const body = (id, a = assumptions(), rev = 0) => ({
  name: "Plano sintético " + id,
  createdBy: "Homologação local",
  expectedRevision: rev,
  commercialPlanId: id,
  templateId: null,
  assumptions: a,
});
const list = (id, env = "LIVE") =>
  request(`${api}/products/${id}?environment=${env}`);
const analyze = (owner, id, status = 200, env = "LIVE") =>
  request(
    `${api}/products/${owner}/revisions/${id}/analysis?environment=${env}`,
    {},
    status,
  );
const catalog = await request(api + "/catalog?productId=95101");
assert.equal(catalog.products.find((p) => p.id === 95101).productTypeId, 951);
const template = await request(api + "/product-types/951", {
  ...body(null),
  name: "Modelo local de imagens",
  commercialPlanId: null,
});
const original = await request(api + "/products/95101", {
  ...body(95101),
  templateId: template.id,
});
assert.equal(original.evaluation.status, "PROJECTED_VIABLE");
assert.equal(original.evaluation.scenarios.length, 4);
assert.equal(original.evaluation.scenarios[0].netRevenueBrl, 800);
assert.equal(original.evaluation.scenarios[0].resultAfterInvestmentBrl, 300);
assert.equal(original.analysis, null);
assert.equal(original.canRequestAnalysis, true);
assert.equal(
  (await request(`${api}/products/95101/revisions/${original.id}`)).id,
  original.id,
);
await request(`${api}/products/95102/revisions/${original.id}`, undefined, 404);
await request(
  `${api}/products/95101/revisions/${original.id}?environment=TEST`,
  undefined,
  404,
);
await request(
  api + "/products/95103",
  { ...body(95103), templateId: template.id },
  409,
);
await request(
  api + "/products/95102",
  { ...body(95102), commercialPlanId: 95101 },
  409,
);
const missing = assumptions();
missing.costs.supportBrl = null;
const draft = await request(api + "/products/95102", body(95102, missing));
assert.equal(draft.evaluation.status, "MISSING_INPUTS");
assert.equal(draft.canRequestAnalysis, false);
await analyze(95102, draft.id, 409);
const invalid = assumptions();
invalid.costs.supportBrl = -1;
await request(api + "/products/95104", body(95104, invalid), 400);
const extreme = assumptions();
extreme.ai.maximumAttempts = 1000001;
await request(api + "/products/95104", body(95104, extreme), 400);
const fraction = assumptions();
fraction.scenarios[0].customers = 1.5;
await request(api + "/products/95104", body(95104, fraction), 400);
const loss = assumptions();
loss.ai.maximumAttempts = 60;
loss.ai.maximumCostPerCustomerBrl = 60;
const poor = await request(api + "/products/95104", body(95104, loss));
assert.equal(poor.evaluation.scenarios[0].viable, true);
assert.equal(poor.evaluation.scenarios.at(-1).viable, false);
assert.equal(poor.evaluation.status, "REVIEW_REQUIRED");
const expired = assumptions();
expired.validUntil = "2000-01-01";
const staleDate = await request(api + "/products/95105", body(95105, expired));
assert.equal(staleDate.stale, true);
await analyze(95105, staleDate.id, 409);
const tests = await request(
  api + "/products/95101?environment=TEST",
  body(95101),
);
assert.equal(tests.canRequestAnalysis, false);
assert.equal((await list(95101, "TEST")).length, 1);
assert.equal((await list(95101)).length, 1);
await analyze(95101, tests.id, 409, "TEST");
assert.equal((await request("/fixture/reviews")).length, 0);
const replies = await Promise.all(
  Array.from({ length: 8 }, () => analyze(95101, original.id)),
);
const eid = replies[0].analysis.executionId;
assert.ok(replies.every((r) => r.analysis.executionId === eid));
const queue = await request("/fixture/reviews");
assert.equal(queue.length, 1);
const context = JSON.parse(queue[0].context_json);
assert.equal(context.financialPlanId, original.id);
assert.equal(context.productId, 95101);
assert.deepEqual(context.assumptions, original.assumptions);
await request(`/fixture/reviews/${eid}/FAILED`, {});
const failed = await analyze(95101, original.id);
assert.equal(failed.analysis.status, "FAILED");
assert.equal(failed.analysis.costUsd, 0.015);
assert.equal((await request("/fixture/reviews")).length, 1);
const duplicate = await request(api + "/products/95101", {
  ...body(95101, assumptions(), 1),
  templateId: template.id,
});
assert.equal(duplicate.id, original.id);
assert.equal((await list(95101)).length, 1);
const changed = assumptions();
changed.priceBrl = 120;
changed.evidence += " Revisão de preço sintética.";
const second = await request(api + "/products/95101", {
  ...body(95101, changed, 1),
  templateId: template.id,
});
assert.equal(second.revision, 2);
assert.equal(second.analysis, null);
await request(api + "/products/95101", body(95101, changed, 1), 409);
const old = await request(`${api}/products/95101/revisions/${original.id}`);
assert.equal(old.assumptions.priceBrl, 100);
assert.equal(old.analysis.executionId, eid);
const secondAnalysis = await analyze(95101, second.id);
await request(
  `/fixture/reviews/${secondAnalysis.analysis.executionId}/COMPLETED`,
  {},
);
assert.equal((await list(95101))[0].analysis.status, "COMPLETED");
const copiedBefore = await request(
  `${api}/products/95101/revisions/${original.id}`,
);
const newTemplate = assumptions();
newTemplate.priceBrl = 140;
await request(api + "/product-types/951", {
  ...body(null, newTemplate, 1),
  name: "Modelo local de pacote ajustado",
  commercialPlanId: null,
});
assert.deepEqual(
  (await request(`${api}/products/95101/revisions/${original.id}`)).assumptions,
  copiedBefore.assumptions,
);
const other = assumptions();
other.ai.currency = "BRL";
other.ai.usdBrl = null;
other.ai.exchangeSource = null;
other.priceBrl = 88;
const held = await request(api + "/products/95103", body(95103, other));
assert.equal(held.assumptions.priceBrl, 88);
assert.equal(held.templateId, null);
const concurrent = await Promise.all(
  [140, 150].map(async (price) => {
    const a = assumptions();
    a.priceBrl = price;
    const r = await fetch(base + api + "/products/95106", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body(95106, a)),
    });
    return r.status;
  }),
);
assert.deepEqual(concurrent.sort(), [200, 409]);
assert.equal((await list(95106)).length, 1);
checks++;
await request("/fixture/plans/95103/version/2", {});
const stale = (await list(95103))[0];
assert.equal(stale.stale, true);
assert.equal(stale.canRequestAnalysis, false);
await analyze(95103, held.id, 409);
assert.equal((await request("/fixture/reviews")).length, 2);
console.log(
  JSON.stringify({
    checks,
    concurrencyRequests: 8,
    queueExecutions: 2,
    realModelCalls: 0,
    result: "API_MATRIX_PASS",
  }),
);
