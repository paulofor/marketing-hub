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

const plannedProcess = (
  sequenceLabel,
  processDefinitionId,
  processCode,
  processName,
) => ({
  stageType: "PROCESS",
  sequenceLabel,
  trackingStatus: "PLANNED",
  processDefinitionId,
  processCode,
  processName,
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
  commitRegistrationAllowed: false,
});

const position = {
  productId: 9,
  commercialStatus: "COMUNICACAO_E_JORNADA",
  resolutionStatus: "IDENTIFIED",
  resolutionMessage: "Posição identificada.",
  chainDefinitionId: 6,
  chainName: "Criação e entrega de valor de Produtos Digitais Experienciais",
  chainVersion: 6,
  processDefinitionId: 43,
  processCode: "pde-communication-sales-journey",
  processName: "Comunicação e jornada de venda do PDE",
  processVersion: 4,
  sequenceNumber: 4,
  processCount: 6,
  processMeasurements: [
    plannedProcess(
      "1",
      31,
      "pde-opportunity-discovery",
      "Descoberta factual da oportunidade PDE",
    ),
    plannedProcess(
      "2",
      37,
      "pde-commercial-plan-offer-v1",
      "Estratégia comercial anterior sem histórico migrado",
    ),
    {
      stageType: "PROCESS",
      sequenceLabel: "3",
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
      commitRegistrationAllowed: true,
    },
    {
      stageType: "PROCESS",
      sequenceLabel: "4",
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
      commitRegistrationAllowed: true,
    },
    plannedProcess(
      "5",
      45,
      "pde-commercial-homologation-activation",
      "Homologação e ativação comercial do PDE",
    ),
    plannedProcess(
      "6",
      46,
      "pde-sales-delivery-learning",
      "Venda, entrega e aprendizado do PDE",
    ),
  ],
  subprocessPosition: {
    trackingStatus: "COMPLETED",
    subprocessCount: 2,
    currentActivityName: "Integrar canal, checkout, acesso e eventos",
    currentSubprocessDefinitionId: null,
    currentSubprocessSequenceNumber: null,
    currentSubprocessCode: null,
    currentSubprocessName: null,
    currentSubprocessObjective: null,
    nextSubprocessDefinitionId: null,
    nextSubprocessCode: null,
    nextSubprocessName: null,
    nextSubprocessObjective: null,
    measurements: [
      {
        stageType: "SUBPROCESS",
        sequenceLabel: "4.1",
        trackingStatus: "COMPLETED",
        processDefinitionId: 48,
        processCode: "creative-production-approval",
        processName: "Criação e Aprovação de Criativos",
        enteredAt: "2026-08-25T21:33:22Z",
        entryEvidence: "FIRST_SUBPROCESS_TASK",
        exitedAt: "2026-08-25T21:33:22Z",
        exitEvidence: "SUBPROCESS_OBJECTIVE_ACHIEVED",
        objectiveAchieved: true,
        elapsedDays: 0,
        knownEstimatedCostUsd: 0.577952,
        costCoverage: "COMPLETE",
        costedExecutionCount: 4,
        uncostedExecutionCount: 0,
        commitRegistrationAllowed: true,
      },
      {
        stageType: "SUBPROCESS",
        sequenceLabel: "4.2",
        trackingStatus: "COMPLETED",
        processDefinitionId: 18,
        processCode: "landing-page-generation",
        processName: "Geração de landing page",
        enteredAt: "2026-08-27T03:26:19Z",
        entryEvidence: "FIRST_SUBPROCESS_TASK",
        exitedAt: "2026-08-28T03:09:30Z",
        exitEvidence: "SUBPROCESS_OBJECTIVE_ACHIEVED",
        objectiveAchieved: true,
        elapsedDays: 0,
        knownEstimatedCostUsd: 1.994548,
        costCoverage: "COMPLETE",
        costedExecutionCount: 4,
        uncostedExecutionCount: 0,
        commitRegistrationAllowed: true,
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
      } else if (pathname === "/api/products/9/process-commits") {
        await route.fulfill({ json: [] });
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
    await expect(
      page.getByText("Próxima atividade", { exact: true }),
    ).toBeVisible();
    const nextStep = page.getByRole("region", {
      name: "Próximo passo do processo",
    });
    await expect(nextStep).toBeVisible();
    await expect(
      nextStep.getByRole("heading", {
        name: "Integrar canal, checkout, acesso e eventos",
      }),
    ).toBeVisible();
    const nextStepLink = nextStep.getByRole("link", {
      name: "Abrir próximo passo",
    });
    await expect(nextStepLink).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/43/activities",
    );
    await expect(page.getByText("Objetivo atingido").last()).toBeVisible();
    await expect(page.getByText("4.2", { exact: true })).toBeVisible();
    await expect(page.getByText("Previsto na cadeia")).toHaveCount(4);
    await expect(
      page.getByRole("heading", {
        name: "Homologação e ativação comercial do PDE",
      }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", {
        name: "Venda, entrega e aprendizado do PDE",
      }),
    ).toBeVisible();
    await expect(page.getByText("21/08/2026, 03:55 UTC")).toBeVisible();
    await expect(page.getByText(/cobertura parcial/i)).toBeVisible();
    await expect(page.getByText("28/08/2026, 03:09 UTC")).toBeVisible();
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
    await nextStepLink.click();
    await expect(page).toHaveURL(
      /\/products\/9\/value-chain-history\/processes\/43\/activities$/,
    );
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Histórico da cadeia aprovado em desktop, iPhone 15 Pro e Pixel 7.",
);
