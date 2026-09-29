import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, locale: "pt-BR" });
  await page.goto(
    "http://191.252.181.168:5173/products/10/value-chain-history/processes/95/activities?chainId=23&sourceReference=experiment%3A93",
    { waitUntil: "networkidle", timeout: 60_000 },
  );
  const button = page.getByRole("button", { name: "Retomar processo" });
  await button.waitFor({ state: "visible" });
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) => candidate.request().method() === "POST" && candidate.url().endsWith("/resume"),
      { timeout: 60_000 },
    ),
    button.click(),
  ]);
  const body = await response.text();
  if (!response.ok()) throw new Error(`Retomada falhou: HTTP ${response.status()} ${body}`);
  console.log(JSON.stringify({ status: response.status(), body: JSON.parse(body) }, null, 2));
} finally {
  await browser.close();
}
