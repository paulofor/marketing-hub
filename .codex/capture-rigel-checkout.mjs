import { writeFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium } = require("@playwright/test");
const offerResponse = await fetch(
  "http://191.252.181.168/api/products/public/kit-whatsapp-pronto/commercial-offer",
  { signal: AbortSignal.timeout(20_000) },
);
if (!offerResponse.ok) throw new Error(`Oferta indisponível: ${offerResponse.status}`);
const offer = await offerResponse.json();
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN });
const context = await browser.newContext({
  locale: "pt-BR",
  viewport: { width: 1440, height: 1000 },
});
const page = await context.newPage();
const errors = [];
page.on("pageerror", (error) => errors.push(error.message));
page.on("console", (message) => {
  if (message.type() === "error") errors.push(message.text());
});
const response = await page.goto(offer.checkoutUrl, {
  waitUntil: "domcontentloaded",
  timeout: 60_000,
});
await page.waitForTimeout(8_000);
const audit = await page.evaluate(() => ({
  title: document.title,
  visibleText: document.body?.innerText?.replace(/\s+/g, " ").trim().slice(0, 8_000),
  forms: document.forms.length,
  buttons: [...document.querySelectorAll("button")].map((button) =>
    button.textContent?.trim(),
  ),
}));
const result = {
  mode: "READ_ONLY_DIRECT_NAVIGATION_NO_CTA_NO_SUBMISSION",
  requestedUrl: offer.checkoutUrl,
  finalUrl: page.url(),
  httpStatus: response?.status() ?? null,
  offerExpectation: {
    productSlug: offer.productSlug,
    experimentId: offer.experimentId,
    priceBrl: offer.priceBrl,
    billingModel: "ONE_TIME",
    supplierLegalName: offer.supplierLegalName,
  },
  rendered: audit,
  consoleErrors: errors,
};
await page.screenshot({
  path: ".codex/attachments/rigel-checkout-read-only.jpg",
  fullPage: true,
  type: "jpeg",
  quality: 90,
});
writeFileSync(
  ".codex/attachments/rigel-checkout-read-only.json",
  `${JSON.stringify(result, null, 2)}\n`,
);
await context.close();
await browser.close();
console.log(
  JSON.stringify({
    mode: result.mode,
    finalUrl: result.finalUrl,
    httpStatus: result.httpStatus,
    title: result.rendered.title,
    visibleText: result.rendered.visibleText,
    consoleErrorCount: errors.length,
  }),
);
