const assert = require("node:assert/strict");
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl =
  process.env.DIRECT_RECRUITMENT_UI_URL || "http://127.0.0.1:4173";
const token = "11111111-2222-4333-8444-555555555555";
const pagePath = `/participar/${token}?utm_source=instagram&utm_medium=organic&utm_campaign=rigel-pilot`;

function publicCampaign(acceptingSubmissions = true) {
  return {
    token,
    experimentId: 89,
    status: acceptingSubmissions ? "ACTIVE" : "COMPLETED",
    acceptingSubmissions,
    productName: "Kit WhatsApp Pronto",
    headline: "Seu atendimento no WhatsApp poderia vender mais?",
    bodyText: "Participe de uma validação rápida e consentida.",
    audienceSummary: "Pequenos prestadores de serviços.",
    consentText: "Aceito participar e conhecer a oferta.",
    consentVersion: "consent-v1",
    privacyPolicyUrl: "https://rigel.example/privacidade",
    targetContacts: 15,
    remainingContacts: acceptingSubmissions ? 1 : 0,
    availabilityMessage: acceptingSubmissions
      ? "A validação está aberta e possui uma vaga."
      : "A validação atingiu a amostra planejada.",
  };
}

async function validateProfile(browser, name, contextOptions) {
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  let acceptingSubmissions = true;
  let submittedPayload;
  let visits = 0;

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (
      request.method() === "GET" &&
      pathname === `/api/public/direct-recruitments/${token}`
    ) {
      await route.fulfill({ json: publicCampaign(acceptingSubmissions) });
      return;
    }
    if (
      request.method() === "POST" &&
      pathname === `/api/public/direct-recruitments/${token}/visits`
    ) {
      visits += 1;
      const payload = request.postDataJSON();
      assert.match(payload.visitorFingerprint, /^[0-9a-f]{64}$/);
      assert.equal(payload.utmSource, "instagram");
      await route.fulfill({ json: { counted: visits === 1, uniqueVisits: 1 } });
      return;
    }
    if (
      request.method() === "POST" &&
      pathname === `/api/public/direct-recruitments/${token}/submissions`
    ) {
      submittedPayload = request.postDataJSON();
      acceptingSubmissions = false;
      await route.fulfill({
        json: {
          submissionId: 34,
          status: "QUALIFIED",
          qualified: true,
          message:
            "Seu perfil é aderente. A apresentação da oferta já está disponível.",
          offerUrl: "https://rigel.example",
          remainingContacts: 0,
          sampleComplete: true,
        },
      });
      return;
    }
    await route.fulfill({
      status: 404,
      json: { message: "Rota não simulada" },
    });
  });

  await page.goto(`${baseUrl}${pagePath}`, { waitUntil: "networkidle" });
  await expect(
    page.getByRole("heading", {
      name: "Seu atendimento no WhatsApp poderia vender mais?",
    }),
  ).toBeVisible();
  await expect(page.locator(".app-shell")).toHaveCount(0);
  await expect(page.getByRole("link", { name: /Conhecer/ })).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "política de privacidade" }),
  ).toHaveAttribute("href", "https://rigel.example/privacidade");

  await page
    .getByLabel("Em qual tipo de serviço você atua?")
    .selectOption("CONSULTING");
  await page
    .getByLabel("Quantas conversas com clientes você costuma ter por semana?")
    .selectOption("ELEVEN_TO_THIRTY");
  await page.getByLabel("Usa WhatsApp para atender?").selectOption("true");
  await page.getByLabel("Decide sobre esse atendimento?").selectOption("true");
  await page
    .getByLabel("Quer uma implantação personalizada?")
    .selectOption("true");
  const rawContact = `teste+recrutamento-${name.toLowerCase().replace(/\s+/g, "-")}@sandbox.local`;
  await page.getByLabel("Seu WhatsApp ou e-mail").fill(rawContact);
  await page.getByLabel(/Aceito participar e conhecer a oferta/).check();
  await page
    .getByRole("button", { name: "Quero participar e conhecer a solução" })
    .click();

  const offer = page.getByRole("link", {
    name: "Conhecer Kit WhatsApp Pronto",
  });
  await expect(offer).toBeVisible();
  await expect(offer).toHaveAttribute("href", "https://rigel.example");
  await expect(page.getByText("Participação encerrada")).toHaveCount(0);
  assert.ok(submittedPayload, `${name}: adesão não foi enviada`);
  assert.match(submittedPayload.contactFingerprint, /^[0-9a-f]{64}$/);
  assert.match(
    submittedPayload.submissionKey,
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
  );
  assert.equal(submittedPayload.utmSource, "instagram");
  assert.equal(submittedPayload.utmMedium, "organic");
  assert.equal(submittedPayload.utmCampaign, "rigel-pilot");
  assert.equal(JSON.stringify(submittedPayload).includes(rawContact), false);
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
    true,
    `${name}: a página criou overflow horizontal`,
  );

  await context.close();
  process.stdout.write(
    `[UI] ${name}: qualificação, privacidade, oferta final e responsividade validadas\n`,
  );
}

async function main() {
  const browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || "/usr/bin/chromium",
  });
  try {
    await validateProfile(browser, "Desktop", {
      viewport: { width: 1440, height: 1000 },
    });
    await validateProfile(browser, "iPhone 15 Pro", devices["iPhone 15 Pro"]);
    await validateProfile(browser, "Pixel 7", devices["Pixel 7"]);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
