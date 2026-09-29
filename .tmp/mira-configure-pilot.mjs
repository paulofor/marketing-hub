import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1400 }, locale: "pt-BR" });
  page.setDefaultTimeout(30_000);
  page.on("dialog", async (dialog) => {
    const message = dialog.message();
    await dialog.dismiss();
    throw new Error(`A tela bloqueou a configuração: ${message}`);
  });
  await page.goto("http://191.252.181.168:5173/experiments/93/edit", { waitUntil: "networkidle", timeout: 60_000 });
  await page.locator("#mediaSpendLimit").fill("25");
  await page.locator("#dailyBudget").fill("25");
  await page.locator("#zeroPurchaseSpendLimit").fill("25");
  await page.locator("#purchaseStopCount").fill("1");
  await page.locator("#startDate").fill("2026-09-29");
  await page.locator("#endDate").fill("2026-09-30");
  const [response] = await Promise.all([
    page.waitForResponse((candidate) => candidate.request().method() === "PUT" && candidate.url().endsWith("/api/experiments/93")),
    page.getByRole("button", { name: "Salvar", exact: true }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Configuração falhou: HTTP ${response.status()} ${body}`);
  const payload = JSON.parse(body);
  console.log(JSON.stringify({
    id: payload.id,
    dailyBudget: payload.dailyBudget,
    mediaSpendLimit: payload.mediaSpendLimit,
    zeroResultSpendLimit: payload.zeroResultSpendLimit,
    zeroPurchaseSpendLimit: payload.zeroPurchaseSpendLimit,
    purchaseStopCount: payload.purchaseStopCount,
    startDate: payload.startDate,
    endDate: payload.endDate,
    status: payload.status,
  }, null, 2));
} finally {
  await browser.close();
}
