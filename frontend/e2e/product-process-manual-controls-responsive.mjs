import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 900 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

function activity({ id, name, owner, sequence, state, achieved, control }) {
  return {
    activityDefinitionId: 586 + sequence,
    activityId: id,
    activityName: name,
    activityObjective: `Objetivo auditável de ${name}.`,
    activityOwnerName: owner,
    sequenceNumber: sequence,
    selectedVersionActivity: true,
    operationalState: state,
    stateReason: achieved
      ? "Objetivo comprovado na instância BPM."
      : control.availabilityReason,
    objectiveAchieved: achieved,
    stateEvidence: achieved ? "DIRECT" : "NOT_RECORDED",
    taskCount: 0,
    tasks: [],
    executionRequestAvailable: control.actionAvailable,
    executionRequestReason: control.availabilityReason,
    executionControl: control,
  };
}

function history(phase) {
  const preflightCompleted = phase !== "PREFLIGHT";
  const authorizationCompleted = phase === "AUTHORIZED";
  return {
    productId: 9,
    productName: "Kit WhatsApp Pronto",
    productInternalName: "Rigel",
    commercialPlanId: 4,
    commercialPlanName: "Plano comercial do Rigel",
    selectedProcessDefinitionId: 56,
    processCode: "pde-commercial-homologation-activation",
    processName: "Homologação comercial e ativação do PDE",
    selectedProcessVersionNumber: 4,
    selectedProcessStatus: "PUBLISHED",
    currentExecutionReference: "experiment:89",
    operationalState: authorizationCompleted ? "COMPLETED" : "PENDING",
    objectiveAchieved: authorizationCompleted,
    selectedActivityCount: 4,
    completedActivityCount: authorizationCompleted
      ? 4
      : preflightCompleted
        ? 3
        : 2,
    remainingActivityCount: authorizationCompleted
      ? 0
      : preflightCompleted
        ? 1
        : 2,
    blockedActivityCount: 0,
    currentActivityId: authorizationCompleted
      ? "authorization"
      : preflightCompleted
        ? "authorization"
        : "preflight",
    currentActivityName: authorizationCompleted
      ? "Autorizar ativação e orçamento"
      : preflightCompleted
        ? "Autorizar ativação e orçamento"
        : "Executar preflight técnico",
    currentActivityState: authorizationCompleted ? "COMPLETED" : "NOT_STARTED",
    currentActivityStateReason: authorizationCompleted
      ? "Decisão humana registrada."
      : "Aguardando comando oficial.",
    activityCount: 4,
    activitiesWithTasksCount: 0,
    uniqueTaskCount: 0,
    knownEstimatedCostUsd: 0,
    costCoverage: "NO_EXECUTIONS",
    activities: [
      activity({
        id: "humanExperienceReview",
        name: "Validar experiência humana da jornada",
        owner: "Psique",
        sequence: 1,
        state: "COMPLETED",
        achieved: true,
        control: {
          executorType: "AGENT",
          interactionType: "COMMAND",
          actionLabel: "Executar atividade",
          description: "Abre a tarefa oficial do agente.",
          actionAvailable: false,
          availabilityReason:
            "O objetivo da atividade já foi atingido neste ciclo.",
          confirmationRequired: false,
          requirements: [],
        },
      }),
      activity({
        id: "commercialIntegrityReview",
        name: "Validar integridade comercial da jornada",
        owner: "Têmis",
        sequence: 2,
        state: "COMPLETED",
        achieved: true,
        control: {
          executorType: "AGENT",
          interactionType: "COMMAND",
          actionLabel: "Executar atividade",
          description: "Abre a tarefa oficial do agente.",
          actionAvailable: false,
          availabilityReason:
            "O objetivo da atividade já foi atingido neste ciclo.",
          confirmationRequired: false,
          requirements: [],
        },
      }),
      activity({
        id: "preflight",
        name: "Executar preflight técnico",
        owner: "Backend",
        sequence: 3,
        state: preflightCompleted ? "COMPLETED" : "NOT_STARTED",
        achieved: preflightCompleted,
        control: {
          executorType: "BACKEND",
          interactionType: "WORKSPACE",
          actionLabel: "Criar e executar preflight",
          description:
            "Cria uma única tentativa, executa gates e preserva as evidências no run oficial.",
          actionAvailable: !preflightCompleted,
          availabilityReason: preflightCompleted
            ? "O run produtivo foi reconciliado com a atividade."
            : "As revisões estão concluídas; o backend pode executar o preflight.",
          confirmationRequired: false,
          workspaceCode: "EXPERIMENT_PREFLIGHT",
          workspaceReferenceId: 89,
          requirements: [
            {
              code: "PREDECESSORS_COMPLETED",
              title: "Revisões anteriores concluídas",
              satisfied: true,
              detail: "Psique e Têmis concluíram seus pareceres.",
              recommendation: "Preserve as evidências aprovadas.",
            },
          ],
        },
      }),
      activity({
        id: "authorization",
        name: "Autorizar ativação e orçamento",
        owner: "Operador humano",
        sequence: 4,
        state: authorizationCompleted ? "COMPLETED" : "NOT_STARTED",
        achieved: authorizationCompleted,
        control: {
          executorType: "HUMAN",
          interactionType: "APPROVAL",
          actionLabel: "Li, entendi e autorizo",
          description:
            "Revise o resumo e autorize com um único comando, sem criar campanha paga.",
          actionAvailable: preflightCompleted && !authorizationCompleted,
          availabilityReason: preflightCompleted
            ? "Preflight, requisitos comerciais e teto financeiro estão prontos."
            : "Conclua primeiro a atividade Executar preflight técnico.",
          confirmationRequired: true,
          confirmationTitle: "Revise e autorize",
          confirmationMessage:
            "O experimento Rigel direto está pronto, com amostra de 15 contatos e teto total de R$ 540,00.",
          confirmationToken:
            "CONFIRM:pde-commercial-homologation-activation:authorization",
          workspaceCode: "EXPERIMENT_ACTIVATION",
          workspaceReferenceId: 89,
          decisionMode: "REVIEW_AND_ACCEPT",
          auditEvidenceReference:
            "experiment:89; experiment-run:12/run-number:1; commercial-plan:4",
          requirements: [
            {
              code: "PREFLIGHT_APPROVED",
              title: "Preflight aprovado",
              satisfied: preflightCompleted,
              detail: preflightCompleted
                ? "Run produtivo aprovado."
                : "O run produtivo ainda não foi concluído.",
              recommendation: preflightCompleted
                ? "Preserve as evidências."
                : "Execute o preflight pela atividade anterior.",
            },
            {
              code: "BUDGET_LIMIT_DEFINED",
              title: "Teto financeiro definido",
              satisfied: true,
              detail: "O plano limita a operação a R$ 540,00.",
              recommendation: "Não ultrapasse o teto sem nova decisão humana.",
            },
          ],
        },
      }),
    ],
  };
}

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [profileName, contextOptions] of profiles) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    const consoleErrors = [];
    const commands = [];
    const unmatchedRequests = [];
    let phase = "PREFLIGHT";
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    await page.route("**/api/**", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (pathname === "/api/ops-monitor/v1/modules/availability") {
        await route.fulfill({ json: [] });
        return;
      }
      if (pathname === "/api/facebook/configuration-status") {
        await route.fulfill({ json: { configured: false } });
        return;
      }
      if (pathname === "/api/creatives/video-review") {
        await route.fulfill({ json: [] });
        return;
      }
      if (
        pathname ===
          "/api/business-processes/56/products/9/activity-executions" &&
        request.method() === "GET"
      ) {
        await route.fulfill({ json: history(phase) });
        return;
      }
      if (
        pathname === "/api/experiments/89/runs" &&
        request.method() === "GET"
      ) {
        await route.fulfill({
          json:
            phase === "PREFLIGHT"
              ? []
              : [
                  {
                    id: 12,
                    experimentId: 89,
                    runNumber: 1,
                    mode: "PRODUCTION",
                    status: "READY_TO_PUBLISH",
                    evidenceValidity: "COMMERCIALLY_VALID",
                    dataQualityStatus: "VALID",
                    stopPolicy: "MANUAL_ONLY",
                    requestedAt: "2026-08-30T15:00:00Z",
                    preflightStartedAt: "2026-08-30T15:01:00Z",
                    preflightCompletedAt: "2026-08-30T15:02:00Z",
                  },
                ],
        });
        return;
      }
      if (
        pathname === "/api/experiment-runs/12/preflight" &&
        request.method() === "GET"
      ) {
        await route.fulfill({
          json: {
            runId: 12,
            runStatus: "READY_TO_PUBLISH",
            hasBlockers: false,
            gates: [],
          },
        });
        return;
      }
      if (
        pathname ===
          "/api/business-processes/56/products/9/activities/preflight/execution-requests" &&
        request.method() === "POST"
      ) {
        commands.push({ activityId: "preflight", body: request.postData() });
        await new Promise((resolve) => setTimeout(resolve, 120));
        phase = "AUTHORIZATION";
        await route.fulfill({
          json: {
            processDefinitionId: 56,
            productId: 9,
            activityId: "preflight",
            sourceReference: "experiment:89",
            tasks: [],
            operationalState: "COMPLETED",
            objectiveAchieved: true,
            message: "Preflight reconciliado.",
          },
        });
        return;
      }
      if (
        pathname ===
          "/api/business-processes/56/products/9/activities/authorization/execution-requests" &&
        request.method() === "POST"
      ) {
        commands.push({
          activityId: "authorization",
          body: request.postDataJSON(),
        });
        await new Promise((resolve) => setTimeout(resolve, 120));
        phase = "AUTHORIZED";
        await route.fulfill({
          json: {
            processDefinitionId: 56,
            productId: 9,
            activityId: "authorization",
            sourceReference: "experiment:89",
            tasks: [],
            operationalState: "COMPLETED",
            objectiveAchieved: true,
            message: "Decisão registrada.",
          },
        });
        return;
      }
      unmatchedRequests.push(`${request.method()} ${pathname}`);
      await route.fulfill({ status: 404, json: { message: "not mocked" } });
    });

    await page.goto(
      `${baseUrl}/products/9/value-chain-history/processes/56/activities`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByRole("heading", {
        name: "Rigel · Homologação comercial e ativação do PDE",
      }),
    ).toBeVisible();
    await expect(page.getByText("Como executar")).toHaveCount(4);
    await expect(
      page
        .getByLabel("Execução de Autorizar ativação e orçamento")
        .getByText("Conclua primeiro a atividade Executar preflight técnico."),
    ).toBeVisible();

    const preflightButton = page.getByRole("button", {
      name: "Criar e executar preflight",
    });
    await preflightButton.click();
    await expect(
      page
        .getByRole("heading", {
          name: "Autorizar ativação e orçamento",
        })
        .last(),
    ).toBeVisible();
    await expect(page.getByText("#1", { exact: true })).toBeVisible();
    assert.equal(
      commands.length,
      1,
      `${profileName}: comando backend duplicado`,
    );
    assert.equal(
      commands[0].body,
      null,
      `${profileName}: backend recebeu corpo artificial`,
    );

    const authorizationButton = page.getByRole("button", {
      name: "Li, entendi e autorizo",
    });
    await expect(authorizationButton).toBeEnabled();
    await expect(page.getByText("2/2 verificações prontas")).toBeVisible();
    await expect(page.getByText(/amostra de 15 contatos/)).toBeVisible();
    await expect(page.getByLabel(/Responsável/)).toHaveCount(0);
    await expect(page.getByLabel(/Justificativa/)).toHaveCount(0);
    await expect(page.getByLabel(/Evidência auditável/)).toHaveCount(0);

    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/product-process-manual-controls-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });

    await authorizationButton.click();
    await expect(
      page.getByText("Decisão humana registrada.", { exact: true }),
    ).toBeVisible();
    assert.equal(
      commands.length,
      2,
      `${profileName}: decisão humana duplicada`,
    );
    assert.deepEqual(commands[1].body, {
      decision: "APPROVE",
      confirmationToken:
        "CONFIRM:pde-commercial-homologation-activation:authorization",
    });
    assert.deepEqual(
      unmatchedRequests,
      [],
      `${profileName}: requisições sem contrato simulado`,
    );
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    assert.deepEqual(consoleErrors, [], `${profileName}: erros no console`);
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Controles backend e humanos aprovados em desktop, iPhone 15 Pro e Pixel 7.",
);
