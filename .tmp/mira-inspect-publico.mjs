import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=publico", { waitUntil: "networkidle", timeout: 60_000 });
  console.log(JSON.stringify({
    buttons: await page.getByRole("button").allInnerTexts(),
    labels: await page.locator("label").allInnerTexts(),
    inputs: await page.locator("input,select,textarea").evaluateAll((elements) => elements.map((e) => ({ tag: e.tagName, type: e.getAttribute("type"), id: e.id, name: e.getAttribute("name"), value: e.value, checked: e.checked, disabled: e.disabled, ariaLabel: e.getAttribute("aria-label") }))),
    text: (await page.locator("main").innerText()).slice(-12_000),
  }));
} finally {
  await browser.close();
}
