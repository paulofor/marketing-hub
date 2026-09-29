import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1400 }, locale: "pt-BR" });
  await page.goto("http://191.252.181.168:5173/financial/plans?productId=10&environment=LIVE", { waitUntil: "networkidle", timeout: 60_000 });
  await page.getByRole("button", { name: /Criar (plano do produto|nova revisão)/ }).click();
  await page.waitForTimeout(3000);
  console.log(JSON.stringify({
    headings: await page.locator("h1,h2,h3,h4,h5,h6").allInnerTexts(),
    buttons: await page.getByRole("button").allInnerTexts(),
    labels: await page.locator("label").allInnerTexts(),
    inputs: await page.locator("input,select,textarea").evaluateAll((elements) => elements.map((e) => ({ tag: e.tagName, type: e.getAttribute("type"), id: e.id, name: e.getAttribute("name"), value: e.value, checked: e.checked, disabled: e.disabled, options: e.tagName === "SELECT" ? Array.from(e.options).map((option) => ({ value: option.value, text: option.textContent?.trim() })) : undefined }))),
    alerts: await page.locator('[role="alert"],.alert').allInnerTexts(),
    text: (await page.locator("main").first().innerText()).slice(-20_000),
  }));
} finally {
  await browser.close();
}
