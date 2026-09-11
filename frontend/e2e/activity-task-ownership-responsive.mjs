import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const output = process.env.EVIDENCE_DIR ?? "../artifacts/task385/browser";
const path = "/products/900004/value-chain-history/processes/70/activities";
const search = "?learningCycleId=900002&chainId=900014";
const source = "experiment:900092";
const correctionName = "Corrigir o protótipo a partir do parecer";
const targets = [
  ["psiqueAdherent", "Psique · cenário aderente", "Psique", 7],
  ["psiqueRecovery", "Psique · fricção e recuperação", "Psique", 8],
  ["psiqueSafety", "Psique · limite e segurança", "Psique", 9],
  [
    "commercialIntegrityReview",
    "Revisar integridade da validação multiagente",
    "Têmis",
    10,
  ],
];

// Os dados e callbacks abaixo são sintéticos; todas as APIs ficam interceptadas no navegador.
function task(id, status, agent = "Dédalo") {
  return {
    taskId: id,
    status,
    assignedAgentNickname: agent,
    assignedAgentKey:
      agent === "Dédalo" ? "landing-generator" : "customer-agent",
    processDefinitionId: 70,
    processVersionNumber: 8,
    sourceReference: source,
    title: "Tarefa sintética de homologação",
    productInternalName: "Vega QA",
    createdAt: "2026-09-11T03:37:31Z",
    startedAt: "2026-09-11T03:38:00Z",
    finishedAt: ["BLOCKED", "COMPLETED"].includes(status)
      ? "2026-09-11T03:40:00Z"
      : null,
    executionError: status === "BLOCKED" ? "Motivo sintético preservado" : null,
    costEstimationStatus: "NOT_APPLICABLE",
    executionMode: "DETERMINISTIC",
    estimatedCostUsd: 0,
    comments: "Resultado sintético; sem efeito comercial.",
  };
}

function group(id, name, agent, sequence, state, tasks, available) {
  return {
    activityDefinitionId: 700 + sequence,
    activityId: id,
    activityName: name,
    activityOwnerName: agent,
    sequenceNumber: sequence,
    selectedVersionActivity: true,
    operationalState: state,
    objectiveAchieved: state === "COMPLETED",
    activityObjective: "Comprovar o objetivo próprio com evidência sintética.",
    stateReason: tasks.length
      ? "Estado persistido da própria atividade."
      : "Nenhuma tarefa nesta atividade.",
    stateEvidence: tasks.length ? "DIRECT" : "NOT_RECORDED",
    tasks,
    taskCount: tasks.length,
    executionRequestAvailable: available,
    executionRequestReason: "Orientação sintética do backend.",
    executionControl: {
      executorType: "AGENT",
      interactionType: "COMMAND",
      actionAvailable: available,
      actionLabel:
        id === "prototypeCorrection"
          ? "Criar tarefa de correção"
          : "Executar atividade",
      description: "Crie e acompanhe a tarefa desta atividade.",
      availabilityReason: available
        ? "Nova tentativa disponível."
        : "Aguardando requisito da atividade.",
      confirmationRequired: false,
      requirements: [],
    },
  };
}

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
await mkdir(output, { recursive: true });
const results = [];
try {
  for (const [profile, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const pageErrors = [],
      unexpectedRequests = [],
      commands = [];
    try {
      let current = task(900385, "BLOCKED"),
        prior = [],
        revision = 1;
      let failCommand = true,
        failProgress = false,
        progressFailures = 0;
      page.on("pageerror", (error) => pageErrors.push(error.message));
      function history() {
        const recovery = {
          activityId: "prototypeCorrection",
          activityName: correctionName,
          sequenceNumber: 6,
          ownerName: "Dédalo",
          actionLabel: "Criar tarefa de correção",
          actionAvailable: current.status === "BLOCKED",
          operationalState: current.status,
          objectiveAchieved: current.status === "COMPLETED",
          availabilityReason: "Correção necessária.",
          latestTask: { ...current, agentName: "Dédalo" },
        };
        const activities = [
          group(
            "prototypeCorrection",
            correctionName,
            "Dédalo",
            6,
            current.status,
            [current, ...prior],
            current.status === "BLOCKED",
          ),
          ...targets.map(([id, name, agent, sequence]) => ({
            ...group(
              id,
              name,
              agent,
              sequence,
              id === "psiqueAdherent" ? "BLOCKED" : "NOT_STARTED",
              id === "psiqueAdherent"
                ? [task(900384, "BLOCKED", "Psique")]
                : [],
              false,
            ),
            recoveryAction: current.status === "COMPLETED" ? null : recovery,
          })),
        ];
        return {
          productId: 900004,
          productName: "Vega QA",
          productInternalName: "Vega QA",
          selectedProcessDefinitionId: 70,
          selectedProcessVersionNumber: 8,
          selectedProcessStatus: "PUBLISHED",
          processCode: "pde-construction-approval",
          processName: "Protótipo, validação multiagente e aprovação do PDE",
          currentExecutionReference: source,
          operationalState: "BLOCKED",
          objectiveAchieved: false,
          selectedActivityCount: 5,
          activityCount: 5,
          completedActivityCount: current.status === "COMPLETED" ? 1 : 0,
          remainingActivityCount: current.status === "COMPLETED" ? 4 : 5,
          blockedActivityCount: current.status === "BLOCKED" ? 2 : 1,
          activitiesWithTasksCount: 2,
          uniqueTaskCount: prior.length + 2,
          knownEstimatedCostUsd: 0,
          costCoverage: "COMPLETE",
          currentActivityId: "prototypeCorrection",
          currentActivityName: correctionName,
          currentActivityState: current.status,
          currentActivityStateReason: "Retorno registrado no ciclo de teste.",
          activities,
        };
      }
      const position = {
        productId: 900004,
        productInternalName: "Vega QA",
        processDefinitionId: 70,
        processCode: "pde-construction-approval",
        sequenceNumber: 3,
        chainDefinitionId: 900014,
        processMeasurements: [],
        resolutionStatus: "IDENTIFIED",
      };
      const cycle = {
        cycleId: 900002,
        cycleNumber: 2,
        experimentId: 900092,
        chainDefinitionId: 900014,
        productVersion: "vega-qa-v9",
        stageLabel: "Corrigir e homologar",
        status: "OPEN",
        hypothesis: "Hipótese sintética de primeiro resultado útil",
        mainChange: "Menor esforço",
        cycleUrl: "/business-process-chains/learning-cycles?cycleId=900002",
        previousLearning: [
          {
            cycleId: 900001,
            experimentId: 900091,
            action: "ADJUST",
            learning: "Aprendizado sintético preservado",
            limitation: "Não comprova venda ou causalidade.",
            evidenceReference: "synthetic://cycle-1",
          },
        ],
        nextWork: null,
      };
      await page.route("**/*", async (route) => {
        const request = route.request(),
          url = new URL(request.url());
        if (url.origin !== new URL(baseUrl).origin) {
          unexpectedRequests.push(request.url());
          return route.abort();
        }
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (
          url.pathname === "/api/creatives/video-review" ||
          url.pathname === "/api/ops-monitor/v1/modules/availability"
        )
          return route.fulfill({ json: [] });
        if (url.pathname === "/api/facebook/configuration-status")
          return route.fulfill({ json: { configured: true } });
        if (url.pathname.endsWith("/process-context"))
          return route.fulfill({ json: cycle });
        if (url.pathname.includes("/value-chain-positions/"))
          return route.fulfill({ json: position });
        if (url.pathname.endsWith("/activity-executions")) {
          assert.equal(url.searchParams.get("learningCycleId"), "900002");
          assert.equal(url.searchParams.get("chainId"), "900014");
          return route.fulfill({ json: history() });
        }
        if (url.pathname.endsWith("/execution-progress")) {
          assert.equal(url.searchParams.get("sourceReference"), source);
          if (failProgress) {
            progressFailures++;
            return route.fulfill({
              status: 503,
              json: { message: "Falha simulada de acompanhamento" },
            });
          }
          return route.fulfill({
            json: [
              {
                taskId: current.taskId,
                status: current.status,
                updatedAt: String(revision),
              },
            ],
          });
        }
        if (url.pathname.endsWith("/execution-requests")) {
          commands.push({
            method: request.method(),
            path: url.pathname,
            cycle: url.searchParams.get("learningCycleId"),
          });
          assert.match(
            url.pathname,
            /\/activities\/prototypeCorrection\/execution-requests$/,
          );
          assert.equal(request.method(), "POST");
          assert.equal(url.searchParams.get("learningCycleId"), "900002");
          if (failCommand)
            return route.fulfill({
              status: 503,
              json: { message: "Falha simulada ao criar tarefa" },
            });
          prior = [current, ...prior];
          current = task(current.taskId + 1, "PENDING");
          revision++;
          await new Promise((resolve) => setTimeout(resolve, 300));
          return route.fulfill({
            json: {
              activityId: "prototypeCorrection",
              sourceReference: source,
              tasks: [{ id: current.taskId }],
              message: "Tarefa registrada no ambiente sintético.",
            },
          });
        }
        unexpectedRequests.push(request.url());
        return route.fulfill({
          status: 404,
          json: { message: "API não prevista na matriz" },
        });
      });
      await page.goto(`${baseUrl}${path}${search}#activity-psiqueRecovery`, {
        waitUntil: "domcontentloaded",
      });
      const origin = page.locator("#activity-prototypeCorrection");
      await expect(
        origin.getByText("Tarefa #900385 · Bloqueada", { exact: true }),
      ).toBeVisible();
      for (const [id] of targets) {
        const card = page.locator(`#activity-${id}`);
        await expect(card.getByText(/#900385/)).toHaveCount(0);
        await expect(
          card.getByRole("button", { name: "Criar tarefa de correção" }),
        ).toHaveCount(0);
        await expect(
          card.getByRole("link", { name: `Ir para 3.6 — ${correctionName}` }),
        ).toHaveAttribute(
          "href",
          `${path}${search}#activity-prototypeCorrection`,
        );
        if (id !== "psiqueAdherent")
          await expect(
            card.getByText("Nenhuma tarefa registrada para esta atividade."),
          ).toBeVisible();
      }
      await expect(
        page
          .locator("#activity-psiqueAdherent")
          .getByText("Tarefa #900384 · Bloqueada"),
      ).toBeVisible();
      await expect(
        page.getByText("Aprendizado sintético preservado"),
      ).toBeVisible();
      await page
        .locator("#activity-psiqueRecovery")
        .screenshot({ path: `${output}/${profile}-dependency.png` });
      await page
        .locator("#activity-psiqueRecovery")
        .getByRole("link", { name: /^Ir para/ })
        .click();
      await expect(page).toHaveURL(
        `${baseUrl}${path}${search}#activity-prototypeCorrection`,
      );
      assert.equal(commands.length, 0);
      await origin
        .getByRole("button", { name: "Criar tarefa de correção" })
        .click();
      await expect(origin.getByRole("alert")).toHaveText(
        "Falha simulada ao criar tarefa",
      );
      assert.equal(current.taskId, 900385);
      failCommand = false;
      await origin
        .getByRole("button", { name: "Criar tarefa de correção" })
        .click();
      await expect(
        origin.getByRole("button", { name: "Executando..." }),
      ).toBeDisabled();
      await expect(
        origin.getByText("Tarefa #900386 · Aguardando o agente"),
      ).toBeVisible();
      assert.equal(commands.length, 2);
      current = task(900386, "IN_PROGRESS");
      revision++;
      await expect(
        origin.getByText("Tarefa #900386 · Em execução"),
      ).toBeVisible({ timeout: 12000 });
      const spinner = origin.locator(
        ".product-process-situation__running-icon",
      );
      const transform1 = await spinner.evaluate(
        (el) => getComputedStyle(el).transform,
      );
      await expect
        .poll(() => spinner.evaluate((el) => getComputedStyle(el).transform))
        .not.toBe(transform1);
      await page.emulateMedia({ reducedMotion: "reduce" });
      await expect(spinner).toHaveCSS("animation-name", "none");
      await page.emulateMedia({ reducedMotion: "no-preference" });
      failProgress = true;
      await expect(
        origin.getByText(/Não foi possível atualizar o andamento/),
      ).toBeVisible({ timeout: 15000 });
      await expect(
        origin.getByText("Tarefa #900386 · Em execução"),
      ).toBeVisible();
      failProgress = false;
      current = task(900386, "BLOCKED");
      revision++;
      await expect(origin.getByText("Tarefa #900386 · Bloqueada")).toBeVisible({
        timeout: 12000,
      });
      await expect(spinner).toHaveCount(0);
      for (const [id] of targets)
        await expect(
          page.locator(`#activity-${id}`).getByText(/#900386/),
        ).toHaveCount(0);
      await origin
        .getByRole("button", { name: "Criar tarefa de correção" })
        .click();
      await expect(
        origin.getByText("Tarefa #900387 · Aguardando o agente"),
      ).toBeVisible();
      current = task(900387, "COMPLETED");
      revision++;
      await expect(origin.getByText("Tarefa #900387 · Concluída")).toBeVisible({
        timeout: 12000,
      });
      await expect(
        page.getByRole("link", { name: /^Ir para 3.6/ }),
      ).toHaveCount(0);
      await expect(origin.getByText(/Tarefa #900385 · Dédalo/)).toBeVisible();
      await expect(
        page
          .locator("#activity-psiqueRecovery")
          .getByText("Não iniciada", { exact: true }),
      ).toBeVisible();
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(
        origin.getByText("Tarefa #900387 · Concluída"),
      ).toBeVisible();
      assert.equal(commands.length, 3);
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
        true,
      );
      await origin.screenshot({ path: `${output}/${profile}-origin.png` });
      assert.deepEqual(pageErrors, []);
      assert.deepEqual(unexpectedRequests, []);
      assert.ok(progressFailures > 0);
      results.push({
        profile,
        status: "PASS",
        commands,
        progressFailures,
        pageErrors,
        unexpectedRequests,
      });
      console.log(
        `${profile}: PASS — autoria, dependência, contexto, comando, estados, falhas, rotação e histórico`,
      );
    } catch (error) {
      results.push({
        profile,
        status: "FAIL",
        error: error.message,
        pageErrors,
        unexpectedRequests,
        commands,
      });
      await page.screenshot({
        path: `${output}/${profile}-failure.png`,
        fullPage: true,
      });
      throw error;
    } finally {
      await context.close();
    }
  }
} finally {
  await browser.close();
  await writeFile(`${output}/results.json`, JSON.stringify(results, null, 2));
}
