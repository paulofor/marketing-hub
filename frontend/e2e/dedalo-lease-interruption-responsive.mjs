import assert from "node:assert/strict";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const text =
  "O acompanhamento da execução foi interrompido. Uma nova chamada foi bloqueada para evitar cobrança duplicada. É necessário conferir o recebimento e o retorno original antes de retomar; o consumo ainda não foi confirmado.";
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const profile of [
    { viewport: { width: 1440, height: 1000 } },
    devices["iPhone 15 Pro"],
    devices["Pixel 7"],
  ]) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", async (route) => {
      if (!new URL(route.request().url()).pathname.startsWith("/api/"))
        return route.continue();
      if (
        new URL(route.request().url()).pathname.endsWith(
          "/activities/journey/executions",
        )
      ) {
        await route.fulfill({
          json: {
            selectedProcessDefinitionId: 8001,
            processCode: "pde-construction-approval",
            processName: "Construção sintética",
            selectedProcessVersionNumber: 1,
            selectedProcessStatus: "PUBLISHED",
            activityId: "journey",
            activityName: "Jornada",
            activityOwnerName: "Dédalo",
            executions: [
              {
                taskId: 8002,
                processDefinitionId: 8001,
                processVersionNumber: 1,
                title: "Jornada sintética",
                status: "BLOCKED",
                sourceReference: "experiment:8003",
                assignedAgentKey: "landing-generator",
                assignedAgentNickname: "Dédalo",
                humanSummary: {
                  text,
                  source: "DETERMINISTIC",
                  decision: "BLOCKED",
                },
                executionError:
                  "DEDALO_LEASE_RECOVERY_EXHAUSTED|diagnóstico sintético",
                executionMode: "NOT_STARTED",
                blockerGuidance: {
                  category: "TECHNICAL_FAILURE",
                  recommendedAction: text,
                  helpLinks: [],
                },
                costEstimationStatus: "NOT_REPORTED",
                createdAt: "2026-01-01T00:00:00Z",
              },
            ],
          },
        });
      } else await route.fulfill({ status: 404, json: {} });
    });
    await page.goto(
      `${base}/business-processes/8001/activities/journey/executions`,
      { waitUntil: "domcontentloaded", timeout: 120000 },
    );
    await expect(page.getByLabel("Resumo do parecer")).toHaveText(text, {
      timeout: 60000,
    });
    await expect(page.getByLabel("Resumo do parecer")).toBeVisible();
    await expect(
      page.locator(".alert-danger").filter({ hasText: "O que fazer:" }),
    ).toContainText(text);
    assert(
      !(await page.locator("body").innerText()).includes(
        "DEDALO_LEASE_RECOVERY_EXHAUSTED",
      ),
    );
    await expect(
      page.getByText("Sem início de modelo registrado", { exact: true }),
    ).toBeVisible();
    assert(
      !(await page.locator("body").innerText()).includes(
        "antes de enviar um prompt",
      ),
    );
    assert.deepEqual(errors, []);
    console.log(
      `PASS: mensagem de interrupção sem código interno em ${profile.viewport.width}px`,
    );
    await context.close();
  }
} finally {
  await browser.close();
}
