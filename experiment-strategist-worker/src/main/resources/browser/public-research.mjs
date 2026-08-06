import { chromium } from "playwright-core";

const urls = JSON.parse(process.argv[2] ?? "[]");
if (!Array.isArray(urls) || urls.length === 0 || urls.length > 10) {
  throw new Error("Informe entre 1 e 10 URLs públicas em um array JSON.");
}

const privateHost = (host) =>
  host === "localhost" || host === "0.0.0.0" || host === "::1" ||
  /^127\./.test(host) || /^10\./.test(host) || /^192\.168\./.test(host) ||
  /^169\.254\./.test(host) || /^172\.(1[6-9]|2\d|3[01])\./.test(host);
const targets = urls.map((value) => new URL(value));
for (const target of targets) {
  if (!['http:', 'https:'].includes(target.protocol) || privateHost(target.hostname)) {
    throw new Error(`Destino não público bloqueado: ${target.hostname}`);
  }
}

const browser = await chromium.launch({headless: true, args: ["--no-sandbox", "--disable-dev-shm-usage"]});
const context = await browser.newContext({viewport: {width: 393, height: 852}, isMobile: true, hasTouch: true});
const results = [];
try {
  for (const target of targets) {
    const page = await context.newPage();
    const response = await page.goto(target.href, {waitUntil: "domcontentloaded", timeout: 45000});
    results.push({
      requestedUrl: target.href,
      finalUrl: page.url(),
      accessedAt: new Date().toISOString(),
      status: response?.status() ?? null,
      title: await page.title(),
      headings: (await page.locator("h1, h2").allTextContents()).slice(0, 20),
      visibleCtas: await page.locator("a, button").evaluateAll((items) => items.filter((item) => item.checkVisibility()).map((item) => item.textContent?.trim()).filter(Boolean).slice(0, 30)),
      textExcerpt: (await page.locator("body").innerText()).replace(/\s+/g, " ").slice(0, 6000)
    });
    await page.close();
  }
} finally {
  await browser.close();
}
process.stdout.write(JSON.stringify({device: "MOBILE_393X852", pages: results}));
