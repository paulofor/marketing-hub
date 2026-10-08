import assert from "node:assert/strict";
import fs from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const api = "http://127.0.0.1:18095",
  ui = "http://127.0.0.1:15175",
  prefix = "/api/financial-plans/v1";
const out =
  process.env.FINANCIAL_PLAN_ARTIFACTS || "/tmp/contribution-target-browser";
await fs.mkdir(out, { recursive: true });
const template = JSON.parse(
  await fs.readFile(
    "backend/ads-service/src/test/resources/financial-plan/assumptions.json",
    "utf8",
  ),
);
template.validUntil = new Date(Date.now() + 30 * 86400000)
  .toISOString()
  .slice(0, 10);
template.minimumMarginPercent = null;
template.ai.perAttempt = null;
let checks = 0;
async function request(path, body, expected = 200) {
  const r = await fetch(api + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  assert.equal(r.status, expected, `${path}: ${await r.clone().text()}`);
  checks++;
  return r.status === 200 ? r.json() : null;
}
const reviewsBefore = await request("/fixture/reviews");
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, options, productId] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }, 95119],
    ["iphone", devices["iPhone 15 Pro"], 95120],
    ["pixel", devices["Pixel 7"], 95119],
  ]) {
    const root = `${prefix}/products/${productId}`;
    const history = await request(`${root}?environment=TEST`);
    const source = await request(`${root}?environment=TEST`, {
      name: "Meta sintética · sem mercado",
      createdBy: "Harness local",
      expectedRevision: history[0]?.revision ?? 0,
      commercialPlanId: productId,
      templateId: null,
      assumptions: structuredClone(template),
    });
    assert.equal(source.environment, "TEST");
    const before = await request(`${root}?environment=TEST`);
    const sourceSnapshot = await request(
      `${root}/revisions/${source.id}?environment=TEST`,
    );
    const page = await browser.newPage(options),
      errors = [];
    let failOnce = true,
      submissions = 0;
    page.on("pageerror", (error) => errors.push(error.message));
    await page.routeWebSocket("**/*", (socket) => socket.close());
    await page.route("**/*", async (route) => {
      const req = route.request(),
        url = new URL(req.url());
      if (url.pathname.startsWith(`${prefix}/`)) {
        if (req.method() === "POST") {
          assert.equal(url.pathname, `${root}/contribution-target`);
          assert.equal(url.searchParams.get("environment"), "TEST");
          assert.deepEqual(req.postDataJSON(), {
            sourceRevisionId: source.id,
            minimumMarginPercent: 31.25,
          });
          submissions++;
          if (failOnce) {
            failOnce = false;
            return route.fulfill({
              status: 503,
              json: { detail: "Falha temporária na gravação local." },
            });
          }
        }
        return route.fulfill({
          response: await route.fetch({ url: api + url.pathname + url.search }),
        });
      }
      if (url.pathname.startsWith("/api/")) return route.fulfill({ json: [] });
      return url.origin === ui ? route.continue() : route.abort();
    });
    await page.goto(
      `${ui}/financial/plans?productId=${productId}&revisionId=${source.id}&environment=TEST&edit=contribution-target`,
    );
    const decision = page.getByRole("region", {
        name: "Decisão da margem mínima",
      }),
      input = decision.getByLabel("Margem mínima proposta (%)", {
        exact: true,
      }),
      save = decision.getByRole("button", {
        name: "Salvar margem mínima",
        exact: true,
      });
    await expect(input).toHaveValue("");
    await expect(decision).toContainText("não é lucro líquido");
    for (const invalid of ["", "0", "-1", "100"]) {
      await input.fill(invalid);
      await save.click();
      assert.equal(submissions, 0);
      checks++;
    }
    await input.fill("31.25");
    await save.click();
    await expect(
      page
        .getByRole("alert")
        .filter({ hasText: "Falha temporária na gravação local." }),
    ).toBeVisible();
    await expect(input).toHaveValue("31.25");
    assert.equal(
      (await request(`${root}?environment=TEST`)).length,
      before.length,
    );
    await decision.screenshot({
      path: `${out}/${name}-contribution-target.png`,
    });
    await save.click();
    await expect(
      page.getByText(/Meta registrada em uma nova revisão/),
    ).toBeVisible();
    const after = await request(`${root}?environment=TEST`);
    assert.equal(after.length, before.length + 1);
    assert.equal(after[0].assumptions.minimumMarginPercent, 31.25);
    assert.equal(after[0].canRequestAnalysis, false);
    assert.equal(after[0].analysis, null);
    const expected = structuredClone(source.assumptions);
    expected.minimumMarginPercent = 31.25;
    expected.realizedCostBaseline = after[0].assumptions.realizedCostBaseline;
    assert.deepEqual(after[0].assumptions, expected);
    assert.deepEqual(
      await request(`${root}/revisions/${source.id}?environment=TEST`),
      sourceSnapshot,
    );
    const changed = await request(
      `${root}/contribution-target?environment=TEST`,
      {
        sourceRevisionId: after[0].id,
        minimumMarginPercent: 35,
      },
    );
    const restored = await request(
      `${root}/contribution-target?environment=TEST`,
      {
        sourceRevisionId: changed.id,
        minimumMarginPercent: 31.25,
      },
    );
    assert.equal(restored.revision, changed.revision + 1);
    assert.equal(
      (await request(`${root}?environment=TEST`))[0].id,
      restored.id,
    );
    const repeated = await request(
      `${root}/contribution-target?environment=TEST`,
      {
        sourceRevisionId: restored.id,
        minimumMarginPercent: 31.25,
      },
    );
    assert.equal(repeated.id, restored.id);
    await request(
      `${root}/contribution-target?environment=TEST`,
      { sourceRevisionId: source.id, minimumMarginPercent: 31.25 },
      409,
    );
    await request(
      `${root}/contribution-target?environment=LIVE`,
      { sourceRevisionId: after[0].id, minimumMarginPercent: 31.25 },
      404,
    );
    await request(
      `${prefix}/products/${productId === 95119 ? 95120 : 95119}/contribution-target?environment=TEST`,
      { sourceRevisionId: after[0].id, minimumMarginPercent: 31.25 },
      404,
    );
    assert.deepEqual(await request(`${root}?environment=LIVE`), []);
    assert.deepEqual(await request("/fixture/reviews"), reviewsBefore);
    assert.deepEqual(errors, []);
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.equal(submissions, 2);
    checks += 12;
    await page.close();
    console.log(
      `PASS ${name}: tela → contrato → MySQL 5.7, recuperação, história e isolamento`,
    );
  }
} finally {
  await browser.close();
}
console.log(
  JSON.stringify({
    result: "PASS",
    checks,
    modelInvoked: false,
    marketEvidence: false,
  }),
);
