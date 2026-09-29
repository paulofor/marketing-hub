import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const path = process.argv[2];
if (!path?.startsWith("/")) throw new Error("Informe um path interno.");
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, locale: "pt-BR" });
  await page.goto(`http://191.252.181.168:5173${path}`, { waitUntil: "networkidle", timeout: 60_000 });
  console.log(JSON.stringify({
    path,
    url: page.url(),
    headings: await page.locator("h1,h2,h3,h4,h5,h6").allInnerTexts(),
    buttons: await page.getByRole("button").allInnerTexts(),
    links: await page.getByRole("link").evaluateAll((links) => links.map((link) => ({ text: link.textContent?.trim(), href: link.getAttribute("href") }))),
    labels: await page.locator("label").allInnerTexts(),
    inputs: await page.locator("input,select,textarea").evaluateAll((elements) => elements.map((e) => ({ tag: e.tagName, type: e.getAttribute("type"), id: e.id, name: e.getAttribute("name"), value: e.value, checked: e.checked, disabled: e.disabled, placeholder: e.getAttribute("placeholder"), options: e.tagName === "SELECT" ? Array.from(e.options).map((option) => ({ value: option.value, text: option.textContent?.trim() })) : undefined }))),
    alerts: await page.locator('[role="alert"],.alert').allInnerTexts(),
    text: (await page.locator("main").first().innerText()).slice(0, 40_000),
  }));
} finally {
  await browser.close();
}
