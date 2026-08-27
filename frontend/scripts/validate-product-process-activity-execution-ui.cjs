const { chromium, devices } = require("@playwright/test");

const pageUrl =
  "http://127.0.0.1:4173/products/4/value-chain-history/processes/45/activities";
const executionPath =
  "/api/business-processes/45/products/4/activities/pdeGate/execution-requests";

const history = {
  productId: 4,
  productName: "Método MUSA 7 Dias",
  productInternalName: "Vega",
  selectedProcessDefinitionId: 45,
  processCode: "pde-commercial-homologation-activation",
  processName: "Homologação e ativação comercial do PDE",
  selectedProcessVersionNumber: 5,
  selectedProcessStatus: "PUBLISHED",
  operationalState: "NOT_STARTED",
  objectiveAchieved: false,
  selectedActivityCount: 1,
  completedActivityCount: 0,
  remainingActivityCount: 1,
  blockedActivityCount: 0,
  currentActivityId: "pdeGate",
  currentActivityName: "Validar fatos, controle e valor do PDE",
  currentActivityState: "NOT_STARTED",
  currentActivityStateReason:
    "Nenhuma tarefa ou instância foi registrada para esta atividade.",
  activityCount: 1,
  activitiesWithTasksCount: 0,
  uniqueTaskCount: 0,
  knownEstimatedCostUsd: 0,
  costCoverage: "NO_EXECUTIONS",
  activities: [
    {
      activityDefinitionId: 301,
      activityId: "pdeGate",
      activityName: "Validar fatos, controle e valor do PDE",
      activityObjective:
        "Comprova Cartão de Decisão, prova, adequação, personalização explicável e caminho neutro.",
      activityOwnerName: "Psique e Têmis",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "NOT_STARTED",
      stateReason:
        "Nenhuma tarefa ou instância foi registrada para esta atividade.",
      objectiveAchieved: false,
      stateEvidence: "NOT_RECORDED",
      taskCount: 0,
      tasks: [],
      executionRequestAvailable: true,
      executionRequestReason:
        "A atividade está pronta para abrir todas as tarefas responsáveis.",
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
    if (
      request.method() === "GET" &&
      url.pathname ===
        "/api/business-processes/45/products/4/activity-executions"
    ) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(history),
      });
      return;
    }
    if (request.method() === "POST" && url.pathname === executionPath) {
      executionRequests += 1;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          processDefinitionId: 45,
          productId: 4,
          activityId: "pdeGate",
          sourceReference: "experiment:90",
          tasks: [{ id: 301 }, { id: 302 }],
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
      name: /Vega · Homologação e ativação comercial do PDE/,
    })
    .waitFor();
  await page.getByRole("button", { name: "Executar atividade" }).click();
  await page.getByRole("status").waitFor();
  if (executionRequests !== 1) {
    throw new Error(
      `${name}: esperado um POST, recebido ${executionRequests}.`,
    );
  }
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1,
  );
  if (overflow) throw new Error(`${name}: a tela possui overflow horizontal.`);
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
      "Tela de execução da atividade aprovada em desktop, iPhone 15 Pro e Pixel 7.",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
