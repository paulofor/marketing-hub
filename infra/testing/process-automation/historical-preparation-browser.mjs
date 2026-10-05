import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const history = JSON.parse(
  await readFile(process.env.HISTORICAL_ACTIVITIES_FIXTURE, "utf8"),
);
const control = JSON.parse(
  await readFile(process.env.HISTORICAL_AUTOMATION_FIXTURE, "utf8"),
);
const workspace = process.env.HISTORICAL_PREFLIGHT_FIXTURE
  ? JSON.parse(await readFile(process.env.HISTORICAL_PREFLIGHT_FIXTURE, "utf8"))
  : null;
if (history.processCode === "experiment-homologation-activation") {
  assert(workspace, "Informe os contratos reais de run e preflight históricos.");
  assert.equal(
    `experiment:${workspace.runs[0].experimentId}`,
    history.currentExecutionReference,
  );
  assert.equal(workspace.runs[0].id, workspace.preflight.runId);
  assert.equal(workspace.preflight.canRenewTechnicalHomologation, false);
  assert(workspace.preflight.executionBlockReason);
}
const base = process.env.HISTORICAL_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["127.0.0.1", "localhost"].includes(new URL(base).hostname));
assert.equal(history.productId, control.productId);
assert.equal(history.selectedProcessDefinitionId, control.processDefinitionId);
assert.equal(history.currentExecutionReference, control.sourceReference);
assert.equal(history.completedActivityCount, control.completedActivities);
if (history.objectiveAchieved) assert.equal(control.status, "COMPLETED");
else {
  assert.equal(history.operationalState, "CLOSED");
  assert(["PAUSED", "CLOSED"].includes(control.status));
  assert.equal(control.canResume, false);
}
const provenActivity = process.env.HISTORICAL_PROVEN_ACTIVITY || "economics";
assert(
  history.activities.some(
    (activity) =>
      activity.activityId === provenActivity && activity.objectiveAchieved,
  ),
);
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output, "Informe destino segregado para as evidências locais.");
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const page = await browser.newPage(options);
    const errors = [];
    const mutations = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("request", (request) => {
      if (
        request.method() !== "GET" &&
        request.method() !== "OPTIONS" &&
        request.url().includes("/api/")
      )
        mutations.push(request.url());
    });
    await page.route("**/api/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith("/activity-executions"))
        return route.fulfill({ json: history });
      if (url.pathname.includes("/automation/v1"))
        return route.fulfill({ json: control });
      if (url.pathname.endsWith("/process-context"))
        return route.fulfill({ status: 204, body: "" });
      if (
        workspace &&
        url.pathname === `/api/experiments/${workspace.runs[0].experimentId}/runs`
      )
        return route.fulfill({ json: workspace.runs });
      if (
        workspace &&
        url.pathname === `/api/experiment-runs/${workspace.preflight.runId}/preflight`
      )
        return route.fulfill({ json: workspace.preflight });
      if (url.pathname.includes("video-review/summary"))
        return route.fulfill({ json: { humanDecisionRequired: 0 } });
      return route.fulfill({ json: [] });
    });
    await page.route("**/ws/**", (route) => route.abort());
    await page.goto(
      `${base}/products/${history.productId}/value-chain-history/processes/${history.selectedProcessDefinitionId}/activities?chainId=${control.chainId}&sourceReference=${encodeURIComponent(control.sourceReference)}#activity-${provenActivity}`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText(
        control.status === "COMPLETED"
          ? "Processo concluído"
          : control.status === "PAUSED"
            ? "Pausado"
            : "Encerrado com pendências",
        { exact: true },
      ),
    ).toBeVisible({
      timeout: 20000,
    });
    await expect(
      panel.getByRole("button", { name: "Retomar processo" }),
    ).toHaveCount(0);
    await expect(
      page.getByText(
        `${history.completedActivityCount} de ${history.selectedActivityCount} atividades concluídas`,
        { exact: true },
      ),
    ).toBeVisible();
    await expect(page.locator(`#activity-${provenActivity}`)).toContainText(
      "Concluída",
    );
    if (!history.objectiveAchieved) {
      await expect(panel).toContainText(control.currentActivityName);
      await expect(
        page.locator(`#activity-${history.currentActivityId}`),
      ).not.toContainText("Objetivo da atividade atingido");
    }
    if (workspace) {
      await expect(
        page
          .locator(`#activity-${provenActivity}`)
          .getByText("Status histórico do run", { exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Criar run", exact: true }),
      ).toHaveCount(0);
      await expect(
        page.getByRole("button", { name: "Rodar preflight", exact: true }),
      ).toHaveCount(0);
    }
    await page.getByText("Ver contexto completo", { exact: true }).click();
    const copiedContext = page.getByLabel("Contexto completo do processo");
    await expect(copiedContext).toContainText(
      `concluídas: ${history.completedActivityCount}`,
    );
    await expect(copiedContext).toContainText(
      `Objetivo do processo comprovado: ${history.objectiveAchieved ? "Sim" : "Não"}`,
    );
    assert(
      !(await page
        .getByText(/Atualize o plano financeiro vencido ou alterado/)
        .count()),
    );
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await page.screenshot({
      path: `${output}/${name}-historical-preparation.png`,
      fullPage: true,
    });
    await page.locator(`#activity-${provenActivity}`).screenshot({
      path: `${output}/${name}-historical-activity.png`,
    });
    await page.close();
    console.log(
      `PASS ${name}: ${history.completedActivityCount}/${history.selectedActivityCount} objetivos preservados e nenhuma tarefa ou chamada paga`,
    );
  }
} finally {
  await browser.close();
}
