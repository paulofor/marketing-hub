import { expect, test } from "@playwright/test";

const readySession = {
  experienceVersion: "mira-commercial-v1",
  status: "READY",
  objective: "Organizar os cuidados que já tenho",
  products: [
    { name: "Sabonete", labelDirections: "Limpar e enxaguar" },
    { name: "Hidratante", labelDirections: "Aplicar após a limpeza" },
  ],
  routine: [
    {
      productName: "Sabonete",
      order: 10,
      documentedDirection: "Limpar e enxaguar",
      safetyNote:
        "Ordem limitada ao texto documentado informado; não é prescrição.",
    },
    {
      productName: "Hidratante",
      order: 20,
      documentedDirection: "Aplicar após a limpeza",
      safetyNote:
        "Ordem limitada ao texto documentado informado; não é prescrição.",
    },
  ],
  blocker: null,
  attemptsUsed: 1,
  attemptsLimit: 2,
  events: ["VALUE_MOMENT", "READY_RESULT_USED"],
  generatedAt: "2026-09-26T10:00:00Z",
  completedAt: null,
};

test("mostra valor, preço e primeiro passo antes do compromisso", async ({
  page,
}) => {
  const mediaRequests: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("mira-commercial-demo-v3-hls")) {
      mediaRequests.push(request.url());
    }
  });
  const events: Array<{
    eventType: string;
    source: string;
    metadata: Record<string, unknown>;
  }> = [];
  await page.route("**/api/pde/access/events", async (route) => {
    events.push(route.request().postDataJSON());
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ status: "RECORDED" }),
    });
  });
  await page.route(
    "**/api/pde/products/pde-planejado-36/commercial-offer?slotCode=v1",
    async (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          checkoutUrl: "https://checkout.example/mira",
          priceBrl: 49,
          primaryCta: "Organizar minha rotina por R$ 49",
          experimentId: 93,
          experienceVersion: "mira-commercial-v1",
          layoutKey: "mira-routine-v1",
          facebookPixelId: "pixel-mira",
        }),
      }),
  );
  await page.route("https://checkout.example/**", async (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<p>checkout</p>",
    }),
  );
  await page.route("**/api/pde/access/login-link", async (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );

  await page.goto("/?mh_test=1");

  await expect(
    page.getByRole("heading", { name: /Cuide de você com mais clareza/i }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => (window as Window & { fbq?: unknown }).fbq === undefined,
    ),
  ).toBe(true);
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    "content",
    "index, follow",
  );
  await expect(
    page.getByText(/duas organizações incluídas por R\$ 49/i),
  ).toBeVisible();
  await expect(
    page.getByText(/duas organizações individualizadas no total/i),
  ).toBeVisible();
  await expect(
    page.getByText(/Cada organização concluída usa uma das duas tentativas/i),
  ).toBeVisible();
  await expect(
    page.getByText(/Paulo Forestieri.*responsável comercial pela Mira/i),
  ).toBeVisible();
  const purchaseProtection = page.getByLabel("Proteção da compra Mira");
  await expect(purchaseProtection).toContainText(
    "R$ 49 uma vez, sem assinatura",
  );
  await expect(purchaseProtection).toContainText(
    "Acesso: após a aprovação do pagamento",
  );
  await expect(purchaseProtection).toContainText(
    "Entrega: na hora, na sua área segura",
  );
  await expect(purchaseProtection).toContainText(
    "Reembolso integral: peça em até 7 dias corridos",
  );
  await expect(purchaseProtection).toContainText(
    "Suporte: disponível por 30 dias",
  );
  await expect(purchaseProtection).toContainText(
    "inclusive quando o método não tiver Compra Garantida do Mercado Pago",
  );
  await expect(purchaseProtection).toContainText(
    "solicitar acesso, correção ou exclusão dos dados",
  );
  const productProofImage = page.getByRole("img", {
    name: "Interface real de Mira com uma rotina organizada",
  });
  await expect(productProofImage).toBeVisible();
  await expect(page.getByLabel("Demonstração de Mira")).toBeVisible();
  expect(
    await page.locator("[data-product-proof]").evaluate((proof) => {
      const video = document.querySelector("video");
      return Boolean(
        video &&
        proof.compareDocumentPosition(video) & Node.DOCUMENT_POSITION_FOLLOWING,
      );
    }),
  ).toBe(true);
  await expect
    .poll(() => mediaRequests.some((url) => url.endsWith("/index.m3u8")))
    .toBe(true);
  const hlsManifest = await page.request.get(
    "/media/mira-commercial-demo-v3-hls/index.m3u8",
  );
  expect(hlsManifest.ok()).toBe(true);
  expect(await hlsManifest.text()).toContain("#EXT-X-ENDLIST");
  const hlsSegment = await page.request.get(
    "/media/mira-commercial-demo-v3-hls/segment-000.ts",
  );
  expect(hlsSegment.ok()).toBe(true);
  const staticControl = await page.request.get(
    "/media/mira-commercial-control-v4.png",
  );
  expect(staticControl.ok()).toBe(true);
  expect(staticControl.headers()["content-type"]).toContain("image/png");
  const productProof = await page.request.get(
    "/media/mira-commercial-product-proof-v1.png",
  );
  expect(productProof.ok()).toBe(true);
  expect(productProof.headers()["content-type"]).toContain("image/png");
  const checkout = page.getByRole("link", {
    name: "Organizar minha rotina por R$ 49",
  });
  await expect(checkout).toHaveAttribute(
    "href",
    "https://checkout.example/mira",
  );
  const checkoutBounds = await checkout.boundingBox();
  const viewport = page.viewportSize();
  expect(checkoutBounds).not.toBeNull();
  expect(viewport).not.toBeNull();
  expect(checkoutBounds!.y).toBeGreaterThanOrEqual(0);
  expect(checkoutBounds!.y + checkoutBounds!.height).toBeLessThanOrEqual(
    viewport!.height,
  );
  await page.getByLabel("E-mail da compra").fill("teste+93@sandbox.local");
  await page
    .getByRole("button", { name: "Receber meu link de acesso" })
    .click();
  await expect(page.getByText(/Enviamos seu link seguro/i)).toBeVisible();
  await page.getByLabel("Demonstração de Mira").dispatchEvent("play");
  await page.getByLabel("Demonstração de Mira").dispatchEvent("ended");
  await checkout.evaluate((element) =>
    element.addEventListener("click", (event) => event.preventDefault(), {
      once: true,
    }),
  );
  await checkout.click();
  await expect
    .poll(() => events.map((event) => event.eventType))
    .toEqual(
      expect.arrayContaining([
        "PAGE_VIEW",
        "CTA_VIEWED",
        "VIDEO_PLAY",
        "VIDEO_COMPLETED",
        "CHECKOUT_STARTED",
      ]),
    );
  expect(events.every((event) => event.source === "mh_test")).toBe(true);
  expect(
    events.every((event) => event.metadata.trafficQuality === "INTERNAL_QA"),
  ).toBe(true);
  expect(
    await page.evaluate(
      () => (window as Window & { fbq?: unknown }).fbq === undefined,
    ),
  ).toBe(true);
});

test("envia PageView e InitiateCheckout ao pixel somente no tráfego comercial", async ({
  page,
}) => {
  await page.route("**/api/pde/access/events", async (route) =>
    route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ status: "RECORDED" }),
    }),
  );
  await page.route(
    "**/api/pde/products/pde-planejado-36/commercial-offer?slotCode=v1",
    async (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          checkoutUrl: "https://checkout.example/mira",
          priceBrl: 49,
          primaryCta: "Organizar minha rotina por R$ 49",
          experimentId: 93,
          experienceVersion: "mira-commercial-v1",
          layoutKey: "mira-routine-v1",
          facebookPixelId: "pixel-mira",
        }),
      }),
  );
  await page.route("https://connect.facebook.net/**", async (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/javascript",
      body: "window.__miraMetaPixelScriptLoaded = true;",
    }),
  );

  await page.goto("/");

  await expect
    .poll(() =>
      page.evaluate(() => {
        const fbq = (window as Window & { fbq?: { queue?: unknown[][] } }).fbq;
        return Boolean(
          fbq?.queue?.some(
            (command) => command[0] === "init" && command[1] === "pixel-mira",
          ) &&
            fbq.queue.some(
              (command) => command[0] === "track" && command[1] === "PageView",
            ),
        );
      }),
    )
    .toBe(true);
  const checkout = page.getByRole("link", {
    name: "Organizar minha rotina por R$ 49",
  });
  await checkout.evaluate((element) =>
    element.addEventListener("click", (event) => event.preventDefault(), {
      once: true,
    }),
  );
  await checkout.click();
  await expect
    .poll(() =>
      page.evaluate(() =>
        (window as Window & { fbq?: { queue?: unknown[][] } }).fbq?.queue?.some(
          (command) =>
            command[0] === "track" && command[1] === "InitiateCheckout",
        ),
      ),
    )
    .toBe(true);
});

test("usa o MP4 canônico quando o navegador não oferece HLS", async ({
  page,
}) => {
  await page.addInitScript(() => {
    ["MediaSource", "WebKitMediaSource", "SourceBuffer"].forEach((name) =>
      Object.defineProperty(window, name, {
        configurable: true,
        value: undefined,
      }),
    );
    const canPlayType = HTMLMediaElement.prototype.canPlayType;
    HTMLMediaElement.prototype.canPlayType = function (type) {
      return type === "application/vnd.apple.mpegurl"
        ? ""
        : canPlayType.call(this, type);
    };
  });
  await page.route(
    "**/api/pde/products/pde-planejado-36/commercial-offer?slotCode=v1",
    async (route) => route.fulfill({ status: 503 }),
  );

  await page.goto("/?mh_test=1");

  await expect
    .poll(() =>
      page
        .getByLabel("Demonstração de Mira")
        .evaluate((video: HTMLVideoElement) => video.currentSrc),
    )
    .toMatch(/mira-commercial-demo-v3[.]mp4$/);
});

test("troca para o MP4 canônico quando o HLS falha em reprodução", async ({
  page,
}) => {
  await page.route(
    "**/api/pde/products/pde-planejado-36/commercial-offer?slotCode=v1",
    async (route) => route.fulfill({ status: 503 }),
  );
  await page.goto("/?mh_test=1");
  const video = page.getByLabel("Demonstração de Mira");
  await expect
    .poll(() =>
      video.evaluate((element: HTMLVideoElement) => element.currentSrc),
    )
    .toMatch(/mira-commercial-demo-v3-hls\/index[.]m3u8$/);

  await video.evaluate((element) =>
    element.dispatchEvent(new Event("error", { bubbles: false })),
  );

  await expect
    .poll(() =>
      video.evaluate((element: HTMLVideoElement) => element.currentSrc),
    )
    .toMatch(/mira-commercial-demo-v3[.]mp4$/);
});

test("retoma a rotina paga sem expor o bearer na URL", async ({ page }) => {
  await page.route("**/api/pde/mira/commercial/v1/session", async (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(readySession),
    }),
  );
  await page.route("**/api/pde/mira/commercial/v1/events", async (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ...readySession,
        events: [...readySession.events, "FIRST_USE"],
      }),
    }),
  );

  await page.goto("/#access=segredo-de-teste");

  await expect(page).toHaveURL(/\/access$/);
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    "content",
    "noindex, nofollow",
  );
  await expect(page.getByText("Sabonete")).toBeVisible();
  await expect(page.getByText("Hidratante")).toBeVisible();
  await page.getByRole("button", { name: "Já usei esta rotina" }).click();
  await expect(
    page.getByRole("button", { name: /Concluir minha primeira aplicação/ }),
  ).toBeVisible();
  expect(await page.evaluate(() => window.location.href)).not.toContain(
    "segredo-de-teste",
  );
});

test("entrega políticas comerciais em rotas públicas", async ({ page }) => {
  await page.goto("/refund-policy");
  await expect(
    page.getByRole("heading", { name: "Política de reembolso" }),
  ).toBeVisible();
  await expect(
    page.getByText(/reembolso integral em até 7 dias/i),
  ).toBeVisible();
  await expect(
    page.getByText(
      /não estiver coberto pela Compra Garantida do Mercado Pago/i,
    ),
  ).toBeVisible();
  await page.goto("/privacy");
  await expect(
    page.getByRole("heading", { name: "Privacidade" }),
  ).toBeVisible();
  await expect(
    page.getByText(/solicitar acesso, correção, oposição ou exclusão/i),
  ).toBeVisible();
});
