const { chromium, devices, expect } = require("@playwright/test");
const { mkdirSync } = require("node:fs");
const assert = require("node:assert/strict");
const base = process.env.IMAGE_COST_UI_URL || "http://127.0.0.1:15291";
(async () => {
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  try {
    mkdirSync("/tmp/image-cost-reconciliation", { recursive: true });
    for (const [name, profile] of [
      ["desktop", { viewport: { width: 1440, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      const writes = [];
      let attempt = 0;
      await page.route("**/api/**", async (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (request.method() !== "GET") {
          assert.equal(request.method(), "POST");
          assert.equal(
            url.pathname,
            "/api/image-generator/generations/synthetic-job/cost-reconciliation",
          );
          assert.equal(url.searchParams.get("productId"), "201");
          assert.equal(url.searchParams.get("commercialPlanId"), "301");
          assert.equal(url.searchParams.get("experimentId"), "401");
          writes.push(url.pathname);
          attempt++;
          if (attempt === 1)
            return route.fulfill({
              status: 422,
              json: { message: "Auditoria temporariamente indisponível" },
            });
          await new Promise((resolve) => setTimeout(resolve, 300));
          return route.fulfill({
            json:
              attempt === 2
                ? {
                    jobId: "synthetic-job",
                    status: "COST_PENDING",
                    estimatedCostUsd: null,
                    evidence: "USAGE_OR_PRICING_MISSING",
                  }
                : {
                    jobId: "synthetic-job",
                    status: "ESTIMATED",
                    estimatedCostUsd: 0.079122,
                    evidence: "AUDITED_TOKEN_RATE_ESTIMATE",
                  },
          });
        }
        let data = [];
        if (url.pathname === "/api/products")
          data = [{ id: 201, name: "Produto sintético" }];
        if (url.pathname === "/api/planning/commercial-plans")
          data = [{ id: 301, name: "Plano sintético", experimentId: 401 }];
        if (url.pathname === "/api/experiments")
          data = [{ id: 401, productId: 201, name: "Experimento sintético" }];
        if (url.pathname === "/api/image-generator/generations/recent")
          data = [
            {
              jobId: "synthetic-job",
              batchJobId: "synthetic-batch",
              model: "gpt-image-2.5-sunburst",
              prompt: "Entrada sintética",
              generatedAt: "2026-10-05T00:00:00Z",
            },
          ];
        if (url.pathname === "/api/creatives/video-review/summary")
          data = { awaitingReviewCount: 0 };
        if (url.pathname === "/api/facebook/configuration-status")
          data = {
            hasConfiguredPages: true,
            worker: { ready: true, messages: [] },
            tokenRenewal: { accounts: [] },
          };
        return route.fulfill({ json: data });
      });
      await page.goto(base + "/ai/image-generator");
      await page.locator("#image-generator-product").selectOption("201");
      await page.locator("#image-generator-plan").selectOption("301");
      await page.locator("#image-generator-experiment").selectOption("401");
      const button = page.getByRole("button", {
        name: "Conciliar custo sem regenerar",
      });
      await button.click();
      await expect(
        page
          .getByRole("alert")
          .filter({ hasText: "Auditoria temporariamente indisponível" }),
      ).toContainText("Auditoria temporariamente indisponível");
      await button.click();
      await expect(
        page.getByRole("button", { name: "Conciliando..." }),
      ).toBeDisabled();
      await expect(
        page.getByText("Custo pendente:", { exact: false }),
      ).toBeVisible();
      await button.click();
      await expect(page.getByText(/Custo estimado: USD 0.07912/)).toBeVisible();
      assert.equal(writes.length, 3);
      assert(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        "Layout deve caber na tela",
      );
      await page.screenshot({
        path: "/tmp/image-cost-reconciliation/" + name + ".png",
        fullPage: true,
      });
      await context.close();
      console.log(
        name +
          ": falha, loading, custo pendente e conciliação sem geração aprovados",
      );
    }
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
