import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

const product = {
  id: 9,
  slug: "kit-whatsapp-pronto",
  name: "Kit WhatsApp Pronto",
  internalName: "Rigel",
  commercialStatus: "COMUNICACAO_E_JORNADA",
  currentPriceBrl: 349,
  niche: "Prestadores locais",
  avatar: "Profissionais que vendem pelo WhatsApp",
  explicitPain: "Atendimento improvisado",
  promise: "Atendimento sob medida",
  uniqueMechanism: "Receitas prontas",
  tripwire: "Diagnóstico",
  riskReversal: "Garantia",
  socialProof: "Provas aprovadas",
  checkoutMonetization: "R$ 349",
  funnel: "Microexperiência",
  creativeVolume: "Pacote aprovado",
  storytelling: "Clareza",
  aiCost: 0,
};

const position = {
  productId: 9,
  commercialStatus: "COMUNICACAO_E_JORNADA",
  resolutionStatus: "IDENTIFIED",
  resolutionMessage: "Posição identificada.",
  chainDefinitionId: 5,
  chainName: "Criação e entrega de valor de Produtos Digitais Experienciais",
  chainVersion: 5,
  processDefinitionId: 43,
  processCode: "pde-communication-sales-journey",
  processName: "Comunicação e jornada de venda do PDE",
  processVersion: 4,
  sequenceNumber: 4,
  processCount: 6,
  processMeasurements: [
    {
      stageType: "PROCESS",
      trackingStatus: "COMPLETED",
      processDefinitionId: 38,
      processCode: "pde-commercial-plan-offer",
      processName: "Plano Comercial e desenho da oferta PDE",
      enteredAt: "2026-08-21T03:55:09Z",
      entryEvidence: "FIRST_PROCESS_EXECUTION",
      exitedAt: "2026-08-21T17:22:51Z",
      exitEvidence: "NEXT_PROCESS_EXECUTION_STARTED",
      objectiveAchieved: true,
      elapsedDays: 0,
      knownEstimatedCostUsd: 1.47759052,
      costCoverage: "COMPLETE",
      costedExecutionCount: 37,
      uncostedExecutionCount: 0,
    },
    {
      stageType: "PROCESS",
      trackingStatus: "CURRENT",
      processDefinitionId: 43,
      processCode: "pde-communication-sales-journey",
      processName: "Comunicação e jornada de venda do PDE",
      enteredAt: "2026-08-22T13:38:03Z",
      entryEvidence: "BACKFILLED_EXECUTION_HISTORY",
      exitedAt: null,
      exitEvidence: null,
      objectiveAchieved: false,
      elapsedDays: 3,
      knownEstimatedCostUsd: 3.5887712,
      costCoverage: "PARTIAL",
      costedExecutionCount: 12,
      uncostedExecutionCount: 2,
    },
  ],
  subprocessPosition: {
    trackingStatus: "IN_PROGRESS",
    subprocessCount: 2,
    currentActivityName: "Executar criação e aprovação de criativos",
    currentSubprocessDefinitionId: 48,
    currentSubprocessCode: "creative-production-approval",
    currentSubprocessName: "Criação e Aprovação de Criativos",
    currentSubprocessObjective: "Criativos aprovados.",
    nextSubprocessDefinitionId: 18,
    nextSubprocessCode: "landing-page-generation",
    nextSubprocessName: "Geração de landing page",
    nextSubprocessObjective:
      "Landing aprovada e pronta para publicação humana.",
    measurements: [
      {
        stageType: "SUBPROCESS",
        trackingStatus: "CURRENT",
        processDefinitionId: 48,
        processCode: "creative-production-approval",
        processName: "Criação e Aprovação de Criativos",
        enteredAt: null,
        entryEvidence: "NOT_RECORDED",
        exitedAt: null,
        exitEvidence: null,
        objectiveAchieved: false,
        elapsedDays: null,
        knownEstimatedCostUsd: 0,
        costCoverage: "NO_EXECUTIONS",
        costedExecutionCount: 0,
        uncostedExecutionCount: 0,
      },
    ],
  },
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN,
});
try {
  for (const [profileName, contextOptions] of profiles) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const pathname = new URL(route.request().url()).pathname;
      if (pathname === "/api/products/value-chain-positions/9") {
        await route.fulfill({ json: position });
      } else if (pathname === "/api/products/value-chain-positions") {
        await route.fulfill({ json: [position] });
      } else if (pathname === "/api/products/9") {
        await route.fulfill({ json: product });
      } else if (pathname === "/api/products") {
        await route.fulfill({ json: [product] });
      } else {
        await route.fulfill({ status: 404, json: { message: "not mocked" } });
      }
    });

    await page.goto(`${baseUrl}/products`, { waitUntil: "domcontentloaded" });
    const historyLink = page.getByRole("link", { name: "Histórico da cadeia" });
    await expect(historyLink).toBeVisible();
    await historyLink.click();
    await expect(page).toHaveURL(/\/products\/9\/value-chain-history$/);
    await expect(
      page.getByRole("heading", { name: "Histórico da cadeia de valor" }),
    ).toBeVisible();
    await expect(page.getByText("Etapa 4 de 6")).toBeVisible();
    await expect(page.getByText("21/08/2026, 03:55 UTC")).toBeVisible();
    await expect(page.getByText(/cobertura parcial/i)).toBeVisible();
    await expect(
      page.getByText("Data e hora ainda não registradas"),
    ).toBeVisible();
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/product-value-chain-history-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Histórico da cadeia aprovado em desktop, iPhone 15 Pro e Pixel 7.",
);
