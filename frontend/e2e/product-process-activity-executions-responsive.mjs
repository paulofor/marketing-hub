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
  name: "Kit WhatsApp Pronto",
  internalName: "Rigel",
  commercialStatus: "COMUNICACAO_E_JORNADA",
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
  processMeasurements: [],
  subprocessPosition: {
    trackingStatus: "IN_PROGRESS",
    subprocessCount: 2,
    currentActivityName: "Avaliar percepção da cliente",
    currentSubprocessDefinitionId: 18,
    currentSubprocessSequenceNumber: 2,
    currentSubprocessCode: "landing-page-generation",
    currentSubprocessName: "Geração de landing page",
    currentSubprocessObjective: "Landing pronta para publicação humana.",
    nextSubprocessDefinitionId: null,
    nextSubprocessCode: null,
    nextSubprocessName: null,
    nextSubprocessObjective: null,
    measurements: [
      {
        stageType: "SUBPROCESS",
        sequenceLabel: "4.2",
        trackingStatus: "CURRENT",
        processDefinitionId: 18,
        processCode: "landing-page-generation",
        processName: "Geração de landing page",
        enteredAt: "2026-08-27T03:26:19Z",
        entryEvidence: "FIRST_SUBPROCESS_TASK",
        exitedAt: null,
        exitEvidence: null,
        objectiveAchieved: false,
        elapsedDays: 0,
        knownEstimatedCostUsd: 1.617624,
        costCoverage: "PARTIAL",
        costedExecutionCount: 2,
        uncostedExecutionCount: 1,
      },
    ],
  },
};

const dedaloTask = {
  taskId: 243,
  processDefinitionId: 18,
  processVersionNumber: 4,
  title: "Experimento #89 · construir e homologar a landing",
  status: "COMPLETED",
  sourceReference: "commercial-plan:4@v3:journey",
  assignedAgentKey: "landing-generator",
  assignedAgentNickname: "Dédalo",
  comments: JSON.stringify({
    summary: "Landing construída com os ativos aprovados.",
  }),
  evidenceJson: JSON.stringify({ approvalRecommendation: "APPROVE" }),
  inputTokens: 947056,
  cachedInputTokens: 796288,
  outputTokens: 25323,
  estimatedCostUsd: 1.4280472,
  costEstimationStatus: "ESTIMATED",
  createdAt: "2026-08-27T03:26:19Z",
  startedAt: "2026-08-27T03:26:45Z",
  finishedAt: "2026-08-27T03:35:14Z",
  modelCode: "gpt-5.6-sol",
  reasoningEffort: null,
  productInternalName: "Rigel",
  promptSent: "Construa a landing com ativos aprovados.",
};

const psiqueTask = {
  ...dedaloTask,
  taskId: 244,
  title: "Avaliar percepção da cliente",
  status: "BLOCKED",
  assignedAgentKey: "customer-agent",
  assignedAgentNickname: "Psique",
  comments: "Checkout ausente na evidência original.",
  estimatedCostUsd: 0.1895768,
  modelCode: null,
};

const themisTask = {
  ...dedaloTask,
  taskId: 245,
  title: "Executar revisão comercial independente",
  status: "PENDING",
  assignedAgentKey: "meta-ad-approver",
  assignedAgentNickname: "Têmis",
  comments: null,
  estimatedCostUsd: null,
  costEstimationStatus: "NOT_REPORTED",
  startedAt: null,
  finishedAt: null,
  modelCode: null,
  promptSent: null,
};

const activityDefinitions = [
  ["select", "Selecionar provas reais da entrega", "Dédalo", [dedaloTask]],
  ["strategy", "Definir estratégia de conversão", "Dédalo", [dedaloTask]],
  [
    "compose",
    "Solicitar composição ou edição visual quando necessária",
    "Backend / Têmis",
    [dedaloTask],
  ],
  [
    "html",
    "Construir HTML completo com ativos aprovados",
    "Dédalo",
    [dedaloTask],
  ],
  ["technical", "Validar técnica e fidelidade visual", "Quality Review", []],
  ["customer", "Avaliar percepção da cliente", "Psique", [psiqueTask]],
  [
    "commercial",
    "Executar revisão comercial independente",
    "Têmis",
    [themisTask],
  ],
  ["human", "Aprovação humana para publicar", "Operador humano", []],
];

const history = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
  selectedProcessDefinitionId: 18,
  processCode: "landing-page-generation",
  processName: "Geração de landing page",
  selectedProcessVersionNumber: 4,
  selectedProcessStatus: "PUBLISHED",
  activityCount: 8,
  activitiesWithTasksCount: 6,
  uniqueTaskCount: 3,
  knownEstimatedCostUsd: 1.617624,
  costCoverage: "PARTIAL",
  activities: activityDefinitions.map(
    ([activityId, activityName, activityOwnerName, tasks], index) => ({
      activityDefinitionId: 119 + index,
      activityId,
      activityName,
      activityObjective: `Objetivo auditável de ${activityName}.`,
      activityOwnerName,
      sequenceNumber: index + 1,
      selectedVersionActivity: true,
      taskCount: tasks.length,
      tasks,
    }),
  ),
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
    await page.route(
      (url) => url.pathname.startsWith("/api/"),
      async (route) => {
        const pathname = new URL(route.request().url()).pathname;
        if (pathname === "/api/products/9") {
          await route.fulfill({ json: product });
        } else if (pathname === "/api/products/value-chain-positions/9") {
          await route.fulfill({ json: position });
        } else if (pathname === "/api/products/9/process-commits") {
          await route.fulfill({ json: [] });
        } else if (
          pathname ===
          "/api/business-processes/18/products/9/activity-executions"
        ) {
          await route.fulfill({ json: history });
        } else {
          await route.fulfill({ status: 404, json: { message: "not mocked" } });
        }
      },
    );

    await page.goto(`${baseUrl}/products/9/value-chain-history`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Histórico da cadeia de valor" }),
    ).toBeVisible();
    await page.getByRole("link", { name: "Atividades e tarefas" }).click();
    await expect(page).toHaveURL(
      /\/products\/9\/value-chain-history\/processes\/18\/activities$/,
    );
    await expect(
      page.getByRole("heading", { name: "Rigel · Geração de landing page" }),
    ).toBeVisible();
    await expect(page.getByText("6 com tarefas reais")).toBeVisible();
    await expect(page.getByText("Cobertura parcial")).toBeVisible();
    await expect(page.getByText(/Tarefa #243/).first()).toBeVisible();
    await expect(page.getByText("gpt-5.6-sol").first()).toBeVisible();
    await expect(
      page.getByText("Nenhuma tarefa registrada para este produto.").first(),
    ).toBeVisible();
    await page.getByText("Visualizar JSON em árvore").first().click();
    await expect(page.getByText("summary:").first()).toBeVisible();
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
      path: `/tmp/product-process-activity-executions-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Atividades e tarefas do produto aprovadas em desktop, iPhone 15 Pro e Pixel 7.",
);
