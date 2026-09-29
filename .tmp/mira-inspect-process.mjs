import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const processId = Number(process.argv[2] ?? 96);
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  await page.goto(`http://191.252.181.168:5173/products/10/value-chain-history/processes/${processId}/activities?chainId=23&sourceReference=experiment%3A93`, { waitUntil: "networkidle", timeout: 60_000 });
  console.log(JSON.stringify({
    processId,
    headings: await page.locator("h1,h2,h3,h4,h5,h6").allInnerTexts(),
    buttons: await page.getByRole("button").allInnerTexts(),
    links: await page.getByRole("link").evaluateAll((links) => links.map((link) => ({ text: link.textContent?.trim(), href: link.getAttribute("href") }))),
    alerts: await page.locator('[role="alert"],.alert').allInnerTexts(),
    text: (await page.locator("main").innerText()).slice(0, 35_000),
  }));
} finally {
  await browser.close();
}
