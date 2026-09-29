import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  page.setDefaultTimeout(30_000);
  await page.goto("http://191.252.181.168:5173/niches/34#niche-targeting", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const card = page.locator(".card").filter({ has: page.getByRole("heading", { name: "Skin care", exact: true }) });
  const [response] = await Promise.all([
    page.waitForResponse((candidate) =>
      candidate.request().method() === "POST" &&
      candidate.url().includes("/api/targeting-elements/397/metaads/reprocess")),
    card.getByRole("button", { name: "Reprocessar na Meta" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Reprocessamento falhou: HTTP ${response.status()} ${body}`);
  console.log(JSON.stringify({ status: response.status(), body: JSON.parse(body) }, null, 2));
} finally {
  await browser.close();
}
