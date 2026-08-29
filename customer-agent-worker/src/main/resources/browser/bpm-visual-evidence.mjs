import { chromium } from "playwright-core";
import { lookup } from "node:dns/promises";
import fs from "node:fs/promises";
import path from "node:path";

const [inputPath, outputPath, evidenceDirectory] = process.argv.slice(2);
const input = JSON.parse(await fs.readFile(inputPath, "utf8"));
const profile = {
  key: "IPHONE_15_PRO",
  viewport: { width: 393, height: 852 },
  deviceScaleFactor: 3,
  userAgent:
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
};
const maxFolds = 60;
const allowPrivateTestHosts =
  process.env.CUSTOMER_AGENT_VISUAL_TEST_MODE === "true";
const publicHostCache = new Map();
const sensitiveQueryParameters = new Set([
  "accesstoken",
  "apikey",
  "authorization",
  "credential",
  "idtoken",
  "jwt",
  "password",
  "refreshtoken",
  "secret",
  "session",
  "signature",
  "token",
]);

const hasSensitiveQuery = (url) =>
  [...url.searchParams.keys()].some((name) =>
    sensitiveQueryParameters.has(
      name.replace(/[^A-Za-z0-9]/g, "").toLowerCase(),
    ),
  );

const isPrivateAddress = (value) => {
  const address = value.replace(/^\[|\]$/g, "").toLowerCase();
  if (address.includes(":")) {
    return (
      address === "::" ||
      address === "::1" ||
      /^(fc|fd)/.test(address) ||
      /^fe[89ab]/.test(address) ||
      /^ff/.test(address) ||
      /^::ffff:(127\.|10\.|192\.168\.|169\.254\.|172\.(1[6-9]|2\d|3[01])\.)/.test(
        address,
      )
    );
  }
  const firstOctet = Number(address.split(".")[0]);
  return (
    address === "localhost" ||
    /^0\./.test(address) ||
    /^127\./.test(address) ||
    /^10\./.test(address) ||
    /^192\.168\./.test(address) ||
    /^169\.254\./.test(address) ||
    /^172\.(1[6-9]|2\d|3[01])\./.test(address) ||
    firstOctet >= 224
  );
};

const isPublicHost = async (hostname) => {
  if (allowPrivateTestHosts) return true;
  const normalized = hostname.replace(/^\[|\]$/g, "").toLowerCase();
  if (publicHostCache.has(normalized)) return publicHostCache.get(normalized);
  const check = (async () => {
    if (!normalized || isPrivateAddress(normalized)) return false;
    try {
      const addresses = await lookup(normalized, { all: true, verbatim: true });
      return (
        addresses.length > 0 &&
        addresses.every((entry) => !isPrivateAddress(entry.address))
      );
    } catch {
      return false;
    }
  })();
  publicHostCache.set(normalized, check);
  return check;
};

const requestedUrl = new URL(input.sourceUrl);
if (
  !["http:", "https:"].includes(requestedUrl.protocol) ||
  requestedUrl.username ||
  requestedUrl.password ||
  hasSensitiveQuery(requestedUrl) ||
  !(await isPublicHost(requestedUrl.hostname))
) {
  throw new Error("URL pública inválida para a prova visual de Psique.");
}

await fs.mkdir(evidenceDirectory, { recursive: true });
const browser = await chromium.launch({
  ...(process.env.CHROMIUM_BIN
    ? { executablePath: process.env.CHROMIUM_BIN }
    : {}),
  headless: true,
  args: ["--no-sandbox", "--disable-dev-shm-usage"],
});
const context = await browser.newContext({
  viewport: profile.viewport,
  deviceScaleFactor: profile.deviceScaleFactor,
  isMobile: true,
  hasTouch: true,
  userAgent: profile.userAgent,
  reducedMotion: "reduce",
});

const allowedNavigationHosts = new Set([requestedUrl.host]);
await context.route("**/*", async (route) => {
  const url = new URL(route.request().url());
  const safeProtocol = ["http:", "https:", "data:", "blob:"].includes(
    url.protocol,
  );
  const publicDestination =
    ["data:", "blob:"].includes(url.protocol) ||
    (await isPublicHost(url.hostname));
  const redirectedNavigation =
    route.request().isNavigationRequest() &&
    route.request().redirectedFrom() !== null;
  const authorizedNavigation =
    !route.request().isNavigationRequest() ||
    allowedNavigationHosts.has(url.host) ||
    redirectedNavigation;
  if (
    safeProtocol &&
    publicDestination &&
    authorizedNavigation &&
    !hasSensitiveQuery(url)
  ) {
    if (redirectedNavigation) allowedNavigationHosts.add(url.host);
    await route.continue();
  } else {
    await route.abort("blockedbyclient");
  }
});

const page = await context.newPage();
const artifacts = [];
try {
  const response = await page.goto(requestedUrl.toString(), {
    waitUntil: "domcontentloaded",
    timeout: 45_000,
  });
  if (!response || response.status() >= 400) {
    throw new Error(
      `Tela retornou HTTP ${response?.status() ?? "sem resposta"}.`,
    );
  }
  await page
    .waitForLoadState("networkidle", { timeout: 15_000 })
    .catch(() => {});
  await page.evaluate(async (foldLimit) => {
    const images = [...document.images];
    for (const image of images) image.loading = "eager";
    const scrollLimit = Math.min(
      document.documentElement.scrollHeight,
      innerHeight * foldLimit,
    );
    for (let y = 0; y < scrollLimit; y += innerHeight) {
      scrollTo(0, y);
      await new Promise((resolve) => setTimeout(resolve, 80));
    }
    await Promise.all(
      images.map(async (image) => {
        if (!image.complete) {
          await new Promise((resolve) => {
            const finish = () => resolve();
            image.addEventListener("load", finish, { once: true });
            image.addEventListener("error", finish, { once: true });
            setTimeout(finish, 8_000);
          });
        }
        if (typeof image.decode === "function" && image.naturalWidth > 0) {
          await image.decode().catch(() => {});
        }
      }),
    );
    if (document.fonts?.ready) await document.fonts.ready;
    scrollTo(0, 0);
  }, maxFolds);
  await page.waitForTimeout(300);

  const pageMetrics = await page.evaluate(() => ({
    width: innerWidth,
    height: innerHeight,
    pageHeight: Math.max(
      document.documentElement.scrollHeight,
      document.body?.scrollHeight ?? 0,
      innerHeight,
    ),
    scrollWidth: Math.max(
      document.documentElement.scrollWidth,
      document.body?.scrollWidth ?? 0,
    ),
  }));
  if (pageMetrics.scrollWidth > pageMetrics.width + 1) {
    throw new Error(
      `Tela possui overflow horizontal: ${pageMetrics.scrollWidth}px para ${pageMetrics.width}px.`,
    );
  }

  const maxScroll = Math.max(0, pageMetrics.pageHeight - pageMetrics.height);
  const positions = [];
  for (let y = 0; y <= maxScroll; y += pageMetrics.height) positions.push(y);
  if (positions.at(-1) !== maxScroll) positions.push(maxScroll);
  if (positions.length > maxFolds) {
    throw new Error(
      `Página excede o limite auditável de ${maxFolds} dobras mobile.`,
    );
  }

  const capturedAt = new Date().toISOString();
  const fullPagePath = path.resolve(
    evidenceDirectory,
    "page-1-iphone-15-pro-full-page.png",
  );
  await page.screenshot({
    path: fullPagePath,
    fullPage: true,
    animations: "disabled",
  });
  artifacts.push({
    captureSessionId: input.captureSessionId,
    evidenceKey: "page-1-iphone-15-pro-full-page",
    evidenceType: "FULL_PAGE",
    deviceProfile: profile.key,
    pageNumber: 1,
    foldNumber: null,
    viewportWidth: pageMetrics.width,
    viewportHeight: pageMetrics.height,
    pageHeightPx: pageMetrics.pageHeight,
    scrollY: 0,
    sourceUrl: requestedUrl.toString(),
    finalUrl: page.url(),
    capturedAt,
    localPath: fullPagePath,
  });

  for (let index = 0; index < positions.length; index += 1) {
    const scrollY = positions[index];
    await page.evaluate((position) => scrollTo(0, position), scrollY);
    await page.waitForTimeout(150);
    const foldNumber = index + 1;
    const foldPath = path.resolve(
      evidenceDirectory,
      `page-1-iphone-15-pro-fold-${foldNumber}.png`,
    );
    await page.screenshot({
      path: foldPath,
      fullPage: false,
      animations: "disabled",
    });
    artifacts.push({
      captureSessionId: input.captureSessionId,
      evidenceKey: `page-1-iphone-15-pro-fold-${foldNumber}`,
      evidenceType: "FOLD",
      deviceProfile: profile.key,
      pageNumber: 1,
      foldNumber,
      viewportWidth: pageMetrics.width,
      viewportHeight: pageMetrics.height,
      pageHeightPx: pageMetrics.pageHeight,
      scrollY,
      sourceUrl: requestedUrl.toString(),
      finalUrl: page.url(),
      capturedAt: new Date().toISOString(),
      localPath: foldPath,
    });
  }

  await fs.writeFile(
    outputPath,
    JSON.stringify({
      captureSessionId: input.captureSessionId,
      deviceProfile: profile.key,
      pages: [
        {
          pageNumber: 1,
          requestedUrl: requestedUrl.toString(),
          finalUrl: page.url(),
          status: response.status(),
          title: await page.title(),
          viewport: pageMetrics,
          headings: await page.locator("h1, h2").allTextContents(),
          visibleCtas: await page.locator("a, button").evaluateAll((elements) =>
            elements
              .filter((element) => element.checkVisibility())
              .map((element) => element.textContent?.trim())
              .filter(Boolean)
              .slice(0, 40),
          ),
        },
      ],
      artifacts,
    }),
    "utf8",
  );
} finally {
  await browser.close();
}
