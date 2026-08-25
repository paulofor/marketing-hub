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
  productTypeInternalName: "Opala",
  commercialStatus: "COMUNICACAO_E_JORNADA",
  currentPriceBrl: 349,
  targetAudience: "Profissionais que vendem pelo WhatsApp",
  primaryHypothesis: "Responder clientes com clareza e velocidade.",
  primaryCta: "Quero o kit",
  updatedAt: "2026-08-25T12:00:00Z",
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
      trackingStatus: "CURRENT",
      processDefinitionId: 43,
      processCode: "pde-communication-sales-journey",
      processName: "Comunicação e jornada de venda do PDE",
      enteredAt: "2026-08-20T12:00:00Z",
      entryEvidence: "COMMERCIAL_STATUS_TRANSITION",
      exitedAt: null,
      exitEvidence: null,
      objectiveAchieved: false,
      elapsedDays: 5,
      knownEstimatedCostUsd: 3.25,
      costCoverage: "PARTIAL",
      costedExecutionCount: 8,
      uncostedExecutionCount: 1,
    },
  ],
  subprocessPosition: {
    trackingStatus: "IN_PROGRESS",
    subprocessCount: 2,
    currentActivityName: "Criar e aprovar peças",
    currentSubprocessDefinitionId: 17,
    currentSubprocessCode: "creative-production-approval",
    currentSubprocessName: "Criação e aprovação de criativos",
    currentSubprocessObjective: "Criativos aprovados e prontos.",
    nextSubprocessDefinitionId: 18,
    nextSubprocessCode: "landing-page-generation",
    nextSubprocessName: "Geração de landing page",
    nextSubprocessObjective: "Landing aprovada para publicação.",
    measurements: [
      {
        stageType: "SUBPROCESS",
        trackingStatus: "CURRENT",
        processDefinitionId: 17,
        processCode: "creative-production-approval",
        processName: "Criação e aprovação de criativos",
        enteredAt: "2026-08-22T12:00:00Z",
        entryEvidence: "FIRST_SUBPROCESS_TASK",
        exitedAt: null,
        exitEvidence: null,
        objectiveAchieved: false,
        elapsedDays: 3,
        knownEstimatedCostUsd: 1.125,
        costCoverage: "COMPLETE",
        costedExecutionCount: 4,
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
      if (pathname === "/api/products") {
        await route.fulfill({ json: [product] });
      } else if (pathname === "/api/products/value-chain-positions") {
        await route.fulfill({ json: [position] });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(baseUrl, { waitUntil: "domcontentloaded" });
    await expect(page.getByText("Etapa 4 de 6")).toBeVisible();
    await expect(page.getByText("5 dias")).toBeVisible();
    await expect(page.getByText("3 dias")).toBeVisible();
    await expect(page.getByText(/cobertura parcial/i)).toBeVisible();
    await expect(page.getByText("Próximo subprocesso")).toBeVisible();
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
      path: `/tmp/product-process-time-cost-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log("Tempo e custo aprovados em desktop, iPhone 15 Pro e Pixel 7.");
