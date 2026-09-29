import assert from "node:assert/strict";
import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1400 },
    locale: "pt-BR",
  });
  page.setDefaultTimeout(120_000);
  await page.goto(
    "http://191.252.181.168:5173/financial/plans?productId=10&environment=LIVE",
    { waitUntil: "networkidle", timeout: 60_000 },
  );

  await page.getByRole("button", { name: /Criar (plano do produto|nova revisão)/ }).click();
  await page.getByRole("button", { name: "Salvar preparação e calcular" }).waitFor();
  const alertText = await page.locator('[role="alert"],.alert').allInnerTexts();
  assert.equal(
    alertText.some((text) => text.includes("venceu ou mudou de contexto")),
    false,
    `A preparação continua bloqueada: ${alertText.join(" | ")}`,
  );

  await page.locator('input[name="supportDays"]').fill("30");
  await page.locator('select[name="personalizedAi"]').selectOption("true");
  const [preparationResponse] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" &&
        candidate.url().includes("/api/financial-plans/v1/products/10/preparation?environment=LIVE"),
    ),
    page.getByRole("button", { name: "Salvar preparação e calcular" }).click(),
  ]);
  const preparationBody = await preparationResponse.text();
  assert.equal(
    preparationResponse.ok(),
    true,
    `Rebase financeiro falhou: HTTP ${preparationResponse.status()} ${preparationBody}`,
  );
  const plan = JSON.parse(preparationBody);
  assert.equal(plan.scope, "PRODUCT");
  assert.equal(plan.scopeId, 10);
  assert.equal(plan.environment, "LIVE");
  assert.equal(plan.revision, 3);
  assert.equal(plan.commercialPlanId, 8);
  assert.equal(plan.commercialPlanVersion, 6);
  assert.equal(plan.assumptions?.productVersion, "MIRA_COMMERCIAL_V1");
  assert.equal(Number(plan.assumptions?.priceBrl), 49);
  assert.equal(Number(plan.assumptions?.maximumCacBrl), 25);
  assert.equal(Number(plan.assumptions?.variableCostEnvelope?.amountPerCustomerBrl), 14);
  assert.equal(Number(plan.assumptions?.fixedCostEnvelope?.amountPerPeriodBrl), 0);
  assert.equal(plan.stale, false);
  assert.equal(plan.canRequestAnalysis, true);

  await page.getByRole("button", { name: "Solicitar parecer de Plutus" }).waitFor();
  const [analysisResponse] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" &&
        candidate.url().includes(
          `/api/financial-plans/v1/products/10/revisions/${plan.id}/analysis?environment=LIVE`,
        ),
    ),
    page.getByRole("button", { name: "Solicitar parecer de Plutus" }).click(),
  ]);
  const analysisBody = await analysisResponse.text();
  assert.equal(
    analysisResponse.ok(),
    true,
    `Solicitação a Plutus falhou: HTTP ${analysisResponse.status()} ${analysisBody}`,
  );
  const requested = JSON.parse(analysisBody);
  assert.equal(requested.id, plan.id);
  assert.ok(requested.analysis?.executionId, "A solicitação não abriu execução auditável.");
  assert.ok(
    ["PENDING", "RUNNING", "COMPLETED"].includes(requested.analysis?.status),
    `Status inesperado de Plutus: ${requested.analysis?.status}`,
  );

  console.log(
    JSON.stringify(
      {
        financialPlan: {
          id: plan.id,
          revision: plan.revision,
          commercialPlanId: plan.commercialPlanId,
          commercialPlanVersion: plan.commercialPlanVersion,
          productVersion: plan.assumptions.productVersion,
          priceBrl: plan.assumptions.priceBrl,
          maximumCacBrl: plan.assumptions.maximumCacBrl,
          variableEnvelopeBrl:
            plan.assumptions.variableCostEnvelope.amountPerCustomerBrl,
          fixedEnvelopeBrl: plan.assumptions.fixedCostEnvelope.amountPerPeriodBrl,
          realizedCostBaseline: plan.assumptions.realizedCostBaseline,
          stale: plan.stale,
          canRequestAnalysis: plan.canRequestAnalysis,
        },
        plutus: requested.analysis,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
