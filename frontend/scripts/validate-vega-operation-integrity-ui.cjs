const { chromium, devices, expect } = require("@playwright/test");

const pageUrl =
  "http://127.0.0.1:4173/products/4/value-chain-history/processes/66/activities";
const historyPath = "/api/business-processes/66/products/4/activity-executions";
const executionPath =
  "/api/business-processes/66/products/4/activities/task-1/execution-requests";

const historicalTask = {
  taskId: 340,
  processDefinitionId: 66,
  processVersionNumber: 5,
  title: "Verificar integridade dos eventos · Vega",
  status: "BLOCKED",
  sourceReference: "experiment:91",
  assignedAgentKey: "growth-operator",
  assignedAgentNickname: "Hermes",
  comments:
    "Tentativa histórica preservada: o sucessor Facebook ainda está planejado.",
  evidenceJson: null,
  executionError: null,
  inputTokens: 154996,
  cachedInputTokens: 118144,
  outputTokens: 2729,
  estimatedCostUsd: 0.2492456,
  costEstimationStatus: "ESTIMATED",
  createdAt: "2026-09-05T03:00:00Z",
  startedAt: "2026-09-05T03:00:01Z",
  finishedAt: "2026-09-05T03:02:00Z",
  modelCode: "gpt-5.6-sol",
  reasoningEffort: "HIGH",
  productInternalName: "Vega",
  promptSent: null,
};

const history = {
  productId: 4,
  productName: "Método MUSA - Presença Elegante em 7 Dias",
  productInternalName: "Vega",
  commercialPlanId: 3,
  commercialPlanName: "Plano Comercial · Vega",
  selectedProcessDefinitionId: 66,
  processCode: "operacao-otimizacao-experimento",
  processName: "Operação e otimização de experimento",
  selectedProcessVersionNumber: 5,
  selectedProcessStatus: "PUBLISHED",
  currentExecutionReference: "experiment:90",
  operationalState: "NOT_STARTED",
  objectiveAchieved: false,
  selectedActivityCount: 1,
  completedActivityCount: 0,
  remainingActivityCount: 1,
  blockedActivityCount: 0,
  currentActivityId: "task-1",
  currentActivityName: "Verificar integridade dos eventos",
  currentActivityState: "NOT_STARTED",
  currentActivityStateReason:
    "O experimento ativo #90 ainda não possui execução desta atividade.",
  activityCount: 1,
  activitiesWithTasksCount: 1,
  uniqueTaskCount: 1,
  knownEstimatedCostUsd: 0.2492456,
  costCoverage: "COMPLETE",
  activities: [
    {
      activityDefinitionId: 610,
      activityId: "task-1",
      activityName: "Verificar integridade dos eventos",
      activityObjective: "Confirma funil e deduplicação antes de otimizar.",
      activityOwnerName: "Hermes",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "NOT_STARTED",
      stateReason:
        "O ciclo atual é o experimento #90; a tentativa #340 do #91 permanece histórica.",
      objectiveAchieved: false,
      stateEvidence: "NONE",
      activityInstanceId: null,
      occurrenceNumber: null,
      taskCount: 1,
      tasks: [historicalTask],
      executionRequestAvailable: true,
      executionRequestReason:
        "Contrato Estratégico de Mercado v2 íntegro e pronto para Hermes.",
    },
  ],
};

async function validateDevice(browser, name, device) {
  const context = await browser.newContext(device);
  const page = await context.newPage();
  let executionRequests = 0;
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (request.method() === "GET" && url.pathname === historyPath) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(history),
      });
      return;
    }
    if (request.method() === "POST" && url.pathname === executionPath) {
      executionRequests += 1;
      const body = request.postDataJSON();
      if (body && Object.keys(body).length > 0) {
        throw new Error(
          `${name}: o comando simples enviou payload inesperado.`,
        );
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          processDefinitionId: 66,
          productId: 4,
          activityId: "task-1",
          sourceReference: "experiment:90",
          tasks: [{ id: 341 }],
          operationalState: "PENDING",
          objectiveAchieved: false,
          message: "Tarefa #341 aberta para o experimento ativo #90.",
        }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    });
  });

  await page.goto(pageUrl, { waitUntil: "networkidle" });
  await page
    .getByRole("heading", {
      name: /Vega · Operação e otimização de experimento/,
    })
    .waitFor();
  await expect(page.getByText(/Tarefa #340/)).toBeVisible();
  await expect(
    page.getByText(/experimento #90; a tentativa #340/).first(),
  ).toBeVisible();
  const execute = page.getByRole("button", { name: "Executar atividade" });
  await expect(execute).toBeVisible();
  await execute.click();
  await expect(page.getByRole("status")).toContainText(
    "Tarefa #341 aberta para o experimento ativo #90.",
  );
  if (executionRequests !== 1) {
    throw new Error(
      `${name}: esperado um POST para o experimento ativo, recebido ${executionRequests}.`,
    );
  }
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1,
  );
  if (overflow) throw new Error(`${name}: a tela possui overflow horizontal.`);
  await page.screenshot({
    path: `/tmp/vega-operacao-${name.toLowerCase().replaceAll(" ", "-")}.png`,
    fullPage: true,
  });
  await context.close();
}

(async () => {
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    headless: true,
  });
  try {
    await validateDevice(browser, "Desktop", {
      viewport: { width: 1440, height: 1000 },
    });
    await validateDevice(browser, "iPhone 15 Pro", devices["iPhone 15 Pro"]);
    await validateDevice(browser, "Pixel 7", devices["Pixel 7"]);
    console.log(
      "Operação do Vega aprovada em desktop, iPhone 15 Pro e Pixel 7.",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
