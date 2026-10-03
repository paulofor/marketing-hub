import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir, writeFile } from "node:fs/promises";
const { chromium, devices } = createRequire(import.meta.url)(
  "@playwright/test",
);
const base = process.env.LOCAL_FRONTEND_URL || "http://127.0.0.1:5174";
assert.ok(["127.0.0.1", "localhost"].includes(new URL(base).hostname));
const output = process.env.PLANNING_DRAFT_EVIDENCE_DIR || "/tmp/planning-draft";
await mkdir(output, { recursive: true });
const product = {
  id: 91031,
  name: "Produto de QA",
  marketNicheId: 91032,
  currentPriceBrl: 59,
  desireAssociationMapJson: JSON.stringify({
    territories: [{ code: "TRANQUILITY", name: "Tranquilidade" }],
  }),
};
const hypothesis = {
  id: "00000000-0000-4000-8000-000000091033",
  productId: product.id,
  title: "Hipótese de QA",
};
const financialBase = "/api/financial-plans/v1";
const financialEndpoint = `${financialBase}/products/${product.id}`;
const previousFinance = {
  id: 91036,
  scope: "PRODUCT",
  scopeId: product.id,
  environment: "LIVE",
  name: "Referência financeira anterior de QA",
  revision: 1,
  commercialPlanId: 91034,
  createdBy: "QA",
  createdAt: "2026-10-01T00:00:00Z",
  stale: true,
  assumptions: {
    productVersion: "v1",
    periodDays: 30,
    validUntil: "2026-10-02",
    evidence: "Referência histórica de QA",
    priceBrl: 59,
    minimumMarginPercent: null,
    maximumCacBrl: null,
    preparation: { supportDays: 7, personalizedAi: true },
    variableCostEnvelope: {
      amountPerCustomerBrl: 11,
      coverage: "ALL_VARIABLE_COSTS_EXCLUDING_CAC",
      sourceReference: "QA anterior",
      checkedOn: "2026-10-01",
    },
    fixedCostEnvelope: {
      amountPerPeriodBrl: 45,
      coverage: "ALL_FIXED_OPERATIONAL_COSTS_FOR_PERIOD",
      sourceReference: "QA anterior",
      checkedOn: "2026-10-01",
    },
    ai: { currency: "BRL", providerModel: null, perAttempt: null },
    costs: {},
    scenarios: ["CONSERVATIVE", "BASE", "OPTIMISTIC"].map((code) => ({
      code,
      customers: null,
      attemptsPerCustomer: null,
      cacBrl: null,
    })),
  },
  evaluation: {
    status: "INPUTS_MISSING",
    label: "Premissas pendentes",
    blockers: ["Custos ainda sem fonte"],
    scenarios: [],
  },
  pendingActions: ["Completar fontes antes do parecer"],
  canRequestAnalysis: false,
  analysis: null,
};
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [device, settings] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(settings);
    const page = await context.newPage();
    const posts = [],
      errors = [],
      dialogs = [];
    let history = [previousFinance];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("dialog", async (dialog) => {
      dialogs.push(dialog.message());
      await dialog.accept();
    });
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.origin !== base) {
        errors.push("Requisição externa: " + url.origin);
        return route.abort();
      }
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (request.method() === "POST") {
        const payload = request.postDataJSON();
        posts.push({ endpoint: url.pathname, payload });
        assert.ok(
          [
            "/api/experiments",
            "/api/planning/commercial-plans",
            financialEndpoint,
          ].includes(url.pathname),
        );
        if (url.pathname === financialEndpoint) {
          const revision = {
            ...previousFinance,
            ...payload,
            id: 91037,
            revision: 2,
            stale: false,
          };
          history = [revision, previousFinance];
          return route.fulfill({ json: revision });
        }
        return route.fulfill({
          json: { id: 91034, ...payload, status: "PLANNED" },
        });
      }
      let json = [];
      if (url.pathname === "/api/products") json = [product];
      else if (url.pathname === "/api/niches")
        json = [{ id: 91032, name: "Nicho de QA" }];
      else if (url.pathname.includes("/hypotheses")) json = [hypothesis];
      else if (url.pathname === "/api/journey-templates")
        json = { content: [{ id: 91035, name: "Jornada de QA" }] };
      else if (url.pathname === financialBase + "/catalog")
        json = {
          products: [
            { id: product.id, name: product.name, productTypeId: 91038 },
          ],
          productTypes: [{ id: 91038, name: "Tipo de QA" }],
          commercialPlans: [{ id: 91034, name: "Preparação QA sem mídia" }],
        };
      else if (url.pathname === financialEndpoint) json = history;
      return route.fulfill({ json });
    });
    await page.goto(
      `${base}/experiments/new?productId=${product.id}&nicheId=91032&hypothesisId=${hypothesis.id}`,
      { waitUntil: "networkidle" },
    );
    await page.locator("#desireTerritoryCode").selectOption("TRANQUILITY");
    for (const [field, value] of [
      [
        "commercialObjective",
        "Preparar sem mídia; metas financeiras ainda pendentes",
      ],
      ["singlePain", "Esforço para preparar divulgação"],
      ["funnelPromise", "Conteúdo pronto para divulgar"],
      ["primaryCta", "Conhecer o kit"],
    ])
      await page.locator("#" + field).fill(value);
    assert.equal(await page.locator("#targetCvr").inputValue(), "");
    await page.getByRole("button", { name: "Salvar", exact: true }).click();
    await page.waitForFunction(
      () => !document.querySelector("button.btn-primary")?.disabled,
    );
    await page.waitForTimeout(300);
    assert.deepEqual(dialogs, ["Teste salvo!"]);
    const draft = posts.find(
      (post) => post.endpoint === "/api/experiments",
    ).payload;
    assert.equal(draft.productId, product.id);
    for (const field of [
      "targetCvr",
      "baselineCvr",
      "sampleSize",
      "dailyBudget",
      "mediaSpendLimit",
      "imageModelId",
      "creativesToGenerate",
    ])
      assert.equal(draft[field], undefined);
    await page.goto(base + "/planning", { waitUntil: "networkidle" });
    await page.getByRole("button", { name: "Novo plano comercial" }).click();
    await page
      .locator("#new-commercial-plan-name")
      .fill("Preparação QA sem mídia");
    await page.locator("#new-commercial-plan-deadline").fill("2026-10-10");
    await page.locator("#new-commercial-plan-budget").fill("0");
    await page
      .locator("#new-commercial-plan-objective")
      .fill("Preparar a entrega sem consumo pago e sem entrevistar pessoas");
    await page.screenshot({
      path: `${output}/${device}-new-plan.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Criar e abrir plano" }).click();
    await page.waitForTimeout(300);
    const plan = posts.find(
      (post) => post.endpoint === "/api/planning/commercial-plans",
    ).payload;
    assert.equal(plan.customerConversationsTarget, 0);
    assert.equal(plan.experimentsToPublish, 0);
    assert.equal(plan.maxBudget, 0);
    await page.goto(`${base}/financial/plans?productId=${product.id}`, {
      waitUntil: "networkidle",
    });
    await page
      .locator("summary")
      .filter({ hasText: "Edição financeira avançada" })
      .click();
    await page
      .getByRole("button", { name: "Editar premissas detalhadas" })
      .click();
    await page
      .locator('[name="name"]')
      .fill("Candidata QA — economia pendente");
    await page.locator('[name="createdBy"]').fill("QA local");
    await page.locator('[name="productVersion"]').fill("candidate-v1");
    await page.locator('[name="validUntil"]').fill("2026-10-10");
    await page.locator('[name="personalizedAi"]').selectOption("false");
    await page.locator('[name="replaceVariableCostEnvelope"]').check();
    await page.locator('[name="replaceFixedCostEnvelope"]').check();
    await page
      .locator('[name="evidence"]')
      .fill(
        "Composição local comprovada; custos restantes ainda sem fonte. Nenhum gasto autorizado.",
      );
    await page
      .getByRole("button", { name: "Salvar revisão e calcular" })
      .click();
    await page
      .getByText("Revisão salva. Os cálculos abaixo", { exact: false })
      .waitFor();
    const finance = posts.find(
      (post) => post.endpoint === financialEndpoint,
    ).payload;
    assert.equal(finance.assumptions.preparation.personalizedAi, false);
    assert.equal(finance.assumptions.variableCostEnvelope, null);
    assert.equal(finance.assumptions.fixedCostEnvelope, null);
    assert.ok(
      Object.values(finance.assumptions.costs).every((value) => value === null),
    );
    assert.equal(previousFinance.assumptions.preparation.personalizedAi, true);
    await page.screenshot({
      path: `${output}/${device}-financial-draft.png`,
      fullPage: true,
    });
    assert.deepEqual(errors, []);
    results.push({
      device,
      draftWithoutInventedMetrics: true,
      planWithoutInterviews: true,
      financialRevisionWithoutInheritedCosts: true,
      simulatedMutations: posts.length,
    });
    await context.close();
  }
  await writeFile(
    `${output}/results.json`,
    JSON.stringify({ results, paidCalls: 0, productionMutations: 0 }, null, 2),
  );
  console.log(
    JSON.stringify({
      tests: results.length,
      success: true,
      paidCalls: 0,
      productionMutations: 0,
    }),
  );
} finally {
  await browser.close();
}
