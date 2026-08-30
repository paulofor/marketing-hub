import { expect, test } from "@playwright/test";

test("carrega a entrada visual do Clube MUSA", async ({ page }) => {
  const completedGuidance = {
    requestId: "diagnostico-visual-1",
    productSlug: "metodo-musa-7-dias",
    missionId: "diagnostico-presenca-publico",
    guidanceType: "MUSA_PUBLIC_PRESENCE_DIAGNOSTIC",
    status: "COMPLETED",
    headline: "Seu plano começa reduzindo ruído visual",
    summary:
      "A Consultora MUSA identificou que sua presença precisa de acabamento simples e repetível.",
    signals: ["Acabamento", "Intenção", "Coerência"],
    microActions: [
      "Dia 1: escolha uma ocasião real e retire um excesso visual.",
      "Dia 2: defina uma peça-sinal para repetir com intenção.",
      "Dia 3: combine duas cores de forma mais limpa.",
      "Dia 4: ajuste cabelo, pele ou acessório antes de sair.",
      "Dia 5: monte uma fórmula simples com o que já existe.",
      "Dia 6: repita a fórmula em uma situação importante.",
      "Dia 7: registre sua assinatura MUSA pessoal.",
    ],
    caution:
      "Use como orientação prática, sem promessa automática de resultado universal.",
  };

  await page.route("**/api/pde/access/events", async (route) => {
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route("/api/pde/products/metodo-musa-7-dias", async (route) => {
    await route.fulfill({
      status: 404,
      json: { error: "Produto carregado pelo fallback do teste visual." },
    });
  });
  await page.route("/api/pde/public/presence-diagnostic", async (route) => {
    await route.fulfill({ json: completedGuidance });
  });
  await page.route(
    "/api/pde/public/presence-diagnostic/diagnostico-visual-1",
    async (route) => {
      await route.fulfill({ json: completedGuidance });
    },
  );
  await page.route("/api/pde/access/magic-link", async (route) => {
    await route.fulfill({
      json: {
        productSlug: "metodo-musa-7-dias",
        email: "teste+diagnostico@sandbox.local",
        deliveryStatus: "SENT",
      },
    });
  });

  await page.goto("/?musa_video_variant=control");

  await expect(
    page.getByRole("heading", {
      name: /falta presença/i,
      level: 1,
    }),
  ).toBeVisible();
  await expect(page.getByText(/Domínios conhecidos apontados/i)).toHaveCount(0);
  await expect(page.getByText(/Slots versionados do Clube MUSA/i)).toHaveCount(
    0,
  );
  await expect(
    page.getByRole("region", { name: "Diagnóstico de Presença" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: /Descobrir meu primeiro ajuste/i }),
  ).toBeDisabled();
  await page.getByRole("button", { name: "Falta acabamento" }).click();
  await page.getByRole("button", { name: "Trabalho ou reunião" }).click();
  await page.getByRole("button", { name: "Elegância discreta" }).click();
  await page.getByRole("button", { name: "Cabelo e pele" }).click();
  await expect(
    page.getByRole("button", { name: /Descobrir meu primeiro ajuste/i }),
  ).toBeEnabled();
  await page
    .getByRole("button", { name: /Descobrir meu primeiro ajuste/i })
    .click();
  await expect(
    page.getByRole("heading", {
      name: /Seu plano começa reduzindo ruído visual/i,
    }),
  ).toBeVisible();
  await expect(page.getByText(/Resultado MUSA gratuito/i)).toBeVisible();
  await expect(page.getByText(/Seu sinal principal hoje/i)).toBeVisible();
  await expect(
    page.getByRole("region", {
      name: /Preview bloqueado do plano MUSA de 7 dias/i,
    }),
  ).toBeVisible();
  await expect(
    page.getByText(
      /Os Dias 2 a 7 e os materiais ficam disponíveis por 90 dias/i,
    ),
  ).toBeVisible();
  await page
    .getByPlaceholder("seuemail@exemplo.com")
    .fill("teste+diagnostico@sandbox.local");
  await expect(
    page.getByText(/libera gratuitamente apenas o Dia 1/i),
  ).toBeVisible();
  await expect(
    page.getByText(
      /pagamento único de R\$ 67, sem assinatura ou renovação automática/i,
    ),
  ).toBeVisible();
  await page
    .getByRole("button", { name: /Salvar resultado e liberar o Dia 1/i })
    .click();
  await expect(page.getByText(/Enviei para seu e-mail/i)).toBeVisible();

  await page.screenshot({
    path: test.info().outputPath("musa-entry.png"),
    fullPage: true,
  });
});

test("v7 emite o funil canônico da degustação com chave idempotente", async ({
  page,
}) => {
  const trackedPayloads: Array<{
    eventType: string;
    metadata?: Record<string, unknown>;
  }> = [];
  const completedGuidance = {
    requestId: "diagnostico-v7-canonico-1",
    productSlug: "metodo-musa-7-dias",
    missionId: "diagnostico-presenca-publico",
    guidanceType: "MUSA_PUBLIC_PRESENCE_DIAGNOSTIC_V7",
    status: "COMPLETED",
    headline: "Seu primeiro sinal já pode ser organizado",
    summary: "Comece por um ajuste pequeno usando o que você já possui.",
    signals: ["Elegância discreta", "Estrutura leve"],
    microActions: ["Dia 1: organize a mensagem visual antes de sair."],
    caution: "A orientação não garante transformação ou aprovação externa.",
  };
  await page.route("**/api/pde/access/events", async (route) => {
    trackedPayloads.push(route.request().postDataJSON());
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route(
    "**/api/pde/products/metodo-musa-7-dias**",
    async (route) => {
      await route.fulfill({
        status: 404,
        json: { error: "Fallback canônico v7 do teste." },
      });
    },
  );
  await page.route("/api/pde/public/presence-diagnostic", async (route) => {
    await route.fulfill({ status: 201, json: completedGuidance });
  });

  await page.goto(
    "/?experienceVersion=musa-pde-entry-v7-espelho-antes-de-sair",
  );
  for (const answer of [
    "Falta presença",
    "Trabalho ou reunião",
    "Elegância discreta",
    "Roupa que já tenho",
  ]) {
    await page.getByRole("button", { name: answer, exact: true }).click();
  }
  await page
    .getByRole("button", { name: /Descobrir meu primeiro ajuste/i })
    .click();
  await expect(
    page.getByRole("heading", { name: completedGuidance.headline }),
  ).toBeVisible();
  await expect
    .poll(() => trackedPayloads.map((payload) => payload.eventType))
    .toEqual(
      expect.arrayContaining([
        "TASTING_STARTED",
        "VALUE_MOMENT",
        "PAYWALL_VIEWED",
      ]),
    );

  const canonicalEvents = trackedPayloads.filter((payload) =>
    ["TASTING_STARTED", "VALUE_MOMENT", "PAYWALL_VIEWED"].includes(
      payload.eventType,
    ),
  );
  expect(canonicalEvents).toHaveLength(3);
  for (const event of canonicalEvents) {
    expect(event.metadata?.idempotencyKey).toMatch(/^[a-z0-9._:-]+$/);
  }
  expect(trackedPayloads.map((payload) => payload.eventType)).not.toEqual(
    expect.arrayContaining([
      "MICRO_EXPERIENCE_STARTED",
      "MICRO_RESULT_RECEIVED",
      "PAID_CONTINUATION_VIEWED",
    ]),
  );
});

test("continua aguardando diagnostico publico quando IA demora mais que 20 segundos", async ({
  page,
}) => {
  const pendingGuidance = {
    requestId: "diagnostico-lento-1",
    productSlug: "metodo-musa-7-dias",
    missionId: "diagnostico-presenca-publico",
    guidanceType: "MUSA_PUBLIC_PRESENCE_DIAGNOSTIC",
    status: "PENDING",
    headline: "",
    summary: "",
    signals: [],
    microActions: [],
    caution: "",
  };
  const completedGuidance = {
    ...pendingGuidance,
    status: "COMPLETED",
    headline: "Seu plano chegou sem travar a tela",
    summary:
      "A Consultora MUSA terminou depois da janela curta antiga e o resultado apareceu corretamente.",
    signals: ["Tempo de IA", "Polling longo", "Resultado entregue"],
    microActions: [
      "Dia 1: escolha uma base simples.",
      "Dia 2: retire um excesso visual.",
      "Dia 3: repita um detalhe de acabamento.",
      "Dia 4: alinhe cabelo ou pele.",
      "Dia 5: fotografe a combinacao.",
      "Dia 6: ajuste postura e presenca.",
      "Dia 7: salve sua formula final.",
    ],
    caution: "Comece pelo que voce ja tem.",
  };
  let pollRequests = 0;

  await page.addInitScript(() => {
    const originalSetTimeout = window.setTimeout;
    window.setTimeout = ((
      handler: TimerHandler,
      timeout?: number,
      ...args: unknown[]
    ) =>
      originalSetTimeout(
        handler,
        Math.min(Number(timeout ?? 0), 5),
        ...args,
      )) as typeof window.setTimeout;
  });
  await page.route("/api/pde/public/presence-diagnostic", async (route) => {
    await route.fulfill({ json: pendingGuidance });
  });
  await page.route(
    "/api/pde/public/presence-diagnostic/diagnostico-lento-1",
    async (route) => {
      pollRequests += 1;
      await route.fulfill({
        json: pollRequests <= 14 ? pendingGuidance : completedGuidance,
      });
    },
  );

  await page.goto("/?musa_video_variant=control");

  await page.getByRole("button", { name: "Falta acabamento" }).click();
  await page.getByRole("button", { name: "Trabalho ou reunião" }).click();
  await page.getByRole("button", { name: "Elegância discreta" }).click();
  await page.getByRole("button", { name: "Roupa que já tenho" }).click();
  await page
    .getByRole("button", { name: /Descobrir meu primeiro ajuste/i })
    .click();

  await expect(
    page.getByRole("button", { name: /Montando seu plano/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /Seu plano chegou sem travar a tela/i }),
  ).toBeVisible();
  expect(pollRequests).toBeGreaterThan(12);
});

test("modo Preview QA nao envia eventos comerciais", async ({ page }) => {
  let trackedEvents = 0;
  await page.route("/api/pde/access/events", async (route) => {
    trackedEvents += 1;
    await route.fulfill({ json: { status: "RECORDED" } });
  });

  await page.goto(
    "/?mh_preview=qa&pde_analytics=off&utm_source=internal&utm_medium=qa&utm_campaign=metodo-musa-7-dias_preview_qa&utm_content=product_card",
  );

  await expect(
    page.getByRole("heading", {
      name: /falta presença/i,
      level: 1,
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Falta acabamento" }).click();
  await page.waitForTimeout(250);

  expect(trackedEvents).toBe(0);
});

test("destaca o preco e atribui a versão no checkout da area MUSA", async ({
  page,
}) => {
  const trackedPayloads: Array<Record<string, unknown>> = [];
  const product = {
    slug: "metodo-musa-7-dias",
    experienceVersion: "musa-pde-entry-v6-video-motivacional",
    funnelVersion: "musa-membership-funnel-v1",
    name: "Método MUSA - Experiência Guiada de 7 Dias",
    promise:
      "Monte em 7 dias uma presença mais elegante com o que você já tem.",
    audience: "Mulheres urbanas",
    priceLabel: "R$67",
    theme: {
      primary: "#7a2444",
      accent: "#d6a75c",
      background: "#fff8f3",
      imageUrl: "/assets/musa-cover.png",
    },
    diagnostic: {
      title: "Mapa de Presença MUSA",
      intro:
        "Descubra o primeiro ajuste para sua imagem comunicar mais intenção hoje.",
      questions: ["O que minha imagem comunica hoje?"],
    },
    missions: [
      {
        id: "dia-1-ruido-visual",
        day: 1,
        title: "Ler o sinal que sua imagem comunica",
        principle:
          "A presença cresce quando você identifica o sinal visual principal.",
        action:
          "Escolha uma combinação real e registre o sinal que quer melhorar.",
        evidence: "Frase preenchida.",
        visualCue: "Compare antes e depois.",
      },
      {
        id: "dia-2-assinatura",
        day: 2,
        title: "Criar sua assinatura simples",
        principle: "Coerência repetida cria reconhecimento.",
        action: "Defina 3 sinais para repetir.",
        evidence: "Lista dos 3 sinais.",
        visualCue: "Monte um pequeno painel.",
      },
    ],
    supportMaterials: [],
    completionOffer: "Continue no Clube MUSA.",
  };

  await page.route("**/api/pde/access/events", async (route) => {
    trackedPayloads.push(
      route.request().postDataJSON() as Record<string, unknown>,
    );
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route("**/runtime-config.js", async (route) => {
    await route.fulfill({
      contentType: "application/javascript",
      body: "window.__MUSA_RUNTIME_CONFIG__ = { VITE_MUSA_CHECKOUT_URL: 'https://pay.example/owm6x' };",
    });
  });
  await page.addInitScript(() => {
    const browserWindow = window as Window & { __lastCheckoutUrl?: string };
    window.open = ((url?: string | URL) => {
      browserWindow.__lastCheckoutUrl = String(url ?? "");
      return null;
    }) as typeof window.open;
  });
  await page.route("/api/pde/access/workspace", async (route) => {
    expect(route.request().headers()["x-pde-access-token"]).toBe(
      "trial-token",
    );
    await route.fulfill({
      json: {
        product,
        email: "teste+paywall@sandbox.local",
        accessSource: "MAGIC_LINK",
        subscriptionStatus: "TRIAL",
        completedMissions: 1,
        totalMissions: 2,
        progressPercent: 50,
        completedMissionIds: ["dia-1-ruido-visual"],
        missionInteractions: [],
      },
    });
  });

  await page.goto("/access#access=trial-token");
  await expect.poll(() => new URL(page.url()).pathname).toBe("/access");

  const paywall = page.getByRole("region", {
    name: "Oferta de acesso completo MUSA",
  });
  await expect(paywall).toBeVisible();
  await expect(
    paywall.getByText("Acesso completo", { exact: true }),
  ).toBeVisible();
  await expect(
    paywall.getByLabel("Preço do acesso completo: R$67"),
  ).toContainText("R$67");
  const checkoutButton = paywall.getByRole("button", {
    name: /Liberar por R\$67/i,
  });
  await expect(checkoutButton).toBeVisible();
  await checkoutButton.dispatchEvent("click");
  await expect
    .poll(() =>
      page.evaluate(
        () =>
          (window as Window & { __lastCheckoutUrl?: string })
            .__lastCheckoutUrl ?? "",
      ),
    )
    .not.toBe("");
  const openedUrl = await page.evaluate(
    () =>
      (window as Window & { __lastCheckoutUrl?: string }).__lastCheckoutUrl ??
      "",
  );
  const checkout = new URL(openedUrl);
  expect(checkout.hostname).toBe("pay.example");
  expect(checkout.searchParams.get("utm_content")).toBe(
    "direct__pde_version__musa-pde-entry-v6-video-motivacional",
  );
  await expect.poll(() => trackedPayloads.length).toBeGreaterThanOrEqual(2);
  const serializedTelemetry = JSON.stringify(trackedPayloads);
  expect(serializedTelemetry).not.toContain("trial-token");
  expect(serializedTelemetry).toContain("/access");
});

test("bloqueia video de slides na versao publicada e permite controle sem player para QA", async ({
  page,
}) => {
  await page.route("/api/pde/access/events", async (route) => {
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route("/api/pde/products/metodo-musa-7-dias", async (route) => {
    await route.fulfill({
      status: 404,
      json: { error: "Produto carregado pelo fallback do teste visual." },
    });
  });

  await page.goto("/");

  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toHaveCount(0);
  await expect(page.locator("video.public-hero-video")).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: /falta presença/i }),
  ).toBeVisible();

  await page.goto("/?musa_video_variant=video");

  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toHaveCount(0);
  await expect(page.locator("video.public-hero-video")).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: /falta presença/i }),
  ).toBeVisible();

  await page.goto("/?musa_video_variant=control");

  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("heading", { name: /falta presença/i }),
  ).toBeVisible();
});

test("exibe player na versao v6 motivacional com video real aprovado", async ({
  page,
}) => {
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE:
        "musa-pde-entry-v6-video-motivacional",
    };
  });
  await page.route("/api/pde/access/events", async (route) => {
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route("/api/pde/products/metodo-musa-7-dias", async (route) => {
    await route.fulfill({
      status: 404,
      json: { error: "Produto carregado pelo fallback do teste visual." },
    });
  });

  await page.goto("/");

  await expect(
    page.getByRole("heading", { name: /falta presença/i }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Vídeo curto Método MUSA" }),
  ).toBeVisible();
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
  await expect(
    page.getByRole("region", { name: "Diagnóstico de Presença" }),
  ).toBeVisible();
});

test("mede reproducao real do video inicial MUSA", async ({ page }) => {
  const events: string[] = [];
  await page.addInitScript(() => {
    window.__MUSA_RUNTIME_CONFIG__ = {
      VITE_MUSA_HERO_VIDEO_URL: "https://cdn.test/musa-video.mp4",
    };
  });
  await page.route("/api/pde/access/events", async (route) => {
    const body = route.request().postDataJSON() as { eventType?: string };
    if (body.eventType) {
      events.push(body.eventType);
    }
    await route.fulfill({ json: { status: "RECORDED" } });
  });
  await page.route("/api/pde/products/metodo-musa-7-dias", async (route) => {
    await route.fulfill({
      status: 404,
      json: { error: "Produto carregado pelo fallback do teste visual." },
    });
  });

  await page.goto("/?musa_video_variant=video");
  const video = page.locator("video.public-hero-video");
  await expect(video).toBeVisible();

  await video.evaluate((element) => {
    const htmlVideo = element as HTMLVideoElement;
    Object.defineProperty(htmlVideo, "duration", {
      configurable: true,
      value: 40,
    });
    Object.defineProperty(htmlVideo, "currentTime", {
      configurable: true,
      value: 0,
    });
    htmlVideo.dispatchEvent(new Event("play"));
    Object.defineProperty(htmlVideo, "currentTime", {
      configurable: true,
      value: 20,
    });
    htmlVideo.dispatchEvent(new Event("timeupdate"));
    Object.defineProperty(htmlVideo, "currentTime", {
      configurable: true,
      value: 39,
    });
    htmlVideo.dispatchEvent(new Event("timeupdate"));
  });

  await expect
    .poll(() => events)
    .toEqual(
      expect.arrayContaining([
        "VIDEO_PLAY",
        "VIDEO_PROGRESS_25",
        "VIDEO_PROGRESS_50",
        "VIDEO_COMPLETED",
      ]),
    );
});
