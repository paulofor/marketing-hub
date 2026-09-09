import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173",
  api = "http://127.0.0.1:18091";
const cycleApi = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/sales-flow-browser";
await mkdir(output, { recursive: true });
async function request(path, body) {
  const response = await fetch(api + path, {
    method: body ? "POST" : "GET",
    headers: { "Content-Type": "application/json" },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  assert(
    response.ok,
    `${path}: ${response.status} ${await response.clone().text()}`,
  );
  return response.json();
}
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    await request("/fixture/reset", {});
    await request("/fixture/experiments/91001/legacy-publication", {});
    const cycle = await request(`${cycleApi}/products/91001`, {
      requestKey: crypto.randomUUID(),
      chainDefinitionId: 91002,
      experimentId: 91001,
      baseline: true,
      productVersion: "fixture-v1",
      hypothesis: "Primeira ação concreta melhora uso",
      mainChange: "Microação útil",
      successCriterion: "Vendas líquidas e valor entregue",
      audience: "Pessoas aderentes",
      offer: "Oferta local",
      acquisition: "Canal simulado",
      budgetLimitBrl: 100,
      windowStart: new Date(Date.now() - 86400000).toISOString(),
      windowEnd: new Date(Date.now() + 86400000).toISOString(),
      sampleTarget: 10,
      minimumNetSales: 5,
      operatorName: "Homologação local",
    });
    assert.equal(cycle.stage, "DECISION");
    const position = await request("/api/products/value-chain-positions/91001");
    const flow = position.subprocessPosition.salesFlow;
    assert.equal(flow.currentActivityId, "learningCycle");
    assert.equal(
      position.subprocessPosition.currentSubprocessSequenceNumber,
      4,
    );
    assert.equal(
      position.subprocessPosition.measurements.find(
        (s) => s.processCode === "operacao-otimizacao-experimento",
      ).trackingStatus,
      "HISTORICAL",
    );
    const context = await browser.newContext({ ...profile, timezoneId: "UTC" });
    const page = await context.newPage();
    const external = [],
      errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.origin);
        return route.abort();
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(cycleApi) &&
        !url.pathname.startsWith("/api/products/value-chain-positions") &&
        !/^\/api\/business-processes\/\d+\/products\/\d+\/activity-executions/.test(
          url.pathname,
        ) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      return route.continue();
    });
    await page.goto(`${base}/products/91001/value-chain-history`, {
      waitUntil: "networkidle",
    });
    await page
      .getByRole("button", { name: "Carregar histórico detalhado" })
      .click();
    const panel = page.getByRole("region", { name: "Fluxo do Processo 6" });
    await expect(panel).toContainText("Atividade atual: 6.4");
    await expect(panel).toContainText("Referência histórica");
    await expect(panel).toContainText("Não aplicável neste período");
    await panel.locator("summary").click();
    await expect(panel).toContainText(
      "Operação atual ou referência histórica?",
    );
    await page.screenshot({
      path: `${output}/${name}-history.png`,
      fullPage: true,
    });
    await panel
      .getByRole("link", { name: "Continuar fluxo registrado" })
      .click();
    await page
      .locator("#activity-learningCycle")
      .getByRole("link", { name: /Retomar subprocesso/ })
      .click();
    await expect(
      page.getByRole("navigation", { name: "Local do ciclo na cadeia" }),
    ).toContainText("Atividade 4");
    await page
      .getByRole("link", { name: "Voltar à atividade 4 do Processo 6" })
      .click();
    await expect(page.locator("#activity-learningCycle")).toContainText(
      "Decisão comercial",
    );
    await expect(page.locator("#activity-optimization")).toContainText(
      "Referência histórica",
    );
    await expect(page.locator("#activity-consolidate")).toContainText(
      "Concluída",
    );
    await expect(page.locator("#activity-consolidate")).not.toContainText(
      "Abre subprocesso",
    );
    await page.screenshot({
      path: `${output}/${name}-activities.png`,
      fullPage: true,
    });
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    );
    const after = await request(`${cycleApi}/products/91001?chainId=91002`);
    assert.equal(
      after[0].revision,
      cycle.revision,
      "Navegação não altera ciclo",
    );
    const other = await request(
      `/api/business-processes/${flow.modelProcessDefinitionId}/products/91002/activity-executions`,
    );
    assert.equal(other.salesFlow, null, "Outro produto não herda o ciclo");
    await request("/fixture/experiments/91001/measurement", {
      snapshot: "delivery-pending",
      netSales: 2,
      refunds: 0,
      deliveryVerified: false,
    });
    // A fixture também precisa revisar a proposta antes de registrar uma decisão humana.
    const proposalPath = `${cycleApi}/products/91001/${cycle.id}/decision-proposal`;
    await expect
      .poll(async () => (await request(proposalPath)).status, {
        timeout: 20000,
      })
      .toBe("READY");
    const proposal = await request(proposalPath);
    await request(`${cycleApi}/products/91001/${cycle.id}/commands`, {
      requestKey: crypto.randomUUID(),
      expectedRevision: cycle.revision,
      action: "FIX_MEASUREMENT",
      operatorName: "Homologação local",
      summary: "Conferir entrega na fonte simulada",
      evidenceReference: "internal://fixture/delivery",
      evidence: {
        decisionProposalId: proposal.id,
        humanApproved: true,
        rootCause: "Venda sem entrega",
        correctionPlan: "Conferir fonte oficial",
      },
    });
    await page.goto(`${base}/products/91001/value-chain-history`, {
      waitUntil: "networkidle",
    });
    await page
      .getByRole("button", { name: "Carregar histórico detalhado" })
      .click();
    await expect(panel).toContainText("Atividade atual: 6.2");
    await panel
      .getByRole("link", { name: "Continuar fluxo registrado" })
      .click();
    assert(page.url().endsWith("#activity-delivery"));
    const delivery = page.locator("#activity-delivery");
    await expect(delivery).toContainText("Há vendas sem entrega comprovada");
    const childLink = delivery.getByRole("link", { name: /subprocesso/ });
    await expect(childLink).toHaveAttribute(
      "href",
      new RegExp(`learningCycleId=${cycle.id}$`),
    );
    await childLink.click();
    assert(page.url().includes(`learningCycleId=${cycle.id}`));
    await page.screenshot({
      path: `${output}/${name}-delivery.png`,
      fullPage: true,
    });
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    await context.close();
    console.log(
      `PASS ${name}: histórico → atividade → subprocesso; posições 6.4/6.2, entrega no contexto correto, dados segregados; sem efeitos externos`,
    );
  }
} finally {
  await browser.close();
}
