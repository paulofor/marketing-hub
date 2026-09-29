import { expect, test } from "@playwright/test";

const productSlug = "pde-planejado-36";
const experienceVersion = "mira-commercial-v1";
const backendBaseUrl =
  process.env.MIRA_COMMERCIAL_BACKEND_URL ??
  "http://pde-platform-backend-mira-commercial-validation:8096";
const mailBaseUrl =
  process.env.MIRA_COMMERCIAL_MAIL_URL ?? "http://sandbox-mail:8025";
const paymentsBaseUrl =
  process.env.MIRA_COMMERCIAL_PAYMENTS_URL ??
  "http://lead-portal-payments-mira-commercial-validation:8080";
const internalToken =
  process.env.MIRA_COMMERCIAL_INTERNAL_TOKEN ?? "pde-local-internal-test";
const paymentToken =
  process.env.MIRA_COMMERCIAL_PAYMENT_TOKEN ?? "pde-local-payment-test";

const internalHeaders = { "X-PDE-Internal-Token": internalToken };
const paymentHeaders = {
  Authorization: `Bearer ${paymentToken}`,
  "Content-Type": "application/json",
};

function paymentPayload(
  email: string,
  paymentId: string,
  paymentStatus: "approved" | "refunded",
  amount = 49,
) {
  return {
    paymentId,
    paymentStatus,
    amount,
    currency: "BRL",
    buyerEmail: email,
    externalReference: productSlug,
    dateApproved: "2026-09-27T12:00:00Z",
    metadata: {
      experimentId: 93,
      productId: 10,
      productKey: productSlug,
    },
  };
}

test.beforeEach(async ({ request }) => {
  const response = await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
    { headers: internalHeaders },
  );
  expect(response.ok()).toBeTruthy();
});

test("expõe desejo, demonstração, preço, checkout e políticas sem iniciar mídia", async ({
  page,
}) => {
  const events: Array<Record<string, unknown>> = [];
  page.on("request", (request) => {
    if (
      request.method() === "POST" &&
      request.url().includes("/api/pde/access/events")
    ) {
      events.push(request.postDataJSON() as Record<string, unknown>);
    }
  });
  await page.route("https://checkout.example/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html; charset=utf-8",
      body: "<h1>Checkout local de Mira</h1><p>R$ 49</p>",
    });
  });

  await page.goto("/?mh_test=1");

  await expect(
    page.getByRole("heading", { name: /Cuide de você com mais clareza/i }),
  ).toBeVisible();
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
  const productProof = page.getByRole("img", {
    name: "Interface real de Mira com uma rotina organizada",
  });
  await expect(productProof).toBeVisible();
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
  const checkout = page.getByRole("link", { name: /R\$ 49/i });
  await expect(checkout).toHaveAttribute(
    "href",
    "https://checkout.example/mira",
  );
  await page.getByLabel("Demonstração de Mira").dispatchEvent("play");
  await page.getByLabel("Demonstração de Mira").dispatchEvent("ended");
  await checkout.click();
  await expect(
    page.getByRole("heading", { name: "Checkout local de Mira" }),
  ).toBeVisible();
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

  for (const path of ["/terms", "/privacy", "/refund-policy"]) {
    await page.goto(path);
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.locator("body")).not.toContainText(
      /homologação interna/i,
    );
  }
});

test("rejeita acesso sem compra e contrato financeiro divergente", async ({
  request,
}, testInfo) => {
  const email = `teste+mira-gate-${testInfo.project.name}@sandbox.local`;
  const login = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    {
      data: { productSlug, email, experienceVersion },
    },
  );
  expect(login.status()).toBe(404);

  const unauthorized = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    {
      data: paymentPayload(
        email,
        `mp-unauthorized-${testInfo.project.name}`,
        "approved",
      ),
    },
  );
  expect(unauthorized.status()).toBe(401);

  const wrongAmount = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    {
      headers: paymentHeaders,
      data: paymentPayload(
        email,
        `mp-wrong-${testInfo.project.name}`,
        "approved",
        48,
      ),
    },
  );
  expect(wrongAmount.status()).toBe(404);

  const paymentsHealth = await request.get(
    `${paymentsBaseUrl}/actuator/health`,
  );
  expect(paymentsHealth.ok()).toBeTruthy();
  await expect(paymentsHealth.json()).resolves.toMatchObject({ status: "UP" });
});

test("conclui compra, e-mail, rotina, retomada, limites, métricas e reembolso", async ({
  page,
  request,
}, testInfo) => {
  const suffix = `${testInfo.project.name}-${Date.now()}`;
  const email = `teste+mira-${suffix}@sandbox.local`;
  const paymentId = `mp-mira-${suffix}`;
  const approvedPayload = paymentPayload(email, paymentId, "approved");

  const approved = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    { headers: paymentHeaders, data: approvedPayload },
  );
  expect(approved.status()).toBe(202);
  await expect(approved.json()).resolves.toMatchObject({
    transactionId: paymentId,
    paymentStatus: "approved",
    result: "RECORDED",
  });
  const approvedRetry = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    { headers: paymentHeaders, data: approvedPayload },
  );
  expect(approvedRetry.status()).toBe(202);
  await expect(approvedRetry.json()).resolves.toMatchObject({
    result: "DUPLICATE_OR_UPDATED",
  });

  await page.goto("/?mh_test=1");
  await page.getByLabel("E-mail da compra").fill(email);
  await page
    .getByRole("button", { name: "Receber meu link de acesso" })
    .click();
  await expect(page.getByText(/Enviamos seu link seguro/i)).toBeVisible();
  const loginResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    { data: { productSlug, email, experienceVersion } },
  );
  expect(loginResponse.ok()).toBeTruthy();
  const login = (await loginResponse.json()) as {
    accessUrl: string;
    deliveryStatus: string;
  };
  expect(login.deliveryStatus).toBe("SENT");
  expect(login.accessUrl).toMatch(/^\/access#access=[a-z0-9-]+$/);
  const accessToken = login.accessUrl.split("#access=")[1];

  await expect
    .poll(async () => {
      const response = await request.get(`${mailBaseUrl}/api/v1/messages`);
      const mailbox = await response.json();
      return mailbox.messages.some((message: { To?: { Address?: string }[] }) =>
        message.To?.some((recipient) => recipient.Address === email),
      );
    })
    .toBeTruthy();

  await page.goto(`/access?mh_test=1#access=${accessToken}`);
  await expect(page).toHaveURL(/\/access$/);
  expect(page.url()).not.toContain(accessToken);
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
    "content",
    "noindex, nofollow",
  );
  await expect(page.getByText("Mira · aplicação web")).toBeVisible();
  await expect(
    page.getByText("2 rotinas individualizadas incluídas"),
  ).toBeVisible();
  const products = page.locator("fieldset");
  await products.nth(0).locator("input").fill("Sabonete suave");
  await products.nth(0).locator("textarea").fill("Limpar e enxaguar");
  await products.nth(1).locator("input").fill("Hidratante diário");
  await products.nth(1).locator("textarea").fill("Aplicar após a limpeza");
  await page.getByRole("button", { name: "Organizar minha rotina" }).click();
  await expect(page.getByText("Sua rotina está pronta")).toBeVisible();
  await expect(
    page.getByText(/não é diagnóstico nem prescrição/i),
  ).toBeVisible();
  await expect(page.locator(".mira-routine-list li").first()).toContainText(
    "Sabonete suave",
  );
  await expect(
    page.getByText("1 rotina individualizada disponível"),
  ).toBeVisible();

  await page.reload();
  await expect(page.getByText("Sua rotina está pronta")).toBeVisible();
  await expect(page.getByText("Hidratante diário")).toBeVisible();
  await page
    .getByRole("button", { name: "Organizar outra combinação" })
    .click();
  await page
    .getByLabel("O que você quer facilitar?")
    .fill("Diagnosticar e tratar manchas");
  await page.getByRole("button", { name: "Organizar minha rotina" }).click();
  await expect(page.getByRole("alert")).toContainText(/conclusão clínica/i);
  await expect(
    page.getByText("1 rotina individualizada disponível"),
  ).toBeVisible();

  await page
    .getByLabel("O que você quer facilitar?")
    .fill("Organizar minha rotina noturna");
  await page.getByRole("button", { name: "Organizar minha rotina" }).click();
  await expect(
    page.getByText("0 rotinas individualizadas disponíveis"),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Organizar outra combinação" }),
  ).toHaveCount(0);

  const thirdAttempt = await request.put(
    `${backendBaseUrl}/api/pde/mira/commercial/v1/input`,
    {
      headers: { "X-PDE-Access-Token": accessToken },
      data: {
        objective: "Terceira organização",
        products: [{ name: "Produto", labelDirections: "Limpar e enxaguar" }],
      },
    },
  );
  expect(thirdAttempt.ok()).toBeFalsy();

  await page.getByRole("button", { name: "Já usei esta rotina" }).click();
  await page
    .getByRole("button", { name: "Concluir minha primeira aplicação" })
    .click();
  await expect(page.getByText(/Primeira aplicação concluída/i)).toBeVisible();

  const privacy = await request.post(
    `${backendBaseUrl}/api/pde/mira/commercial/v1/privacy`,
    {
      headers: { "X-PDE-Access-Token": accessToken },
      data: { action: "ACCESS" },
    },
  );
  expect(privacy.ok()).toBeTruthy();
  await expect(privacy.json()).resolves.toHaveProperty("data.miraCommercial");

  const qaSummary = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary?includeNonHumanTraffic=true&experienceVersion=${experienceVersion}`,
    { headers: internalHeaders },
  );
  expect(qaSummary.ok()).toBeTruthy();
  await expect(qaSummary.json()).resolves.toMatchObject({
    productSlug,
    currentExperienceVersion: experienceVersion,
    humanSessions: 0,
  });
  expect((await qaSummary.json()).internalQaSessions).toBeGreaterThan(0);

  const refundedPayload = paymentPayload(email, paymentId, "refunded");
  const refunded = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    { headers: paymentHeaders, data: refundedPayload },
  );
  expect(refunded.status()).toBe(202);
  const refundedRetry = await request.post(
    `${backendBaseUrl}/api/internal/pde/mercado-pago/entitlements`,
    { headers: paymentHeaders, data: refundedPayload },
  );
  expect(refundedRetry.status()).toBe(202);

  const revokedSession = await request.get(
    `${backendBaseUrl}/api/pde/mira/commercial/v1/session`,
    { headers: { "X-PDE-Access-Token": accessToken } },
  );
  expect(revokedSession.status()).toBe(403);
});
