import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const tabs = ["video", "creatives", "publico", "construction", "execucao", "process"];
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1100 }, locale: "pt-BR" });
  for (const tab of tabs) {
    const page = await context.newPage();
    await page.goto(`http://191.252.181.168:5173/experiments/93?tab=${tab}`, {
      waitUntil: "networkidle",
      timeout: 60_000,
    });
    console.log(
      JSON.stringify({
        tab,
        headings: await page.locator("h1,h2,h3,h4,h5,h6").allInnerTexts(),
        buttons: await page.getByRole("button").allInnerTexts(),
        text: (await page.locator("main").innerText()).slice(0, 18_000),
      }),
    );
    await page.close();
  }
} finally {
  await browser.close();
}
