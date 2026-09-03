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

const project = {
  id: 1,
  productId: 4,
  salesVideoProfileId: 55,
  videoCategory: "COMMERCIAL_SHORT",
  contextType: "PDE",
  productionMode: "STORY_FIRST_AUDIO_VIDEO",
  targetChannel: "INSTAGRAM",
  format: "VERTICAL_9_16",
  title: "Vega #91",
  objective: "Converter interesse em diagnóstico",
  targetDurationSeconds: 10,
  status: "READY_FOR_SCRIPT",
};

const cycle = {
  id: 11,
  videoProjectId: 1,
  status: "QUEUED_FOR_APOLLO",
  budgetLimitUsd: 2,
  knownCostUsd: 0,
  learningObjective: "Validar retenção do criativo",
  successCriterion: "Aumentar CTA sem elevar retrabalho",
  providerPreflight: {
    id: 31,
    status: "READY",
    productionProfile: "FINAL_CAMPAIGN",
    aggregatorName: "Runway",
    accountKey: "RUNWAY_PRIMARY",
    routerConfigId: "marketing-hub-campaign-final-v1",
    estimatedCredits: 50,
    estimatedCostUsd: 0.5,
    maximumAuthorizedCredits: 80,
    maximumAuthorizedCostUsd: 0.8,
    officialBalanceCredits: 3000,
    reservedCreditsSnapshot: 0,
    availableCreditsSnapshot: 3000,
    maxMonthlyCreditSpend: 10000,
    quotaSnapshotJson:
      '{"models":[{"model":"gen4_turbo","remainingDailyGenerations":19}]}',
    sourceUrl: "https://api.dev.runwayml.com/v1/organization",
    observedAt: "2026-09-03T17:00:00Z",
    expiresAt: "2026-09-03T17:05:00Z",
    reservation: {
      id: 91,
      status: "RESERVED",
      reservedCredits: 80,
      reservedCostUsd: 0.8,
      expiresAt: "2099-09-03T18:00:00Z",
      reservedAt: "2026-09-03T17:00:02Z",
    },
  },
  financialDecision: "APPROVED",
  financialReason: "Rota apta dentro do teto.",
  recommendedAggregator: "Runway",
  recommendedRoute: "RUNWAY_ROUTER:marketing-hub-campaign-final-v1",
  estimatedCostUsd: 0.5,
  costBenefitBasis: "Dry run oficial e rota homologada para o perfil final.",
  creditAction: "NO_PURCHASE",
  monitoredTaskCount: 0,
  monitoredCredits: 0,
  budgetMonitorStatus: "WATCHING",
  providerClipDurationSeconds: 10,
  generationClipCount: 1,
  editCutCount: 4,
  textAppliedInPostProduction: true,
  createdAt: "2026-09-03T16:59:00Z",
};

const finance = {
  provider: "RUNWAY",
  status: "UNKNOWN_CONSUMPTION",
  balanceNature: "OFFICIAL_PROVIDER_SNAPSHOT",
  purchasedCredits: 3000,
  estimatedConsumedCredits: null,
  estimatedAvailableCredits: null,
  referenceModel: "gen4_turbo",
  referenceClipSeconds: 10,
  referenceClipCredits: 50,
  estimatedReferenceClips: null,
  lastPurchaseAt: null,
  lastCreditFailureAt: null,
  lastCreditFailureJobId: null,
  lastCreditFailureDetail: null,
  knownConsumedCostUsd: 0,
  unknownCostAttempts: 680,
  acceptedSceneRequests: 0,
  sceneRequests: [],
  creditsUrl: "https://dev.runwayml.com/",
  aggregatorName: "Runway",
  accountKey: "RUNWAY_PRIMARY",
  officialSnapshotStatus: "READY",
  officialBalanceCredits: 3000,
  reservedCredits: 80,
  officialAvailableCredits: 2920,
  maxMonthlyCreditSpend: 10000,
  quotaSnapshotJson:
    '{"models":[{"model":"gen4_turbo","remainingDailyGenerations":19}]}',
  officialObservedAt: "2026-09-03T17:00:00Z",
  officialExpiresAt: "2026-09-03T17:05:00Z",
  officialSourceUrl: "https://api.dev.runwayml.com/v1/organization",
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [name, options] of profiles) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const pageErrors = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const pathname = new URL(route.request().url()).pathname;
      if (pathname === "/api/sales-videos/projects/1") {
        await route.fulfill({ json: project });
      } else if (
        pathname === "/api/sales-videos/projects/1/autonomy/v1/cycles"
      ) {
        await route.fulfill({ json: [cycle] });
      } else if (pathname === "/api/sales-videos/studio/catalog") {
        await route.fulfill({ json: { characters: [], captionPresets: [] } });
      } else if (
        pathname === "/api/financial-agent/v1/video-providers/credit-balances"
      ) {
        await route.fulfill({ json: [finance] });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(`${baseUrl}/audio-video-studio/projects/1`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("region", { name: /preflight financeiro do provider/i }),
    ).toBeVisible();
    await expect(page.getByText(/Runway · RUNWAY_PRIMARY/)).toBeVisible();
    await expect(page.getByText(/RESERVED · 80 créditos/i)).toBeVisible();
    await expect(
      page.getByRole("region", {
        name: /parecer de custo-benefício de Plutus/i,
      }),
    ).toBeVisible();
    await page.getByText(/ver saldo de quota por modelo/i).click();
    await expect(page.getByText(/remainingDailyGenerations/)).toBeVisible();
    await assertNoHorizontalOverflow(page, `${name}: Estúdio`);

    await page.goto(`${baseUrl}/financial/video-providers`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByText(/Runway · RUNWAY_PRIMARY/)).toBeVisible();
    await expect(
      page.getByText("2.920 créditos", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: /fonte oficial do saldo e quota/i }),
    ).toHaveAttribute("target", "_blank");
    await assertNoHorizontalOverflow(page, `${name}: Financeiro`);
    assert.deepEqual(pageErrors, [], `${name}: erros não tratados`);
    await page.screenshot({
      path: `/tmp/runway-plutus-preflight-${name.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Preflight Runway/Plutus aprovado em desktop, iPhone 15 Pro e Pixel 7.",
);

async function assertNoHorizontalOverflow(page, context) {
  const sizes = await page.evaluate(() => {
    const viewport = document.documentElement.clientWidth;
    const offenders = Array.from(document.querySelectorAll("body *"))
      .filter((element) => !element.closest(".main-navigation"))
      .filter(
        (element) => !element.closest(".audio-video-studio-page__stage-grid"),
      )
      .map((element) => {
        const bounds = element.getBoundingClientRect();
        return {
          tag: element.tagName,
          className: element.className?.toString().slice(0, 120),
          left: Math.round(bounds.left),
          right: Math.round(bounds.right),
          width: Math.round(bounds.width),
        };
      })
      .filter(
        (item) =>
          (item.left < -1 || item.right > viewport + 1) &&
          item.right <= viewport + 200,
      )
      .sort((first, second) => second.right - first.right)
      .slice(0, 5);
    return {
      viewport,
      content: document.documentElement.scrollWidth,
      offenders,
      landmarks: [
        ".app-shell",
        ".app-shell__content",
        ".audio-video-studio-page",
        ".audio-video-studio-page__stage-grid",
        ".audio-video-studio-page__workspace",
        ".audio-video-studio-page__briefing",
        ".audio-video-studio-page__briefing-grid",
      ].map((selector) => {
        const element = document.querySelector(selector);
        const bounds = element?.getBoundingClientRect();
        return {
          selector,
          left: Math.round(bounds?.left ?? 0),
          right: Math.round(bounds?.right ?? 0),
          width: Math.round(bounds?.width ?? 0),
          clientWidth: element?.clientWidth ?? 0,
          scrollWidth: element?.scrollWidth ?? 0,
          boxSizing: element ? getComputedStyle(element).boxSizing : "",
          minWidth: element ? getComputedStyle(element).minWidth : "",
          gridTemplateColumns: element
            ? getComputedStyle(element).gridTemplateColumns
            : "",
        };
      }),
    };
  });
  assert.ok(
    sizes.content <= sizes.viewport + 1,
    `${context}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px; ${JSON.stringify(sizes.offenders)}; ${JSON.stringify(sizes.landmarks)}`,
  );
}
