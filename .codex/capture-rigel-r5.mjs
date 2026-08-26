import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("@playwright/test");

const html = readFileSync(".codex/attachments/rigel-r5.html", "utf8");
const checkout =
  "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081";
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_BIN });
const results = [];

const scenarios = [
  { name: "desktop", context: { viewport: { width: 1440, height: 1000 } } },
  { name: "iphone15pro", context: devices["iPhone 15 Pro"] },
  { name: "pixel7", context: devices["Pixel 7"] },
];

for (const scenario of scenarios) {
  const context = await browser.newContext(scenario.context);
  const page = await context.newPage();
  const errors = [];
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  page.on("pageerror", (error) => errors.push(error.message));
  await page.setContent(html, { waitUntil: "networkidle", timeout: 45_000 });
  await page.waitForFunction(
    () => [...document.images].every((image) => image.complete && image.naturalWidth > 0),
    undefined,
    { timeout: 45_000 },
  );
  const audit = await page.evaluate((canonicalCheckout) => {
    const checkoutLinks = [...document.querySelectorAll("a")].filter(
      (anchor) => anchor.href === canonicalCheckout,
    );
    return {
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      imageCount: document.images.length,
      brokenImages: [...document.images]
        .filter((image) => image.naturalWidth === 0)
        .map((image) => image.src),
      checkoutCount: checkoutLinks.length,
      checkoutTargets: [...new Set(checkoutLinks.map((anchor) => anchor.href))],
      primaryCheckoutIdCount: document.querySelectorAll("#checkout-cta-primary").length,
      analyticsCheckoutCount: document.querySelectorAll(
        '[data-analytics-role="primary-checkout"]',
      ).length,
      placeholderLinks: [...document.querySelectorAll('a[href="#"]')].length,
      scripts: document.scripts.length,
      forms: document.forms.length,
      h1Count: document.querySelectorAll("h1").length,
      policyLinks: [...document.querySelectorAll(".footer-links a")].map(
        (anchor) => ({ href: anchor.href, target: anchor.target }),
      ),
    };
  }, checkout);
  if (
    audit.documentWidth > audit.viewportWidth ||
    audit.imageCount !== 4 ||
    audit.brokenImages.length > 0 ||
    audit.checkoutCount !== 3 ||
    audit.checkoutTargets.length !== 1 ||
    audit.primaryCheckoutIdCount !== 1 ||
    audit.analyticsCheckoutCount !== 2 ||
    audit.placeholderLinks !== 0 ||
    audit.scripts !== 0 ||
    audit.forms !== 0 ||
    audit.h1Count !== 1 ||
    audit.policyLinks.length !== 3 ||
    audit.policyLinks.some((link) => link.target !== "_blank") ||
    errors.length > 0
  ) {
    throw new Error(`${scenario.name} falhou: ${JSON.stringify({ audit, errors })}`);
  }
  await page.screenshot({
    path: `.codex/attachments/rigel-r5-${scenario.name}-full.jpg`,
    fullPage: true,
    quality: 88,
    type: "jpeg",
  });
  if (scenario.name !== "pixel7") {
    await page.locator(".gallery").screenshot({
      path: `.codex/attachments/rigel-r5-${scenario.name}-proof.jpg`,
      quality: 92,
      type: "jpeg",
    });
  }
  results.push({ device: scenario.name, ...audit, consoleErrors: errors });
  await context.close();
}

await browser.close();
writeFileSync(
  ".codex/attachments/rigel-r5-browser-audit.json",
  `${JSON.stringify(results, null, 2)}\n`,
);
console.log(JSON.stringify(results));
