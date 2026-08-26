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

const experiment = {
  id: "88",
  nicheId: 1,
  hypothesisId: "2",
  name: "Rigel",
  hypothesis: "Experiência personalizada aumenta a intenção de compra.",
  creativeApproved: true,
  status: "PLANNED",
  platform: "DIRECT_ONE_TO_ONE",
  stage: "SALES",
  startDate: null,
  endDate: null,
  createdAt: "2026-08-20T10:00:00Z",
  updatedAt: "2026-08-25T18:00:00Z",
};

const processInstances = [
  {
    processDefinitionId: 9,
    processCode: "landing-page-generation",
    processVersionNumber: 2,
    sourceReference: "experiment:88",
    activities: [
      {
        activityInstanceId: 501,
        activityDefinitionId: 401,
        activityId: "html",
        activityName: "Construir HTML",
        objective: "Entregar HTML funcional e responsivo.",
        occurrenceNumber: 1,
        status: "COMPLETED",
        operationalState: "COMPLETED",
        stateReason: "Objetivo da atividade comprovado.",
        enteredAt: "2026-08-25T10:00:00Z",
        exitedAt: "2026-08-25T10:35:00Z",
        objectiveAchieved: true,
        knownCostUsd: 0.3,
        costCoverage: "COMPLETE",
        evidenceQuality: "DIRECT",
        tasks: [
          {
            taskId: 601,
            activityInstanceId: 501,
            attemptNumber: 1,
            activityName: "Construir HTML",
            agentKey: "landing-generator",
            agentNickname: "Dédalo",
            taskStatus: "BLOCKED",
            operationalState: "BLOCKED",
            stateReason: "Primeira tentativa reprovada.",
            estimatedCostUsd: 0.1,
            costEstimationStatus: "ESTIMATED",
          },
          {
            taskId: 602,
            activityInstanceId: 501,
            attemptNumber: 2,
            activityName: "Construir HTML",
            agentKey: "landing-generator",
            agentNickname: "Dédalo",
            taskStatus: "COMPLETED",
            operationalState: "COMPLETED",
            stateReason: "Correção aprovada.",
            estimatedCostUsd: 0.2,
            costEstimationStatus: "ESTIMATED",
          },
        ],
      },
      {
        activityInstanceId: 502,
        activityDefinitionId: 402,
        activityId: "review",
        activityName: "Avaliar percepção",
        objective: "Comprovar clareza e valor percebido.",
        occurrenceNumber: 1,
        status: "PENDING",
        operationalState: "WAITING_PREDECESSOR",
        stateReason: "Aguardando a atividade predecessora.",
        enteredAt: "2026-08-25T10:36:00Z",
        objectiveAchieved: false,
        costCoverage: "NOT_REPORTED",
        evidenceQuality: "DIRECT",
        tasks: [
          {
            taskId: 603,
            activityInstanceId: 502,
            attemptNumber: 1,
            activityName: "Avaliar percepção",
            agentKey: "customer-psychology",
            agentNickname: "Psique",
            taskStatus: "PENDING",
            operationalState: "WAITING_PREDECESSOR",
            stateReason: "Aguardando a atividade predecessora.",
            costEstimationStatus: "NOT_REPORTED",
          },
        ],
      },
    ],
    tasks: [],
    supersededLegacyTasks: [
      {
        taskId: 599,
        attemptNumber: 1,
        activityName: "Correção antiga",
        agentKey: "landing-generator",
        agentNickname: "Dédalo",
        taskStatus: "PENDING",
        operationalState: "SUPERSEDED_LEGACY",
        stateReason: "Tarefa legada substituída.",
        costEstimationStatus: "NOT_REPORTED",
      },
    ],
  },
];

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
      if (pathname === "/api/experiments/88") {
        await route.fulfill({ json: experiment });
      } else if (pathname === "/api/agent-tasks/process-instances") {
        await route.fulfill({ json: processInstances });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(`${baseUrl}/experiments/88`, {
      waitUntil: "domcontentloaded",
    });
    await page.getByRole("tab", { name: "Processo", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "landing-page-generation · v2" }),
    ).toBeVisible();
    await expect(page.getByText("Atividade 1. Construir HTML")).toBeVisible();
    await expect(
      page.getByText("Objetivo: Entregar HTML funcional e responsivo."),
    ).toBeVisible();
    await expect(page.getByText("Ocorrência #1").first()).toBeVisible();
    await expect(page.getByText(/US\$\s*0,3000/)).toBeVisible();
    await expect(page.getByText("Sim")).toBeVisible();
    await page.getByText("2 tarefa(s)/tentativa(s)").click();
    await expect(page.getByText("Tentativa 1 · tarefa #601")).toBeVisible();
    await expect(page.getByText("Tentativa 2 · tarefa #602")).toBeVisible();
    await expect(
      page.getByText(/1 tarefa\(s\) legada\(s\) substituída\(s\)/),
    ).toBeVisible();
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
      path: `/tmp/experiment-activity-instances-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Atividade, instância e tentativas aprovadas em desktop, iPhone 15 Pro e Pixel 7.",
);
