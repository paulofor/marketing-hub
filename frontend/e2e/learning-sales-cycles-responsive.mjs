import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = "http://127.0.0.1:15173";
const api = "http://127.0.0.1:18091";
const cycleApi = "/api/business-process-chains/learning-cycles/v1";
const output =
  process.env.LEARNING_CYCLES_EVIDENCE_DIR || "/tmp/learning-cycles-browser";
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
  headless: true,
});
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iphone", devices["iPhone 15 Pro"]],
  ["pixel", devices["Pixel 7"]],
];
let journeys = 0;
try {
  for (const [name, profile] of profiles) {
    await fixture("/fixture/reset");
    const context = await browser.newContext({ ...profile, timezoneId: "UTC" });
    const page = await context.newPage();
    await page.addInitScript(() => {
      delete Object.getPrototypeOf(window.crypto).randomUUID;
      delete window.crypto.randomUUID;
    });
    const errors = [];
    const external = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.origin !== base) {
        external.push(url.origin);
        await route.abort();
        return;
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(cycleApi) &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      ) {
        await route.fulfill({ json: [] });
        return;
      }
      await route.continue();
    });
    await page.goto(
      `${base}/business-process-chains/learning-cycles?productId=91001&chainId=91001`,
      { waitUntil: "networkidle" },
    );
    await expect(
      page.getByRole("heading", { name: "Ciclos de aprendizado e vendas" }),
    ).toBeVisible();
    await page
      .getByRole("button", { name: "Abrir ciclo", exact: true })
      .click();
    const create = page.getByRole("form", {
      name: "Abrir ciclo de aprendizado",
    });
    const fields = {
      productVersion: "fixture-v1",
      hypothesis: "Primeiro ajuste executável aumenta uso",
      mainChange: "Microação de entrada",
      successCriterion: "Vendas líquidas com contribuição e uso",
      audience: "Pessoas aderentes consentidas",
      offer: "R$ 67 por sete dias",
      acquisition: "Canal de teste isolado",
      operatorName: "Operador local",
      budgetLimitBrl: "100",
      sampleTarget: "10",
      minimumNetSales: "5",
    };
    await create.locator('[name="experimentId"]').selectOption("91001");
    for (const [key, value] of Object.entries(fields))
      await create.locator(`[name="${key}"]`).fill(value);
    const start = new Date(Date.now() - 86400000).toISOString().slice(0, 16),
      end = new Date(Date.now() + 86400000).toISOString().slice(0, 16);
    await create.locator('[name="windowStart"]').fill(start);
    await create.locator('[name="windowEnd"]').fill(end);
    await create
      .getByRole("button", { name: "Abrir ciclo", exact: true })
      .click();
    await expect(
      page.getByText("Aprendizado e hipótese", { exact: true }).first(),
    ).toBeVisible();
    const form = () => page.getByRole("form", { name: "Decisão do ciclo" });
    async function submit(
      values,
      button = "Concluir etapa com evidência",
      action = "COMPLETE",
    ) {
      await form().locator('[name="action"]').selectOption(action);
      for (const [key, value] of Object.entries({
        operatorName: "Operador local",
        summary: "Registro de evidência segregada",
        evidenceReference: "internal://fixture/local",
        ...values,
      })) {
        const field = form().locator(`[name="${key}"]`);
        if (typeof value === "boolean") await field.setChecked(value);
        else if (await field.evaluate((node) => node.tagName === "SELECT"))
          await field.selectOption(String(value));
        else await field.fill(String(value));
      }
      const response = page.waitForResponse(
        (response) =>
          response.url().includes("/commands") &&
          response.request().method() === "POST",
      );
      await form().getByRole("button", { name: button, exact: true }).click();
      const result = await response;
      assert.equal(result.status(), 200, await result.text());
      await page.waitForLoadState("networkidle");
      return result.json();
    }
    await submit({
      learning: "Microação vaga dificulta uso",
      competingExplanation: "Amostra de tráfego reduzida",
    });
    await expect(
      page.getByText("Planejar o experimento", { exact: true }).first(),
    ).toBeVisible();
    await submit({
      planReference: "internal://plan/local",
      stopRule: "Parar no limite ou janela",
    });
    await submit({
      productVersion: "fixture-v1",
      changeEvidence: "internal://change/local",
    });
    await expect(
      page.getByText("Definir os dois vídeos", { exact: true }).first(),
    ).toBeVisible();
    const refs = await fixture("/fixture/videos", {
      experimentId: 91001,
      productVersion: "fixture-v1",
    });
    await submit(
      Object.fromEntries(
        [
          "briefReference",
          "campaignGoal",
          "campaignCta",
          "campaignMetric",
          "pdeGoal",
          "pdeCta",
          "pdeMetric",
          "controlledVariables",
          "productionBudgetReference",
        ].map((key) => [key, `Briefing local: ${key}`]),
      ),
    );
    await page
      .getByRole("button", { name: "Atualizar leitura", exact: true })
      .click();
    await expect(
      page.getByRole("navigation", {
        name: "Produção e integração dos vídeos",
      }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Produzir no Estúdio", exact: true }),
    ).toHaveAttribute("href", "/audio-video-studio");
    await submit({
      campaignVideoAssetId: refs.campaignVideoAssetId,
      productionEvidence: "internal://studio/ad",
    });
    await submit({
      pdeVideoAssetId: refs.pdeVideoAssetId,
      productionEvidence: "internal://studio/entry",
    });
    await page.screenshot({
      path: `${output}/${name}-videos.png`,
      fullPage: true,
    });
    await submit({
      creativeId: refs.creativeId,
      pdeSlotId: refs.pdeSlotId,
      technicalEvidence: "internal://qa/playback",
      customerReviewEvidence: "internal://qa/customer",
      captionsVerified: true,
      mobileVerified: true,
      optionalPlaybackVerified: true,
      testDataExcluded: true,
    });
    await expect(
      page.getByText("Homologar a mesma versão", { exact: true }).first(),
    ).toBeVisible();
    await expect(
      form().getByRole("option", {
        name: "Concluir etapa com evidência · bloqueado",
      }),
    ).toHaveJSProperty("disabled", true);
    const proof = await fixture("/fixture/approval", {
      productId: 91001,
      productVersion: "fixture-v1",
    });
    await page
      .getByRole("button", { name: "Atualizar leitura", exact: true })
      .click();
    await expect(
      form().getByRole("option", {
        name: "Concluir etapa com evidência",
        exact: true,
      }),
    ).toHaveJSProperty("disabled", false);
    await submit({
      approvalInstanceId: proof.approvalInstanceId,
      journeyEvidence: "internal://journey/local",
      humanObservationEvidence: "internal://consent/observed",
      instrumentationVerified: true,
    });
    await submit(
      { productVersion: "fixture-v1", budgetLimitBrl: 100, confirmed: true },
      "Registrar autorização",
    );
    await fixture("/fixture/experiments/91001/publish");
    let current = await submit({});
    assert.equal(current.stage, "DECISION");
    assert.equal(current.events.at(-1).action, "MEASURE");
    assert.equal(current.events.at(-1).evidence.automatic, true);
    await expect(
      form().getByRole("option", { name: "Solicitar escala · bloqueado" }),
    ).toHaveJSProperty("disabled", true);
    await page
      .getByText("BPM · decisões e retornos do ciclo", { exact: true })
      .click();
    await expect(
      page.getByRole("img", { name: "Losango de decisão" }),
    ).toHaveCount(2);
    const returnLink = page.getByRole("link", {
      name: /Ajustar: novo experimento com memória/,
    });
    await returnLink.click();
    await page.screenshot({
      path: `${output}/${name}-bpm.png`,
      fullPage: true,
    });
    const size = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
    }));
    assert(size.scroll <= size.viewport + 1, JSON.stringify(size));
    await fixture("/fixture/experiments/91001/stop");
    const response = await fetch(
      `${api}${cycleApi}/catalog?chainId=91001&productId=91001`,
    );
    const catalog = await response.json();
    const target = catalog.returnTargets.find(
      (target) => target.activityId === "rework",
    );
    current = await submit(
      {
        rootCause: "Baixa clareza do ajuste",
        learning: "Melhorar instrução antes de novo tráfego",
        nextHypothesis: "Microação guiada",
        returnTarget: `${target.processDefinitionId}:${target.activityId}`,
      },
      "Encerrar ciclo e preparar sucessor",
      "ADJUST",
    );
    await page
      .getByRole("button", {
        name: "Criar ciclo sucessor com aprendizado",
        exact: true,
      })
      .click();
    const successor = page.getByRole("form", {
      name: "Abrir ciclo de aprendizado",
    });
    await successor.locator('[name="experimentId"]').selectOption("91002");
    for (const [key, value] of Object.entries({
      ...fields,
      productVersion: "fixture-v2",
    }))
      await successor.locator(`[name="${key}"]`).fill(value);
    await successor.locator('[name="windowStart"]').fill(start);
    await successor.locator('[name="windowEnd"]').fill(end);
    await successor
      .getByRole("button", { name: "Abrir ciclo", exact: true })
      .click();
    await expect(
      page.getByRole("heading", {
        name: "Aprendizado recebido do experimento #91001",
      }),
    ).toBeVisible();
    await page.reload({ waitUntil: "networkidle" });
    await expect(
      page.getByRole("heading", { name: /Ciclo #.*experimento #91002/ }),
    ).toBeVisible();
    await expect(
      page.getByText("Aprendizado: Melhorar instrução antes de novo tráfego"),
    ).toBeVisible();
    await page.screenshot({
      path: `${output}/${name}-sucessor.png`,
      fullPage: true,
    });
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    journeys += 4;
    console.log(
      JSON.stringify({
        profile: name,
        journeys: 4,
        api: "real local",
        database: "MySQL 5.7",
        errors: 0,
        externalRequests: 0,
      }),
    );
    await context.close();
  }
  console.log(
    JSON.stringify({ profiles: profiles.length, journeys, success: true }),
  );
} finally {
  await browser.close();
}
