const assert = require("node:assert/strict");
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.DIRECT_CONTACT_UI_URL || "http://127.0.0.1:4173";
const pagePath = "/products/9/value-chain-history/processes/66/activities";

const history = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
  commercialPlanId: 4,
  commercialPlanName: "Plano comercial do Rigel",
  selectedProcessDefinitionId: 66,
  processCode: "operacao-otimizacao-experimento",
  processName: "Operação e otimização do experimento",
  selectedProcessVersionNumber: 5,
  selectedProcessStatus: "PUBLISHED",
  currentExecutionReference: "experiment:89",
  operationalState: "BLOCKED",
  objectiveAchieved: false,
  selectedActivityCount: 2,
  completedActivityCount: 1,
  remainingActivityCount: 1,
  blockedActivityCount: 1,
  currentActivityId: "task-2",
  currentActivityName: "Acumular amostra e respeitar janela",
  currentActivityState: "BLOCKED",
  currentActivityStateReason: "Aguardando 15 contatos consentidos reais.",
  activityCount: 2,
  activitiesWithTasksCount: 2,
  uniqueTaskCount: 2,
  knownEstimatedCostUsd: 0.6939976,
  costCoverage: "COMPLETE",
  activities: [
    {
      activityDefinitionId: 301,
      activityId: "task-1",
      activityName: "Verificar integridade dos eventos",
      activityObjective: "Comprovar a instrumentação do canal direto.",
      activityOwnerName: "Hermes",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "COMPLETED",
      stateReason: "Integridade comprovada.",
      objectiveAchieved: true,
      stateEvidence: "DIRECT",
      taskCount: 0,
      tasks: [],
      executionRequestAvailable: false,
    },
    {
      activityDefinitionId: 302,
      activityId: "task-2",
      activityName: "Acumular amostra e respeitar janela",
      activityObjective: "Acumular 15 contatos consentidos e aderentes.",
      activityOwnerName: "Hermes",
      sequenceNumber: 2,
      selectedVersionActivity: true,
      operationalState: "BLOCKED",
      stateReason: "Aguardando amostra direta: 0 de 15 contatos.",
      objectiveAchieved: false,
      stateEvidence: "DIRECT",
      taskCount: 0,
      tasks: [],
      executionRequestAvailable: false,
      executionRequestReason:
        "Aguardando amostra direta: 0 de 15 contatos consentidos e aderentes registrados; faltam 15.",
    },
  ],
};

function sample(recordedContacts) {
  return {
    experimentId: 89,
    platform: "DIRECT_ONE_TO_ONE",
    experimentStatus: "RUNNING",
    targetContacts: 15,
    recordedContacts,
    remainingContacts: Math.max(0, 15 - recordedContacts),
    readyForHermesReview: recordedContacts >= 15,
    operationalStatus:
      recordedContacts >= 15
        ? "READY_FOR_HERMES_REVIEW"
        : "ACCUMULATING_CONSENTED_SAMPLE",
    contacts: [],
  };
}

async function validateProfile(browser, name, contextOptions) {
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  let recordedContacts = 0;
  let postedPayload;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (
      request.method() === "GET" &&
      pathname ===
        "/api/business-processes/66/products/9/activity-executions"
    ) {
      await route.fulfill({ json: history });
      return;
    }
    if (
      request.method() === "GET" &&
      pathname === "/api/experiments/89/direct-contact-sample"
    ) {
      await route.fulfill({ json: sample(recordedContacts) });
      return;
    }
    if (
      request.method() === "POST" &&
      pathname === "/api/experiments/89/direct-contact-sample/contacts"
    ) {
      postedPayload = request.postDataJSON();
      recordedContacts += 1;
      await route.fulfill({ json: sample(recordedContacts) });
      return;
    }
    await route.fulfill({ status: 404, json: { message: "Rota não simulada" } });
  });

  await page.goto(`${baseUrl}${pagePath}`, { waitUntil: "networkidle" });
  await expect(
    page.getByRole("heading", { name: "Amostra individual consentida" }),
  ).toBeVisible();
  await expect(page.getByText("0/15 contatos")).toBeVisible();
  await expect(page.getByText(/Faltam 15 contatos/)).toBeVisible();
  await expect(page.getByRole("button", { name: "Reiniciar tarefa" })).toHaveCount(0);

  await page
    .getByLabel("Telefone ou e-mail do contato")
    .fill(`teste+rigel-${name.toLowerCase().replace(/\s+/g, "-")}@sandbox.local`);
  await page
    .getByLabel("Referência da evidência de consentimento")
    .fill(`internal://consentimentos/homologacao-${name}`);
  await page.getByLabel("Registrado por").fill("Homologação local");
  await page
    .getByLabel(
      "Confirmo que o contato consentiu e pertence ao público do experimento.",
    )
    .check();
  await page.getByRole("button", { name: "Registrar contato realizado" }).click();
  await expect(page.getByText("1/15 contatos")).toBeVisible();

  assert.equal(postedPayload.contactFingerprint.length, 64);
  assert.match(postedPayload.contactFingerprint, /^[0-9a-f]{64}$/);
  assert.equal(JSON.stringify(postedPayload).includes("@sandbox.local"), false);
  assert.equal(postedPayload.audienceFitConfirmed, true);

  recordedContacts = 15;
  await page.reload({ waitUntil: "networkidle" });
  await expect(page.getByText("15/15 contatos")).toBeVisible();
  await expect(page.getByText(/A amostra atingiu a meta/)).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Registrar contato realizado" }),
  ).toBeDisabled();
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
    true,
    `${name}: a página criou overflow horizontal`,
  );

  await context.close();
  process.stdout.write(`[UI] ${name}: amostra 0/15, registro pseudonimizado e gate 15/15 validados\n`);
}

async function main() {
  const browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || "/usr/bin/chromium",
  });
  try {
    await validateProfile(browser, "Desktop", {
      viewport: { width: 1440, height: 1000 },
    });
    await validateProfile(browser, "iPhone 15 Pro", devices["iPhone 15 Pro"]);
    await validateProfile(browser, "Pixel 7", devices["Pixel 7"]);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
