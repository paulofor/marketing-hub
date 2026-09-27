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
    page.getByLabel("Demonstração de Mira").locator("source"),
  ).toHaveAttribute("src", "/media/mira-commercial-demo-v1.mp4");
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
