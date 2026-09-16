// Homologa a UI real com contratos HTTP simulados; bloqueia todas as origens externas.
import { readFile, mkdir } from "node:fs/promises";
import vm from "node:vm";
import { createRequire } from "node:module";
import {
  chromium,
  devices,
  expect,
} from "../../../frontend/node_modules/@playwright/test/index.mjs";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const ts = require("typescript");
const root = new URL("../../../", import.meta.url);
function fixture(source, start, end, names) {
  const block = source.slice(source.indexOf(start), source.indexOf(end));
  return vm.runInNewContext(
    ts.transpile(block + `\nglobalThis.fixture = {${names}};`, {
      target: ts.ScriptTarget.ES2022,
    }) + "\nfixture",
  );
}
const cycleSource = await readFile(
  new URL("frontend/src/pages/learningCycle/LearningCyclesPage.test.tsx", root),
  "utf8",
);
const { catalog, cycle } = fixture(
  cycleSource,
  "const catalog:",
  "const decisionProposal",
  "catalog,cycle",
);
const historySource = await readFile(
  new URL(
    "frontend/src/pages/product/ProductProcessActivityExecutionsPage.test.tsx",
    root,
  ),
  "utf8",
);
const { history } = fixture(
  historySource,
  "const dedaloTask",
  "function renderPage",
  "history",
);
const child =
  "/products/4/value-chain-history/processes/99/activities?learningCycleId=2&chainId=15";
const parent =
  "/products/4/value-chain-history/processes/77/activities?learningCycleId=2&chainId=15";
Object.assign(cycle, {
  experimentId: 92,
  chainDefinitionId: 15,
  stage: "PUBLICATION",
  stageLabel: "Preparação comercial",
  commands: [
    {
      action: "COMPLETE",
      label: "Conferir publicação",
      available: false,
      reason: "Conclua a preparação",
    },
  ],
  workUrl: child,
  commercialPreparation: {
    readyForReview: false,
    guidance: "Os agentes preparam os ativos da mesma versão.",
    experimentUrl: "/experiments/92",
    preparationUrl: child,
    preparationLabel: "Abrir preparação Opala com os agentes",
    requirements: [],
  },
});
Object.assign(catalog.entry, {
  chainDefinitionId: 15,
  parentProcessDefinitionId: 77,
  parentUrl: parent,
});
const steps = [
  "Preparar entrada do próprio PDE",
  "Vincular e preparar criativo aprovado",
  "Configurar checkout e acesso",
  "Preparar público aprovado por Atena",
  "Validar custos, limites e margem",
  "Homologar experiência comercial",
  "Revisar integridade da jornada",
  "Concluir preparação comercial",
];
Object.assign(history, {
  productId: 4,
  productInternalName: "Opala de teste",
  productName: "Experiência local",
  selectedProcessDefinitionId: 99,
  processCode: "opala-commercial-preparation-v1",
  processName: "Preparar operação comercial Opala",
  selectedProcessVersionNumber: 1,
  currentExecutionReference: "experiment:92",
  operationalState: "NOT_STARTED",
  selectedActivityCount: 8,
  completedActivityCount: 0,
  remainingActivityCount: 8,
  blockedActivityCount: 0,
  activityCount: 8,
  activitiesWithTasksCount: 0,
  uniqueTaskCount: 0,
  knownEstimatedCostUsd: 0,
  currentActivityId: "entry",
  currentActivityName: steps[0],
  currentActivityState: "NOT_STARTED",
  currentActivityStateReason: "Preparação pendente",
});
history.activities = steps.map((name, i) => ({
  activityDefinitionId: 200 + i,
  activityId: [
    "entry",
    "creative",
    "checkout",
    "targeting",
    "economics",
    "humanExperienceReview",
    "commercialIntegrityReview",
    "ready",
  ][i],
  activityName: name,
  activityObjective: "Preparar e comprovar a mesma ocorrência",
  activityOwnerName: [
    "Dédalo",
    "Dédalo",
    "Dédalo",
    "Dédalo",
    "Plutus",
    "Psique",
    "Têmis",
    "Backend",
  ][i],
  sequenceNumber: i + 1,
  selectedVersionActivity: true,
  operationalState: "NOT_STARTED",
  stateReason: "Aguardar execução do responsável",
  objectiveAchieved: false,
  stateEvidence: "NOT_RECORDED",
  taskCount: 0,
  tasks: [],
  executionRequestAvailable: false,
}));
const automation = {
  id: null,
  productId: 4,
  processDefinitionId: 99,
  chainId: 15,
  learningCycleId: 2,
  sourceReference: "experiment:92",
  status: "NOT_STARTED",
  reason: "Preparação por agentes",
  currentActivityId: "entry",
  currentActivityName: steps[0],
  currentOwnerName: "Dédalo",
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
      processDefinitionId: 77,
      processName: "Venda, entrega e aprendizado",
      processVersion: 7,
      activityId: "commercialPreparation",
      activityName: "Preparar operação comercial Opala",
      navigationUrl: parent,
    },
  ],
  subprocesses: [],
};
const output = new URL("artifacts/opala-commercial/browser/", root);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  headless: true,
});
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const errors = [];
    let mutations = 0;
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.hostname !== "127.0.0.1") return route.abort();
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (route.request().method() !== "GET") {
        mutations++;
        return route.fulfill({
          status: 409,
          json: { message: "Escrita externa não permitida na fixture" },
        });
      }
      let body = [];
      if (url.pathname.endsWith("/catalog")) body = catalog;
      else if (
        url.pathname.includes("/learning-cycles/v1/products/4") &&
        !url.pathname.includes("process-context")
      )
        body = [cycle];
      else if (url.pathname.endsWith("/process-context")) body = null;
      else if (url.pathname.endsWith("/activity-executions")) body = history;
      else if (url.pathname.endsWith("/automation/v1")) body = automation;
      else if (url.pathname.endsWith("/value-chain-position"))
        body = { productId: 4, chainDefinitionId: 15, processMeasurements: [] };
      else if (url.pathname === "/api/products")
        body = [
          { id: 4, name: "Experiência local", internalName: "Opala de teste" },
        ];
      else if (url.pathname === "/api/business-process-chains")
        body = [
          {
            id: 15,
            name: "Cadeia de teste",
            status: "PUBLISHED",
            versionNumber: 15,
            items: [],
          },
        ];
      return route.fulfill({ json: body });
    });
    await page.goto(
      "http://127.0.0.1:15173/business-process-chains/learning-cycles?productId=4&chainId=15&cycleId=2",
    );
    await page.waitForLoadState("networkidle");
    await page
      .getByRole("link", { name: "Abrir preparação Opala com os agentes" })
      .click();
    await expect(page).toHaveURL("http://127.0.0.1:15173" + child);
    await expect(
      page.getByText(steps[4], { exact: true }).first(),
    ).toBeVisible();
    await expect(page.getByText(/Responsável: Plutus/).first()).toBeVisible();
    await page.screenshot({
      path: new URL(name + ".png", output).pathname,
      fullPage: true,
    });
    await page.getByRole("link", { name: /Voltar ao processo pai/ }).click();
    await expect(page).toHaveURL("http://127.0.0.1:15173" + parent);
    expect(errors).toEqual([]);
    expect(mutations).toBe(0);
    console.log("PASS", name, "navegação, atividades, retorno e zero escritas");
    await context.close();
  }
} finally {
  await browser.close();
}
