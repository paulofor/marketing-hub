import assert from "node:assert/strict";
import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium, devices } = playwright;
const profiles = [
  { name: "desktop", options: { viewport: { width: 1440, height: 1100 } } },
  { name: "iphone-15-pro", options: devices["iPhone 15 Pro"] },
  { name: "pixel-7", options: devices["Pixel 7"] },
];
const qaUrl =
  "https://mira.digicomdigital.com.br/?mh_test=1&utm_source=codex_preflight&utm_medium=internal_qa&utm_campaign=mira_93_process5";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});
const results = [];

try {
  for (const profile of profiles) {
    const context = await browser.newContext({ ...profile.options, locale: "pt-BR" });
    const page = await context.newPage();
    page.setDefaultTimeout(45_000);
    const eventPayloads = [];
    const failedRequests = [];
    const consoleErrors = [];
    let offer;
    page.on("request", (request) => {
      if (
        request.method() === "POST" &&
        request.url().includes("/api/pde/access/events")
      ) {
        eventPayloads.push(request.postDataJSON());
      }
    });
    page.on("requestfailed", (request) => {
      if (!request.url().includes("favicon")) {
        failedRequests.push({ url: request.url(), error: request.failure()?.errorText });
      }
    });
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    page.on("response", async (response) => {
      if (response.url().includes("/commercial-offer?slotCode=v1") && response.ok()) {
        offer = await response.json();
      }
    });

    await page.goto(qaUrl, { waitUntil: "domcontentloaded", timeout: 60_000 });
    await page.getByRole("heading", { name: /Cuide de você com mais clareza/i }).waitFor();
    await page
      .getByRole("link", { name: /organizar.*R\$ 49/i })
      .first()
      .waitFor();
    await page
      .getByText(/Paulo Forestieri.*responsável comercial pela Mira/is)
      .waitFor();
    await page
      .getByRole("link", { name: /organizar.*R\$ 49/i })
      .first()
      .scrollIntoViewIfNeeded();
    await page.waitForTimeout(750);
    assert.match(await page.locator("body").innerText(), /duas organizações incluídas por R\$ 49/i);
    assert.match(
      await page.locator("body").innerText(),
      /Cada organização concluída usa uma das duas tentativas/i,
    );
    assert.match(
      await page.locator("body").innerText(),
      /Paulo Forestieri.*responsável comercial pela Mira/is,
    );

    const video = page.getByLabel("Demonstração de Mira");
    await video.waitFor();
    const currentVideo = await video.evaluate((element) => element.currentSrc);
    assert.match(currentVideo, /mira-commercial-demo-v3/);
    await video.dispatchEvent("play");
    await video.dispatchEvent("ended");

    const assets = [];
    for (const path of [
      "/media/mira-commercial-demo-v3-poster.jpg",
      "/media/mira-commercial-control-v4.png",
      "/media/mira-commercial-product-proof-v1.png",
      "/media/mira-commercial-demo-v3-hls/index.m3u8",
    ]) {
      const response = await context.request.get(
        `https://mira.digicomdigital.com.br${path}`,
      );
      assert.equal(response.ok(), true, `Ativo ${path} falhou em ${profile.name}.`);
      assets.push({
        path,
        status: response.status(),
        contentType: response.headers()["content-type"],
      });
    }
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    assert.ok(overflow <= 1, `Overflow horizontal de ${overflow}px em ${profile.name}.`);

    const checkoutHref = await page
      .getByRole("link", { name: /organizar.*R\$ 49/i })
      .first()
      .getAttribute("href");
    assert.ok(checkoutHref, `Checkout ausente em ${profile.name}.`);
    const checkout = new URL(checkoutHref);
    assert.equal(checkout.protocol, "https:");
    assert.match(checkout.hostname, /(mercadopago|mpago)/i);
    assert.equal(offer?.experimentId, 93);
    assert.equal(offer?.experienceVersion, "mira-commercial-v1");
    assert.equal(Number(offer?.priceBrl), 49);
    assert.equal(offer?.checkoutUrl, checkoutHref);

    for (const path of ["/terms", "/privacy", "/refund-policy"]) {
      const response = await context.request.get(
        `https://mira.digicomdigital.com.br${path}?mh_test=1`,
      );
      assert.equal(response.ok(), true, `${path} falhou em ${profile.name}.`);
    }

    await page.waitForTimeout(1500);
    const requiredEvents = ["PAGE_VIEW", "CTA_VIEWED", "VIDEO_PLAY", "VIDEO_COMPLETED"];
    const eventTypes = eventPayloads.map((event) => event.eventType);
    for (const eventType of requiredEvents) {
      assert.ok(eventTypes.includes(eventType), `${eventType} ausente em ${profile.name}.`);
    }
    assert.ok(
      eventPayloads.every(
        (event) =>
          event.source === "mh_test" &&
          event.metadata?.trafficQuality === "INTERNAL_QA" &&
          event.metadata?.mh_internal_test === true,
      ),
      `Evento sem segregação em ${profile.name}: ${JSON.stringify(eventPayloads)}`,
    );
    assert.deepEqual(failedRequests, []);
    assert.deepEqual(consoleErrors, []);

    const screenshot = `/tmp/mira-process5-${profile.name}.png`;
    await page.screenshot({ path: screenshot, fullPage: true });
    results.push({
      profile: profile.name,
      url: page.url(),
      checkoutHost: checkout.hostname,
      video: currentVideo,
      assets,
      eventTypes,
      overflow,
      screenshot,
    });
    await context.close();
  }
  console.log(JSON.stringify(results, null, 2));
} finally {
  await browser.close();
}
