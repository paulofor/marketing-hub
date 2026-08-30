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

const catalog = [
  {
    processDefinitionId: 52,
    processCode: "pde-opportunity-discovery",
    name: "Descoberta e priorização da oportunidade PDE",
    purpose: "Reunir evidências factuais antes de criar qualquer produto.",
    ownerName: "Argos",
    triggerDescription: "Uma pergunta real de mercado.",
    outcomeDescription: "Dossiê factual auditável.",
    versionNumber: 6,
    executionAvailable: true,
    executionAvailabilityReason: "Pronto para iniciar sem produto.",
    inputFields: [],
  },
];

const execution = {
  id: 93,
  requestKey: "b82df168-e383-4acd-8ca4-ab858b39fd3e",
  processDefinitionId: 52,
  processCode: "pde-opportunity-discovery",
  processName: "Descoberta e priorização da oportunidade PDE",
  processVersionNumber: 6,
  sourceReference: "product-discovery-cycle:77",
  displayName: "autocuidado feminino visual",
  requestedByName: "Marketing Hub",
  input: {
    theme: "autocuidado feminino visual",
    marketType: "B2C",
    acquisitionChannel: "Instagram",
    country: "BR",
  },
  status: "COMPLETED",
  activityCount: 1,
  completedActivityCount: 1,
  costCoverage: "PARTIAL",
  estimatedCostUsd: 0.17,
  inputTokens: 1200,
  outputTokens: 500,
  createdAt: "2026-08-30T20:00:00Z",
  startedAt: "2026-08-30T20:00:05Z",
  finishedAt: "2026-08-30T20:01:00Z",
};

const detail = {
  execution,
  activities: [
    {
      activityId: "marketEvidence",
      activityName: "Reunir evidências factuais de mercado",
      status: "COMPLETED",
      tasks: [
        {
          taskId: 281,
          status: "COMPLETED",
          assignedAgentKey: "market-radar",
          assignedAgentNickname: "Argos",
          title: "Reunir evidências factuais",
          evidence: {
            researchEvidenceReport: {
              metaCoverage: [
                {
                  investigationId: 72,
                  publisherPlatform: "INSTAGRAM",
                  sourceStatus: "AWAITING_SUPERVISED_OBSERVATION",
                },
              ],
            },
          },
          costEstimationStatus: "ESTIMATED",
          createdAt: execution.createdAt,
          startedAt: execution.startedAt,
          finishedAt: execution.finishedAt,
        },
      ],
    },
  ],
};

const awaitingSession = {
  cycleId: 77,
  investigationId: 72,
  cycleStatus: "COMPLETED",
  query: "autocuidado feminino visual",
  country: "BR",
  publisherPlatform: "INSTAGRAM",
  sourceStatus: "AWAITING_SUPERVISED_OBSERVATION",
  collectionMode: "SUPERVISED",
  collectionReason:
    "No Brasil, a observação de anúncios comerciais gerais deve ser supervisionada.",
  searchUrl:
    "https://www.facebook.com/ads/library/?active_status=active&ad_type=all&country=BR&q=autocuidado+feminino+visual",
  adsObserved: 0,
  activeAds: 0,
  advertisersObserved: 0,
  interpretation:
    "Cobertura aguardando observação; isso não significa ausência de mercado.",
  canRegisterObservation: true,
  canResume: false,
  resumeReason: "Registre um anúncio atual, ativo e observado no Instagram.",
  items: [],
};

function observedSession(cycleStatus = "COMPLETED") {
  return {
    ...awaitingSession,
    cycleStatus,
    sourceStatus: "OBSERVED",
    adsObserved: 1,
    activeAds: 1,
    advertisersObserved: 1,
    latestObservationAt: "2026-08-30T20:10:00Z",
    interpretation:
      "Cobertura OBSERVED: 1 anúncio aderente, 1 ativo e 1 anunciante. Isso não comprova vendas.",
    canRegisterObservation: cycleStatus === "COMPLETED",
    canResume: cycleStatus === "COMPLETED",
    resumeReason:
      cycleStatus === "COMPLETED"
        ? "A evidência está pronta para uma nova tentativa auditável de Argos."
        : "A reanálise de Argos já está na fila ou em execução.",
    items: [
      {
        metaAdId: "ad-72",
        advertiserName: "Marca observada",
        adTexts: ["Seu ritual de cinco minutos começa agora."],
        publisherPlatforms: ["INSTAGRAM"],
        formatTypes: ["VIDEO"],
        destinationUrl: "https://example.test/oferta",
        snapshotUrl: "https://www.facebook.com/ads/library/?id=ad-72",
        active: true,
        commercialSignal: true,
        observations: 1,
        longevityDays: 0,
        sustainedInvestmentSignal: false,
        evidenceConfidence: "LOW",
        firstObservedAt: "2026-08-30T20:10:00Z",
        lastObservedAt: "2026-08-30T20:10:00Z",
      },
    ],
  };
}

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [profileName, contextOptions] of profiles) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    const consoleErrors = [];
    const observationPayloads = [];
    let resumeCount = 0;
    let session = awaitingSession;
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    await page.route("**/api/**", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (pathname === "/api/independent-business-process-executions/catalog") {
        await route.fulfill({ json: catalog });
        return;
      }
      if (
        pathname === "/api/independent-business-process-executions" &&
        request.method() === "GET"
      ) {
        await route.fulfill({ json: [execution] });
        return;
      }
      if (pathname === "/api/independent-business-process-executions/93") {
        await route.fulfill({ json: detail });
        return;
      }
      if (
        pathname ===
          "/api/product-discovery/v1/cycles/77/supervised-meta-session" &&
        request.method() === "GET"
      ) {
        await route.fulfill({ json: session });
        return;
      }
      if (
        pathname ===
          "/api/product-discovery/v1/cycles/77/supervised-meta-session/observations" &&
        request.method() === "POST"
      ) {
        observationPayloads.push(request.postDataJSON());
        session = observedSession();
        await route.fulfill({ json: session });
        return;
      }
      if (
        pathname ===
          "/api/product-discovery/v1/cycles/77/supervised-meta-session/resume" &&
        request.method() === "POST"
      ) {
        resumeCount += 1;
        session = observedSession("READY_FOR_RESEARCH");
        await route.fulfill({ json: session });
        return;
      }
      await route.fulfill({ json: [] });
    });

    await page.goto(`${baseUrl}/business-process-executions`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", {
        name: "Confirmar anúncios e linguagem no Instagram",
      }),
    ).toBeVisible();
    const officialLink = page.getByRole("link", {
      name: "Abrir Biblioteca Meta",
    });
    await expect(officialLink).toHaveAttribute("target", "_blank");
    await expect(officialLink).toHaveAttribute(
      "href",
      /facebook\.com\/ads\/library/,
    );

    await page.getByLabel("ID do anúncio *").fill("ad-72");
    await page.getByLabel("Anunciante *").fill("Marca observada");
    await page
      .getByLabel("URL oficial do anúncio *")
      .fill("https://business.facebook.com/ads/library/?id=ad-72");
    await page
      .getByLabel("Texto comercial visível *")
      .fill("Seu ritual de cinco minutos começa agora.");
    await page
      .getByLabel("Página de destino observada")
      .fill("https://example.test/oferta");
    await page.getByLabel("Há preço, oferta ou checkout verificável").check();
    await page.screenshot({
      path: `/tmp/argos-meta-session-form-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await page.getByRole("button", { name: "Registrar observação" }).click();

    await expect(
      page.getByText("Seu ritual de cinco minutos começa agora."),
    ).toBeVisible();
    const resumeButton = page.getByRole("button", {
      name: "Reanalisar com Argos",
    });
    await expect(resumeButton).toBeEnabled();
    await resumeButton.click();
    await expect(
      page.getByText("A reanálise de Argos já está na fila ou em execução."),
    ).toBeVisible();
    await expect(resumeButton).toBeDisabled();

    assert.equal(
      observationPayloads.length,
      1,
      `${profileName}: observação duplicada`,
    );
    assert.deepEqual(observationPayloads[0].publisherPlatforms, ["INSTAGRAM"]);
    assert.equal(observationPayloads[0].pageActive, true);
    assert.equal(observationPayloads[0].commercialSignal, true);
    assert.equal("productId" in observationPayloads[0], false);
    assert.equal("experimentId" in observationPayloads[0], false);
    assert.equal(resumeCount, 1, `${profileName}: retomada duplicada`);
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    assert.deepEqual(consoleErrors, [], `${profileName}: erros no console`);
    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    const actionHeights = await page
      .locator(
        ".argos-meta-session__source a, .argos-meta-session__resume button, .argos-meta-session__form button, .argos-meta-session__form .form-control:not(textarea), .argos-meta-session__form .form-select, .argos-meta-session__checks label",
      )
      .evaluateAll((elements) =>
        elements.map((element) => element.getBoundingClientRect().height),
      );
    assert.ok(
      actionHeights.every((height) => height >= 44),
      `${profileName}: alvo de toque menor que 44px`,
    );
    await page.screenshot({
      path: `/tmp/argos-meta-session-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Sessão supervisionada de Argos aprovada em desktop, iPhone 15 Pro e Pixel 7.",
);
