import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(30_000);
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=publico", { waitUntil: "networkidle", timeout: 60_000 });
  const checkbox = page.locator("#INTEREST\\:\\:397");
  await checkbox.check();
  const [response] = await Promise.all([
    page.waitForResponse((candidate) => candidate.request().method() === "PUT" && candidate.url().includes("/api/experiments/93/targeting-selections")),
    page.getByRole("button", { name: "Salvar público" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Salvamento falhou: HTTP ${response.status()} ${body}`);
  console.log(JSON.stringify(JSON.parse(body), null, 2));
} finally {
  await browser.close();
}
