import assert from "node:assert/strict";
import { readFile, mkdir } from "node:fs/promises";
import { chromium, devices, expect } from "@playwright/test";

// O teste de persistência Java produz o contrato de controle usado nesta navegação.
const control = JSON.parse(await readFile(process.env.CONTROL_FIXTURE, "utf8"));
assert.equal(control.status, "CLOSED");
const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:15173";
const output = process.env.EVIDENCE_DIR || "/tmp/mira-flow/visual";
await mkdir(output, { recursive: true });
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
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const errors = [],
      mutations = [];
    page.on("pageerror", (e) => errors.push(e.message));
    const { productId, processDefinitionId, chainId } = control;
    const navigation = {
      productId,
      chainDefinitionId: chainId,
      cycleId: 96031,
      experimentId: 96021,
      stage: "DECISION",
      status: "ADJUSTED",
      reason: "O ajuste já foi aprovado. O sucessor ainda não foi preparado.",
      url: `/business-process-chains/learning-cycles?productId=${productId}&chainId=${chainId}&cycleId=96031`,
    };
    const position = {
      productId,
      chainDefinitionId: chainId,
      resolutionStatus: "IDENTIFIED",
      processDefinitionId,
      processName: "Homologação técnica",
      sequenceNumber: 5,
      learningCycleNavigation: navigation,
    };
    const history = {
      productId,
      productName: "Produto sintético",
      productInternalName: "QA local",
      selectedProcessDefinitionId: processDefinitionId,
      processCode: "experiment-homologation-activation",
      processName: "Homologação técnica",
      selectedProcessVersionNumber: 1,
      selectedProcessStatus: "PUBLISHED",
      currentExecutionReference: control.sourceReference,
      operationalState: "CLOSED",
      objectiveAchieved: false,
      selectedActivityCount: 4,
      completedActivityCount: 3,
      remainingActivityCount: 1,
      blockedActivityCount: 0,
      currentActivityId: "financialGuardrails",
      currentActivityName: "Validar limites financeiros persistidos",
      currentActivityState: "NOT_STARTED",
      currentActivityStateReason: control.reason,
      activityCount: 4,
      activitiesWithTasksCount: 0,
      uniqueTaskCount: 0,
      knownEstimatedCostUsd: 0.75,
      costCoverage: "COMPLETE",
      chainPosition: { sequenceLabel: "5.4" },
      activities: [
        "surfaces",
        "transaction",
        "measurement",
        "financialGuardrails",
      ].map((activityId, index) => ({
        activityDefinitionId: 96041 + index,
        activityId,
        activityName:
          index === 3
            ? "Validar limites financeiros persistidos"
            : `Prova ${index + 1}`,
        activityOwnerName: "Backend",
        activityObjective: "Prova QA local",
        sequenceNumber: index + 1,
        selectedVersionActivity: true,
        operationalState: index < 3 ? "COMPLETED" : "NOT_STARTED",
        objectiveAchieved: index < 3,
        stateReason: control.reason,
        stateEvidence: "REUSED_DIRECT",
        taskCount: 0,
        tasks: [],
        executionControl: {
          executorType: "BACKEND",
          interactionType: "WORKSPACE",
          actionAvailable: false,
          actionLabel: "Comprovar atividade",
          availabilityReason: control.reason,
          requirements: [],
        },
      })),
    };
    await page.route("**/api/**", async (route) => {
      if (route.request().method() !== "GET") {
        mutations.push(route.request().url());
        return route.abort();
      }
      const path = new URL(route.request().url()).pathname;
      if (!path.startsWith("/api/")) return route.continue();
      let data = [];
      if (path.endsWith("process-context")) data = null;
      if (path.endsWith("activity-executions")) data = history;
      if (path.endsWith("automation/v1")) data = control;
      if (path.endsWith(`/value-chain-positions/${productId}`)) data = position;
      return route.fulfill({ json: data });
    });
    await page.goto(
      `${base}/products/${productId}/value-chain-history/processes/${processDefinitionId}/activities?chainId=${chainId}`,
    );
    await page.waitForLoadState("networkidle");
    const run = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      run.getByText("Encerrado com pendências", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Histórico da tentativa encerrada" }),
    ).toBeVisible();
    assert.equal(
      await page
        .getByText("Ir para a atividade atual", { exact: true })
        .count(),
      0,
    );
    assert.equal(
      await page.getByText(/executada automaticamente pelo controle/).count(),
      0,
    );
    const links = page.getByRole("link", {
      name: "Ver próximo passo · ciclo #96031",
    });
    await expect(links).toHaveCount(3);
    for (const link of await links.all())
      await expect(link).toHaveAttribute("href", navigation.url);
    await page
      .locator("#activity-financialGuardrails")
      .scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-activity.png` });
    assert.ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
      "Sem overflow",
    );
    await links.last().click();
    await expect(page).toHaveURL(base + navigation.url);
    assert.deepEqual(mutations, []);
    assert.deepEqual(errors, []);
    console.log(`${name}: histórico → continuidade; sem mutações ou erros`);
    await context.close();
  }
} finally {
  await browser.close();
}
