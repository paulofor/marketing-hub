import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { expect, test } from "@playwright/test";

const productSlug = "kit-whatsapp-pronto";
const productContractV1 = JSON.parse(
  readFileSync(
    resolve(process.cwd(), "../contracts/kit-whatsapp-pronto-v1.json"),
    "utf8",
  ),
);
const productContractV2 = JSON.parse(
  readFileSync(
    resolve(
      process.cwd(),
      "../contracts/kit-whatsapp-pronto-commercial-v2.json",
    ),
    "utf8",
  ),
);
const productContract = { ...productContractV1, ...productContractV2 };
const privacyInventory = JSON.parse(
  readFileSync(
    resolve(
      process.cwd(),
      "../contracts/kit-whatsapp-privacy-inventory-v1.json",
    ),
    "utf8",
  ),
);
const productionLegalIdentity = JSON.parse(
  readFileSync(
    resolve(
      process.cwd(),
      "../contracts/kit-whatsapp-production-legal-identity-v1.json",
    ),
    "utf8",
  ),
);

const commercialOffer = {
  productSlug,
  experienceVersion: "kit-whatsapp-pronto-pde-v2",
  layoutKey: "assisted-service-v2",
  experimentId: 89,
  experimentStatus: "PLANNED",
  acquisitionChannel: "DIRECT_ONE_TO_ONE",
  pain: "Você perde oportunidades no WhatsApp porque improvisa respostas e follow-ups.",
  proof:
    "Veja uma resposta e duas perguntas personalizadas para um cenário real do seu atendimento.",
  promise: productContract.promise,
  primaryCta: "Quero meu atendimento sob medida",
  priceBrl: 349,
  checkoutUrl: "https://pay.example/kit-whatsapp",
  salesPageUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br",
  targetAudience: "Pequenos prestadores que atendem pelo WhatsApp",
  productFormat: "IMPLANTACAO_PERSONALIZADA",
  deliveryMode: "ASSISTIDA_MANUAL",
  valueUnit: "Respostas, perguntas e follow-ups prontos para revisar e usar",
  supplierDisplayName: productionLegalIdentity.supplierDisplayName,
  supplierRegistrationNumber:
    productionLegalIdentity.supplierRegistrationNumber,
  supportEmail: productionLegalIdentity.supportEmail,
  termsUrl: "/terms",
  privacyUrl: "/privacy",
  refundPolicyUrl: "/refund-policy",
};

test.beforeEach(async ({ context }) => {
  await context.route(`**/api/pde/products/${productSlug}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(productContract),
    });
  });
  await context.route(
    `**/api/pde/products/${productSlug}/commercial-offer`,
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(commercialOffer),
      });
    },
  );
});

test("segrega eventos de homologação mesmo no build público", async ({
  context,
  page,
}) => {
  const events: Array<{
    eventType: string;
    source: string;
    metadata?: Record<string, unknown>;
  }> = [];
  await context.route("**/api/pde/access/events", async (route) => {
    events.push(route.request().postDataJSON());
    await route.fulfill({ status: 204, body: "" });
  });

  await page.goto("/?mh_test=1");
  await page
    .getByTestId("commercial-offer")
    .getByRole("link", { name: "Quero meu atendimento sob medida" })
    .scrollIntoViewIfNeeded();
  await page.getByLabel("Qual serviço você oferece?").fill("manicure");
  await page.getByRole("button", { name: "Gerar minha amostra" }).click();
  const policyPagePromise = page.waitForEvent("popup");
  await page.getByRole("link", { name: "Termos", exact: true }).first().click();
  const policyPage = await policyPagePromise;
  await expect(
    policyPage.getByRole("heading", {
      name: "Termos da implantação personalizada",
    }),
  ).toBeVisible();

  await expect
    .poll(() => events.map((event) => event.eventType))
    .toEqual(
      expect.arrayContaining([
        "PAGE_VIEW",
        "CTA_VIEWED",
        "TASTING_STARTED",
        "VALUE_MOMENT",
        "PAYWALL_VIEWED",
      ]),
    );
  expect(events.every((event) => event.source === "mh_test")).toBeTruthy();
  const entitlementEvents = events.filter((event) =>
    ["TASTING_STARTED", "VALUE_MOMENT", "PAYWALL_VIEWED"].includes(
      event.eventType,
    ),
  );
  expect(entitlementEvents).toHaveLength(3);
  for (const event of entitlementEvents) {
    expect(event.metadata?.idempotencyKey).toMatch(/^[a-z0-9._:-]+$/);
  }
  expect(entitlementEvents[0].metadata?.idempotencyKey).toBe(
    "tasting-started:kit-whatsapp-tasting-v1",
  );
  expect(entitlementEvents[1].metadata?.idempotencyKey).toBe(
    entitlementEvents[2].metadata?.idempotencyKey,
  );
});

test("não envia analytics no preview administrativo", async ({
  context,
  page,
}) => {
  let analyticsRequests = 0;
  await context.route("**/api/pde/access/events", async (route) => {
    analyticsRequests += 1;
    await route.fulfill({ status: 204, body: "" });
  });

  await page.goto("/?mh_preview=qa&pde_analytics=off");
  await page.getByLabel("Qual serviço você oferece?").fill("manicure");
  await page.getByRole("button", { name: "Gerar minha amostra" }).click();
  await expect(page.getByTestId("assisted-tasting-result")).toBeVisible();
  const policyPagePromise = page.waitForEvent("popup");
  await page.getByRole("link", { name: "Termos", exact: true }).first().click();
  const policyPage = await policyPagePromise;
  await expect(
    policyPage.getByRole("heading", {
      name: "Termos da implantação personalizada",
    }),
  ).toBeVisible();
  await page.waitForTimeout(300);

  expect(analyticsRequests).toBe(0);
});

test("bloqueia checkout quando oferta e experiência usam versões diferentes", async ({
  context,
  page,
}) => {
  await context.route("**/api/pde/access/events", async (route) => {
    await route.fulfill({ status: 204, body: "" });
  });
  await context.route(
    `**/api/pde/products/${productSlug}/commercial-offer`,
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...commercialOffer,
          experienceVersion: "kit-whatsapp-pronto-pde-v1",
        }),
      });
    },
  );

  await page.goto("/?mh_test=1");

  await expect(page.getByTestId("commercial-offer")).toHaveCount(0);
  await expect(page.getByText(/checkout foi bloqueado/i)).toBeVisible();
});

test("bloqueia checkout quando a promessa não pertence à experiência publicada", async ({
  context,
  page,
}) => {
  await context.route("**/api/pde/access/events", async (route) => {
    await route.fulfill({ status: 204, body: "" });
  });
  await context.route(
    `**/api/pde/products/${productSlug}/commercial-offer`,
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ...commercialOffer,
          promise: "Promessa antiga de outra versão",
        }),
      });
    },
  );

  await page.goto("/?mh_test=1");

  await expect(page.getByTestId("commercial-offer")).toHaveCount(0);
  await expect(page.getByText(/checkout foi bloqueado/i)).toBeVisible();
});

test("preserva a origem humana na navegação pública normal", async ({
  context,
  page,
}) => {
  const sources: string[] = [];
  await context.route("**/api/pde/access/events", async (route) => {
    sources.push(route.request().postDataJSON().source);
    await route.fulfill({ status: 204, body: "" });
  });

  await page.goto("/");
  await expect.poll(() => sources).toContain("pde-assisted-service");
  expect(sources.every((source) => source === "pde-assisted-service")).toBe(
    true,
  );
});

test("declara toda telemetria persistida, retenção e identidade legal produtiva", async ({
  context,
  page,
}) => {
  const events: Array<{
    eventType: string;
    pageUrl: string;
    metadata: Record<string, unknown>;
  }> = [];
  await context.route("**/api/pde/access/events", async (route) => {
    events.push(route.request().postDataJSON());
    await route.fulfill({ status: 204, body: "" });
  });

  await page.goto("/privacy?mh_test=1");
  await expect(
    page.getByRole("heading", { name: "Privacidade e uso de dados" }),
  ).toBeVisible();
  for (const disclosure of privacyInventory.publicDisclosureTerms) {
    await expect(page.getByText(disclosure, { exact: false })).toBeVisible();
  }
  await expect(
    page.getByText(productionLegalIdentity.supplierDisplayName),
  ).toBeVisible();
  await expect(
    page.getByText(
      `CNPJ ${productionLegalIdentity.supplierRegistrationNumber}`,
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: productionLegalIdentity.supportEmail }),
  ).toBeVisible();

  await expect
    .poll(() => events.find((event) => event.eventType === "PAGE_VIEW"))
    .toBeTruthy();
  const pageView = events.find((event) => event.eventType === "PAGE_VIEW");
  expect(Object.keys(pageView?.metadata ?? {}).sort()).toEqual(
    [...privacyInventory.browserEventMetadataFields].sort(),
  );
  expect(pageView?.metadata).not.toHaveProperty("userAgent");
  expect(pageView?.metadata).not.toHaveProperty("clientIp");
  expect(pageView?.pageUrl).toContain("/privacy");
  expect(pageView?.pageUrl).not.toContain("accessToken");

  const accessServiceSource = readFileSync(
    resolve(
      process.cwd(),
      "../backend/src/main/java/com/marketinghub/pde/service/AccessService.java",
    ),
    "utf8",
  );
  const retentionServiceSource = readFileSync(
    resolve(
      process.cwd(),
      "../backend/src/main/java/com/marketinghub/pde/service/PdeFunnelTelemetryRetentionService.java",
    ),
    "utf8",
  );
  const persistedTelemetrySource = `${accessServiceSource}\n${retentionServiceSource}`;
  for (const column of privacyInventory.persistedTechnicalColumns) {
    expect(persistedTelemetrySource).toContain(column);
  }
  expect(retentionServiceSource).toContain("anonymizeExpiredDetailedTelemetry");
  expect(privacyInventory.retention.detailedTelemetryDays).toBe(180);

  expect(productionLegalIdentity.supplierRegistrationNumber).toBe(
    "25.215.414/0001-69",
  );
  expect(productionLegalIdentity.supportEmail).toBe(
    "contato@digicomdigital.com.br",
  );
  for (const legalUrl of [
    productionLegalIdentity.termsUrl,
    productionLegalIdentity.privacyUrl,
    productionLegalIdentity.refundPolicyUrl,
  ]) {
    expect(new URL(legalUrl).origin).toBe(
      "https://kit-whatsapp-pronto.digicomdigital.com.br",
    );
  }
});
