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

function recruitmentCampaign(status, recordedContacts) {
  const active = status === "ACTIVE";
  return {
    id: status === "NOT_CREATED" ? null : 10,
    experimentId: 89,
    productName: "Kit WhatsApp Pronto",
    status,
    contractVersion: "direct-recruitment-v1",
    headline: "Seu atendimento no WhatsApp poderia vender mais?",
    bodyText: "Participe de uma validação rápida e consentida.",
    audienceSummary: "Pequenos prestadores de serviços.",
    consentText: "Aceito participar e conhecer a oferta.",
    consentVersion: "consent-v1",
    offerUrl: "https://rigel.example",
    offerCta: "Conhecer o Rigel",
    privacyPolicyUrl: "https://rigel.example/privacidade",
    publicPath:
      status === "NOT_CREATED"
        ? null
        : "/participar/11111111-2222-4333-8444-555555555555",
    targetContacts: 15,
    remainingContacts: Math.max(0, 15 - recordedContacts),
    uniqueVisits: 0,
    submissions: 0,
    qualifiedSubmissions: 0,
    notQualifiedSubmissions: 0,
    recordedContacts,
    connectedOrganicAccounts: 0,
    acquisitionStatus:
      recordedContacts >= 15
        ? "SAMPLE_COMPLETE"
        : status === "NOT_CREATED"
          ? "NOT_CREATED"
          : active
            ? "ACTIVE_WITHOUT_DISTRIBUTION"
            : "DRAFT_REQUIRES_APPROVAL",
    distributionGuidance:
      recordedContacts >= 15
        ? "A amostra está pronta para uma única revisão de Hermes."
        : active
          ? "Conecte uma conta orgânica no Marketing Hub."
          : "Revise e aprove o convite.",
    createdBy: status === "NOT_CREATED" ? null : "Homologação local",
    statusChangedBy: active ? "Homologação local" : null,
    statusReason: null,
    createdAt: status === "NOT_CREATED" ? null : "2026-09-01T10:00:00Z",
    updatedAt: status === "NOT_CREATED" ? null : "2026-09-01T10:00:00Z",
    activatedAt: active ? "2026-09-01T10:01:00Z" : null,
    pausedAt: null,
    completedAt: null,
  };
}

async function validateProfile(browser, name, contextOptions) {
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  let recordedContacts = 0;
  let recruitmentStatus = "NOT_CREATED";
  let postedPayload;
  const recruitmentCommands = [];

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (
      request.method() === "GET" &&
      pathname === "/api/business-processes/66/products/9/activity-executions"
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
      request.method() === "GET" &&
      pathname === "/api/experiments/89/direct-recruitment"
    ) {
      await route.fulfill({
        json: recruitmentCampaign(recruitmentStatus, recordedContacts),
      });
      return;
    }
    if (
      request.method() === "POST" &&
      pathname === "/api/experiments/89/direct-recruitment/draft"
    ) {
      recruitmentCommands.push({ pathname, payload: request.postDataJSON() });
      recruitmentStatus = "DRAFT";
      await route.fulfill({
        json: recruitmentCampaign(recruitmentStatus, recordedContacts),
      });
      return;
    }
    if (
      request.method() === "POST" &&
      pathname === "/api/experiments/89/direct-recruitment/activate"
    ) {
      recruitmentCommands.push({ pathname, payload: request.postDataJSON() });
      recruitmentStatus = "ACTIVE";
      await route.fulfill({
        json: recruitmentCampaign(recruitmentStatus, recordedContacts),
      });
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
    await route.fulfill({
      status: 404,
      json: { message: "Rota não simulada" },
    });
  });

  await page.goto(`${baseUrl}${pagePath}`, { waitUntil: "networkidle" });
  await expect(
    page.getByRole("heading", { name: "Aquisição consentida da amostra" }),
  ).toBeVisible();
  await expect(page.getByText("Atividade não preparada")).toBeVisible();
  await page.getByLabel("Responsável pela atividade").fill("Homologação local");
  await page
    .getByRole("button", { name: "Preparar convite para aprovação" })
    .click();
  await expect(page.getByText("Rascunho aguardando aprovação")).toBeVisible();
  await expect(page.getByText("Link público rastreável")).toHaveCount(0);
  await page
    .getByLabel(/Aprovo esta comunicação e confirmo que ativar/)
    .check();
  await page.getByRole("button", { name: "Aprovar e ativar convite" }).click();
  await expect(
    page.getByText("Ativo, sem canal de distribuição"),
  ).toBeVisible();
  await expect(page.getByText("Link público rastreável")).toBeVisible();
  await expect(
    page.locator('a[href*="/participar/11111111-2222-4333-8444-555555555555"]'),
  ).toBeVisible();
  assert.deepEqual(
    recruitmentCommands.map((command) => command.pathname),
    [
      "/api/experiments/89/direct-recruitment/draft",
      "/api/experiments/89/direct-recruitment/activate",
    ],
  );
  assert.equal(recruitmentCommands[1].payload.approvalConfirmed, true);

  await expect(
    page.getByRole("heading", { name: "Amostra individual consentida" }),
  ).toBeVisible();
  await expect(page.getByText("0/15 contatos")).toBeVisible();
  await expect(page.getByText(/Faltam 15 contatos/)).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Reiniciar tarefa" }),
  ).toHaveCount(0);

  await page
    .getByLabel("Telefone ou e-mail do contato")
    .fill(
      `teste+rigel-${name.toLowerCase().replace(/\s+/g, "-")}@sandbox.local`,
    );
  await page
    .getByLabel("Referência da evidência de consentimento")
    .fill(`internal://consentimentos/homologacao-${name}`);
  await page.getByLabel("Registrado por").fill("Homologação local");
  await page
    .getByLabel(
      "Confirmo que o contato consentiu e pertence ao público do experimento.",
    )
    .check();
  await page
    .getByRole("button", { name: "Registrar contato realizado" })
    .click();
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
  process.stdout.write(
    `[UI] ${name}: amostra 0/15, registro pseudonimizado e gate 15/15 validados\n`,
  );
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
