// Homologa a interface real usando contratos sintéticos e bloqueando rede comercial.
import { readFile, mkdir, writeFile } from "node:fs/promises";
import vm from "node:vm";
import { createRequire } from "node:module";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const ts = require("typescript");
const root = new URL("../../../", import.meta.url);
const source = await readFile(
  new URL(
    "frontend/src/pages/product/ProductProcessActivityExecutionsPage.test.tsx",
    root,
  ),
  "utf8",
);
const block = source.slice(
  source.indexOf("const dedaloTask"),
  source.indexOf("function renderPage"),
);
const base = vm.runInNewContext(
  ts.transpile(block + "\nglobalThis.fixture = history;", {
    target: ts.ScriptTarget.ES2022,
  }) + "\nfixture",
);
const child =
  "/products/7/value-chain-history/processes/90/activities?chainId=17&sourceReference=experiment%3A88";
const parent =
  "/products/7/value-chain-history/processes/89/activities?chainId=17&sourceReference=experiment%3A88";
const steps = [
  ["entry", "Preparar página de venda e prova real", "Backend"],
  ["creative", "Conferir criativo final aprovado", "Backend"],
  ["checkout", "Preparar checkout e contrato de entrega", "Backend"],
  ["targeting", "Conferir público aprovado por Atena", "Backend"],
  ["economics", "Conferir margem e parecer de Plutus", "Backend"],
  [
    "humanExperienceReview",
    "Homologar a experiência de compra e uso do kit",
    "Psique",
  ],
  [
    "commercialIntegrityReview",
    "Revisar integridade da oferta Quartzo",
    "Têmis",
  ],
  ["ready", "Concluir preparação comercial Quartzo", "Backend"],
];
const history = {
  ...base,
  productId: 7,
  productName: "Kit sintético local",
  productInternalName: "Quartzo de teste",
  selectedProcessDefinitionId: 90,
  selectedProcessVersionNumber: 1,
  selectedProcessStatus: "PUBLISHED",
  processCode: "quartzo-commercial-preparation-v1",
  processName: "Preparar operação comercial Quartzo",
  currentExecutionReference: "experiment:88",
  operationalState: "NOT_STARTED",
  objectiveAchieved: false,
  selectedActivityCount: 8,
  completedActivityCount: 0,
  remainingActivityCount: 8,
  blockedActivityCount: 0,
  activityCount: 8,
  activitiesWithTasksCount: 0,
  uniqueTaskCount: 0,
  knownEstimatedCostUsd: 0,
  currentActivityId: "entry",
  currentActivityName: steps[0][1],
  currentActivityState: "NOT_STARTED",
  currentActivityStateReason: "Fontes sintéticas locais",
  chainPosition: {
    sequenceLabel: "5.1",
    parentProcessCode: "pde-commercial-homologation-activation",
    parentProcessName: "Homologação comercial",
  },
  activities: steps.map(([id, name, owner], i) => ({
    activityDefinitionId: 900 + i,
    activityId: id,
    activityName: name,
    activityObjective: "Comprovar as fontes da mesma versão",
    activityOwnerName: owner,
    sequenceNumber: i + 1,
    selectedVersionActivity: true,
    operationalState: "NOT_STARTED",
    stateReason: "Preparação local",
    objectiveAchieved: false,
    stateEvidence: "NOT_RECORDED",
    taskCount: 0,
    tasks: [],
    executionRequestAvailable: false,
  })),
};
const automation = {
  id: null,
  productId: 7,
  processDefinitionId: 90,
  chainId: 17,
  learningCycleId: null,
  sourceReference: "experiment:88",
  status: "NOT_STARTED",
  reason: "Preparação local sem publicação",
  currentActivityId: "entry",
  currentActivityName: steps[0][1],
  currentOwnerName: "Backend",
  currentSequence: 1,
  totalActivities: 8,
  completedActivities: 0,
  remainingActivities: 8,
  omittedActivities: 0,
  completionPercentage: 0,
  knownCostUsd: 0,
  costCoverage: "NO_EXECUTIONS",
  canStart: true,
  canPause: false,
  canResume: false,
  automaticExecution: false,
  revision: 0,
  parentProcesses: [
    {
      processDefinitionId: 89,
      processName: "Homologação comercial",
      processVersion: 8,
      activityId: "commercialPreparation",
      activityName: "Preparar operação comercial conforme o tipo",
      navigationUrl: parent,
    },
  ],
  subprocesses: [],
};
const output = new URL("artifacts/capella-quartzo/browser/", root);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  headless: true,
});
const results = [];
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options),
      page = await context.newPage();
    const errors = [],
      reads = [];
    let mutations = 0,
      mode = "pending";
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.hostname !== "127.0.0.1") return route.abort();
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (route.request().method() !== "GET") {
        mutations++;
        return route.fulfill({
          status: 409,
          json: { message: "Sem escritas externas na fixture" },
        });
      }
      let body = [];
      const h = structuredClone(history),
        a = structuredClone(automation);
      if (mode === "blocked") {
        h.operationalState = "BLOCKED";
        h.currentActivityState = "BLOCKED";
        h.currentActivityStateReason =
          "O checkout do experimento diverge da página auditada.";
        h.activities[2].operationalState = "BLOCKED";
        h.activities[2].stateReason = h.currentActivityStateReason;
        a.status = "BLOCKED";
        a.reason = h.currentActivityStateReason;
        a.canStart = false;
      }
      if (mode === "completed") {
        h.operationalState = "COMPLETED";
        h.objectiveAchieved = true;
        h.completedActivityCount = 8;
        h.remainingActivityCount = 0;
        h.activities.forEach((item) => {
          item.objectiveAchieved = true;
          item.operationalState = "COMPLETED";
        });
        Object.assign(a, {
          status: "COMPLETED",
          completedActivities: 8,
          remainingActivities: 0,
          completionPercentage: 100,
          canStart: false,
        });
      }
      if (url.pathname.includes("/business-processes/89/")) {
        h.selectedProcessDefinitionId = 89;
        h.processName = "Homologação comercial";
        h.activities = [
          {
            ...h.activities[0],
            activityId: "commercialPreparation",
            activityName: "Preparar operação comercial conforme o tipo",
            executionControl: {
              executorType: "BACKEND",
              interactionType: "SUBPROCESS",
              actionLabel: "Abrir Quartzo",
              actionAvailable: true,
              confirmationRequired: false,
              requirements: [],
              targetProcessDefinitionId: 90,
              navigationUrl:
                "/products/7/value-chain-history/processes/90/activities?sourceReference=experiment%3A88",
            },
          },
        ];
      }
      if (url.pathname.endsWith("/activity-executions")) {
        reads.push(url.searchParams.get("sourceReference"));
        body = h;
      } else if (url.pathname.endsWith("/automation/v1")) body = a;
      else if (url.pathname.endsWith("/process-context")) body = null;
      else if (url.pathname.endsWith("/value-chain-position"))
        body = { productId: 7, chainDefinitionId: 17, processMeasurements: [] };
      else if (url.pathname === "/api/business-process-chains")
        body = [
          {
            id: 17,
            name: "Cadeia local",
            status: "PUBLISHED",
            versionNumber: 17,
            items: [],
          },
        ];
      return route.fulfill({ json: body });
    });
    await page.goto("http://127.0.0.1:15173" + parent);
    await page.waitForLoadState("networkidle");
    await page
      .getByRole("link", { name: "Abrir Quartzo", exact: true })
      .click();
    await page.waitForLoadState("networkidle");
    const selected = new URL(page.url());
    expect(selected.pathname).toBe(child.split("?")[0]);
    expect(selected.searchParams.get("chainId")).toBe("17");
    expect(selected.searchParams.get("sourceReference")).toBe("experiment:88");
    await expect(
      page.getByRole("heading", {
        name: /Preparar operação comercial Quartzo/,
        level: 1,
      }),
    ).toBeVisible();
    await expect(page.getByText("0 de 8 atividades concluídas")).toBeVisible();
    for (const [, label] of steps)
      await expect(
        page.getByText(label, { exact: true }).first(),
      ).toBeVisible();
    mode = "blocked";
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(
      page
        .getByText("O checkout do experimento diverge da página auditada.")
        .first(),
    ).toBeVisible();
    await page.screenshot({
      path: new URL(name + "-blocked.png", output).pathname,
      fullPage: true,
    });
    mode = "completed";
    await page.reload();
    await page.waitForLoadState("networkidle");
    await expect(page.getByText("8 de 8 atividades concluídas")).toBeVisible();
    await page.screenshot({
      path: new URL(name + "-completed.png", output).pathname,
      fullPage: true,
    });
    await page.getByRole("link", { name: /Voltar ao processo pai/ }).click();
    await expect(page).toHaveURL("http://127.0.0.1:15173" + parent);
    expect(errors).toEqual([]);
    expect(mutations).toBe(0);
    expect(reads.every((ref) => ref === "experiment:88")).toBe(true);
    results.push({
      device: name,
      status: "PASS",
      cases: [
        "atividades",
        "bloqueio",
        "conclusão",
        "retorno",
        "referência sem ciclo",
      ],
      mutations,
    });
    console.log("PASS", name);
    await context.close();
  }
} finally {
  await browser.close();
  await writeFile(
    new URL("result.json", output),
    JSON.stringify(results, null, 2),
  );
}
