import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173";
const api = "http://127.0.0.1:18091";
const cycleApi = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/cycle-legacy-browser";
await mkdir(output, { recursive: true });

async function fixture(path, body = {}) {
  const response = await fetch(api + path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  assert(response.ok, `${path}: ${response.status}`);
  return response.json();
}

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [name, profile] of [
    ["desktop", { viewport: { width: 1440, height: 1100 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    await fixture("/fixture/reset");
    await fixture("/fixture/experiments/91001/legacy-publication");
    const context = await browser.newContext({ ...profile, timezoneId: "UTC" });
    const page = await context.newPage();
    const errors = [],
      external = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.origin);
        return route.abort();
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(cycleApi) &&
        !/^\/api\/business-processes\/\d+\/products\/\d+\/activity-executions$/.test(
          url.pathname,
        ) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      ) {
        return route.fulfill({ json: [] });
      }
      return route.continue();
    });
    await page.goto(
      `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91001`,
      { waitUntil: "networkidle" },
    );
    await page
      .getByRole("button", { name: "Abrir ciclo", exact: true })
      .click();
    const start = new Date(Date.now() - 86400000).toISOString().slice(0, 16);
    const end = new Date(Date.now() + 86400000).toISOString().slice(0, 16);
    async function create(experimentId, version) {
      const form = page.getByRole("form", {
        name: "Abrir ciclo de aprendizado",
      });
      await form
        .locator('[name="experimentId"]')
        .selectOption(String(experimentId));
      for (const [key, value] of Object.entries({
        productVersion: version,
        hypothesis: "Ação executável melhora primeiro uso",
        mainChange: "Primeira microação",
        successCriterion: "Vendas líquidas com uso",
        audience: "Pessoas aderentes",
        offer: "R$ 67 por sete dias",
        acquisition: "Canal simulado",
        operatorName: "Operador local",
        budgetLimitBrl: "100",
        sampleTarget: "10",
        minimumNetSales: "5",
        windowStart: start,
        windowEnd: end,
      }))
        await form.locator(`[name="${key}"]`).fill(value);
      const result = page.waitForResponse(
        (r) =>
          r.url().endsWith("/products/91001") &&
          r.request().method() === "POST",
      );
      await form
        .getByRole("button", { name: "Abrir ciclo", exact: true })
        .click();
      const response = await result;
      assert.equal(response.status(), 200, await response.text());
      const value = await response.json();
      await expect(page).toHaveURL(
        new RegExp(`[?&]cycleId=${value.id}(?:&|$)`),
      );
      await expect(form).toHaveCount(0);
      await expect(
        page.getByRole("heading", {
          name: `Ciclo #${value.id} · experimento #${experimentId}`,
          exact: true,
        }),
      ).toBeVisible();
      return value;
    }
    let current = await create(91001, "fixture-v1");
    assert.equal(current.stage, "DECISION");
    await page
      .getByText("Histórico de decisões e evidências (2)", { exact: true })
      .click();
    await expect(
      page.getByText(/Campanha Meta histórica comprovada por recibo externo/),
    ).toBeVisible();
    assert.equal(current.events[0].evidence.preflightRecorded, false);
    assert.equal(current.events[0].evidence.source, "LEGACY_META_CAMPAIGN");
    assert.equal(current.events[1].action, "MEASURE");
    assert.equal(current.events[1].evidence.automatic, true);
    await expect(
      page.getByText(/Leitura automática das fontes oficiais/),
    ).toBeVisible();
    await page.screenshot({
      path: `${output}/${name}-adocao.png`,
      fullPage: true,
    });
    async function command(action, button, values) {
      const form = page.getByRole("form", { name: "Decisão do ciclo" });
      await form.locator('[name="action"]').selectOption(action);
      for (const [key, value] of Object.entries({
        operatorName: "Operador local",
        summary: "Aprendizado segregado",
        evidenceReference: "internal://fixture/legacy",
        ...values,
      })) {
        const field = form.locator(`[name="${key}"]`);
        if (typeof value === "boolean") await field.setChecked(value);
        else if (await field.evaluate((node) => node.tagName === "SELECT"))
          await field.selectOption(String(value));
        else await field.fill(String(value));
      }
      const result = page.waitForResponse(
        (r) => r.url().includes("/commands") && r.request().method() === "POST",
      );
      await form.getByRole("button", { name: button, exact: true }).click();
      const response = await result;
      assert.equal(response.status(), 200, await response.text());
      await page.waitForLoadState("networkidle");
      return response.json();
    }
    const catalog = await (
      await fetch(`${api}${cycleApi}/catalog?chainId=91001&productId=91001`)
    ).json();
    await page.route("**/api/products/value-chain-positions/*", (route) =>
      route.fulfill({
        json: {
          processDefinitionId: catalog.entry.parentProcessDefinitionId,
          sequenceNumber: catalog.entry.sequenceNumber,
          processMeasurements: [],
        },
      }),
    );
    const returnToParent = async (expectedState, label) => {
      const beforeNavigation = await (
        await fetch(`${api}${cycleApi}/products/91001?chainId=91001`)
      ).json();
      await page
        .getByRole("link", { name: "Voltar à atividade 4 do Processo 6" })
        .click();
      const call = page.locator("#activity-learningCycle");
      await expect(call).toBeInViewport();
      await expect(page.locator("#activity-optimization")).toContainText(
        "Abre subprocesso",
      );
      await expect(page.locator("#activity-delivery")).toContainText(
        "Abre subprocesso",
      );
      await expect(page.locator("#activity-consolidate")).not.toContainText(
        "Abre subprocesso",
      );
      await expect(call).toContainText("Atividade 6.4 · Abre subprocesso");
      await expect(call).toContainText(`Ciclo #${current.id}`);
      await expect(call).toContainText(label);
      await expect(
        page.getByRole("region", { name: "Ciclo dentro do processo de venda" }),
      ).toHaveCount(0);
      const history = await (
        await fetch(
          `${api}/api/business-processes/${catalog.entry.parentProcessDefinitionId}/products/91001/activity-executions`,
        )
      ).json();
      assert.equal(
        history.activities.find((a) => a.activityId === "learningCycle")
          .operationalState,
        expectedState,
      );
      assert.equal(history.activities.length, 4);
      assert.equal(history.uniqueTaskCount, 0);
      if (expectedState === "IN_PROGRESS") {
        assert.equal(history.currentActivityId, "learningCycle");
        assert.equal(history.operationalState, "IN_PROGRESS");
      }
      assert(
        history.activities
          .filter((a) => a.activityId !== "learningCycle")
          .every((a) => !a.objectiveAchieved),
      );
      assert(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth + 1,
        ),
      );
      await page.screenshot({
        path: `${output}/${name}-processo6-${expectedState}.png`,
        fullPage: true,
      });
      await call
        .getByRole("link", {
          name: /Retomar subprocesso|Consultar subprocesso/,
        })
        .click();
      await expect(
        page.getByRole("navigation", { name: "Local do ciclo na cadeia" }),
      ).toContainText("Atividade 4: Conduzir o ciclo de aprendizado e vendas");
      const afterNavigation = await (
        await fetch(`${api}${cycleApi}/products/91001?chainId=91001`)
      ).json();
      assert.deepEqual(
        afterNavigation,
        beforeNavigation,
        "Navegação alterou o ciclo",
      );
    };
    await returnToParent("IN_PROGRESS", "Decisão comercial");
    const target = catalog.returnTargets.find((t) => t.activityId === "rework");
    current = await command("ADJUST", "Encerrar ciclo e preparar sucessor", {
      rootCause: "Microação pouco clara",
      learning:
        "Melhorar a primeira entrega; amostra insuficiente para concluir rejeição",
      nextHypothesis: "Ação concreta melhora uso",
      returnTarget: `${target.processDefinitionId}:${target.activityId}`,
    });
    await returnToParent("COMPLETED", "Decisão encerrada");
    await page
      .getByRole("button", {
        name: "Criar ciclo sucessor com aprendizado",
        exact: true,
      })
      .click();
    const successor = await create(91002, "fixture-v2");
    assert.equal(successor.previousCycleId, current.id);
    assert.equal(successor.stage, "LEARNING");
    assert.equal(successor.events.length, 0);
    assert.equal(
      successor.inheritedLearning.events[0].evidence.source,
      "LEGACY_META_CAMPAIGN",
    );
    await page.reload({ waitUntil: "networkidle" });
    await expect(
      page.getByRole("heading", {
        name: "Aprendizado recebido do experimento #91001",
      }),
    ).toBeVisible();
    await expect(
      page.getByText(/Campanha Meta histórica comprovada por recibo externo/),
    ).toBeVisible();
    const before = await (
      await fetch(`${api}/fixture/experiments/91001/state`)
    ).json();
    assert.deepEqual(before, {
      status: "USER_STOPPED",
      runCount: 0,
      campaignCount: 1,
    });
    assert.deepEqual(
      await (await fetch(`${api}/fixture/experiments/91002/state`)).json(),
      {
        status: "PLANNED",
        runCount: 0,
        campaignCount: 0,
      },
    );
    const width = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      viewport: innerWidth,
    }));
    assert(width.scroll <= width.viewport + 1, JSON.stringify(width));
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    await page.screenshot({
      path: `${output}/${name}-sucessor.png`,
      fullPage: true,
    });
    console.log(
      JSON.stringify({
        profile: name,
        legacyAdoption: true,
        successorMemory: true,
        productionCalls: 0,
      }),
    );
    await context.close();
  }
} finally {
  await browser.close();
}
