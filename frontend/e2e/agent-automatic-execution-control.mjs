import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const profiles = [
  ["desktop", { viewport: { width: 1366, height: 768 } }],
  ["iphone-15-pro", devices["iPhone 15 Pro"]],
  ["pixel-7", devices["Pixel 7"]],
];

const agents = [
  [1, "videomaker", "Apolo", "Agente Videomaker", 2],
  [2, "market-radar", "Argos", "Agente Radar de Mercado", 2],
  [3, "experiment-strategist", "Atena", "Estrategista de Experimentos", 3],
  [4, "landing-generator", "Dédalo", "Agente Gerador de Landing", 2],
  [5, "growth-operator", "Hermes", "Operador de Crescimento", 4],
  [6, "financial-agent", "Plutus", "Agente Financeiro", 3],
  [7, "customer-agent", "Psique", "Agente Cliente", 3],
  [8, "meta-ad-approver", "Têmis", "Aprovador de Anúncios Meta", 2],
  [9, "communication-director", "Íris", "Diretora e Materializadora de Comunicação", 1],
];

for (const [profileName, contextOptions] of profiles) {
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
  });
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  const state = new Map(agents.map(([id]) => [id, true]));
  const mutations = [];

  await page.route("**/api/agents/work-monitor/**", async (route) => {
    const match = route
      .request()
      .url()
      .match(/work-monitor\/(\d+)\/automatic-execution$/);
    if (!match || route.request().method() !== "PUT") return route.fallback();
    const agentId = Number(match[1]);
    const body = route.request().postDataJSON();
    mutations.push({ agentId, ...body });
    await new Promise((resolve) => setTimeout(resolve, 300));
    state.set(agentId, body.automaticExecutionEnabled);
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        agentId,
        agentKey: agents.find(([id]) => id === agentId)[1],
        automaticExecutionEnabled: body.automaticExecutionEnabled,
        automaticExecutionStatus: body.automaticExecutionEnabled
          ? "PLAY"
          : "STOP",
      }),
    });
  });
  await page.route("**/api/agents/work-monitor", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(
        agents.map(([agentId, agentKey, nickname, agentName, version]) => ({
          agentId,
          agentKey,
          nickname,
          agentName,
          automaticExecutionEnabled: state.get(agentId),
          automaticExecutionStatus: state.get(agentId) ? "PLAY" : "STOP",
          workStatus: "IDLE",
          currentWork: "Sem trabalho ativo",
          externalDecisionRequired: false,
          dailyTokens: 0,
          dailyTokenDate: "2026-08-20",
          executorHealth: {
            status: "READY",
            expectedVersion: version,
            deployedVersion: version,
            versionCurrent: true,
            backendAccessible: true,
            codexAuthenticated: true,
            detail: "Executor pronto.",
          },
          combinedStatus: "READY",
        })),
      ),
    }),
  );
  await page.route("**/api/agents/maturity", (route) =>
    route.fulfill({ contentType: "application/json", body: "[]" }),
  );
  await page.route("**/api/agents", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(
        agents.map(([id, agentKey, nickname, name, currentVersion]) => ({
          id,
          agentKey,
          nickname,
          name,
          status: "ACTIVE",
          currentVersion,
          executionMode: "EVENT_DRIVEN",
          inputs: [],
          outputs: [],
          internalFunctions: [],
        })),
      ),
    }),
  );

  await page.goto("http://127.0.0.1:15174/agents");
  await expect(page.getByText("PLAY", { exact: true })).toHaveCount(9);
  const stop = page.getByRole("button", {
    name: "Parar execução automática de Apolo",
  });
  await stop.click();
  await expect(
    page.getByRole("button", { name: "Ativar execução automática de Apolo" }),
  ).toBeVisible();
  expect(mutations[0]).toEqual({
    agentId: 1,
    automaticExecutionEnabled: false,
  });
  await page
    .getByRole("button", { name: "Ativar execução automática de Apolo" })
    .click();
  await expect(
    page.getByRole("button", { name: "Parar execução automática de Apolo" }),
  ).toBeVisible();
  expect(mutations[1]).toEqual({
    agentId: 1,
    automaticExecutionEnabled: true,
  });
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: `/tmp/agent-control-${profileName}.png`,
    fullPage: true,
  });
  await browser.close();
}

console.log(
  "PLAY/STOP visual e funcional aprovado em desktop, iPhone 15 Pro e Pixel 7.",
);
