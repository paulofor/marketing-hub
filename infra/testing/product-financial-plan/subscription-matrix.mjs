import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const { chromium, devices, expect } = createRequire(import.meta.url)(
  "@playwright/test",
);
const base = "http://127.0.0.1:18095";
const api = "/api/financial-plans/v1/products";
const seed = JSON.parse(
  await readFile(
    "backend/ads-service/src/test/resources/financial-plan/assumptions.json",
    "utf8",
  ),
);
seed.validUntil = new Date(Date.now() + 7 * 86400000)
  .toISOString()
  .slice(0, 10);
async function request(path, body, status = 200) {
  const response = await fetch(base + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const raw = await response.text();
  assert.equal(response.status, status, path + " " + raw);
  return raw ? JSON.parse(raw) : null;
}
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  const before = await request("/fixture/reviews");
  for (const [id, device] of [
    [95124, { viewport: { width: 1440, height: 900 } }],
    [95125, devices["iPhone 15 Pro"]],
    [95126, devices["Pixel 7"]],
  ]) {
    const url = api + "/" + id;
    const sale = {
      name: "Pacote histórico",
      expectedRevision: 0,
      createdBy: "Fixture",
      commercialPlanId: id,
      assumptions: seed,
    };
    const live = await request(url, sale);
    const historical = await request(url + "?environment=TEST", sale);
    await request("/fixture/products/" + id + "/type/953", {});
    const [current, prior] = await request(url);
    assert.equal(current.revision, 2);
    assert.equal(current.assumptions.priceBrl, null);
    assert.equal(current.assumptions.minimumMarginPercent, null);
    assert.equal(current.assumptions.preparation, null);
    assert.equal(
      current.evaluation.subscriptionMonth.recurringRevenueBrl,
      null,
    );
    assert.equal(prior.historicalType, true);
    assert.equal(prior.id, live.id);
    assert.deepEqual(prior.assumptions, live.assumptions);
    await request("/fixture/products/" + id + "/type/953", {});
    assert.equal((await request(url)).length, 2);
    await request(
      url + "?environment=TEST",
      { ...sale, expectedRevision: 1, expectedProductTypeId: 951 },
      409,
    );
    const draft = await request(url + "/preparation?environment=TEST");
    assert.equal(draft.canPrepare, true);
    assert.equal(draft.sourceRevisionId, null);
    assert.equal(draft.priceBrl, null);
    assert.equal(draft.structure, "MONTHLY_SUBSCRIPTION");
    await request(
      url + "/preparation?environment=TEST",
      { ...draft, subscription: { renewals: 1.5 } },
      400,
    );
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (err) => errors.push(err.message));
    await page.route("**/api/**", (route) => {
      if (new URL(route.request().url()).origin !== "http://127.0.0.1:15175") {
        errors.push("Destino externo inesperado");
        return route.abort();
      }
      return route.request().url().includes("/api/financial-plans/v1/")
        ? route.continue()
        : route.fulfill({ status: 200, json: [] });
    });
    await page.goto(
      "http://127.0.0.1:15175/financial/plans?productId=" +
        id +
        "&environment=TEST",
    );
    await expect(
      page.getByRole("heading", { name: "Hipótese de assinatura mensal" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Pacote histórico" }),
    ).toHaveCount(0);
    await page.getByRole("button", { name: "Criar plano do produto" }).click();
    await expect(
      page.getByLabel("Mensalidade premium proposta (R$)"),
    ).toHaveValue("");
    await page.getByLabel("Mensalidade premium proposta (R$)").fill("20");
    await page.getByLabel("Novas assinaturas previstas no mês").fill("3");
    await page.getByLabel("Renovações previstas no mês").fill("7");
    await page.getByLabel("Pessoas ativas na base gratuita").fill("40");
    await page
      .getByLabel("Custo mensal por pessoa na base gratuita (R$)")
      .fill("0.5");
    await page
      .getByRole("button", { name: "Salvar preparação e calcular" })
      .click();
    await expect(
      page.getByText("Preparação salva.", { exact: false }),
    ).toBeVisible();
    await expect(
      page.getByText(/Receita recorrente projetada:.*200,00/),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Solicitar parecer de Plutus" }),
    ).toBeDisabled();
    await page
      .getByLabel("Histórico de revisões")
      .selectOption(String(historical.id));
    await expect(
      page.getByText("Revisão histórica do tipo anterior."),
    ).toBeVisible();
    await page.reload();
    const [saved] = await request(url + "?environment=TEST");
    assert.equal(saved.evaluation.subscriptionMonth.freeTierCostBrl, 20);
    assert.equal(saved.assumptions.maximumCacBrl, null);
    assert.equal(saved.assumptions.variableCostEnvelope, null);
    assert.equal((await request(url))[0].assumptions.priceBrl, null);
    const active = await request(url + "/preparation?environment=TEST");
    await request("/fixture/products/" + id + "/type/951", {});
    await request(
      url + "/preparation?environment=TEST",
      { ...active, monthlyPriceBrl: 20 },
      409,
    );
    const reverted = await request(url);
    assert.equal(reverted.length, 3);
    assert.equal(reverted[0].assumptions.subscription, null);
    assert.equal(reverted[0].assumptions.priceBrl, null);
    assert.equal(reverted[2].historicalType, true);
    assert.equal(reverted[2].canRequestAnalysis, false);
    assert.deepEqual(errors, []);
    await context.close();
    console.log(
      "PASS · produto sintético",
      id,
      "· troca, recorrência, histórico, isolamento, retorno",
    );
  }
  assert.deepEqual(await request("/fixture/reviews"), before);
} finally {
  await browser.close();
}
