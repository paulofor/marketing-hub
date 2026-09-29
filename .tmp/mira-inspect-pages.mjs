import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const baseUrl = "http://191.252.181.168:5173";
const pages = [
  {
    name: "process4",
    path: "/products/10/value-chain-history/processes/95/activities?chainId=23&sourceReference=experiment%3A93",
  },
  { name: "audience", path: "/niches/34#niche-targeting" },
  {
    name: "process5",
    path: "/products/10/value-chain-history/processes/96/activities?chainId=23&sourceReference=experiment%3A93",
  },
  {
    name: "process5-safira",
    path: "/products/10/value-chain-history/processes/98/activities?chainId=23&sourceReference=experiment%3A93",
  },
];

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1100 } });
  for (const target of pages) {
    const page = await context.newPage();
    page.setDefaultTimeout(15_000);
    await page.goto(`${baseUrl}${target.path}`, {
      waitUntil: "networkidle",
      timeout: 45_000,
    });
    const visible = async (selector) =>
      page
        .locator(selector)
        .filter({ visible: true })
        .allInnerTexts()
        .catch(() => []);
    const data = {
      name: target.name,
      url: page.url(),
      title: await page.title(),
      headings: await visible("h1, h2, h3, h4, h5, h6"),
      buttons: await visible("button"),
      labels: await visible("label"),
      alerts: await visible('[role="alert"], .alert'),
      text: (await page.locator("main").innerText().catch(() => page.locator("body").innerText())).slice(
        0,
        15_000,
      ),
    };
    await page.screenshot({ path: `/tmp/mira-inspect-${target.name}.png`, fullPage: true });
    process.stdout.write(`${JSON.stringify(data)}\n`);
    await page.close();
  }
} finally {
  await browser.close();
}
