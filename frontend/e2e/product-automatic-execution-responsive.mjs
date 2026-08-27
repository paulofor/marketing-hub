import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:4173";
const profiles = [
  { name: "desktop", viewport: { width: 1440, height: 1000 } },
  { name: "iPhone 15 Pro", device: devices["iPhone 15 Pro"] },
  { name: "Pixel 7", device: devices["Pixel 7"] },
];

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  headless: true,
});

const results = [];
try {
  for (const profile of profiles) {
    const context = await browser.newContext(
      profile.device || { viewport: profile.viewport },
    );
    const page = await context.newPage();
    let vegaEnabled = true;
    const receivedBodies = [];

    await page.route("**/api/**", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (pathname === "/api/products/value-chain-positions") {
        await route.fulfill({ status: 200, json: [] });
        return;
      }
      if (pathname === "/api/products" && request.method() === "GET") {
        await route.fulfill({
          status: 200,
          json: [
            {
              id: 1,
              slug: "vega",
              name: "Vega",
              commercialStatus: "VALIDACAO_COMERCIAL",
              automaticExecutionEnabled: vegaEnabled,
              automaticExecutionStatus: vegaEnabled ? "PLAY" : "STOP",
            },
            {
              id: 2,
              slug: "rigel",
              name: "Rigel",
              commercialStatus: "COMUNICACAO_E_JORNADA",
              automaticExecutionEnabled: false,
              automaticExecutionStatus: "STOP",
            },
          ],
        });
        return;
      }
      if (
        pathname === "/api/products/1/automatic-execution" &&
        request.method() === "PUT"
      ) {
        receivedBodies.push(request.postDataJSON());
        vegaEnabled = false;
        await new Promise((resolve) => setTimeout(resolve, 250));
        await route.fulfill({
          status: 200,
          json: {
            productId: 1,
            automaticExecutionEnabled: false,
            automaticExecutionStatus: "STOP",
          },
        });
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });

    await page.goto(`${baseUrl}/products`, { waitUntil: "domcontentloaded" });
    const stopVega = page.getByRole("button", {
      name: "Parar execução automática de Vega",
    });
    const playRigel = page.getByRole("button", {
      name: "Ativar execução automática de Rigel",
    });
    await stopVega.waitFor();
    await playRigel.waitFor();

    await stopVega.click({ noWaitAfter: true });
    await page.waitForFunction(
      () =>
        document
          .querySelector(
            'button[aria-label="Parar execução automática de Vega"]',
          )
          ?.hasAttribute("disabled") === true,
    );
    await page
      .getByRole("button", {
        name: "Ativar execução automática de Vega",
      })
      .waitFor();

    if (
      receivedBodies.length !== 1 ||
      receivedBodies[0].automaticExecutionEnabled !== false
    ) {
      throw new Error(`${profile.name}: payload STOP incorreto ou duplicado`);
    }
    if (await playRigel.isDisabled()) {
      throw new Error(`${profile.name}: comando de Rigel foi afetado por Vega`);
    }
    const hasOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth,
    );
    if (hasOverflow) {
      throw new Error(`${profile.name}: tela apresentou overflow horizontal`);
    }

    results.push({ profile: profile.name, status: "passed" });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(JSON.stringify({ results }));
