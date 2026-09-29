import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1400 }, locale: "pt-BR" });
  page.setDefaultTimeout(120_000);
  await page.goto("http://191.252.181.168:5173/financial/plans?productId=10&environment=LIVE", { waitUntil: "networkidle", timeout: 60_000 });
  console.log("Página financeira carregada");
  await page.getByRole("button", { name: "Criar plano do produto" }).click();
  await page.getByRole("button", { name: "Salvar preparação e calcular" }).waitFor();
  console.log("Preparação Safira carregada");
  await page.locator('input[name="supportDays"]').fill("30");
  await page.locator('select[name="personalizedAi"]').selectOption("true");
  const [response] = await Promise.all([
    page.waitForResponse((candidate) => candidate.request().method() === "POST" && candidate.url().includes("/api/financial-plans/v1/products/10/preparation?environment=LIVE")),
    page.getByRole("button", { name: "Salvar preparação e calcular" }).click(),
  ]);
  console.log(`Resposta financeira HTTP ${response.status()}`);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Plano financeiro falhou: HTTP ${response.status()} ${body}`);
  const payload = JSON.parse(body);
  console.log(JSON.stringify({
    id: payload.id,
    scope: payload.scope,
    environment: payload.environment,
    name: payload.name,
    revision: payload.revision,
    commercialPlanId: payload.commercialPlanId,
    commercialPlanVersion: payload.commercialPlanVersion,
    productVersion: payload.assumptions?.productVersion,
    preparation: payload.assumptions?.preparation,
    priceBrl: payload.assumptions?.priceBrl,
    evaluation: payload.evaluation,
    pendingActions: payload.pendingActions,
    canRequestAnalysis: payload.canRequestAnalysis,
  }, null, 2));
} finally {
  await browser.close();
}
