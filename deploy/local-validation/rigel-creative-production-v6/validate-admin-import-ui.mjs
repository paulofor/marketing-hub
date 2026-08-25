import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
import { join, resolve } from "node:path";

const localDirectory = resolve(process.argv[2] ?? ".");
const outputDirectory = resolve(
  process.argv[3] ?? join(localDirectory, "evidence/admin-ui"),
);
const repositoryRoot = resolve(localDirectory, "../../..");
const frontendDirectory = join(repositoryRoot, "frontend");
const require = createRequire(join(frontendDirectory, "package.json"));
const { chromium, devices } = require("@playwright/test");
const { createServer } = require("vite");
await mkdir(outputDirectory, { recursive: true });
process.chdir(frontendDirectory);

const viteServer = await createServer({
  root: frontendDirectory,
  server: { host: "127.0.0.1", port: 0, strictPort: true },
});
await viteServer.listen();
const serverAddress = viteServer.httpServer?.address();
if (!serverAddress || typeof serverAddress === "string") {
  await viteServer.close();
  throw new Error("Vite não informou a porta efêmera usada na homologação.");
}
const baseUrl = `http://127.0.0.1:${serverAddress.port}`;

const plan = {
  id: 4,
  name: "Rigel · Kit WhatsApp Pronto",
  planType: "FIRST_SALE",
  status: "IN_PROGRESS",
  experimentId: 89,
  deadline: "2026-08-31",
  daysRemaining: 6,
  maxBudget: 400,
  targetRevenue: 1745,
  offerPriceBrl: 349,
  actualCampaignCost: 0,
  actualAiCost: 0,
  actualTotalCost: 0,
  actualRevenue: 0,
  milestones: [],
  simulations: [],
};
const importedAssets = [
  {
    id: 101,
    assetUrl:
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
    mediaType: "IMAGE",
    label: "rigel-direct-card-1080x1350.png",
    purpose: "ADS",
    purposes: ["ADS", "SOCIAL"],
    origin: "Compositor determinístico",
    rightsStatement: "Uso autorizado",
    contentSha256: "a".repeat(64),
    creativePackageId: "b".repeat(64),
    versionNumber: 1,
    status: "APPROVED",
    agentReviewStatus: "APPROVED",
    agentReviewSummary: "Oferta, prova e CTA coerentes.",
    customerReviewStatus: "APPROVED",
    customerReviewSummary: "Benefício e controle manual compreensíveis.",
    createdAt: "2026-08-25T10:00:00Z",
    updatedAt: "2026-08-25T10:00:00Z",
  },
];

async function fulfillApi(route, state) {
  const request = route.request();
  const path = new URL(request.url()).pathname;
  if (
    path.endsWith("/visual-assets/approved-package") &&
    request.method() === "POST"
  ) {
    state.imports += 1;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(importedAssets),
    });
    return;
  }
  let body = [];
  if (path === "/api/planning/commercial-plans") body = [plan];
  else if (path.endsWith("/visual-assets"))
    body = state.imports > 0 ? importedAssets : [];
  else if (path.endsWith("/operational-flow")) {
    body = {
      commercialPlanId: 4,
      currentStage: "CREATIVES",
      status: "EM_ANDAMENTO",
      nextAction: "Importar o pacote criativo aprovado.",
      expectedMetric: "Pacote aprovado e auditável",
      decisionCriterion: "Psique e Têmis aprovam sem ajustes",
      stages: [{ code: "CREATIVES", label: "Criativos", status: "ATUAL" }],
      specialistDecisions: [],
    };
  } else if (path.endsWith("/agent-activity")) {
    body = {
      commercialPlanId: 4,
      currentVersion: 3,
      videoBudgetLimitUsd: 0,
      videoKnownCostUsd: 0,
      openTasks: 0,
      pendingDecisions: 0,
      entries: [],
    };
  }
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iphone-15-pro", devices["iPhone 15 Pro"]],
  ["pixel-7", devices["Pixel 7"]],
];
const results = [];
let browser;
try {
  browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ??
      process.env.CHROMIUM_BIN ??
      process.env.CHROME_BIN,
  });
  for (const [name, profile] of profiles) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const state = { imports: 0 };
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") errors.push(message.text());
    });
    await page.route("http://127.0.0.1/api/**", (route) =>
      fulfillApi(route, state),
    );
    await page.goto(`${baseUrl}/planning/4`, { waitUntil: "networkidle" });
    const packageInput = page.getByLabel("Pacote ZIP aprovado *");
    try {
      await packageInput.waitFor({ state: "visible", timeout: 15_000 });
    } catch (error) {
      throw new Error(
        `${name}: formulário não renderizado após a hidratação. URL=${page.url()} BODY=${(await page.locator("body").innerText()).slice(0, 4000)} ERRORS=${errors.join(" | ")} CAUSE=${error instanceof Error ? error.message : String(error)}`,
      );
    }
    await packageInput.setInputFiles({
      name: "rigel-approved.zip",
      mimeType: "application/zip",
      buffer: Buffer.from("pacote-local-auditavel"),
    });
    const button = page.getByRole("button", { name: "Importar pacote" });
    if (await button.isEnabled())
      throw new Error(`${name}: importação liberada sem decisão humana.`);
    await page.getByLabel("Selecionei este pacote para uso no plano *").check();
    await button.click();
    await page
      .getByText("Pacote importado com peças e pareceres auditáveis.")
      .waitFor();
    await page.getByText(/Percepção de Psique: APPROVED/).waitFor();
    const overflow = await page.evaluate(
      () =>
        document.documentElement.scrollWidth >
        document.documentElement.clientWidth,
    );
    if (overflow) throw new Error(`${name}: a tela criou overflow horizontal.`);
    if (state.imports !== 1)
      throw new Error(`${name}: importação executada ${state.imports} vezes.`);
    if (errors.length > 0)
      throw new Error(`${name}: erros no navegador: ${errors.join(" | ")}`);
    await page.screenshot({
      path: join(outputDirectory, `${name}.png`),
      fullPage: true,
      animations: "disabled",
      caret: "hide",
      timeout: 90_000,
    });
    results.push({
      device: name,
      status: "APPROVED",
      imports: state.imports,
      overflow: false,
    });
    await context.close();
  }
} finally {
  if (browser) await browser.close();
  await viteServer.close();
}

process.stdout.write(
  `${JSON.stringify({ status: "APPROVED", devices: results }, null, 2)}\n`,
);
