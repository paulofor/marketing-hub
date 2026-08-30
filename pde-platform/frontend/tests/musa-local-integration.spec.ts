import { expect, test } from "@playwright/test";

const productSlug = "metodo-musa-7-dias";
const v5ExperienceVersion = "musa-pde-entry-v5-video-explicativo";
const v6ExperienceVersion = "musa-pde-entry-v6-video-motivacional";
const v7ExperienceVersion = "musa-pde-entry-v7-espelho-antes-de-sair";
const backendBaseUrl =
  process.env.PDE_TEST_BACKEND_URL ?? "http://127.0.0.1:8096";
const frontendBaseUrl =
  process.env.PDE_TEST_FRONTEND_URL ?? "http://127.0.0.1:57180";
const contractServerBaseUrl =
  process.env.PDE_TEST_CONTRACT_SERVER_URL ?? "http://127.0.0.1:57181";
const internalToken =
  process.env.PDE_TEST_INTERNAL_TOKEN ?? "pde-local-internal-test";
const internalHeaders = { "X-PDE-Internal-Token": internalToken };
const accessHeaders = (token: string) => ({ "X-PDE-Access-Token": token });
const v7MissionSemanticContract = [
  [
    "dia-1-ruido-visual",
    "O espelho não vê roupa, vê mensagem",
    "Organize a mensagem que sua imagem parece transmitir hoje.",
    ["presenceFocus", "mainObstacle", "desiredSignal", "startingResource"],
  ],
  [
    "dia-2-assinatura",
    "A peça que muda seu estado interno",
    "Escolha uma peça-sinal que tenha significado para você.",
    ["pieceSignal", "personalMeaning", "realScene"],
  ],
  [
    "dia-3-base-acessivel",
    "Formalidade sem rigidez",
    "Eleve uma combinação comum com um sinal de estrutura.",
    ["commonLook", "structureSignal", "desiredFinish"],
  ],
  [
    "dia-4-checklist-12-minutos",
    "Primeiras impressões são leituras rápidas",
    "Planeje o primeiro sinal de uma situação real.",
    ["occasion", "firstSignal", "finalDetail"],
  ],
  [
    "dia-5-compra-inteligente",
    "Cor como direção, não decoração",
    "Monte uma direção de duas cores para uma ocasião real.",
    ["baseColor", "signalColor", "realOccasion"],
  ],
  [
    "dia-6-situacao-chave",
    "Assinatura pessoal: ser reconhecida sem esforço",
    "Defina três sinais repetíveis da sua assinatura pessoal.",
    ["finishSignal", "signatureBase", "memorableSignal"],
  ],
  [
    "dia-7-plano-pessoal",
    "Seu algoritmo de presença elegante",
    "Transforme a semana em sua fórmula MUSA pessoal.",
    [
      "bestSignal",
      "mostRelevantOccasion",
      "antiImpulseRule",
      "checklistPriority",
    ],
  ],
] as const;

function versionedFrontendUrl(version: string, pathAndQuery: string) {
  const url = new URL(pathAndQuery, frontendBaseUrl);
  url.searchParams.set("experienceVersion", version);
  return url.toString();
}

function projectEmailKey(projectName: string) {
  if (projectName.includes("iphone")) return "iphone";
  if (projectName.includes("pixel")) return "pixel";
  return "desktop";
}

test.beforeEach(async ({ request }) => {
  const response = await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
    { headers: internalHeaders },
  );
  expect(response.ok()).toBeTruthy();
});

test("v5, v6 e v7 usam backend PDE local real sem misturar contratos versionados", async ({
  page,
  request,
}) => {
  await page.goto(
    versionedFrontendUrl(
      v5ExperienceVersion,
      "/?utm_source=local&utm_campaign=v5_local_validation",
    ),
  );
  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toHaveCount(0);
  await expect(page.locator("video.public-hero-video")).toHaveCount(0);
  await expect
    .poll(async () => {
      const response = await request.get(
        `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v5ExperienceVersion)}`,
      );
      const summary = await response.json();
      return (
        summary.currentExperienceVersion === v5ExperienceVersion &&
        summary.rawTotalEvents > 0
      );
    })
    .toBeTruthy();

  await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
    { headers: internalHeaders },
  );

  await page.goto(
    versionedFrontendUrl(
      v6ExperienceVersion,
      "/?utm_source=local&utm_campaign=v6_local_validation",
    ),
  );
  await expect(
    page.getByRole("heading", {
      name: "Se o look parece certo, por que você ainda sente que falta presença?",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Revelar meu ajuste MUSA de hoje" }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toBeVisible();
  await expect(page.locator("video.public-hero-video")).toHaveCount(1);
  await expect(page.locator("video.public-hero-video")).toHaveJSProperty(
    "muted",
    false,
  );
  await expect(page.locator("video.public-hero-video")).toHaveJSProperty(
    "autoplay",
    false,
  );
  await expect(page.locator("video.public-hero-video")).toHaveJSProperty(
    "controls",
    true,
  );
  await expect(page.locator("video.public-hero-video")).not.toHaveAttribute(
    "poster",
  );

  await expect
    .poll(async () => {
      const response = await request.get(
        `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v6ExperienceVersion)}`,
      );
      const summary = await response.json();
      return (
        summary.currentExperienceVersion === v6ExperienceVersion &&
        summary.rawTotalEvents > 0
      );
    })
    .toBeTruthy();

  await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
    { headers: internalHeaders },
  );

  await page.goto(
    versionedFrontendUrl(
      v7ExperienceVersion,
      "/?utm_source=local&utm_campaign=v7_local_validation",
    ),
  );
  await expect(
    page.getByText(
      "Se sua imagem falasse antes de você hoje, qual mensagem ela passaria sem intenção?",
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("button", {
      name: "Minha imagem está coerente; quero apenas organizar minhas escolhas",
    }),
  ).toBeVisible();
  await expect(
    page.getByText("Quando você se olha pronta, o que mais te incomoda?"),
  ).toHaveCount(0);
  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("region", { name: "Privacidade e controle dos dados MUSA" }),
  ).toBeVisible();
  await expect(
    page.getByText(
      /As sete missões não pedem foto nem texto livre e não enviam suas respostas/,
    ),
  ).toBeVisible();
  await expect(
    page.getByText(
      /suporte, poderá escrever voluntariamente uma mensagem breve/,
    ),
  ).toBeVisible();

  await expect
    .poll(async () => {
      const response = await request.get(
        `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v7ExperienceVersion)}`,
      );
      const summary = await response.json();
      return (
        summary.currentExperienceVersion === v7ExperienceVersion &&
        summary.rawTotalEvents > 0
      );
    })
    .toBeTruthy();
});

test("v7 entrega degustação local e acesso único de 90 dias sem fila de IA", async ({
  page,
  request,
}, testInfo) => {
  const neutralPublicGuidanceResponse = await request.post(
    `${backendBaseUrl}/api/pde/public/presence-diagnostic`,
    {
      data: {
        experienceVersion: v7ExperienceVersion,
        answers: {
          mainObstacle:
            "Minha imagem está coerente; quero apenas organizar minhas escolhas",
          presenceFocus: "Rotina comum",
          desiredSignal: "Elegância discreta",
          startingResource: "Roupa que já tenho",
        },
      },
    },
  );
  expect(neutralPublicGuidanceResponse.status()).toBe(201);
  const neutralPublicGuidance = await neutralPublicGuidanceResponse.json();
  expect(neutralPublicGuidance.headline).toBe(
    "Sua escolha atual foi preservada",
  );
  expect(neutralPublicGuidance.summary).toContain(
    "não precisa corrigir sua imagem",
  );
  expect(neutralPublicGuidance.inputTokens).toBe(0);
  expect(neutralPublicGuidance.outputTokens).toBe(0);

  const publicGuidanceResponse = await request.post(
    `${backendBaseUrl}/api/pde/public/presence-diagnostic`,
    {
      data: {
        experienceVersion: v7ExperienceVersion,
        answers: {
          mainObstacle: "Falta presença",
          presenceFocus: "Trabalho ou reunião",
          desiredSignal: "Elegância discreta",
          startingResource: "Roupa que já tenho",
        },
      },
    },
  );
  expect(publicGuidanceResponse.status()).toBe(201);
  const publicGuidance = await publicGuidanceResponse.json();
  expect(publicGuidance.status).toBe("COMPLETED");
  expect(publicGuidance.model).toBe("MUSA_LOCAL_RULES_V1");
  expect(publicGuidance.inputTokens).toBe(0);
  expect(publicGuidance.outputTokens).toBe(0);

  const pendingResponse = await request.get(
    `${backendBaseUrl}/api/internal/pde/ai-guidance/stage-executions/pending`,
    {
      headers: internalHeaders,
    },
  );
  expect(await pendingResponse.json()).toEqual([]);

  const email = `teste+musa-v7-${projectEmailKey(testInfo.project.name)}-${Date.now()}@sandbox.local`;
  const checkoutResponse = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-access`,
    {
      headers: internalHeaders,
      data: { productSlug, email, experienceVersion: v7ExperienceVersion },
    },
  );
  expect(checkoutResponse.status()).toBe(201);
  const access = await checkoutResponse.json();
  for (const legacyPath of ["login", "register"]) {
    const legacyResponse = await request.post(
      `${backendBaseUrl}/api/pde/access/${legacyPath}`,
      {
        data: { productSlug, email, experienceVersion: v7ExperienceVersion },
      },
    );
    expect(legacyResponse.status()).toBe(404);
    expect(await legacyResponse.text()).not.toContain(access.token);
  }
  for (const invalidAnswers of [
    { freeText: "conteúdo arbitrário" },
    { mainObstacle: "conteúdo arbitrário" },
    { mainObstacle: "Trabalho ou reunião" },
  ]) {
    const invalidInteraction = await request.post(
      `${backendBaseUrl}/api/pde/access/missions/dia-1-ruido-visual/interactions`,
      {
        headers: accessHeaders(access.token),
        data: { answers: invalidAnswers },
      },
    );
    expect(invalidInteraction.ok()).toBeFalsy();
  }
  const workspaceResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/workspace`,
    { headers: accessHeaders(access.token) },
  );
  const workspace = await workspaceResponse.json();
  expect(workspace.subscriptionStatus).toBe("ACTIVE");
  expect(workspace.accessSource).toBe("INTERNAL_QA");
  expect(workspace.experienceVersion).toBe(v7ExperienceVersion);
  expect(
    workspace.product.missions.map(
      (mission: {
        id: string;
        title: string;
        interaction: { title: string; fields: { key: string }[] };
      }) => [
        mission.id,
        mission.title,
        mission.interaction.title,
        mission.interaction.fields.map((field) => field.key),
      ],
    ),
  ).toEqual(v7MissionSemanticContract);
  const remainingDays =
    (Date.parse(workspace.accessExpiresAt) - Date.now()) / 86_400_000;
  expect(remainingDays).toBeGreaterThan(89);
  expect(remainingDays).toBeLessThanOrEqual(90);

  await page.goto(
    versionedFrontendUrl(v7ExperienceVersion, `/access/${access.token}`),
  );
  await expect(page.getByText("Produto ativo")).toBeVisible();
  await expect(page.getByText(/Método MUSA liberado até/)).toBeVisible();
  await expect(
    page.getByText(
      "Regras locais: suas escolhas não são enviadas para IA ou gerador de vídeo.",
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Privacidade e controle dos dados MUSA" }),
  ).toBeVisible();
  await expect(page.locator(".mission-detail > h2")).toHaveText(
    v7MissionSemanticContract[0][1],
  );
  await expect(page.locator(".personalization-panel > h3")).toHaveText(
    v7MissionSemanticContract[0][2],
  );
  await page
    .getByRole("button", { name: "Manter como está por enquanto" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Sua escolha atual foi preservada" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Registrar Dia 1 concluído" }),
  ).toBeEnabled();
  await page
    .getByLabel("Como podemos ajudar?")
    .fill(
      "Quero confirmar como retomar minha jornada sem fazer o ajuste de hoje.",
    );
  await page.getByRole("button", { name: "Pedir suporte" }).click();
  await expect(
    page.getByText("Você já possui um pedido de suporte aberto."),
  ).toBeVisible();
  await expect(
    page.getByText(/A equipe responderá pelo e-mail usado na MUSA/),
  ).toBeVisible();
  await page.getByRole("button", { name: "Registrar Dia 1 concluído" }).click();
  for (let day = 2; day <= 7; day += 1) {
    await page
      .locator(".mission-tabs button")
      .nth(day - 1)
      .click();
    await expect(page.locator(".mission-detail > h2")).toHaveText(
      v7MissionSemanticContract[day - 1][1],
    );
    await expect(page.locator(".personalization-panel > h3")).toHaveText(
      v7MissionSemanticContract[day - 1][2],
    );
    await page
      .getByRole("button", { name: "Manter como está por enquanto" })
      .click();
    await expect(
      page.getByRole("heading", { name: "Sua escolha atual foi preservada" }),
    ).toBeVisible();
    await page
      .getByRole("button", { name: `Registrar Dia ${day} concluído` })
      .click();
  }
  await expect(
    page.getByRole("heading", { name: "Sua jornada MUSA está concluída" }),
  ).toBeVisible();

  const materialUrl = workspace.product.supportMaterials[0].url;
  expect(materialUrl).toBe("/materials/musa-v7/mapa-dos-7-sinais.html");
  const unauthenticatedMaterial = await request.get(
    `${frontendBaseUrl}${materialUrl}`,
  );
  expect(unauthenticatedMaterial.status()).toBe(403);
  const authenticatedMaterial = await request.get(
    `${frontendBaseUrl}${materialUrl}`,
    {
      headers: { "X-PDE-Access-Token": access.token },
    },
  );
  expect(authenticatedMaterial.ok()).toBeTruthy();
  const materialHtml = await authenticatedMaterial.text();
  for (const expectedText of [
    "Mensagem no espelho",
    "Peça-sinal",
    "Estrutura leve",
    "Primeira leitura",
    "Cor com direção",
    "Assinatura pessoal",
    "Fórmula MUSA",
  ]) {
    expect(materialHtml).toContain(expectedText);
  }
  expect(materialHtml).not.toContain("../imagens/");
  const materialPage = await page.context().newPage();
  await materialPage.setContent(materialHtml);
  await expect(
    materialPage.getByRole("heading", {
      name: "Mapa dos 7 Sinais de Presença",
    }),
  ).toBeVisible();
  await expect(materialPage.locator("article")).toHaveCount(7);
  expect(
    await materialPage.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBeTruthy();
  await materialPage.close();

  const qaSummaryResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?includeNonHumanTraffic=true&experienceVersion=${encodeURIComponent(v7ExperienceVersion)}`,
    { headers: internalHeaders },
  );
  expect(qaSummaryResponse.ok()).toBeTruthy();
  const qaSummary = await qaSummaryResponse.json();
  expect(qaSummary.subscriptionApproved).toBe(0);
  expect(qaSummary.accessReleased).toBe(0);

  const correctedEmail = email.replace(
    "@sandbox.local",
    "+corrigido@sandbox.local",
  );
  const correctionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/api/pde/access/privacy-requests`) &&
      response.request().method() === "POST",
  );
  await page.getByLabel("Corrigir e-mail do acesso").fill(correctedEmail);
  await page.getByRole("button", { name: "Corrigir meu e-mail" }).click();
  expect((await correctionResponsePromise).ok()).toBeTruthy();
  await expect(
    page.getByText(
      "E-mail corrigido. Use o novo endereço para retomar sua jornada.",
    ),
  ).toBeVisible();
  await expect(
    page
      .getByLabel("Resumo da Área MUSA")
      .getByText(correctedEmail, { exact: true }),
  ).toBeVisible();

  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Baixar meus dados" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain("meus-dados-musa");

  page.once("dialog", (dialog) => dialog.accept());
  const deletionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/api/pde/access/privacy-requests`) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Excluir respostas e progresso" })
    .click();
  expect((await deletionResponsePromise).ok()).toBeTruthy();
  await expect(
    page.getByText(/Seus dados de uso e progresso foram excluídos/),
  ).toBeVisible();
  expect(
    (
      await request.get(
        `${backendBaseUrl}/api/pde/access/workspace`,
        { headers: accessHeaders(access.token) },
      )
    ).status(),
  ).toBe(404);
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBeTruthy();

  const expiredEmail = `teste+exp-${projectEmailKey(testInfo.project.name)}-${Date.now()}@sandbox.local`;
  const expiredAccessResponse = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-access`,
    {
      headers: internalHeaders,
      data: {
        productSlug,
        email: expiredEmail,
        experienceVersion: v7ExperienceVersion,
      },
    },
  );
  expect(expiredAccessResponse.status()).toBe(201);
  const expiredAccess = await expiredAccessResponse.json();
  const expirationResponse = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-access/expire`,
    {
      headers: { ...internalHeaders, ...accessHeaders(expiredAccess.token) },
    },
  );
  expect(expirationResponse.ok()).toBeTruthy();
  expect((await expirationResponse.json()).subscriptionStatus).toBe("EXPIRED");

  await page.goto(
    versionedFrontendUrl(v7ExperienceVersion, `/access/${expiredAccess.token}`),
  );
  await expect(page.getByText("Expirado", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Acesso MUSA expirado" }),
  ).toBeVisible();
  await expect(
    page.getByText(/Sua compra anterior permanece reconhecida/).first(),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Oferta de acesso completo MUSA" }),
  ).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Liberar por/ })).toHaveCount(
    0,
  );
  await expect(
    page.getByText(/Não inclua saúde, intimidade, documentos/),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Privacidade e controle dos dados MUSA" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBeTruthy();

  const expiredDeletionResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/privacy-requests`,
    {
      headers: accessHeaders(expiredAccess.token),
      data: { action: "DELETION" },
    },
  );
  expect(expiredDeletionResponse.ok()).toBeTruthy();
});

test("v7 reconcilia compra e reembolso Pepper sem duplicar venda líquida", async ({
  request,
}, testInfo) => {
  const transactionId = `tx-musa-v7-${testInfo.project.name}-${Date.now()}`;
  const email = `teste+pepper-${projectEmailKey(testInfo.project.name)}-${Date.now()}@sandbox.local`;
  const configurePaid = await request.post(
    `${contractServerBaseUrl}/test/pepper/transactions/${transactionId}`,
    {
      data: { status: "paid", email },
    },
  );
  expect(configurePaid.ok()).toBeTruthy();

  const paidWebhook = await request.post(
    `${backendBaseUrl}/api/pde/access/pepper/webhook`,
    {
      data: { productSlug, transactionId, status: "paid" },
    },
  );
  expect(paidWebhook.status()).toBe(201);
  const paidAccess = await paidWebhook.json();
  expect(paidAccess.source).toBe("PEPPER");
  const activeWorkspaceResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/workspace`,
    { headers: accessHeaders(paidAccess.token) },
  );
  expect(activeWorkspaceResponse.ok()).toBeTruthy();
  const activeWorkspace = await activeWorkspaceResponse.json();
  expect(activeWorkspace.subscriptionStatus).toBe("ACTIVE");
  expect(activeWorkspace.experienceVersion).toBe(v7ExperienceVersion);

  const paidRetry = await request.post(
    `${backendBaseUrl}/api/pde/access/pepper/webhook`,
    {
      data: { productSlug, transactionId, status: "paid" },
    },
  );
  expect(paidRetry.status()).toBe(201);

  for (const mission of activeWorkspace.product.missions) {
    expect(mission.interaction?.fields?.length).toBeGreaterThanOrEqual(3);
    const neutralAnswers = Object.fromEntries(
      mission.interaction.fields.map((field: { key: string }) => [
        field.key,
        "Manter como está por enquanto",
      ]),
    );
    const guidanceResponse = await request.post(
      `${backendBaseUrl}/api/pde/access/missions/${mission.id}/ai-guidance`,
      {
        headers: accessHeaders(paidAccess.token),
        data: {
          guidanceType: mission.interaction.guidanceType,
          answers: neutralAnswers,
          experienceVersion: v7ExperienceVersion,
        },
      },
    );
    expect(guidanceResponse.ok()).toBeTruthy();
    expect((await guidanceResponse.json()).status).toBe("COMPLETED");
    const completionResponse = await request.post(
      `${backendBaseUrl}/api/pde/access/missions/${mission.id}/complete`,
      { headers: accessHeaders(paidAccess.token) },
    );
    expect(completionResponse.ok()).toBeTruthy();
  }
  const lastMission = activeWorkspace.product.missions.at(-1);
  expect(lastMission).toBeTruthy();
  if (!lastMission) {
    throw new Error(
      "A experiência MUSA v7 precisa declarar ao menos uma missão.",
    );
  }
  const repeatedDeliveryResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/missions/${lastMission.id}/complete`,
    { headers: accessHeaders(paidAccess.token) },
  );
  expect(repeatedDeliveryResponse.ok()).toBeTruthy();

  const configureRefund = await request.post(
    `${contractServerBaseUrl}/test/pepper/transactions/${transactionId}`,
    {
      data: { status: "refunded", email },
    },
  );
  expect(configureRefund.ok()).toBeTruthy();
  const refundWebhook = await request.post(
    `${backendBaseUrl}/api/pde/access/pepper/webhook`,
    {
      data: { productSlug, transactionId, status: "refunded" },
    },
  );
  expect(refundWebhook.status()).toBe(201);
  expect(await refundWebhook.json()).toMatchObject({
    newlyRecorded: true,
    accessRevoked: true,
  });

  const refundRetry = await request.post(
    `${backendBaseUrl}/api/pde/access/pepper/webhook`,
    {
      data: { productSlug, transactionId, status: "refunded" },
    },
  );
  expect(refundRetry.status()).toBe(201);
  expect(await refundRetry.json()).toMatchObject({
    newlyRecorded: false,
    accessRevoked: false,
  });

  const refundedWorkspaceResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/workspace`,
    { headers: accessHeaders(paidAccess.token) },
  );
  expect(refundedWorkspaceResponse.ok()).toBeTruthy();
  expect((await refundedWorkspaceResponse.json()).subscriptionStatus).toBe(
    "REFUNDED",
  );
  const materialResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/materials/authorize`,
    { headers: { "X-PDE-Access-Token": paidAccess.token } },
  );
  expect(materialResponse.status()).toBe(403);

  const summaryResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?experienceVersion=${encodeURIComponent(v7ExperienceVersion)}`,
  );
  expect(summaryResponse.ok()).toBeTruthy();
  const summary = await summaryResponse.json();
  expect(summary.totalEvents).toBe(0);
  expect(summary.rawTotalEvents).toBeGreaterThan(0);
  expect(summary.purchaseCompleted).toBe(0);
  expect(summary.accessReleased).toBe(0);
  expect(summary.deliveryCompleted).toBe(0);
  expect(summary.refundsConfirmed).toBe(0);
  expect(summary.netSalesApproved).toBe(0);
  expect(summary.humanSessions).toBe(0);
  expect(summary.trafficQualityBreakdown).toEqual([
    expect.objectContaining({
      trafficQuality: "INTERNAL_QA",
      events: summary.rawTotalEvents,
    }),
  ]);
});
