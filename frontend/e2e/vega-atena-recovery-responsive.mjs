import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
assert.ok(
  new URL(baseUrl).hostname === "127.0.0.1",
  "Use somente o frontend local",
);
const path =
  "/products/4/value-chain-history/processes/67/activities?learningCycleId=2&chainId=14#activity-marketStrategy";
const processName = "Estratégia, economia e protótipo privado do PDE";

// Simula exclusivamente os contratos HTTP locais; não registra tarefas ou métricas produtivas.
function history(state) {
  const completed = state === "COMPLETED";
  const retryable = ["BLOCKED", "CANCELLED"].includes(state);
  const activities = [
    ["marketStrategy", "Selecionar para protótipo privado", "Atena"],
    ["economics", "Limitar economia da validação", "Plutus"],
    ["productArchitecture", "Projetar protótipo e harness PDE", "Dédalo"],
  ].map(([activityId, activityName, activityOwnerName], index) => ({
    activityDefinitionId: 200 + index,
    activityId,
    activityName,
    activityOwnerName,
    activityObjective:
      "Planejar o sucessor com aprendizado preservado e limites próprios.",
    sequenceNumber: index + 1,
    selectedVersionActivity: true,
    operationalState: index === 0 ? state : "NOT_STARTED",
    stateReason:
      index === 0 ? `Atena: ${state}` : "Aguardando predecessora aprovada.",
    objectiveAchieved: index === 0 && completed,
    stateEvidence: index === 0 ? "DIRECT" : "NOT_RECORDED",
    executionRequestAvailable: index === 0 ? retryable : true,
    executionControl: {
      executorType: "AGENT",
      interactionType: "COMMAND",
      actionLabel:
        index === 0 && retryable ? "Reiniciar tarefa" : "Executar atividade",
      description:
        "Abre todas as tarefas responsáveis no mesmo ciclo auditável.",
      actionAvailable: index === 0 ? retryable : true,
      availabilityReason:
        index === 0
          ? `Atena: ${state}`
          : "Solicitação disponível; consumo depende das predecessoras.",
      confirmationRequired: false,
      requirements: [],
    },
    taskCount: index === 0 ? 1 : 0,
    tasks:
      index === 0
        ? [
            {
              taskId: 358,
              processDefinitionId: 67,
              processVersionNumber: 7,
              title:
                "Vega · experimento #92 · estratégia de melhoria (fixture local)",
              status: state,
              sourceReference: "experiment:92",
              assignedAgentKey: "experiment-strategist",
              assignedAgentNickname: "Atena",
              createdAt: "2026-09-09T20:00:00Z",
              startedAt: "2026-09-09T20:00:49Z",
              finishedAt: completed ? "2026-09-09T20:03:40Z" : null,
              modelCode: "fixture-atena-no-network",
              executionMode: "MODEL",
              reasoningEffort: "high",
              inputTokens: 100,
              cachedInputTokens: 20,
              outputTokens: 10,
              costEstimationStatus: "NOT_REPORTED",
              agentPromptPart: "Atena: planeje uma hipótese testável.",
              activityPromptPart:
                "Ciclo #2; experimento #92; aprendizado do #91.",
              promptSent:
                "Atena: planeje uma hipótese testável.\nCiclo #2; experimento #92; aprendizado do #91.",
              comments: completed
                ? '{"decision":"APPROVE","fixture":true}'
                : null,
              blockerGuidance:
                state === "BLOCKED"
                  ? {
                      category: "TECHNICAL_FAILURE",
                      recommendedAction:
                        "Corrija a falha técnica e reinicie a atividade de Atena pelo BPM.",
                      helpLinks: [{ label: "Tarefas", url: "/agent-tasks" }],
                    }
                  : null,
            },
          ]
        : [],
  }));
  return {
    productId: 4,
    productName: "Vega — fixture local",
    productInternalName: "Vega",
    selectedProcessDefinitionId: 67,
    processCode: "pde-commercial-plan-offer",
    processName,
    selectedProcessVersionNumber: 7,
    selectedProcessStatus: "PUBLISHED",
    currentExecutionReference: "experiment:92",
    operationalState: completed ? "IN_PROGRESS" : state,
    objectiveAchieved: false,
    selectedActivityCount: 3,
    completedActivityCount: completed ? 1 : 0,
    remainingActivityCount: completed ? 2 : 3,
    blockedActivityCount: state === "BLOCKED" ? 1 : 0,
    currentActivityId: completed ? "economics" : "marketStrategy",
    currentActivityName: completed
      ? activities[1].activityName
      : activities[0].activityName,
    currentActivityState: completed ? "NOT_STARTED" : state,
    currentActivityStateReason: completed
      ? "Estratégia concluída; validar limites econômicos."
      : "Auditoria de Atena.",
    activityCount: 3,
    activitiesWithTasksCount: 1,
    uniqueTaskCount: 1,
    knownEstimatedCostUsd: 0,
    costCoverage: "NOT_REPORTED",
    activities,
  };
}

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN,
});
try {
  for (const [profile, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iPhone-15-Pro", devices["iPhone 15 Pro"]],
    ["Pixel-7", devices["Pixel 7"]],
  ]) {
    let state = "IN_PROGRESS";
    const posts = [];
    const errors = [];
    const context = await browser.newContext(options);
    const page = await context.newPage();
    page.on("pageerror", (error) => errors.push(error.message));
    // Intercepta somente endpoints; módulos /src/api do Vite precisam continuar carregando.
    await context.route(`${new URL(baseUrl).origin}/api/**`, async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname.endsWith("/activity-executions")) {
        assert.equal(url.searchParams.get("learningCycleId"), "2");
        assert.equal(url.searchParams.get("chainId"), "14");
        return route.fulfill({ json: history(state) });
      }
      if (request.method() === "POST") {
        assert.match(
          url.pathname,
          /^\/api\/business-processes\/67\/products\/4\/activities\/(marketStrategy|economics)\/execution-requests$/,
        );
        assert.equal(url.searchParams.get("learningCycleId"), "2");
        posts.push(url.pathname);
        state = "IN_PROGRESS";
        return route.fulfill({
          json: {
            message: "Tarefa local solicitada",
            createdTaskCount: 1,
            reusedTaskCount: 0,
            tasks: [],
          },
        });
      }
      if (url.pathname === "/api/products/value-chain-positions/4") {
        return route.fulfill({
          json: {
            productId: 4,
            processDefinitionId: 67,
            sequenceNumber: 2,
            processName,
            processMeasurements: [],
          },
        });
      }
      return route.fulfill({
        status: 404,
        json: { message: "Contrato fora desta fixture" },
      });
    });
    await page.goto(`${baseUrl}${path}`);
    await expect(
      page.getByRole("heading", { name: `Vega · Processo 2 — ${processName}` }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Voltar ao ciclo #2" }),
    ).toBeVisible();
    await expect(
      page
        .locator("#activity-marketStrategy")
        .getByRole("button", { name: "Reiniciar tarefa" }),
    ).toHaveCount(0);

    // Bloqueio e cancelamento preservados liberam nova tentativa; trabalho ativo não é duplicado.
    for (const previousState of ["BLOCKED", "CANCELLED"]) {
      state = previousState;
      await page.reload();
      await page
        .locator("#activity-marketStrategy")
        .getByRole("button", { name: "Reiniciar tarefa" })
        .click();
      await expect
        .poll(() => posts.length)
        .toBe(previousState === "BLOCKED" ? 1 : 2);
      await expect(
        page
          .locator("#activity-marketStrategy")
          .getByRole("button", { name: "Reiniciar tarefa" }),
      ).toHaveCount(0);
      await expect(
        page
          .locator("#activity-marketStrategy")
          .getByText("Atena: IN_PROGRESS", { exact: true }),
      ).toBeVisible();
    }

    // O backend pode aceitar solicitações antecipadas; só consome a fila após a predecessora.
    // Aqui a UI deve refletir a conclusão recebida e enviar o próximo comando no mesmo ciclo.
    state = "COMPLETED";
    await page.reload();
    const next = page.locator("#activity-economics");
    await expect(
      next.getByRole("button", { name: "Executar atividade" }),
    ).toBeVisible();
    await expect(next.getByText("Responsável: Plutus")).toBeVisible();
    await expect(
      page
        .locator("#activity-marketStrategy")
        .getByText("Atena: COMPLETED", { exact: true }),
    ).toBeVisible();
    await page.screenshot({
      path: `/tmp/vega358-recovery-${profile}.png`,
      fullPage: true,
    });
    await next.getByRole("button", { name: "Executar atividade" }).click();
    await expect.poll(() => posts.length).toBe(3);
    assert.ok(posts[2].includes("/economics/"));
    assert.deepEqual(errors, []);
    const width = await page.evaluate(() => [
      document.documentElement.scrollWidth,
      document.documentElement.clientWidth,
    ]);
    assert.ok(width[0] <= width[1] + 1, `${profile}: overflow horizontal`);
    await context.close();
    console.log(`PASS Vega recuperação e próxima atividade: ${profile}`);
  }
} finally {
  await browser.close();
}
