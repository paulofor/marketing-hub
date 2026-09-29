import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;

const baseUrl = "http://191.252.181.168:5173";
const label = "Mira comercial · prova da aplicação paga v1";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  page.setDefaultTimeout(12_000);
  await page.goto(`${baseUrl}/planning/8`, {
    waitUntil: "networkidle",
    timeout: 30_000,
  });
  const section = page.locator("section").filter({
    has: page.getByRole("heading", { name: "Biblioteca de Imagens e Vídeos" }),
  });
  let card = section.locator(".border.rounded.p-2.h-100").filter({ hasText: label });
  let asset;
  if ((await card.count()) === 0) {
    const form = section
      .getByRole("button", { name: "Anexar ao kit" })
      .locator("xpath=ancestor::form");
    const inputs = form.locator("input.form-control");
    const selects = form.locator("select.form-select");
    await inputs.nth(0).fill(
      "https://mira.digicomdigital.com.br/media/mira-commercial-product-proof-v1.png",
    );
    await selects.nth(0).selectOption("IMAGE");
    await inputs.nth(1).fill(label);
    await selects.nth(1).selectOption("PRODUCT_PROOF");
    await inputs.nth(2).fill(
      "Código versionado no PR #5411; commit de09bc8; SHA-256 4ffda62d502d8ea644fa06a0cd6cf0768c39c4278b43bdcefa3665e9d578c2f3.",
    );
    await inputs.nth(3).fill(
      "Interface e conteúdo próprios do produto Mira/Marketing Hub, gerados por código versionado e sem material de terceiros; uso comercial autorizado para o produto #10 em anúncios, landing pages e redes sociais.",
    );
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/api/planning/commercial-plans/8/visual-assets") &&
        response.request().method() === "POST",
    );
    await form.getByRole("button", { name: "Anexar ao kit" }).click();
    const response = await responsePromise;
    if (!response.ok()) {
      throw new Error(`Cadastro da prova falhou: HTTP ${response.status()} ${await response.text()}`);
    }
    asset = await response.json();
    card = section.locator(".border.rounded.p-2.h-100").filter({ hasText: label });
    await card.waitFor({ state: "visible" });
  }
  const approve = card.getByRole("button", { name: "Aprovar", exact: true });
  if ((await approve.count()) > 0) {
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/visual-assets/") &&
        response.url().endsWith("/status") &&
        response.request().method() === "PATCH",
    );
    await approve.click();
    const response = await responsePromise;
    if (!response.ok()) {
      throw new Error(`Aprovação da prova falhou: HTTP ${response.status()} ${await response.text()}`);
    }
    asset = await response.json();
  }
  await card.getByText(/PRODUCT_PROOF.*APPROVED/).waitFor({ state: "visible" });
  await card.screenshot({ path: "/tmp/mira-proof-approved-card.png" });
  process.stdout.write(`${JSON.stringify({ asset, cardText: await card.innerText() }, null, 2)}\n`);
} finally {
  await browser.close();
}
