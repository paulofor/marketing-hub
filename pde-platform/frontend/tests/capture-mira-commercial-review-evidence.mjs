import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { chromium, devices } from "@playwright/test";

const productUrl =
  process.env.MIRA_COMMERCIAL_FRONTEND_URL ??
  "https://mira.digicomdigital.com.br";
const outputDir =
  process.env.MIRA_REVIEW_EVIDENCE_DIR ??
  join(process.cwd(), "test-results/mira-commercial-review-evidence");
const expectedPromise =
  "Organize os produtos que você já tem em uma rotina individualizada, clara e consultável, por R$ 49, com limites explícitos.";

await mkdir(outputDir, { recursive: true });
const browser = await chromium.launch({
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ??
    process.env.CHROMIUM_BIN ??
    "/usr/bin/chromium",
});
let commercialOffer = null;
const evidence = [];

try {
  for (const [name, device] of [
    ["desktop", devices["Desktop Chrome"]],
    ["iphone-15-pro", devices["iPhone 15 Pro"]],
    ["pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const offerResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/commercial-offer?slotCode=v1") &&
        response.status() === 200,
    );
    await page.goto(`${productUrl}/?mh_test=1`, {
      waitUntil: "domcontentloaded",
    });
    const offerResponse = await offerResponsePromise;
    const offer = await offerResponse.json();
    if (offer.experimentId !== 93 || offer.priceBrl !== 49) {
      throw new Error(
        "Oferta pública de Mira diverge do produto #10 e experimento #93.",
      );
    }
    if (offer.promise !== expectedPromise) {
      throw new Error(
        "Promessa pública de Mira ainda contém contrato divergente.",
      );
    }
    const checkoutCta = page.locator("[data-checkout-cta]");
    await checkoutCta.waitFor({ state: "visible" });
    await page
      .getByText(/Paulo Forestieri.*responsável comercial pela Mira/i)
      .waitFor({ state: "visible" });
    await page.getByText(/duas organizações incluídas por R\$ 49/i).waitFor({
      state: "visible",
    });
    await page
      .getByText(/duas organizações individualizadas no total/i)
      .waitFor({ state: "visible" });
    await page
      .getByText(/Cada organização concluída usa uma das duas tentativas/i)
      .waitFor({ state: "visible" });
    await page.evaluate(() => document.fonts.ready);

    const screenshotPath = join(outputDir, `mira-destination-${name}.png`);
    await page.screenshot({
      path: screenshotPath,
      fullPage: true,
      animations: "disabled",
    });
    const bytes = await readFile(screenshotPath);
    evidence.push({
      device: name,
      path: screenshotPath,
      sha256: createHash("sha256").update(bytes).digest("hex"),
      checkoutCta: await checkoutCta.innerText(),
      merchantDisclosureVisible: true,
    });
    if (name === "desktop") {
      commercialOffer = offer;
      await writeFile(
        join(outputDir, "mira-rendered.html"),
        await page.content(),
      );
    }
    await context.close();
  }
} finally {
  await browser.close();
}

if (!commercialOffer)
  throw new Error("Oferta comercial de Mira não foi capturada.");
await writeFile(
  join(outputDir, "commercial-offer.json"),
  `${JSON.stringify(commercialOffer, null, 2)}\n`,
);
await writeFile(
  join(outputDir, "capture-evidence.json"),
  `${JSON.stringify({ productUrl, evidence }, null, 2)}\n`,
);
process.stdout.write(
  `${JSON.stringify({ screenshots: evidence.length, offerCaptured: true })}\n`,
);
