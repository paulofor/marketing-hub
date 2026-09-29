import { mkdir } from "node:fs/promises";
import playwright from "../pde-platform/frontend/node_modules/@playwright/test/index.js";

const { chromium, devices } = playwright;

const output = ".tmp/mira-v8-visual";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ executablePath: "/usr/bin/chromium" });

try {
  for (const [name, device] of [
    ["desktop", devices["Desktop Chrome"]],
    ["iphone-15-pro", { ...devices["iPhone 15 Pro"], browserName: "chromium" }],
    ["pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    await page.route("**/api/pde/products/**/commercial-offer?slotCode=v1", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          checkoutUrl: "https://checkout.example/mira",
          priceBrl: 49,
          primaryCta: "Organizar minha rotina por R$ 49",
          experimentId: 93,
          experienceVersion: "mira-commercial-v1",
          layoutKey: "mira-routine-v1",
        }),
      }),
    );
    await page.route("**/api/pde/access/events", (route) =>
      route.fulfill({ status: 201, contentType: "application/json", body: "{}" }),
    );
    await page.goto("http://127.0.0.1:57181/?mh_test=1", {
      waitUntil: "networkidle",
    });
    await page.screenshot({
      path: `${output}/${name}-first-fold.png`,
      animations: "disabled",
    });
    await page.screenshot({
      path: `${output}/${name}-full.png`,
      fullPage: true,
      animations: "disabled",
    });
    await context.close();
  }
} finally {
  await browser.close();
}
