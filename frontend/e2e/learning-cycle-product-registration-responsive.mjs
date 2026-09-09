import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/cycle-product-registration";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1100 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const errors = [],
      mutations = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("dialog", async (dialog) => {
      if (dialog.message() !== "Teste salvo!") errors.push(dialog.message());
      await dialog.dismiss();
    });
    const product = {
      id: 91001,
      name: "Vega · fixture local",
      internalName: "Vega · fixture local",
      marketNicheId: 91031,
      currentPriceBrl: 67,
      desireAssociationMapJson: JSON.stringify({
        territories: [{ code: "LOCAL", name: "Utilidade local" }],
      }),
    };
    const hypothesis = {
      id: "11111111-1111-4111-8111-111111111111",
      title: "LOCAL-H001",
      productId: 91001,
    };
    await page.route("**/*", async (route) => {
      const req = route.request(),
        url = new URL(req.url());
      if (url.origin !== base) {
        errors.push(`Origem externa: ${url.origin}`);
        return route.abort();
      }
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (req.method() !== "GET") {
        mutations.push({ path: url.pathname, body: req.postDataJSON() });
        assert.equal(url.pathname, "/api/experiments");
        return route.fulfill({
          json: {
            id: 91003,
            name: "LOCAL-H001-E002",
            status: "PLANNED",
            productId: 91001,
          },
        });
      }
      if (url.pathname === "/api/products")
        return route.fulfill({ json: [product] });
      if (url.pathname === "/api/niches")
        return route.fulfill({ json: [{ id: 91031, name: "Nicho local" }] });
      if (url.pathname === "/api/niches/91031/hypotheses")
        return route.fulfill({ json: [hypothesis] });
      if (url.pathname === "/api/journey-templates")
        return route.fulfill({
          json: { content: [{ id: 91019, name: "Jornada local" }] },
        });
      if (url.pathname.endsWith("stage-executions/latest"))
        return route.fulfill({ status: 204, body: "" });
      return route.fulfill({ json: [] });
    });
    await page.goto(`${base}/experiments/new?productId=91001&nicheId=91031`, {
      waitUntil: "networkidle",
    });
    await expect(page.locator("#productId")).toHaveValue("91001");
    await page
      .locator("#experimentType")
      .selectOption("PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL");
    await page.locator("#desireTerritoryCode").selectOption("LOCAL");
    await page
      .locator("select")
      .filter({
        has: page.locator(
          'option[value="11111111-1111-4111-8111-111111111111"]',
        ),
      })
      .selectOption(hypothesis.id);
    for (const [id, value] of Object.entries({
      commercialObjective:
        "Validar utilidade e continuidade até compra; planejamento sem gasto.",
      singlePain: "Não saber qual primeiro ajuste aplicar",
      freeReward: "Primeira microação gratuita",
      funnelPromise: "Um ajuste aplicável com o que a pessoa possui",
      primaryCta: "Ver meu primeiro ajuste",
      sampleSize: "100",
      targetCvr: "5",
      unitPrice: "67",
    }))
      await page.locator(`#${id}`).fill(value);
    assert.equal(mutations.length, 0);
    await page.getByRole("button", { name: "Salvar", exact: true }).click();
    await expect.poll(() => mutations.length).toBe(1);
    assert.equal(mutations[0].body.productId, 91001);
    assert.equal(mutations[0].body.marketNicheId, 91031);
    assert.equal(mutations[0].body.hypothesisId, hypothesis.id);
    assert.equal(
      mutations[0].body.experimentType,
      "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL",
    );
    assert.equal(mutations[0].body.dailyBudget, undefined);
    assert.equal(mutations[0].body.mediaSpendLimit, undefined);
    assert.deepEqual(errors, []);
    await page.screenshot({ path: `${output}/${name}.png`, fullPage: true });
    await context.close();
    console.log(
      `PASS ${name}: cadastro do mesmo produto/nicho/hipótese sem mídia nem descoberta duplicada`,
    );
  }
} finally {
  await browser.close();
}
