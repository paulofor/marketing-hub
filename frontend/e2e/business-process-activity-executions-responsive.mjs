import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

const executions = Array.from({ length: 4 }, (_, index) => ({
  taskId: 126 - index * 3,
  processDefinitionId: 22,
  processVersionNumber: 1,
  title: `Comprovar dor e demanda · rodada ${4 - index}`,
  status: "COMPLETED",
  sourceReference: `pde-opportunity:ready-ai-workflows:round-${4 - index}`,
  assignedAgentKey: "market-radar",
  assignedAgentNickname: "Argos",
  comments: JSON.stringify({
    decision: "APPROVE",
    signalPattern: "Pessoas valorizam uma solução pronta que reduza esforço.",
  }),
  evidenceJson: JSON.stringify({ validationRound: 4 - index, sources: 7 }),
  inputTokens: 2834,
  cachedInputTokens: 2304,
  outputTokens: 5861,
  estimatedCostUsd: 0.0134724,
  costEstimationStatus: "ESTIMATED",
  createdAt: `2026-08-20T21:4${index}:00Z`,
  startedAt: `2026-08-20T21:4${index}:00Z`,
  finishedAt: `2026-08-20T21:4${index + 1}:24Z`,
  modelCode: "gpt-5.4-mini-2026-03-17",
}));

const history = {
  selectedProcessDefinitionId: 37,
  processCode: "pde-opportunity-discovery",
  processName: "Descoberta e priorização da oportunidade PDE",
  selectedProcessVersionNumber: 4,
  selectedProcessStatus: "RETIRED",
  activityId: "evidence",
  activityName: "Comprovar dor e demanda",
  activityOwnerName: "Argos",
  executions,
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN,
});
try {
  for (const [profileName, contextOptions] of profiles) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const pathname = new URL(route.request().url()).pathname;
      if (pathname === "/api/business-processes/37/activities/evidence/executions") {
        await route.fulfill({ json: history });
      } else {
        await route.fulfill({ status: 404, json: { message: "not mocked" } });
      }
    });

    await page.goto(`${baseUrl}/business-processes/37/activities/evidence/executions`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", {
        name: "Descoberta e priorização da oportunidade PDE · Comprovar dor e demanda",
      }),
    ).toBeVisible();
    await expect(page.getByText("responsável: Argos")).toBeVisible();
    await expect(page.getByText(/Tarefa #126/)).toBeVisible();
    await expect(page.getByText("gpt-5.4-mini-2026-03-17").first()).toBeVisible();
    await expect(page.getByText("US$ 0.01347240").first()).toBeVisible();
    await expect(
      page.getByText("Prompt não registrado nesta execução legada.").first(),
    ).toBeVisible();
    await page.getByText("Visualizar JSON em árvore").first().click();
    await expect(page.getByText("signalPattern:").first()).toBeVisible();
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/business-process-activity-executions-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log("Execuções BPM aprovadas em desktop, iPhone 15 Pro e Pixel 7.");
