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
    if (request.url().includes("mira-commercial-demo-v2-hls")) {
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
          primaryCta: "Quero organizar minha rotina por R$ 49",
          experimentId: 93,
          experienceVersion: "mira-commercial-v1",
          layoutKey: "mira-routine-v1",
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
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    "content",
    "index, follow",
  );
  await expect(
    page.getByText(/duas organizações incluídas por R\$ 49/i),
  ).toBeVisible();
  await expect(
    page.getByText(/Paulo Forestieri.*responsável comercial pela Mira/i),
  ).toBeVisible();
  await expect(page.getByLabel("Demonstração de Mira")).toBeVisible();
  await expect
    .poll(() => mediaRequests.some((url) => url.endsWith("/index.m3u8")))
    .toBe(true);
  const hlsManifest = await page.request.get(
    "/media/mira-commercial-demo-v2-hls/index.m3u8",
  );
  expect(hlsManifest.ok()).toBe(true);
  expect(await hlsManifest.text()).toContain("#EXT-X-ENDLIST");
  const hlsSegment = await page.request.get(
    "/media/mira-commercial-demo-v2-hls/segment-000.ts",
  );
  expect(hlsSegment.ok()).toBe(true);
  const staticControl = await page.request.get(
    "/media/mira-commercial-control-v2.png",
  );
  expect(staticControl.ok()).toBe(true);
  expect(staticControl.headers()["content-type"]).toContain("image/png");
  await expect(
    page.getByRole("link", { name: /Quero organizar/i }),
  ).toHaveAttribute("href", "https://checkout.example/mira");
  await page.getByLabel("E-mail da compra").fill("teste+93@sandbox.local");
  await page
    .getByRole("button", { name: "Receber meu link de acesso" })
    .click();
  await expect(page.getByText(/Enviamos seu link seguro/i)).toBeVisible();
  await page.getByLabel("Demonstração de Mira").dispatchEvent("play");
  await page.getByLabel("Demonstração de Mira").dispatchEvent("ended");
  await page.getByRole("link", { name: /Quero organizar/i }).click();
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
    .toMatch(/mira-commercial-demo-v2[.]mp4$/);
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
    .toMatch(/mira-commercial-demo-v2-hls\/index[.]m3u8$/);

  await video.evaluate((element) =>
    element.dispatchEvent(new Event("error", { bubbles: false })),
  );

  await expect
    .poll(() =>
      video.evaluate((element: HTMLVideoElement) => element.currentSrc),
    )
    .toMatch(/mira-commercial-demo-v2[.]mp4$/);
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
  await expect(page.getByText(/garantia legal aplicável/i)).toBeVisible();
  await page.goto("/privacy");
  await expect(
    page.getByRole("heading", { name: "Privacidade" }),
  ).toBeVisible();
});
