// Confere a retomada na UI local com respostas do controller/JPA real, sem serviços externos.
const { chromium, devices, expect } = require("@playwright/test");
const fs = require("node:fs");
const assert = require("node:assert/strict");
const fixture = JSON.parse(
  fs.readFileSync(
    process.env.HANDOFF_API_FIXTURE ||
      "backend/ads-service/target/handoff-http-fixture.json",
    "utf8",
  ),
);
const base = process.env.ATENA_UI_BASE || "http://127.0.0.1:4173";
const output = process.env.ATENA_UI_OUTPUT || "/tmp/atena-assumption-replay";
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [name, profile] of [
      ["desktop", { viewport: { width: 1440, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      let writes = 0;
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      await page.route("**/*", async (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (!url.pathname.startsWith("/api/"))
          return url.origin === base ? route.continue() : route.abort();
        let data = [];
        if (request.method() === "POST") {
          assert.equal(
            url.pathname,
            `/api/experiment-strategist/v1/commercial-plans/${fixture.planId}/commercial-assumptions`,
          );
          writes++;
          data = fixture.proposal;
        } else {
          assert.equal(request.method(), "GET");
          if (url.pathname === "/api/planning/commercial-plans")
            data = [
              {
                id: fixture.planId,
                name: "QA isolada de retomada",
                planType: "FIRST_SALE",
                status: "DRAFT",
                experiments: [],
                milestones: [],
                simulations: [],
                maxBudget: 0,
              },
            ];
          else if (url.pathname.endsWith("/commercial-assumptions"))
            data = writes ? fixture.financial : [];
          else if (url.pathname.endsWith("/configuration-status"))
            data = { accounts: [] };
          else if (url.pathname.endsWith("/operational-flow"))
            data = { stages: [], specialistDecisions: [], status: "BLOQUEADO" };
          else if (url.pathname.endsWith("/agent-activity"))
            data = { entries: [] };
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(data),
        });
      });
      await page.goto(`${base}/planning/${fixture.planId}`, {
        waitUntil: "domcontentloaded",
      });
      const button = page.getByRole("button", {
        name: "Definir premissas ausentes",
        exact: true,
      });
      await expect(button).toBeVisible();
      await expect(
        page.getByText(/retoma Plutus sem repetir a pesquisa de Atena/),
      ).toBeVisible();
      for (let i = 1; i <= 2; i++) {
        await button.click();
        await expect.poll(() => writes).toBe(i);
        await expect(
          page.getByText(`Validação conjunta #${fixture.financial[0].id}`, {
            exact: true,
          }),
        ).toBeVisible();
        await expect(button).toBeEnabled();
      }
      assert.deepEqual(errors, []);
      await page.screenshot({ path: `${output}/${name}.png`, fullPage: true });
      results.push({
        profile: name,
        planId: fixture.planId,
        proposalId: fixture.proposal.id,
        financialExecutionId: fixture.financial[0].id,
        commands: writes,
        externalRequests: 0,
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(`${output}/summary.json`, JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
