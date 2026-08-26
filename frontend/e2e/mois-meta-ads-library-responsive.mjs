import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];
const now = "2026-08-26T15:00:00Z";
const investigation = {
  id: 7,
  workspaceId: "workspace-001",
  searchTerms: "treino entrevista emprego",
  countryCode: "BR",
  publisherPlatform: "INSTAGRAM",
  status: "ACTIVE_SUPERVISED",
  collection: {
    mode: "SUPERVISED",
    reason: "No Brasil, a observação comercial é supervisionada.",
    searchUrl:
      "https://www.facebook.com/ads/library/?active_status=active&ad_type=all&country=BR&media_type=all&q=treino+entrevista+emprego&search_type=keyword_unordered",
    nextObservationAt: now,
  },
  gateDecision: "INVESTIGAR",
  evidences: ["1 anúncio real observado no Instagram"],
  gaps: ["Reobservar o anúncio após 30 dias"],
  ethicalModeling: {
    pain: "Ainda não comprovada",
    audience: "Ainda não comprovado",
    mechanism: "Ainda não comprovado",
    offerStructure: "Ainda não comprovada",
    angles: [],
    patterns: [],
    prohibitedCopies: ["marca", "texto", "criativo"],
  },
  creativeBrief: {
    status: "UNAVAILABLE",
    title: "",
    originalHook: "",
    visualDirection: "",
    offerAngle: "",
    callToAction: "",
    sourceEvidences: [],
    confidence: "INSUFFICIENT",
    requiresAdSpecialistApproval: true,
  },
  adsObserved: 1,
  createdAt: now,
  updatedAt: now,
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [profileName, contextOptions] of profiles) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    let createPayload;
    let observationPayload;
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/*", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (!pathname.startsWith("/api/")) {
        await route.continue();
        return;
      }
      if (pathname === "/api/ops-monitor/v1/modules/availability") {
        await route.fulfill({ json: [] });
      } else if (pathname === "/api/creatives/video-review") {
        await route.fulfill({ json: [] });
      } else if (pathname === "/api/facebook/configuration-status") {
        await route.fulfill({ json: { configured: false } });
      } else if (
        pathname === "/api/v1/mois/meta-ad-investigations" &&
        request.method() === "GET"
      ) {
        await route.fulfill({ json: { items: [investigation] } });
      } else if (
        pathname === "/api/v1/mois/meta-ad-investigations" &&
        request.method() === "POST"
      ) {
        createPayload = request.postDataJSON();
        await route.fulfill({
          status: 201,
          json: {
            ...investigation,
            id: 8,
            searchTerms: createPayload.searchTerms,
          },
        });
      } else if (
        pathname === "/api/v1/mois/meta-ad-investigations/7/observations" &&
        request.method() === "POST"
      ) {
        observationPayload = request.postDataJSON();
        await route.fulfill({
          json: {
            investigationId: 7,
            accepted: 1,
            gateDecision: "INVESTIGAR",
            gaps: [],
          },
        });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(`${baseUrl}/mois/auto-collection`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", {
        name: "Radar supervisionado de anúncios comerciais",
      }),
    ).toBeVisible();
    await expect(
      page.getByText("treino entrevista emprego", { exact: true }),
    ).toBeVisible();
    const officialLink = page.getByRole("link", {
      name: "Abrir busca oficial",
    });
    await expect(officialLink).toHaveAttribute("target", "_blank");
    await expect(officialLink).toHaveAttribute(
      "href",
      /facebook\.com\/ads\/library/,
    );

    await page
      .getByPlaceholder("Produto, dor ou promessa a investigar")
      .fill("negociação salarial carreira");
    await page.locator("#meta-platform").selectOption("INSTAGRAM");
    await page.getByRole("button", { name: "Criar acompanhamento" }).click();
    await expect(
      page.getByPlaceholder("Produto, dor ou promessa a investigar"),
    ).toHaveValue("");
    assert.equal(createPayload.publisherPlatform, "INSTAGRAM");
    assert.equal(createPayload.countryCode, "BR");

    await page.locator("#meta-investigation").selectOption("7");
    await page.locator("#meta-ad-reference").fill("ad-qa-1");
    await page.locator("#meta-observed-platform").selectOption("INSTAGRAM");
    await page.locator("#meta-advertiser").fill("Marca QA");
    await page
      .locator("#meta-library-url")
      .fill("https://www.facebook.com/ads/library/?id=ad-qa-1");
    await page
      .locator("#meta-ad-text")
      .fill("Treino de entrevista com demonstração visível");
    await page.locator("#meta-page-active").check();
    await page.locator("#meta-commercial-signal").check();
    await page.getByRole("button", { name: "Registrar observação" }).click();
    await expect(page.locator("#meta-ad-reference")).toHaveValue("");
    assert.deepEqual(observationPayload.publisherPlatforms, ["INSTAGRAM"]);
    assert.equal(observationPayload.pageActive, true);
    assert.equal(observationPayload.commercialSignal, true);
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/mois-meta-ads-library-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Radar Meta aprovado em desktop, iPhone 15 Pro e Pixel 7 sem publicação externa.",
);
