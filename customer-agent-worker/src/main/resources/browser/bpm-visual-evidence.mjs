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

/** Recusa pixels obtidos em posição diferente da declarada na evidência. */
async function verifyCapturePosition(expectedY) {
  const observed = await page.evaluate(() => ({ x: scrollX, y: scrollY }));
  if (observed.x !== 0 || observed.y !== expectedY) {
    throw new Error(
      `Captura visual fora da posição: esperado x=0,y=${expectedY}; observado x=${observed.x},y=${observed.y}.`,
    );
  }
}

/** Posiciona sem herdar a animação de rolagem da página, preservando seu HTML e estilo. */
async function positionForCapture(expectedY) {
  await page.evaluate(
    (top) => scrollTo({ left: 0, top, behavior: "instant" }),
    expectedY,
  );
  await page.waitForTimeout(150);
  await verifyCapturePosition(expectedY);
}

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
      scrollTo({ left: 0, top: y, behavior: "instant" });
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
    scrollTo({ left: 0, top: 0, behavior: "instant" });
  }, maxFolds);
  await positionForCapture(0);

  const runtimeIdentity = await page.evaluate(async () => {
    const publicationSourceSha256 =
      document
        .querySelector('meta[name="mh-publication-source-sha256"]')
        ?.getAttribute("content") ?? null;
    const servedHtmlSha256 =
      document
        .querySelector('meta[name="mh-served-html-sha256"]')
        ?.getAttribute("content") ?? null;
    let value = {};
    try {
      const endpoint = new URL("/version-diagnostics.json", location.href);
      const response = await fetch(endpoint, { cache: "no-store" });
      const contentType = response.headers.get("content-type") ?? "";
      if (
        response.ok &&
        contentType.toLowerCase().includes("application/json")
      ) {
        value = await response.json();
      }
    } catch {
      // A identidade do HTML publicado continua verificável sem diagnóstico de imagem PDE.
    }
    return {
      version: value.version ?? null,
      experienceVersion: value.experienceVersion ?? null,
      frontendSourceSha256: value.frontendSourceSha256 ?? null,
      imageTag: value.imageTag ?? null,
      commitSha: value.commitSha ?? null,
      publicationSourceSha256,
      servedHtmlSha256,
    };
  });

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
  await verifyCapturePosition(0);
  await page.screenshot({
    path: fullPagePath,
    fullPage: true,
    scale: "css",
    animations: "disabled",
  });
  await verifyCapturePosition(0);
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
    await positionForCapture(scrollY);
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
    await verifyCapturePosition(scrollY);
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

  await positionForCapture(0);

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
          firstFoldCtas: await page
            .locator("a, button")
            .evaluateAll((elements) =>
              elements
                .filter((element) => {
                  if (!element.checkVisibility()) return false;
                  const bounds = element.getBoundingClientRect();
                  return bounds.top >= 0 && bounds.bottom <= innerHeight;
                })
                .map((element) => element.textContent?.trim())
                .filter(Boolean)
                .slice(0, 20),
            ),
          visibleText: await page.locator("body").innerText(),
          runtimeIdentity,
        },
      ],
      artifacts,
    }),
    "utf8",
  );
} finally {
  await browser.close();
}
