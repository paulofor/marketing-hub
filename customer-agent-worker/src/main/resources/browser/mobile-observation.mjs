import { chromium } from "playwright-core";
import fs from "node:fs/promises";
import path from "node:path";

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
const input = JSON.parse(await fs.readFile(inputPath, "utf8"));
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox", "--disable-dev-shm-usage"],
});
const context = await browser.newContext({
  viewport: { width: 393, height: 852 },
  deviceScaleFactor: 3,
  isMobile: true,
  hasTouch: true,
  userAgent:
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
});

await fs.mkdir(evidenceDirectory, { recursive: true });
const allowedHosts = new Set(input.urls.map((value) => new URL(value).host));
const isPrivateHost = (hostname) =>
  hostname === "localhost"
  || hostname === "0.0.0.0"
  || hostname === "::1"
  || /^127\./.test(hostname)
  || /^10\./.test(hostname)
  || /^192\.168\./.test(hostname)
  || /^169\.254\./.test(hostname)
  || /^172\.(1[6-9]|2\d|3[01])\./.test(hostname);
await context.route("**/*", async (route) => {
  const url = new URL(route.request().url());
  const safeProtocol = ["http:", "https:", "data:", "blob:"].includes(url.protocol);
  const authorizedNavigation = !route.request().isNavigationRequest() || allowedHosts.has(url.host);
  const publicDestination = url.protocol === "data:" || url.protocol === "blob:" || !isPrivateHost(url.hostname);
  if (safeProtocol && authorizedNavigation && publicDestination) {
    await route.continue();
  } else {
    await route.abort("blockedbyclient");
  }
});

const pages = [];
try {
  for (let index = 0; index < input.urls.length; index += 1) {
    const requestedUrl = input.urls[index];
    const page = await context.newPage();
    const startedAt = new Date().toISOString();
    const response = await page.goto(requestedUrl, { waitUntil: "domcontentloaded", timeout: 45_000 });
    await page.waitForTimeout(1_500);
    const videos = await page.locator("video").count();
    let videoPlayback = null;
    if (videos > 0) {
      videoPlayback = await page.locator("video").first().evaluate(async (video) => {
        try {
          video.muted = true;
          await video.play();
          await new Promise((resolve) => setTimeout(resolve, 1200));
          return {
            played: !video.paused && video.currentTime > 0,
            currentTime: video.currentTime,
            duration: Number.isFinite(video.duration) ? video.duration : null,
            hasAudio: Boolean(video.mozHasAudio || video.webkitAudioDecodedByteCount > 0 || video.audioTracks?.length),
          };
        } catch (error) {
          return { played: false, error: String(error) };
        }
      });
    }
    const screenshot = path.join(evidenceDirectory, `page-${index + 1}.png`);
    await page.screenshot({ path: screenshot, fullPage: true });
    pages.push({
      requestedUrl,
      finalUrl: page.url(),
      observedAt: startedAt,
      status: response?.status() ?? null,
      title: await page.title(),
      viewport: await page.evaluate(() => ({ width: innerWidth, height: innerHeight, scrollWidth: document.documentElement.scrollWidth })),
      headings: await page.locator("h1, h2").allTextContents(),
      visibleCtas: await page.locator("a, button").evaluateAll((elements) =>
        elements.filter((element) => element.checkVisibility()).map((element) => element.textContent?.trim()).filter(Boolean).slice(0, 30)),
      formCount: await page.locator("form").count(),
      videoCount: videos,
      videoPlayback,
      screenshot,
    });
    await page.close();
  }
} finally {
  await browser.close();
}

await fs.writeFile(outputPath, JSON.stringify({ deviceProfile: "IPHONE_15_PRO", pages }), "utf8");
