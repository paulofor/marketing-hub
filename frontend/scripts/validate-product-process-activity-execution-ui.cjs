const { chromium, devices, expect } = require("@playwright/test");

const pageUrl =
  "http://127.0.0.1:4173/products/9/value-chain-history/processes/56/activities";
const executionPath =
  "/api/business-processes/56/products/9/activities/humanExperienceReview/execution-requests";

const blockedTask = {
  taskId: 254,
  processDefinitionId: 56,
  processVersionNumber: 6,
  title: "Validar experiência humana da jornada · Rigel",
  status: "BLOCKED",
  sourceReference: "experiment:89",
  assignedAgentKey: "customer-agent",
  assignedAgentNickname: "Psique",
  comments: null,
  evidenceJson: JSON.stringify({ reviewer: "Psique" }),
  executionError:
    "java.io.IOException: SHA-256 divergente para a prova comercial: pde-platform/frontend/src/App.tsx",
  inputTokens: null,
  cachedInputTokens: null,
  outputTokens: null,
  estimatedCostUsd: null,
  costEstimationStatus: "NOT_REPORTED",
  createdAt: "2026-08-28T19:14:48Z",
  startedAt: "2026-08-28T19:15:48Z",
  finishedAt: "2026-08-28T19:15:48Z",
  modelCode: "gpt-5.6-sol",
  reasoningEffort: null,
  productInternalName: "Rigel",
  promptSent: null,
};

const history = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
  commercialPlanId: 4,
  commercialPlanName: "Plano Comercial · Kit WhatsApp Pronto v1",
  selectedProcessDefinitionId: 56,
  processCode: "pde-commercial-homologation-activation",
  processName: "Homologação e ativação comercial do PDE",
  selectedProcessVersionNumber: 6,
  selectedProcessStatus: "PUBLISHED",
  currentExecutionReference: "experiment:89",
  operationalState: "BLOCKED",
  objectiveAchieved: false,
  selectedActivityCount: 1,
  completedActivityCount: 0,
  remainingActivityCount: 1,
  blockedActivityCount: 1,
  currentActivityId: "humanExperienceReview",
  currentActivityName: "Validar experiência humana da jornada",
  currentActivityState: "BLOCKED",
  currentActivityStateReason: blockedTask.executionError,
  activityCount: 1,
  activitiesWithTasksCount: 1,
  uniqueTaskCount: 1,
  knownEstimatedCostUsd: 0,
  costCoverage: "NOT_REPORTED",
  activities: [
    {
      activityDefinitionId: 587,
      activityId: "humanExperienceReview",
      activityName: "Validar experiência humana da jornada",
      activityObjective:
        "Decide se a pessoa entende, deseja, confia e percebe valor com esforço aceitável.",
      activityOwnerName: "Psique",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "BLOCKED",
      stateReason: blockedTask.executionError,
      objectiveAchieved: false,
      stateEvidence: "DIRECT",
      activityInstanceId: 139,
      occurrenceNumber: 1,
      taskCount: 1,
      tasks: [blockedTask],
      executionRequestAvailable: true,
      executionRequestReason:
        "A tentativa bloqueada será preservada e uma nova tarefa será aberta.",
    },
  ],
};

async function validateDevice(browser, name, device) {
  const context = await browser.newContext(device);
  const page = await context.newPage();
  let executionRequests = 0;
  let markRequestStarted;
  let releaseRequest;
  const requestStarted = new Promise((resolve) => {
    markRequestStarted = resolve;
  });
  const responseGate = new Promise((resolve) => {
    releaseRequest = resolve;
  });
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (
      request.method() === "GET" &&
      url.pathname ===
        "/api/business-processes/56/products/9/activity-executions"
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
      markRequestStarted();
      await responseGate;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          processDefinitionId: 56,
          productId: 9,
          activityId: "humanExperienceReview",
          sourceReference: "experiment:89",
          tasks: [{ id: 257 }],
          operationalState: "PENDING",
          objectiveAchieved: false,
          message:
            "Nova tentativa aberta; a tarefa bloqueada permanece no histórico.",
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
      name: /Rigel · Homologação e ativação comercial do PDE/,
    })
    .waitFor();
  await expect(page.getByText(/Tarefa #254/)).toBeVisible();
  const restart = page.getByRole("button", { name: "Reiniciar tarefa" });
  await expect(restart).toBeVisible();
  await expect(restart.locator(".lucide-rotate-ccw")).toBeVisible();

  await restart.click();
  await requestStarted;
  const restarting = page.getByRole("button", { name: "Reiniciando..." });
  await expect(restarting).toBeDisabled();
  await expect(restarting).toContainText("Reiniciando...");
  releaseRequest();
  await expect(page.getByRole("status")).toContainText(
    "Nova tentativa aberta; a tarefa bloqueada permanece no histórico.",
  );
  if (executionRequests !== 1) {
    throw new Error(
      `${name}: esperado um POST de reinício, recebido ${executionRequests}.`,
    );
  }
  await expect(page.getByText(/Tarefa #254/)).toBeVisible();
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1,
  );
  if (overflow) throw new Error(`${name}: a tela possui overflow horizontal.`);
  await page.screenshot({
    path: `/tmp/reinicio-tarefa-${name.toLowerCase().replaceAll(" ", "-")}.png`,
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
      "Reinício auditável aprovado em desktop, iPhone 15 Pro e Pixel 7.",
    );
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
