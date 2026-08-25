import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { chromium, devices } from "@playwright/test";

const productUrl =
  process.env.PDE_ASSISTED_FRONTEND_URL ??
  "http://pde-platform-frontend-kit-validation";
const outputDir = join(process.cwd(), "test-results/rigel-review-evidence");
await mkdir(outputDir, { recursive: true });

const browser = await chromium.launch({ args: ["--no-sandbox"] });
let commercialOffer = null;
try {
  for (const [name, device] of [
    ["desktop", devices["Desktop Chrome"]],
    ["iphone-15-pro", devices["iPhone 15 Pro"]],
    ["pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    await page.goto(`${productUrl}/?mh_test=1`, { waitUntil: "networkidle" });
    await page
      .getByRole("heading", {
        name: "Retome conversas no WhatsApp sem improvisar a próxima mensagem",
      })
      .waitFor();
    await page.screenshot({
      path: join(outputDir, `rigel-${name}.png`),
      fullPage: true,
    });
    if (name === "desktop") {
      await writeFile(
        join(outputDir, "rigel-rendered.html"),
        await page.content(),
      );
      commercialOffer = await page.evaluate(async () => {
        const response = await fetch(
          "/api/pde/products/kit-whatsapp-pronto/commercial-offer",
        );
        if (!response.ok) throw new Error("Oferta comercial indisponível");
        return response.json();
      });
    }
    await context.close();
  }
} finally {
  await browser.close();
}

if (!commercialOffer) throw new Error("Oferta comercial não foi capturada");
await writeFile(
  join(outputDir, "commercial-offer.json"),
  `${JSON.stringify(commercialOffer, null, 2)}\n`,
);
console.log(JSON.stringify({ screenshots: 3, offerCaptured: true }));
