import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
  const network = [];
  const consoleMessages = [];
  page.on("response", (response) => {
    if (response.url().includes("/api/") || response.status() >= 400) {
      network.push({ url: response.url(), status: response.status() });
    }
  });
  page.on("requestfailed", (request) =>
    network.push({ url: request.url(), failed: request.failure()?.errorText }),
  );
  page.on("console", (message) =>
    consoleMessages.push({ type: message.type(), text: message.text() }),
  );
  await page.goto("https://mira.digicomdigital.com.br/?mh_test=1", {
    waitUntil: "domcontentloaded",
    timeout: 60_000,
  });
  await page.waitForTimeout(10_000);
  const directOffer = await page.evaluate(async () => {
    const response = await fetch(
      "/api/pde/products/pde-planejado-36/commercial-offer?slotCode=v1",
    );
    return { status: response.status, body: await response.text() };
  });
  console.log(
    JSON.stringify(
      {
        url: page.url(),
        network,
        consoleMessages,
        directOffer,
        links: await page.locator("a").evaluateAll((elements) =>
          elements.map((element) => ({ text: element.textContent?.trim(), href: element.href })),
        ),
        body: (await page.locator("body").innerText()).slice(0, 20_000),
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
