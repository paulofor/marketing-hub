import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { chromium, devices, expect } from "@playwright/test";

const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:15173";
const output = process.env.EVIDENCE_DIR || "/tmp/process-agent-portrait";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});

try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const errors = [],
      mutations = [];
    let owner = "Plutus";
    let portrait = "/portraits/plutus.svg";
    const activityId = "financialGuardrails";
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("console", (message) => {
      if (
        message.type() === "error" &&
        /TypeError|ReferenceError/.test(message.text())
      ) {
        errors.push(message.text());
        console.error(message.text());
      }
    });
    await page.route("**/portraits/**", (route) => {
      if (route.request().url().endsWith("missing.png"))
        return route.fulfill({ status: 404, body: "" });
      return route.fulfill({
        contentType: "image/svg+xml",
        body: '<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96"><rect width="96" height="96" fill="#dce5f5"/><circle cx="48" cy="36" r="18" fill="#2563eb"/><path d="M16 96a32 32 0 0 1 64 0" fill="#2563eb"/></svg>',
      });
    });
    await page.route("**/api/**", async (route) => {
      if (route.request().method() !== "GET") {
        mutations.push(route.request().method());
        return route.abort();
      }
      const path = new URL(route.request().url()).pathname;
      let data = [];
      if (path.endsWith("process-context")) data = null;
      if (path.endsWith("automation/v1"))
        data = {
          id: 95041,
          productId: 95001,
          processDefinitionId: 95012,
          chainId: 95014,
          sourceReference: "experiment:95022",
          status: "WAITING_ACTIVITY",
          reason: "Atividade em execução pelo responsável.",
          currentActivityId: activityId,
          currentActivityName: "Conferir economia da oferta",
          currentOwnerName: owner,
          currentOwnerPortraitUrl: portrait,
          currentSequence: 2,
          totalActivities: 3,
          completedActivities: 1,
          remainingActivities: 2,
          omittedActivities: 0,
          completionPercentage: 33,
          canPause: true,
          canStart: false,
          canResume: false,
          revision: 1,
        };
      if (path.endsWith("activity-executions"))
        data = {
          productId: 95001,
          productName: "Produto de teste",
          activityCount: 1,
          activitiesWithTasksCount: 0,
          uniqueTaskCount: 0,
          knownEstimatedCostUsd: 0,
          costCoverage: "NO_EXECUTIONS",
          productInternalName: "QA local",
          selectedProcessDefinitionId: 95012,
          processName: "Planejamento da oferta",
          processCode: "local-test",
          selectedProcessVersionNumber: 1,
          selectedProcessStatus: "PUBLISHED",
          currentExecutionReference: "experiment:95022",
          operationalState: "IN_PROGRESS",
          selectedActivityCount: 1,
          completedActivityCount: 0,
          remainingActivityCount: 1,
          currentActivityId: activityId,
          currentActivityName: "Conferir economia da oferta",
          currentActivityState: "IN_PROGRESS",
          activities: [
            {
              activityDefinitionId: 95015,
              activityId,
              activityName: "Conferir economia da oferta",
              activityOwnerName: owner,
              activityObjective: "Conferir projeções locais.",
              sequenceNumber: 2,
              selectedVersionActivity: true,
              operationalState: "IN_PROGRESS",
              taskCount: 0,
              tasks: [],
              executionControl: {
                executorType: "AGENT",
                interactionType: "COMMAND",
                actionLabel: "Executar atividade",
                actionAvailable: false,
                requirements: [],
              },
            },
          ],
        };
      return route.fulfill({ json: data });
    });
    await page.goto(
      `${base}/products/95001/value-chain-history/processes/95012/activities?chainId=95014&sourceReference=experiment%3A95022#process-execution`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    const plutus = panel.getByRole("img", {
      name: "Figura mitológica de Plutus",
    });
    await expect(plutus).toBeVisible();
    await expect(plutus).toHaveJSProperty("naturalWidth", 96);
    const box = await plutus.boundingBox();
    assert.equal(box.width, 48);
    assert.equal(box.height, 48);
    await page.screenshot({ path: `${output}/${name}.png`, fullPage: true });
    owner = "Psique";
    portrait = "/portraits/psique.svg";
    await expect(
      panel.getByRole("img", { name: "Figura mitológica de Psique" }),
    ).toBeVisible({ timeout: 12000 });
    await expect(plutus).toHaveCount(0);
    portrait = "/portraits/missing.png";
    await expect(
      panel.locator(".product-process-automation__portrait"),
    ).toHaveCount(0, { timeout: 12000 });
    await expect(
      panel.getByRole("link", { name: /Conferir economia da oferta · Psique/ }),
    ).toBeVisible();
    owner = "Backend";
    portrait = null;
    const link = panel.getByRole("link", {
      name: /Conferir economia da oferta · Backend/,
    });
    await expect(link).toBeVisible({ timeout: 12000 });
    await expect(
      panel.locator(".product-process-automation__portrait"),
    ).toHaveCount(0);
    assert.ok(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
      `${name}: overflow`,
    );
    await link.click();
    await expect(page).toHaveURL(new RegExp(`#activity-${activityId}$`));
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    console.log(`${name}: retrato, troca, falha, backend e link aprovados`);
    await context.close();
  }
} finally {
  await browser.close();
}
