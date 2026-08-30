import { expect, test } from "@playwright/test";

const productSlug = "kit-whatsapp-pronto";
const backendBaseUrl =
  process.env.PDE_ASSISTED_BACKEND_URL ?? "http://127.0.0.1:8096";
const mailBaseUrl =
  process.env.PDE_ASSISTED_MAIL_URL ?? "http://sandbox-mail:8025";
const paymentsBaseUrl =
  process.env.PDE_ASSISTED_PAYMENTS_URL ?? "http://127.0.0.1:8080";
const internalToken =
  process.env.PDE_ASSISTED_INTERNAL_TOKEN ?? "pde-local-internal-test";
const internalHeaders = { "X-PDE-Internal-Token": internalToken };
const accessHeaders = (token: string) => ({ "X-PDE-Access-Token": token });
const operationHeaders = (token: string, operationToken: string) => ({
  "X-PDE-Access-Token": token,
  "X-PDE-Operation-Token": operationToken,
});
const psiquePageHeightLimit = 8500;
const crossBrowserRenderingSafetyMargin = 900;

const relativeLuminance = (color: string) => {
  const channels = color
    .match(/[\d.]+/g)
    ?.slice(0, 3)
    .map(Number);
  if (!channels || channels.length !== 3) {
    throw new Error(`Cor RGB inválida no contrato visual: ${color}`);
  }
  const linearChannels = channels.map((channel) => {
    const normalized = channel / 255;
    return normalized <= 0.03928
      ? normalized / 12.92
      : ((normalized + 0.055) / 1.055) ** 2.4;
  });
  return (
    linearChannels[0] * 0.2126 +
    linearChannels[1] * 0.7152 +
    linearChannels[2] * 0.0722
  );
};

const contrastRatio = (foreground: string, background: string) => {
  const foregroundLuminance = relativeLuminance(foreground);
  const backgroundLuminance = relativeLuminance(background);
  const lighter = Math.max(foregroundLuminance, backgroundLuminance);
  const darker = Math.min(foregroundLuminance, backgroundLuminance);
  return (lighter + 0.05) / (darker + 0.05);
};

test.beforeEach(async ({ context, page, request }) => {
  await context.route(
    "**/api/pde/products/kit-whatsapp-pronto/commercial-offer",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          productSlug,
          experienceVersion: "kit-whatsapp-pronto-pde-v2",
          layoutKey: "assisted-service-v2",
          experimentId: 89,
          experimentStatus: "PLANNED",
          acquisitionChannel: "DIRECT_ONE_TO_ONE",
          pain: "Você perde oportunidades no WhatsApp porque improvisa respostas e follow-ups.",
          proof:
            "Veja uma resposta e duas perguntas personalizadas para um cenário real do seu atendimento.",
          promise:
            "Após o pagamento confirmado e o briefing mínimo completo, em até 48 horas receba seu atendimento de WhatsApp personalizado e revisado: respostas, perguntas, follow-ups e regras prontas para conduzir cada conversa ao próximo passo com mais clareza e menos improviso.",
          primaryCta: "Quero meu atendimento sob medida",
          priceBrl: 349,
          checkoutUrl: "https://pay.example/kit-whatsapp",
          salesPageUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br",
          targetAudience: "Pequenos prestadores que atendem pelo WhatsApp",
          productFormat: "IMPLANTACAO_PERSONALIZADA",
          deliveryMode: "ASSISTIDA_MANUAL",
          valueUnit:
            "Respostas, perguntas e follow-ups prontos para revisar e usar",
          supplierDisplayName: "Digicom Digital",
          supplierRegistrationNumber: "00.000.000/0001-00",
          supportEmail: "teste@sandbox.local",
          termsUrl: "http://127.0.0.1:57182/terms",
          privacyUrl: "http://127.0.0.1:57182/privacy",
          refundPolicyUrl: "http://127.0.0.1:57182/refund-policy",
        }),
      });
    },
  );
  await context.route("https://pay.example/kit-whatsapp", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html; charset=utf-8",
      body: `<!doctype html>
        <html lang="pt-BR"><head><meta charset="utf-8"><title>Checkout de homologação</title></head>
        <body>
          <h1>Checkout de homologação — Kit WhatsApp Pronto</h1>
          <p>R$ 349</p>
          <p>Pagamento único, sem recorrência</p>
          <p>Digicom Digital</p>
          <p>Nenhuma cobrança adicional será criada neste ambiente local.</p>
        </body></html>`,
    });
  });
  const resetResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
    { headers: internalHeaders },
  );
  expect(resetResponse.ok()).toBeTruthy();
  await page.goto("/?mh_test=1");
});

test("autentica a ingestão financeira antes de validar o payload", async ({
  request,
}) => {
  const response = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    { data: {} },
  );

  expect(response.status()).toBe(401);
  await expect(response.json()).resolves.toMatchObject({
    error: "Entitlement financeiro não autorizado",
  });
});

test("inicia pagamentos com a integração de entitlement habilitada", async ({
  request,
}) => {
  const response = await request.get(`${paymentsBaseUrl}/actuator/health`);

  expect(response.ok()).toBeTruthy();
  await expect(response.json()).resolves.toMatchObject({ status: "UP" });
});

test("conclui a jornada assistida com marcos operacionais, preserva progresso e abre materiais", async ({
  page,
  request,
}, testInfo) => {
  const commercialEvents: Array<Record<string, unknown>> = [];
  const browserAccessRequests: Array<{
    url: string;
    postData: string;
    referrer: string;
    accessHeader: string;
  }> = [];
  const captureBrowserRequest = (browserRequest: {
    method(): string;
    url(): string;
    postData(): string | null;
    headers(): Record<string, string>;
    postDataJSON(): unknown;
  }) => {
    if (
      browserRequest.method() === "POST" &&
      browserRequest.url().includes("/api/pde/access/events")
    ) {
      const payload = browserRequest.postDataJSON() as Record<string, unknown>;
      commercialEvents.push(payload);
    }
    if (browserRequest.url().includes("/api/pde/access/")) {
      const headers = browserRequest.headers();
      browserAccessRequests.push({
        url: browserRequest.url(),
        postData: browserRequest.postData() ?? "",
        referrer: headers.referer ?? "",
        accessHeader: headers["x-pde-access-token"] ?? "",
      });
    }
  };
  page.on("request", captureBrowserRequest);

  const canonicalOfferResponse = await request.get(
    `${backendBaseUrl}/api/pde/products/${productSlug}/commercial-offer`,
  );
  expect(canonicalOfferResponse.ok()).toBeTruthy();
  const canonicalOffer = await canonicalOfferResponse.json();
  expect(canonicalOffer).toMatchObject({
    productSlug,
    experienceVersion: "kit-whatsapp-pronto-pde-v2",
    layoutKey: "assisted-service-v2",
    experimentId: 89,
    acquisitionChannel: "DIRECT_ONE_TO_ONE",
    priceBrl: 349,
    supplierDisplayName: "Digicom Digital",
  });
  expect(canonicalOffer).not.toHaveProperty("supplierLegalName");
  expect(canonicalOffer).not.toHaveProperty("supplierAddress");

  await expect(
    page.getByRole("heading", {
      name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
    }),
  ).toBeVisible();
  const previewCta = page.getByTestId("assisted-preview-cta");
  await expect(previewCta).toHaveAttribute("href", "#assisted-tasting-title");
  await expect(previewCta).toContainText(
    "Experimentar uma amostra antes de comprar",
  );
  if (testInfo.project.name !== "desktop-chromium") {
    const mobileCommercialLayout = await page.evaluate(() => {
      const headline = document.querySelector(
        ".assisted-pde-copy h1",
      ) as HTMLElement;
      const firstAction = document.querySelector(
        '[data-testid="assisted-preview-cta"]',
      ) as HTMLElement;
      const proofCard = document.querySelector(
        ".assisted-pde-proof-grid article",
      ) as HTMLElement;
      const proofContent = document.querySelector(
        ".assisted-pde-proof-grid article > div",
      ) as HTMLElement;
      const tastingService = document.querySelector(
        "#assisted-tasting-service",
      ) as HTMLInputElement;
      const tastingScenario = document.querySelector(
        "#assisted-tasting-scenario",
      ) as HTMLSelectElement;
      const transformation = document.querySelector(
        '[data-testid="assisted-transformation"]',
      ) as HTMLElement;
      const transformationHeading = document.querySelector(
        "#assisted-transformation-title",
      ) as HTMLElement;
      const shell = document.querySelector(
        ".assisted-pde-shell",
      ) as HTMLElement;
      const scenarioStyle = window.getComputedStyle(tastingScenario);
      const canvas = document.createElement("canvas");
      const canvasContext = canvas.getContext("2d");
      if (canvasContext) canvasContext.font = scenarioStyle.font;
      return {
        headlineFontSize: Number.parseFloat(
          window.getComputedStyle(headline).fontSize,
        ),
        headlineHeight: headline.getBoundingClientRect().height,
        firstActionBottom: firstAction.getBoundingClientRect().bottom,
        proofColumns: window.getComputedStyle(proofCard).gridTemplateColumns,
        proofContentWidth: proofContent.getBoundingClientRect().width,
        tastingControlWidth: tastingScenario.getBoundingClientRect().width,
        longestScenarioLabel: Math.max(
          ...Array.from(tastingScenario.options).map(
            (option) => option.textContent?.trim().length ?? 0,
          ),
        ),
        widestScenarioLabel: Math.max(
          ...Array.from(tastingScenario.options).map((option) =>
            canvasContext
              ? canvasContext.measureText(option.textContent?.trim() ?? "")
                  .width
              : Number.POSITIVE_INFINITY,
          ),
        ),
        transformationHeadingColor: window.getComputedStyle(
          transformationHeading,
        ).color,
        transformationBackgroundColor:
          window.getComputedStyle(transformation).backgroundColor,
        textSizeAdjust:
          window
            .getComputedStyle(shell)
            .getPropertyValue("-webkit-text-size-adjust") ||
          window.getComputedStyle(shell).getPropertyValue("text-size-adjust"),
        servicePlaceholderLength: tastingService.placeholder.length,
        pageHeight: document.documentElement.scrollHeight,
        viewportHeight: window.innerHeight,
      };
    });
    expect(mobileCommercialLayout.headlineFontSize).toBeLessThanOrEqual(38);
    expect(mobileCommercialLayout.headlineHeight).toBeLessThanOrEqual(210);
    expect(mobileCommercialLayout.firstActionBottom).toBeLessThanOrEqual(
      mobileCommercialLayout.viewportHeight,
    );
    expect(
      mobileCommercialLayout.proofColumns.trim().split(/\s+/),
    ).toHaveLength(1);
    expect(mobileCommercialLayout.proofContentWidth).toBeGreaterThanOrEqual(
      270,
    );
    expect(mobileCommercialLayout.tastingControlWidth).toBeGreaterThanOrEqual(
      270,
    );
    expect(mobileCommercialLayout.longestScenarioLabel).toBeLessThanOrEqual(24);
    expect(mobileCommercialLayout.widestScenarioLabel).toBeLessThanOrEqual(
      mobileCommercialLayout.tastingControlWidth - 52,
    );
    expect(mobileCommercialLayout.textSizeAdjust).toBe("100%");
    expect(
      contrastRatio(
        mobileCommercialLayout.transformationHeadingColor,
        mobileCommercialLayout.transformationBackgroundColor,
      ),
    ).toBeGreaterThanOrEqual(4.5);
    expect(mobileCommercialLayout.servicePlaceholderLength).toBeLessThanOrEqual(
      30,
    );
    expect(mobileCommercialLayout.pageHeight).toBeLessThanOrEqual(
      psiquePageHeightLimit - crossBrowserRenderingSafetyMargin,
    );
  }
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth),
  ).toBeLessThanOrEqual(
    await page.evaluate(() => document.documentElement.clientWidth),
  );
  await expect(
    page.getByText("Prévia para validar o tom em até 12 horas"),
  ).toBeVisible();
  await expect(page.getByText("Revisão humana antes do uso")).toBeVisible();
  await expect(page.getByTestId("commercial-offer")).toContainText("R$ 349");
  await expect(page.getByTestId("commercial-offer")).toContainText(
    "Pagamento único, sem recorrência",
  );
  await expect(page.getByTestId("commercial-offer")).toContainText(
    /briefing inicial está incluído/i,
  );
  await expect(page.getByTestId("commercial-offer")).toContainText(
    /prazo de até 48 horas começa após o pagamento confirmado/i,
  );
  const checkout = page
    .getByTestId("commercial-offer")
    .getByRole("link", { name: "Quero meu atendimento sob medida" });
  await expect(checkout).toHaveAttribute(
    "href",
    "https://pay.example/kit-whatsapp",
  );
  await expect(page.getByTestId("assisted-scope")).toContainText(
    "15 respostas personalizadas",
  );
  await expect(page.getByTestId("assisted-public-proofs")).toContainText(
    "Três follow-ups manuais",
  );
  await expect(page.getByTestId("assisted-process")).toContainText(
    "Primeira aplicação",
  );
  await expect(page.getByTestId("assisted-transformation")).toContainText(
    "Da resposta solta ao próximo passo",
  );
  await expect(page.getByTestId("assisted-closing-offer")).toContainText(
    "Tenha a próxima mensagem pronta antes de a conversa esfriar",
  );
  await expect(
    page
      .getByTestId("assisted-closing-offer")
      .getByRole("link", { name: "Quero meu atendimento sob medida" }),
  ).toHaveAttribute("href", "https://pay.example/kit-whatsapp");
  await expect(
    page.getByRole("heading", { name: "Acesse sua área" }),
  ).toHaveCount(0);
  await page.getByRole("link", { name: "Já comprou? Acesse sua área" }).click();
  await expect(
    page.getByRole("heading", { name: "Acesse sua área" }),
  ).toBeVisible();
  await page.goBack();
  await expect(
    page.getByRole("heading", {
      name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
    }),
  ).toBeVisible();
  await expect(page.getByTestId("commercial-legal")).toContainText(
    "Digicom Digital",
  );
  await expect(page.getByTestId("commercial-legal")).toContainText(
    "CNPJ 00.000.000/0001-00",
  );
  await expect(page.getByTestId("commercial-legal")).not.toContainText(
    "Endereço de homologação",
  );
  await expect(page.getByRole("link", { name: "Termos" })).toHaveCount(2);
  const tasting = page.getByTestId("assisted-tasting");
  await expect(tasting).toContainText(
    "Experimente uma sequência antes de comprar",
  );
  await tasting.getByLabel("Qual serviço você oferece?").fill("m");
  await tasting.getByRole("button", { name: "Gerar minha amostra" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Informe um serviço genérico",
  );
  await tasting.getByLabel("Qual serviço você oferece?").fill("manicure");
  const publicResponseProof = await page
    .locator('[data-proof-id="sample-response"] p')
    .innerText();
  const tastingResult = page.getByTestId("assisted-tasting-result");
  const materializedResponses = new Set<string>();
  for (const scenarioId of ["orcamento-sem-resposta", "pedido-de-preco"]) {
    for (const toneId of ["acolhedor", "direto", "profissional"]) {
      await tasting.getByLabel("Situação").selectOption(scenarioId);
      await tasting.getByLabel("Tom").selectOption(toneId);
      await tasting
        .getByRole("button", { name: "Gerar minha amostra" })
        .click();
      await expect(tastingResult).toContainText("manicure");
      await expect(tastingResult).toContainText("Pergunta de qualificação");
      await expect(tastingResult.locator("ol li")).toHaveCount(3);
      if (scenarioId === "orcamento-sem-resposta" && toneId === "acolhedor") {
        const tastingResponseProof = await tastingResult
          .getByRole("heading", { name: "Resposta inicial" })
          .locator("xpath=following-sibling::p[1]")
          .innerText();
        expect(tastingResponseProof).toBe(publicResponseProof);
      }
      materializedResponses.add(
        await tastingResult
          .getByRole("heading", { name: "Resposta inicial" })
          .locator("xpath=following-sibling::p[1]")
          .innerText(),
      );
    }
  }
  expect(materializedResponses.size).toBe(6);
  await tasting.getByLabel("Situação").selectOption("orcamento-sem-resposta");
  await tasting.getByLabel("Tom").selectOption("acolhedor");
  await tasting.getByRole("button", { name: "Gerar minha amostra" }).click();
  await expect(tastingResult).toContainText("manicure");
  await expect(tastingResult).toContainText(
    "A implantação paga inclui briefing",
  );
  await expect
    .poll(() => commercialEvents.map((event) => event.eventType))
    .toEqual(
      expect.arrayContaining([
        "TASTING_STARTED",
        "VALUE_MOMENT",
        "PAYWALL_VIEWED",
      ]),
    );
  await expect
    .poll(
      () =>
        commercialEvents.filter(
          (event) => event.eventType === "TASTING_STARTED",
        ).length,
    )
    .toBe(1);
  await expect
    .poll(
      () =>
        commercialEvents.filter((event) => event.eventType === "VALUE_MOMENT")
          .length,
    )
    .toBe(7);
  await expect
    .poll(
      () =>
        commercialEvents.filter((event) => event.eventType === "PAYWALL_VIEWED")
          .length,
    )
    .toBe(7);
  const eventKeys = (eventType: string) =>
    commercialEvents
      .filter((event) => event.eventType === eventType)
      .map(
        (event) =>
          (event.metadata as Record<string, unknown>).idempotencyKey as string,
      );
  const tastingStartedKeys = eventKeys("TASTING_STARTED");
  const valueMomentKeys = eventKeys("VALUE_MOMENT");
  const paywallKeys = eventKeys("PAYWALL_VIEWED");
  expect(tastingStartedKeys).toEqual([
    "tasting-started:kit-whatsapp-tasting-v1",
  ]);
  expect(new Set(valueMomentKeys).size).toBe(6);
  expect(new Set(paywallKeys)).toEqual(new Set(valueMomentKeys));
  expect(valueMomentKeys.at(-1)).toBe(valueMomentKeys[0]);
  expect(paywallKeys.at(-1)).toBe(paywallKeys[0]);
  for (const key of [
    ...tastingStartedKeys,
    ...valueMomentKeys,
    ...paywallKeys,
  ]) {
    expect(key).toMatch(/^[a-z0-9._:-]+$/);
  }
  expect(JSON.stringify(commercialEvents)).not.toContain("manicure");

  const popupPromise = page.waitForEvent("popup");
  await checkout.click();
  const checkoutPage = await popupPromise;
  await expect(
    checkoutPage.getByRole("heading", {
      name: "Checkout de homologação — Kit WhatsApp Pronto",
    }),
  ).toBeVisible();
  await expect(checkoutPage.getByText("R$ 349")).toBeVisible();
  await expect(
    checkoutPage.getByText("Pagamento único, sem recorrência"),
  ).toBeVisible();
  await expect(checkoutPage.getByText("Digicom Digital")).toBeVisible();
  await expect(
    checkoutPage.getByText(
      "Nenhuma cobrança adicional será criada neste ambiente local.",
    ),
  ).toBeVisible();
  await checkoutPage.close();
  const checkoutRetryPromise = page.waitForEvent("popup");
  await checkout.click();
  const checkoutRetryPage = await checkoutRetryPromise;
  await checkoutRetryPage.close();
  await expect
    .poll(() => commercialEvents.map((event) => event.eventType))
    .toContain("CHECKOUT_STARTED");
  await expect
    .poll(
      () =>
        commercialEvents.filter(
          (event) => event.eventType === "CHECKOUT_STARTED",
        ).length,
    )
    .toBe(2);
  const checkoutEvents = commercialEvents.filter(
    (event) => event.eventType === "CHECKOUT_STARTED",
  );
  const checkoutEvent = checkoutEvents[0];
  expect(checkoutEvent).toMatchObject({
    productSlug,
    source: "mh_test",
    metadata: {
      experimentId: 89,
      contractVersion: "kit-whatsapp-tasting-v1",
      priceBrl: 349,
      checkoutHost: "pay.example",
    },
  });
  expect(
    (checkoutEvent?.metadata as Record<string, unknown>).idempotencyKey,
  ).toMatch(/^checkout:[a-z0-9._:-]+$/);
  expect(
    new Set(
      checkoutEvents.map(
        (event) => (event.metadata as Record<string, unknown>).idempotencyKey,
      ),
    ).size,
  ).toBe(1);

  const email = `teste+kit-whatsapp-${testInfo.project.name}-${Date.now()}@sandbox.local`;
  const unknownLogin = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    { data: { productSlug, email } },
  );
  expect(unknownLogin.status()).toBe(404);
  const paymentTransactionId = `mp-rigel-${testInfo.project.name}-${Date.now()}`;
  const approvedPayment = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-payment-entitlements`,
    {
      headers: internalHeaders,
      data: {
        email,
        transactionId: paymentTransactionId,
        paymentStatus: "approved",
        experienceVersion: "kit-whatsapp-pronto-pde-v2",
      },
    },
  );
  expect(approvedPayment.status()).toBe(201);
  const paidLoginResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    { data: { productSlug, email } },
  );
  expect(paidLoginResponse.ok()).toBeTruthy();
  const paidLogin = (await paidLoginResponse.json()) as {
    accessUrl: string;
    deliveryStatus: string;
  };
  expect(paidLogin.deliveryStatus).toBe("SENT");
  expect(paidLogin.accessUrl).toMatch(/^\/access#access=[a-z0-9-]+$/);
  const expectedAccessToken = paidLogin.accessUrl.split("#access=")[1];
  expect(expectedAccessToken).toBeTruthy();
  const paidLoginRetry = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    { data: { productSlug, email } },
  );
  expect((await paidLoginRetry.json()).accessUrl).toBe(paidLogin.accessUrl);
  await page.goto(`/access?mh_test=1#access=${expectedAccessToken}`);

  await expect(page.getByTestId("assisted-workspace")).toBeVisible();
  expect(page.url()).not.toContain(expectedAccessToken);
  expect(new URL(page.url()).pathname).toBe("/access");
  expect(new URL(page.url()).hash).toBe("");
  expect(new URL(page.url()).searchParams.get("mh_test")).toBe("1");
  await expect(page.getByText(email)).toBeVisible();
  await expect(page.getByText("0 de 6 etapas concluídas")).toBeVisible();
  await expect(
    page.getByText(
      /prazos começam após pagamento aprovado e entrada completa/i,
    ),
  ).toBeVisible();
  await expect(
    page.getByText(/não informe nomes, telefones, endereços/i),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "Abrir material" })).toHaveCount(
    0,
  );
  await expect(
    page.getByText(
      /materiais liberados após a equipe concluir a entrega completa/i,
    ),
  ).toBeVisible();
  await expect(
    page.getByText(/aguardando a equipe concluir esta etapa/i),
  ).toHaveCount(4);

  await page
    .getByLabel("Serviços principais")
    .fill("Manicure, alongamento e manutenção de unhas.");
  await page
    .getByLabel("Dúvidas e objeções recorrentes")
    .fill("Preço, duração e agenda disponível.");
  await page
    .getByLabel("Regras de preço, agenda e área atendida")
    .fill("Atendimento com hora marcada na região central.");
  await page
    .getByLabel("Tom de voz desejado")
    .fill("Acolhedor, profissional e direto.");
  await page
    .getByLabel("Cinco situações equivalentes ou exemplos anonimizados")
    .fill(
      "1. Pedido de preço; 2. Horário; 3. Durabilidade; 4. Reagendamento; 5. Primeira visita.",
    );
  await page.getByRole("button", { name: "Concluir etapa" }).first().click();
  await expect(page.getByText("1 de 6 etapas concluídas")).toBeVisible();
  await expect(
    page.getByText("Etapa salva. Você pode sair e retomar com o mesmo acesso."),
  ).toBeVisible();

  await page.reload();
  await expect(page.getByTestId("assisted-workspace")).toBeVisible();
  await expect(page.getByText("1 de 6 etapas concluídas")).toBeVisible();

  const accessToken = await page.evaluate(
    (slug) => localStorage.getItem(`pde-access:${slug}`),
    productSlug,
  );
  expect(accessToken).toBeTruthy();
  expect(accessToken).toBe(expectedAccessToken);

  const deliveryBeforeOperation = await request.get(
    `${backendBaseUrl}/api/pde/access/deliveries/microvalor-12h/download`,
    { headers: { "X-PDE-Access-Token": accessToken! } },
  );
  expect(deliveryBeforeOperation.ok()).toBeFalsy();

  const unauthorizedCustomerAdvance = await request.post(
    `${backendBaseUrl}/api/pde/access/missions/conferencia-de-completude/complete`,
    { headers: accessHeaders(accessToken!) },
  );
  expect(unauthorizedCustomerAdvance.ok()).toBeFalsy();

  const unauthorizedOperationAdvance = await request.post(
    `${backendBaseUrl}/api/internal/pde/assisted-operation/access/missions/conferencia-de-completude/complete`,
    { headers: operationHeaders(accessToken!, "token-incorreto") },
  );
  expect(unauthorizedOperationAdvance.status()).toBe(403);

  const operationalStages = [
    { missionId: "conferencia-de-completude" },
    { missionId: "diagnostico-humano" },
    {
      missionId: "microvalor-12h",
      deliveryTitle: "Microentrega personalizada — Studio Aurora",
      deliveryVersion: "micro-v1",
      deliveryContent:
        "# Microentrega\n\nCenários: preço, agenda e durabilidade.\n\nPerguntas: qual serviço deseja e para quando precisa?\n\nResposta inicial: Olá! Para manicure com hora marcada, diga o serviço e o melhor período para eu confirmar a agenda. Revisar antes de enviar.",
    },
    {
      missionId: "entrega-completa-48h",
      deliveryTitle: "Kit completo personalizado — Studio Aurora",
      deliveryVersion: "kit-v1",
      deliverySections: [
        {
          sectionId: "responses",
          items: Array.from(
            { length: 15 },
            (_, index) =>
              `Resposta ${index + 1}: Olá! Para manicure, alongamento ou manutenção com hora marcada, confirme o serviço e o melhor período; responderei com preço e disponibilidade em tom acolhedor.`,
          ),
        },
        {
          sectionId: "qualificationQuestions",
          items: [
            "Qual serviço você deseja: manicure, alongamento ou manutenção?",
            "Para qual dia e período você precisa do atendimento?",
            "É sua primeira visita ao Studio Aurora?",
            "Você precisa remover ou manter algum alongamento anterior?",
            "Qual região central facilita seu atendimento?",
            "Existe alguma preferência de acabamento?",
            "Você já conhece nossas regras de reagendamento?",
            "Posso confirmar a duração prevista antes de reservar?",
          ],
        },
        {
          sectionId: "followUps",
          items: [
            "Follow-up 1: Posso reservar o melhor período para você hoje?",
            "Follow-up 2: Ainda quer que eu confirme preço e duração deste serviço?",
            "Follow-up 3: Se o horário mudou, diga outro período e eu verifico.",
            "Follow-up 4: Posso encerrar por agora e retomar quando você preferir.",
          ],
        },
        {
          sectionId: "escalationRules",
          items: [
            "Interromper o modelo e revisar pessoalmente qualquer reclamação.",
            "Não inventar preço, desconto, duração ou disponibilidade.",
            "Confirmar com uma pessoa qualquer exceção de reagendamento.",
            "Não reutilizar dados pessoais de conversas anteriores.",
          ],
        },
        {
          sectionId: "usageGuide",
          items: [
            "Escolha a resposta correspondente à intenção recebida.",
            "Substitua somente os campos confirmados pelo negócio.",
            "Leia integralmente antes de copiar para o WhatsApp.",
            "Faça as perguntas de qualificação uma por vez.",
            "Registre ajustes pequenos para a revisão do kit.",
          ],
        },
        {
          sectionId: "checklist",
          items: [
            "Serviço confirmado.",
            "Preço confirmado.",
            "Agenda confirmada.",
            "Área atendida confirmada.",
            "Tom acolhedor preservado.",
            "Nenhum dado pessoal reaproveitado.",
            "Exceções revisadas por pessoa.",
            "Mensagem lida antes do envio.",
          ],
        },
      ],
    },
  ];
  const incompleteDelivery = await request.post(
    `${backendBaseUrl}/api/internal/pde/assisted-operation/access/missions/microvalor-12h/complete`,
    {
      headers: operationHeaders(accessToken!, "pde-local-operation-test"),
    },
  );
  expect(incompleteDelivery.ok()).toBeFalsy();

  for (const stage of operationalStages) {
    if (stage.missionId === "entrega-completa-48h") {
      for (const invalidCount of [
        { sectionId: "responses", count: 14 },
        { sectionId: "qualificationQuestions", count: 7 },
        { sectionId: "followUps", count: 3 },
      ]) {
        const incompleteSections = stage.deliverySections?.map((section) =>
          section.sectionId === invalidCount.sectionId
            ? { ...section, items: section.items.slice(0, invalidCount.count) }
            : section,
        );
        const incompleteFullDelivery = await request.post(
          `${backendBaseUrl}/api/internal/pde/assisted-operation/access/missions/${stage.missionId}/complete`,
          {
            headers: operationHeaders(
              accessToken!,
              "pde-local-operation-test",
            ),
            data: {
              deliveryTitle: stage.deliveryTitle,
              deliveryVersion: stage.deliveryVersion,
              deliverySections: incompleteSections,
            },
          },
        );
        expect(
          incompleteFullDelivery.ok(),
          `escopo parcial aceito: ${invalidCount.sectionId}=${invalidCount.count}`,
        ).toBeFalsy();
      }
    }
    const operationAdvance = await request.post(
      `${backendBaseUrl}/api/internal/pde/assisted-operation/access/missions/${stage.missionId}/complete`,
      {
        headers: operationHeaders(accessToken!, "pde-local-operation-test"),
        data:
          stage.deliveryContent || stage.deliverySections
            ? {
                deliveryTitle: stage.deliveryTitle,
                deliveryVersion: stage.deliveryVersion,
                deliveryContent: stage.deliveryContent,
                deliverySections: stage.deliverySections,
              }
            : undefined,
      },
    );
    expect(
      operationAdvance.ok(),
      `etapa operacional não concluída: ${stage.missionId}`,
    ).toBeTruthy();
  }

  await page.reload();
  await expect(page.getByText("5 de 6 etapas concluídas")).toBeVisible();
  await expect(page.getByTestId("delivery-microvalor-12h")).toContainText(
    "preço, agenda e durabilidade",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Resposta 15",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Perguntas de qualificação",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Checklist de revisão",
  );
  await expect(
    page.getByRole("link", { name: "Baixar entrega personalizada" }),
  ).toHaveCount(2);
  await expect(page.getByRole("link", { name: "Abrir material" })).toHaveCount(
    7,
  );

  const firstMaterialUrl = await page
    .getByRole("link", { name: "Abrir material" })
    .first()
    .getAttribute("href");
  expect(firstMaterialUrl).toBeTruthy();
  const unauthenticatedMaterial = await request.get(firstMaterialUrl!);
  expect(unauthenticatedMaterial.status()).toBe(403);
  const protectedMaterialRequest = page.waitForRequest(
    (browserRequest) =>
      browserRequest.url().includes(firstMaterialUrl!) &&
      browserRequest.headers()["x-pde-access-token"] === accessToken,
  );
  await page.getByRole("link", { name: "Abrir material" }).first().click();
  await protectedMaterialRequest;

  const personalDeliveryLinks = await page
    .getByRole("link", { name: "Baixar entrega personalizada" })
    .evaluateAll((nodes) =>
      nodes.map((node) => (node as HTMLAnchorElement).href),
    );
  for (const link of personalDeliveryLinks) {
    expect(new URL(link).pathname).toMatch(
      /^\/api\/pde\/access\/deliveries\/[a-z0-9-]+\/download$/,
    );
    expect(link).not.toContain(accessToken);
    expect((await request.get(link)).status()).toBe(403);
    const response = await request.get(link, {
      headers: { "X-PDE-Access-Token": accessToken! },
    });
    expect(
      response.ok(),
      `entrega personalizada indisponível: ${link}`,
    ).toBeTruthy();
    expect(response.headers()["content-disposition"]).toContain("attachment");
    expect((await response.text()).length).toBeGreaterThan(100);
  }
  const protectedDeliveryRequest = page.waitForRequest(
    (browserRequest) =>
      browserRequest.url().includes(personalDeliveryLinks[0]) &&
      browserRequest.headers()["x-pde-access-token"] === accessToken,
  );
  await page
    .getByRole("link", { name: "Baixar entrega personalizada" })
    .first()
    .click();
  await protectedDeliveryRequest;

  for (const link of await page
    .getByRole("link", { name: "Abrir material" })
    .evaluateAll((nodes) =>
      nodes.map((node) => (node as HTMLAnchorElement).href),
    )) {
    const response = await request.get(link, {
      headers: { "X-PDE-Access-Token": accessToken! },
    });
    expect(response.ok(), `material indisponível: ${link}`).toBeTruthy();
    expect((await response.text()).length).toBeGreaterThan(150);
  }

  await page
    .getByLabel("Três respostas escolhidas")
    .fill("Preço, agenda e durabilidade.");
  await page
    .getByLabel("Bloco de qualificação escolhido")
    .fill("Serviço desejado e melhor período.");
  await page
    .getByLabel("Regra de escalonamento escolhida")
    .fill("Interromper o modelo em reclamações ou exceções de preço.");
  await page
    .getByLabel("Situação da primeira aplicação")
    .selectOption("PLANNED");
  await page
    .getByLabel("Retorno observado na primeira aplicação")
    .fill("Ainda não aplicado; apenas planejado para homologar o bloqueio.");
  await page
    .getByLabel("Revisão da primeira aplicação")
    .fill("Planejamento revisado, sem afirmar que houve uso real.");
  await page.getByRole("button", { name: "Concluir etapa" }).click();
  await expect(page.getByRole("alert")).toContainText(
    /primeira aplicação manual real/i,
  );
  await page
    .getByLabel("Situação da primeira aplicação")
    .selectOption("APPLIED");
  await page
    .getByLabel("Retorno observado na primeira aplicação")
    .fill(
      "Uso manual de homologação concluído: a pergunta de serviço produziu resposta clara, sem envio a cliente real.",
    );
  await page
    .getByLabel("Revisão da primeira aplicação")
    .fill(
      "O fluxo exigiu leitura humana e não publicou, enviou ou tratou a homologação como venda.",
    );
  await page.getByRole("button", { name: "Concluir etapa" }).click();
  await expect(page.getByText("6 de 6 etapas concluídas")).toBeVisible();

  await page
    .getByLabel("Como podemos ajudar?")
    .fill("Preciso revisar uma resposta antes da primeira aplicação.");
  await page
    .getByRole("button", { name: "Solicitar suporte ou revisão" })
    .click();
  await expect(page.getByText(/pedido de suporte registrado/i)).toBeVisible();
  await page.reload();
  await expect(
    page.getByText(/existe um pedido de suporte aberto/i),
  ).toBeVisible();

  const magicLinkResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    {
      data: { productSlug, email },
    },
  );
  expect(magicLinkResponse.ok()).toBeTruthy();
  expect((await magicLinkResponse.json()).deliveryStatus).toBe("SENT");
  await expect
    .poll(async () => {
      const mailResponse = await request.get(`${mailBaseUrl}/api/v1/messages`);
      const mailbox = await mailResponse.json();
      return mailbox.messages.some((message: { To?: { Address?: string }[] }) =>
        message.To?.some((recipient) => recipient.Address === email),
      );
    })
    .toBeTruthy();

  await page.evaluate(
    (slug) => localStorage.removeItem(`pde-access:${slug}`),
    productSlug,
  );
  const secondPage = await page.context().newPage();
  secondPage.on("request", captureBrowserRequest);
  await secondPage.goto(`/access?mh_test=1#access=${accessToken}`);
  await expect(secondPage.getByTestId("assisted-workspace")).toBeVisible();
  expect(secondPage.url()).not.toContain(accessToken);
  expect(new URL(secondPage.url()).pathname).toBe("/access");
  expect(new URL(secondPage.url()).searchParams.get("mh_test")).toBe("1");
  await expect(secondPage.getByText(email)).toBeVisible();
  await expect(secondPage.getByText("6 de 6 etapas concluídas")).toBeVisible();
  await expect
    .poll(() =>
      secondPage.evaluate(
        (slug) => localStorage.getItem(`pde-access:${slug}`),
        productSlug,
      ),
    )
    .toBe(accessToken);
  await secondPage.close();

  const refundedPayment = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-payment-entitlements`,
    {
      headers: internalHeaders,
      data: {
        email,
        transactionId: paymentTransactionId,
        paymentStatus: "refunded",
        experienceVersion: "kit-whatsapp-pronto-pde-v2",
      },
    },
  );
  expect(refundedPayment.status()).toBe(201);
  const refundedPaymentRetry = await request.post(
    `${backendBaseUrl}/api/internal/pde/test-payment-entitlements`,
    {
      headers: internalHeaders,
      data: {
        email,
        transactionId: paymentTransactionId,
        paymentStatus: "refunded",
        experienceVersion: "kit-whatsapp-pronto-pde-v2",
      },
    },
  );
  expect(refundedPaymentRetry.status()).toBe(201);
  expect(
    (
      await request.get(
        `${backendBaseUrl}/api/pde/access/workspace`,
        { headers: accessHeaders(accessToken!) },
      )
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.post(
        `${backendBaseUrl}/api/pde/access/missions/entrada-guiada/interactions`,
        {
          headers: accessHeaders(accessToken!),
          data: { answers: { services: "não deve persistir" } },
        },
      )
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.post(
        `${backendBaseUrl}/api/internal/pde/assisted-operation/access/missions/conferencia-de-completude/complete`,
        {
          headers: operationHeaders(
            accessToken!,
            "pde-local-operation-test",
          ),
        },
      )
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.get(
        `${backendBaseUrl}/api/pde/access/materials/authorize`,
        {
          headers: { "X-PDE-Access-Token": accessToken! },
        },
      )
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.get(
        `${backendBaseUrl}/api/pde/access/deliveries/microvalor-12h/download`,
        { headers: { "X-PDE-Access-Token": accessToken! } },
      )
    ).status(),
  ).toBe(403);
  expect(
    (
      await request.post(
        `${backendBaseUrl}/api/pde/access/support-requests`,
        {
          headers: accessHeaders(accessToken!),
          data: { message: "Suporte após reembolso continua acessível." },
        },
      )
    ).ok(),
  ).toBeTruthy();
  expect(
    (
      await request.post(
        `${backendBaseUrl}/api/pde/access/privacy-requests`,
        {
          headers: accessHeaders(accessToken!),
          data: { action: "ACCESS" },
        },
      )
    ).ok(),
  ).toBeTruthy();

  await expect
    .poll(async () => {
      const response = await request.get(
        `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary`,
      );
      const summary = await response.json();
      return {
        commercialEvents: summary.totalEvents,
        rawEvents: summary.rawTotalEvents,
        humanSessions: summary.humanSessions,
        hasInternalQaSession: summary.internalQaSessions >= 1,
      };
    })
    .toEqual({
      commercialEvents: 0,
      rawEvents: expect.any(Number),
      humanSessions: 0,
      hasInternalQaSession: true,
    });

  const summaryResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary`,
  );
  const persistedAnalytics = await summaryResponse.json();
  expect(persistedAnalytics.rawTotalEvents).toBeGreaterThanOrEqual(12);
  expect(JSON.stringify(commercialEvents)).not.toContain(accessToken);
  expect(JSON.stringify(persistedAnalytics)).not.toContain(accessToken);
  const authenticatedRequests = browserAccessRequests.filter((entry) =>
    /\/(workspace|missions|deliveries|materials|support-requests|privacy-requests|ai-guidance)(\/|$)/.test(
      new URL(entry.url).pathname,
    ),
  );
  expect(authenticatedRequests.length).toBeGreaterThan(6);
  for (const entry of authenticatedRequests) {
    expect(entry.url).not.toContain(accessToken);
    expect(entry.postData).not.toContain(accessToken);
    expect(entry.referrer).not.toContain(accessToken);
    expect(entry.accessHeader).toBe(accessToken);
  }
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth + 1,
    ),
  ).toBeTruthy();
});

test("bloqueia e orienta acesso inválido sem perder o contrato público", async ({
  page,
}) => {
  await page.goto("/?access=token-invalido&mh_test=1");
  await expect(
    page.getByRole("heading", {
      name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
    }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(
    /acesso|encontrado|solicitação/i,
  );
  await expect(page.getByTestId("assisted-workspace")).toHaveCount(0);
});

test("publica termos, privacidade e reembolso com o fornecedor da oferta", async ({
  page,
}) => {
  const policies = [
    ["/terms", "Termos da implantação personalizada"],
    ["/privacy", "Privacidade e uso de dados"],
    ["/refund-policy", "Cancelamento e reembolso"],
  ] as const;

  for (const [path, heading] of policies) {
    await page.goto(path);
    await expect(page.getByRole("heading", { name: heading })).toBeVisible();
    await expect(page.getByText("Digicom Digital").first()).toBeVisible();
    await expect(page.getByText("CNPJ 00.000.000/0001-00")).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
    ).toBeTruthy();
  }
});

test("exige e-mail válido antes de criar acesso segregado", async ({
  page,
}) => {
  await page.getByRole("link", { name: "Já comprou? Acesse sua área" }).click();
  await page.getByLabel("E-mail").fill("email-invalido");
  await page.getByRole("button", { name: "Entrar na homologação" }).click();
  await expect(page.getByTestId("assisted-workspace")).toHaveCount(0);
  expect(
    await page
      .getByLabel("E-mail")
      .evaluate((input: HTMLInputElement) => input.validity.typeMismatch),
  ).toBeTruthy();
});
