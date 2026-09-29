import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1400 }, locale: "pt-BR" });
  await page.goto("http://191.252.181.168:5173/experiments/93/edit", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  console.log(JSON.stringify({
    url: page.url(),
    headings: await page.locator("h1,h2,h3,h4,h5,h6").allInnerTexts(),
    buttons: await page.getByRole("button").allInnerTexts(),
    labels: await page.locator("label").allInnerTexts(),
    inputs: await page.locator("input,select,textarea").evaluateAll((elements) => elements.map((element) => ({
      tag: element.tagName,
      type: element.getAttribute("type"),
      name: element.getAttribute("name"),
      id: element.id,
      value: element.value,
      checked: element.checked,
      disabled: element.disabled,
    }))),
    text: (await page.locator("main").innerText()).slice(0, 30_000),
  }));
} finally {
  await browser.close();
}
