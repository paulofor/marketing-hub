import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173",
  backend = "http://127.0.0.1:18091";
const api = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR ||
  "/tmp/learning-cycle-decision-browser";
await mkdir(output, { recursive: true });
async function request(path, body) {
  const r = await fetch(
    backend + path,
    body === undefined
      ? {}
      : {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
  );
  const text = await r.text();
  assert(r.ok, `${r.status}: ${text}`);
  return text ? JSON.parse(text) : null;
}
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1100 } }],
  ["iphone", devices["iPhone 15 Pro"]],
  ["pixel", devices["Pixel 7"]],
];
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
let checks = 0;
try {
  for (const [name, device] of profiles) {
    await request("/fixture/reset", {});
    await request("/fixture/experiments/91001/legacy-publication", {});
    const now = Date.now();
    const cycle = await request(`${api}/products/91001`, {
      requestKey: crypto.randomUUID(),
      chainDefinitionId: 91002,
      experimentId: 91001,
      baseline: true,
      previousCycleId: null,
      productVersion: "fixture-v1",
      hypothesis: "Primeira ação útil aumenta continuidade",
      mainChange: "Reduzir esforço inicial",
      successCriterion: "Venda líquida com valor entregue",
      audience: "Pessoas locais de teste",
      offer: "Oferta simulada",
      acquisition: "Canal simulado",
      budgetLimitBrl: 100,
      windowStart: new Date(now - 86400000).toISOString(),
      windowEnd: new Date(now + 86400000).toISOString(),
      sampleTarget: 10,
      minimumNetSales: 5,
      operatorName: "Responsável local declarado",
    });
    const context = await browser.newContext({ ...device, timezoneId: "UTC" });
    const page = await context.newPage();
    const errors = [],
      writes = [],
      external = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.origin);
        await route.abort();
        return;
      }
      if (route.request().method() === "POST") writes.push(url.pathname);
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(api) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      ) {
        await route.fulfill({ json: [] });
        return;
      }
      await route.continue();
    });
    const url = `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91002&cycleId=${cycle.id}`;
    await page.goto(url);
    const form = page.getByRole("form", { name: "Decisão do ciclo" });
    await expect(
      form.getByRole("button", { name: "Aprovar decisão e registrar no BPM" }),
    ).toBeVisible({ timeout: 30000 });
    assert.equal(writes.length, 0, "Navegação não executa comando ou criação");
    await expect(form.locator('[name="operatorName"]')).toHaveValue(
      "Responsável local declarado",
    );
    await expect(form.locator('[name="summary"]')).not.toHaveValue("");
    for (const field of [
      "rootCause",
      "learning",
      "nextHypothesis",
      "returnTarget",
      "evidenceReference",
    ])
      await expect(form.locator(`[name="${field}"]`)).not.toHaveValue("");
    await expect(page.getByText(/Modelo simulado na sandbox/)).toBeVisible();
    const original = await form.locator('[name="summary"]').inputValue();
    const edited = `Revisão humana ${name}: testar uma ação mais fácil e medir continuidade até compra.`;
    await form.locator('[name="summary"]').fill(edited);
    await page
      .getByRole("button", { name: "Atualizar leitura", exact: true })
      .click();
    await expect(form.locator('[name="summary"]')).toHaveValue(edited);
    await page.screenshot({
      path: `${output}/${name}-proposta-editavel.png`,
      fullPage: true,
    });
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth + 2,
    );
    assert.equal(overflow, false, `Overflow em ${name}`);
    const response = page.waitForResponse(
      (r) => r.url().endsWith("/commands") && r.request().method() === "POST",
    );
    await form
      .getByRole("button", { name: "Aprovar decisão e registrar no BPM" })
      .click();
    const sent = await response;
    assert.equal(sent.status(), 200, await sent.text());
    const final = await sent.json();
    assert.equal(final.status, "ADJUSTED");
    assert.equal(final.returnActivityId, "rework");
    assert.equal(final.events.at(-1).summary, edited);
    assert.equal(final.events.at(-1).evidence.humanApproved, true);
    assert(final.events.at(-1).evidence.decisionProposalId);
    await page.reload();
    await expect(page.getByText(/Proposta #[0-9]+ aprovada/)).toBeVisible();
    const saved = await request(
      `${api}/products/91001/${cycle.id}/decision-proposal`,
    );
    assert.equal(saved.proposal.summary, original);
    assert.equal(saved.status, "APPROVED");
    const audit = await request(
      `${api}/products/91001/${cycle.id}/decision-proposal/audit`,
    );
    assert.equal(
      audit[0].executionAudit.model,
      "fixture-atena-no-external-model",
    );
    assert(audit[0].rawResponse);
    assert.equal(audit[0].usage.inputTokens, 500);
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    assert.equal(writes.length, 1);
    checks++;
    await context.close();
  }
} finally {
  await browser.close();
}
console.log(
  JSON.stringify({
    status: "PASS",
    profiles: checks,
    approval: "HUMAN_EXPLICIT",
    model: "SIMULATED",
  }),
);
