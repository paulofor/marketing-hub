import assert from "node:assert/strict";
import fs from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const api = "http://127.0.0.1:18095";
const output =
  process.env.FINANCIAL_PLAN_ARTIFACTS || "/tmp/financial-discovery-browser";
await fs.mkdir(output, { recursive: true });
const seed = JSON.parse(
  await fs.readFile(
    "backend/ads-service/src/test/resources/financial-plan/discovery.json",
    "utf8",
  ),
);
seed.validUntil = new Date(Date.now() + 10 * 86400000)
  .toISOString()
  .slice(0, 10);
let checks = 0;
async function request(path, body, expected = 200) {
  const r = await fetch(api + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await r.text();
  assert.equal(r.status, expected, `${path}: ${text}`);
  checks++;
  return text ? JSON.parse(text) : null;
}
let activePage;
const browser = await chromium.launch({
  headless: true,
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [label, device, id] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }, 95117],
    ["iphone", devices["iPhone 15 Pro"], 95118],
    ["pixel", devices["Pixel 7"], 95119],
  ]) {
    const assumptions = structuredClone(seed);
    assumptions.productVersion = "fixture-product-v1";
    delete assumptions.discoveryInvestment;
    const previous = await request(`/api/financial-plans/v1/products/${id}`);
    const source = await request(`/api/financial-plans/v1/products/${id}`, {
      name: `Descoberta ${label}`,
      createdBy: "Operador sintético",
      expectedRevision: previous[0]?.revision || 0,
      commercialPlanId: id,
      assumptions,
    });
    assert.equal(source.evaluation.status, "REVIEW_REQUIRED");
    assert.equal(source.canRequestAnalysis, false);
    checks += 2;
    const context = await browser.newContext(device);
    const p = await context.newPage();
    activePage = p;
    const externalRequests = [];
    await p.route("**/*", (route) => {
      const u = new URL(route.request().url());
      if (
        ["http:", "https:"].includes(u.protocol) &&
        !["127.0.0.1", "localhost"].includes(u.hostname)
      ) {
        externalRequests.push(u.origin + u.pathname);
        return route.abort("blockedbyclient");
      }
      return route.continue();
    });
    const errors = [];
    p.on("pageerror", (e) => errors.push(e.message));
    await p.goto(`http://127.0.0.1:15175/financial/plans?productId=${id}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      p.getByRole("heading", { name: `Descoberta ${label}`, exact: true }),
    ).toBeVisible();
    await p.getByText("Edição financeira avançada", { exact: true }).click();
    await p
      .getByRole("button", { name: "Editar premissas detalhadas" })
      .click();
    const accept = p.getByRole("checkbox", { name: /Aceito resultado abaixo/ });
    await expect(accept).not.toBeChecked();
    checks++;
    await accept.check();
    await p
      .getByLabel("Responsável pelo registro", { exact: true })
      .fill("Operador sintético · aceite de descoberta");
    for (const [name, value] of Object.entries({
      "Experimento da descoberta": id + 2000,
      "Ciclo da descoberta": id + 1000,
      "Teto total de mídia da descoberta (R$)": 100,
      "Duração máxima da descoberta (dias)": 5,
      "Referência da decisão de descoberta": `fixture://operator/${id}`,
    }))
      await p.getByLabel(name, { exact: true }).fill(String(value));
    await p.getByRole("button", { name: "Salvar revisão e calcular" }).click();
    await expect(
      p.getByText("Descoberta autorizada · requer parecer e gates", {
        exact: true,
      }),
    ).toBeVisible();
    const current = (
      await request(`/api/financial-plans/v1/products/${id}`)
    )[0];
    assert.equal(current.assumptions.minimumMarginPercent, 30);
    assert.equal(current.assumptions.discoveryInvestment.mediaBudgetBrl, 100);
    assert.equal(current.canRequestAnalysis, true);
    checks += 3;
    assert.ok(current.evaluation.scenarios.some((s) => s.marginPercent < 0));
    checks++;
    await p.reload({ waitUntil: "domcontentloaded" });
    await expect(
      p.getByText(/Descoberta limitada ao experimento/),
    ).toBeVisible();
    checks++;
    assert.equal(
      await p.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 2,
      ),
      false,
    );
    checks++;
    await p.getByText(/Descoberta limitada ao experimento/).scrollIntoViewIfNeeded();
    await p.screenshot({ path: `${output}/${label}.png`, fullPage: false });
    await p
      .getByRole("button", { name: "Solicitar parecer de Plutus" })
      .click();
    await expect(p.getByText(/Parecer #\d+ · PENDING/)).toBeVisible();
    const queued = (await request(`/api/financial-plans/v1/products/${id}`))[0];
    const rows = await request("/fixture/reviews");
    const row = rows.find((r) => r.id === queued.analysis.executionId);
    const sent = JSON.parse(row.context_json);
    assert.equal(
      sent.projectionBasis.discoveryInvestment.experimentId,
      id + 2000,
    );
    assert.equal(sent.assumptions.minimumMarginPercent, 30);
    checks += 2;
    await request(
      `/fixture/reviews/${queued.analysis.executionId}/COMPLETED`,
      {},
    );
    const replay = await request(
      `/api/financial-plans/v1/products/${id}/revisions/${queued.id}/analysis`,
      {},
    );
    assert.equal(replay.analysis.executionId, queued.analysis.executionId);
    assert.equal(replay.analysis.costUsd, 0.015);
    checks += 2;
    assert.equal((await request("/fixture/reviews")).length, rows.length);
    checks++;
    assert.equal(
      (
        await request(
          `/api/financial-plans/v1/products/${id}/revisions/${source.id}`,
        )
      ).assumptions.discoveryInvestment,
      null,
    );
    checks++;
    const foreign = {
      name: "Não transferir",
      createdBy: "Fixture",
      expectedRevision: 0,
      commercialPlanId: 95120,
      assumptions: current.assumptions,
    };
    await request("/api/financial-plans/v1/products/95120", foreign, 409);
    const over = structuredClone(current.assumptions);
    over.discoveryInvestment.mediaBudgetBrl = 99;
    await request(
      `/api/financial-plans/v1/products/${id}`,
      {
        name: "Teto insuficiente",
        createdBy: "Fixture",
        expectedRevision: current.revision,
        commercialPlanId: id,
        assumptions: over,
      },
      409,
    );
    const short = structuredClone(current.assumptions);
    short.discoveryInvestment.durationDays = 0;
    await request(
      `/api/financial-plans/v1/products/${id}`,
      {
        name: "Duração inválida",
        createdBy: "Fixture",
        expectedRevision: current.revision,
        commercialPlanId: id,
        assumptions: short,
      },
      400,
    );
    assert.deepEqual(externalRequests, []);
    assert.deepEqual(errors, []);
    checks++;
    await context.close();
  }
  console.log(
    JSON.stringify({
      result: "PASS",
      checks,
      providerCalls: 0,
      trafficClass: "LOCAL_SYNTHETIC",
      devices: ["desktop", "iPhone 15 Pro", "Pixel 7"],
    }),
  );
} catch (error) {
  if (activePage && !activePage.isClosed()) {
    await activePage.screenshot({ path: `${output}/failure.png` });
    await fs.writeFile(
      `${output}/failure.txt`,
      await activePage.locator("body").innerText(),
    );
  }
  throw error;
} finally {
  await browser.close();
}
