import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1400 } });
  page.setDefaultTimeout(60_000);
  await page.goto("http://191.252.181.168:5173/planning/8", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  await page.getByRole("button", { name: "Editar plano" }).click();
  console.log(
    JSON.stringify(
      {
        buttons: await page.getByRole("button").allInnerTexts(),
        labels: await page.locator("label").allInnerTexts(),
        inputs: await page.locator("input,select,textarea").evaluateAll((elements) =>
          elements.map((element) => ({
            tag: element.tagName,
            id: element.id,
            name: element.getAttribute("name"),
            type: element.getAttribute("type"),
            value: element.value,
            disabled: element.disabled,
          })),
        ),
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
