// Valida o contexto estratégico pela tela local, sem chamadas a agentes ou serviços externos.
const { chromium, devices, expect } = require("@playwright/test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const base = process.env.ATENA_UI_BASE || "http://127.0.0.1:4173";
const output = process.env.ATENA_UI_OUTPUT || "/tmp/atena-plan-question";
const planId = 8101;
const makePlan = (blocker) => ({
  id: planId,
  name: "Plano sintético de contexto",
  planType: "FIRST_SALE",
  status: "DRAFT",
  experiments: [],
  milestones: [],
  simulations: [],
  daysRemaining: 0,
  deadline: "2026-10-15",
  currentBlocker: blocker,
  maxBudget: 0,
});
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [name, profile] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      const writes = [],
        errors = [];
      let blocker = "Preparar primeiro experimento sem tráfego anterior";
      let failRequest = false;
      page.on("pageerror", (error) => errors.push(error.message));
      await page.route("**/*", async (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (url.pathname.startsWith("/api/")) {
          let data = [];
          if (request.method() === "POST") {
            assert.equal(
              url.pathname,
              `/api/experiment-strategist/v1/commercial-plans/${planId}/executions`,
            );
            writes.push(request.postDataJSON());
            return route.fulfill({
              status: failRequest ? 503 : 200,
              contentType: "application/json",
              body: JSON.stringify(
                failRequest
                  ? { message: "Falha simulada" }
                  : { id: 991, status: "PENDING" },
              ),
            });
          }
          assert.equal(request.method(), "GET");
          if (url.pathname === "/api/planning/commercial-plans") {
            await new Promise((resolve) => setTimeout(resolve, 900));
            data = [makePlan(blocker)];
          } else if (url.pathname.endsWith("/configuration-status"))
            data = { accounts: [] };
          else if (url.pathname.endsWith("/operational-flow"))
            data = {
              stages: [],
              specialistDecisions: [],
              status: "BLOQUEADO",
              nextAction: blocker,
            };
          else if (url.pathname.endsWith("/agent-activity"))
            data = { entries: [] };
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(data),
          });
        }
        if (url.origin === base) return route.continue();
        return route.abort();
      });
      await page.goto(`${base}/planning/${planId}`, {
        waitUntil: "domcontentloaded",
      });
      const question = page.locator("#experiment-strategist-question");
      await expect(question).toHaveValue(blocker);
      await page
        .getByText("Operação avançada dos especialistas", { exact: true })
        .click();
      const submit = page.getByRole("button", {
        name: "Solicitar parecer estratégico",
        exact: true,
      });
      await expect(submit).toBeEnabled();
      await submit.click();
      await expect.poll(() => writes.length).toBe(1);
      assert.deepEqual(writes[0], { researchQuestion: blocker });
      await question.fill("Pergunta editada e específica do plano sintético");
      failRequest = true;
      await submit.click();
      await expect(
        page.getByText("Não foi possível solicitar o parecer.", {
          exact: true,
        }),
      ).toBeVisible();
      assert.deepEqual(writes[1], {
        researchQuestion: "Pergunta editada e específica do plano sintético",
      });
      await question.fill("");
      await expect(submit).toBeDisabled();
      assert.equal(writes.length, 2);
      assert.deepEqual(errors, []);
      await page.screenshot({
        path: path.join(output, `${name}.png`),
        fullPage: true,
      });
      results.push({
        profile: name,
        officialContext: true,
        exactEndpoint: true,
        manualQuestion: true,
        visibleFailure: true,
        blankBlocked: true,
        realAgentCalls: 0,
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(
    path.join(output, "results.json"),
    JSON.stringify(results, null, 2),
  );
  console.log(JSON.stringify(results));
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
