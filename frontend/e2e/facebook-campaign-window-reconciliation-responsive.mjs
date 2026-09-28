import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
assert.equal(
  new URL(baseUrl).hostname,
  "127.0.0.1",
  "A homologação visual deve usar somente o frontend local",
);

const expiredCampaign = {
  id: 91,
  name: "Vega #91",
  hypothesis: "Campanha Meta encerrada no prazo autorizado",
  kpiTargetCpl: 20,
  startDate: "2026-09-20",
  endDate: "2026-09-26",
  nicheName: "Carreira",
  hypothesisTitle: "Vega",
  missingConfiguration: [],
  metrics: {
    dateStart: "2026-09-20",
    dateStop: "2026-09-26",
    impressions: 1146,
    clicks: 35,
    leads: 0,
    spend: 149.53,
    cpc: 4.27,
    cpl: null,
    lastSyncedAt: "2026-09-28T09:22:00Z",
    lastSyncError: null,
  },
  leadPortalFunnel: { formAccesses: 0, formSubmissions: 0 },
  campaignStrategy: null,
  campaignOperation: {
    campaignId: "meta-campaign-91",
    configuredStatus: "ACTIVE",
    effectiveStatus: "ACTIVE",
    persistedStatus: "PAUSED",
    deliveryState: "WINDOW_ENDED",
    deliveringNow: false,
    windowStart: "2026-09-07T03:13:13Z",
    windowEnd: "2026-09-27T02:59:59Z",
    statusLastSyncedAt: "2026-09-28T09:22:00Z",
    budgetRemainingMinor: 47,
    metricsFinalSyncedAt: "2026-09-28T09:22:00Z",
  },
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [profileName, contextOptions] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iPhone-15-Pro", devices["iPhone 15 Pro"]],
    ["Pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    const experimentStatuses = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (!url.pathname.startsWith("/api/")) {
        await route.continue();
        return;
      }
      if (url.pathname === "/api/facebook/configuration-status") {
        await route.fulfill({
          json: {
            hasConfiguredPages: true,
            worker: { hasAccount: true, ready: true, messages: [] },
            tokenRenewal: {
              enabledAccounts: 1,
              eligibleAccounts: 1,
              accounts: [],
            },
          },
        });
        return;
      }
      if (url.pathname === "/api/facebook-campaigns/experiments") {
        const status = url.searchParams.get("status");
        experimentStatuses.push(status);
        await route.fulfill({
          json: status === "INCONCLUSIVE" ? [expiredCampaign] : [],
        });
        return;
      }
      await route.fulfill({ json: [] });
    });

    await page.goto(`${baseUrl}/facebook-campaigns`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Experimentos para Campanha" }),
    ).toBeVisible();
    await page.getByRole("button", { name: "Inconclusivas" }).click();
    await expect(page.getByRole("link", { name: "Vega #91" })).toBeVisible();

    const row = page.getByRole("row").filter({ hasText: "Vega #91" });
    await expect(row.getByText("ACTIVE", { exact: true })).toBeVisible();
    await expect(
      row.getByText("Janela encerrada", { exact: true }),
    ).toBeVisible();
    await expect(
      row.getByText("Saldo Meta R$ 0,47", { exact: true }),
    ).toBeVisible();
    await expect(row.getByText(/Impr\.\s+1\.146/)).toBeVisible();
    await expect(row.getByText(/Cliques\s+35/)).toBeVisible();
    await expect(row.getByText(/Custo\s+R\$\s*149,53/)).toBeVisible();
    await expect(page.getByText("Em dia", { exact: true })).toHaveCount(0);
    await expect(page.getByText("Ativo", { exact: true })).toHaveCount(0);
    assert.ok(
      experimentStatuses.includes("INCONCLUSIVE"),
      `${profileName}: a aba deve consultar o estado terminal reconciliado`,
    );
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    const widths = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      widths.content <= widths.viewport + 1,
      `${profileName}: overflow horizontal ${widths.content}px > ${widths.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/facebook-window-reconciliation-${profileName}.png`,
      fullPage: true,
    });
    await context.close();
    console.log(`PASS reconciliação Meta responsiva: ${profileName}`);
  }
} finally {
  await browser.close();
}
