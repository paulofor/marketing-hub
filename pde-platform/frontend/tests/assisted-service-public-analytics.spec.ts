import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { expect, test } from "@playwright/test";

const productSlug = "kit-whatsapp-pronto";
const productContract = JSON.parse(
  readFileSync(
    resolve(process.cwd(), "../contracts/kit-whatsapp-pronto-v1.json"),
    "utf8",
  ),
);

const commercialOffer = {
  productSlug,
  experimentId: 89,
  experimentStatus: "PLANNED",
  acquisitionChannel: "DIRECT_ONE_TO_ONE",
  pain: "Você perde oportunidades no WhatsApp porque improvisa respostas e follow-ups.",
  proof:
    "Veja uma resposta e duas perguntas personalizadas para um cenário real do seu atendimento.",
  promise:
    "Em até 48 horas, receba seu atendimento no WhatsApp sob medida e revisado por uma pessoa.",
  primaryCta: "Quero meu atendimento sob medida",
  priceBrl: 349,
  checkoutUrl: "https://pay.example/kit-whatsapp",
  salesPageUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br",
  targetAudience: "Pequenos prestadores que atendem pelo WhatsApp",
  productFormat: "IMPLANTACAO_PERSONALIZADA",
  deliveryMode: "ASSISTIDA_MANUAL",
  valueUnit: "Respostas, perguntas e follow-ups prontos para revisar e usar",
  supplierLegalName: "Fornecedor de Homologação Ltda.",
  supplierRegistrationNumber: "00.000.000/0001-00",
  supplierAddress: "Endereço de homologação, 100",
  supportEmail: "teste@sandbox.local",
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
  const events: Array<{ eventType: string; source: string }> = [];
  await context.route("**/api/pde/access/events", async (route) => {
    events.push(route.request().postDataJSON());
    await route.fulfill({ status: 204, body: "" });
  });

  await page.goto("/?mh_test=1");
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
        "TASTING_STARTED",
        "VALUE_MOMENT",
        "PAYWALL_VIEWED",
      ]),
    );
  expect(events.every((event) => event.source === "mh_test")).toBeTruthy();
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
