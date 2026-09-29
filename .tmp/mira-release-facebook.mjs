import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(60_000);
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=execucao", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const button = page.getByRole("button", { name: "Liberar para Facebook Ads Worker" });
  await button.scrollIntoViewIfNeeded();
  if (await button.isDisabled()) throw new Error("A liberação continua bloqueada pelos gates.");
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" &&
        candidate.url().endsWith("/api/experiments/93/facebook-release"),
    ),
    button.click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Liberação falhou: HTTP ${response.status()} ${body}`);
  console.log(JSON.stringify({ status: response.status(), experiment: JSON.parse(body) }, null, 2));
} finally {
  await browser.close();
}
