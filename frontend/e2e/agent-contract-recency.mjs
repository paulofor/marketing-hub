import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const profiles = [
  ["desktop", { viewport: { width: 1366, height: 768 } }],
  ["iphone-15-pro", devices["iPhone 15 Pro"]],
  ["pixel-7", devices["Pixel 7"]],
];

const jsonHeaders = {
  "access-control-allow-origin": "*",
  "content-type": "application/json",
};

const agents = [
  [1, "videomaker", "Apolo", "Agente Videomaker", "2026-08-20T10:00:00Z"],
  [2, "market-radar", "Argos", "Agente Radar de Mercado", null],
  [
    3,
    "experiment-strategist",
    "Atena",
    "Estrategista de Experimentos",
    "2026-08-24T12:00:00Z",
  ],
  [
    4,
    "landing-generator",
    "Dédalo",
    "Agente Gerador de Landing",
    "2026-08-12T16:36:24Z",
  ],
  [
    5,
    "growth-operator",
    "Hermes",
    "Operador de Crescimento",
    "2026-08-10T21:27:23Z",
  ],
  [6, "financial-agent", "Plutus", "Agente Financeiro", "2026-08-10T21:27:32Z"],
  [7, "customer-agent", "Psique", "Agente Cliente", "2026-08-10T21:27:30Z"],
  [
    8,
    "meta-ad-approver",
    "Têmis",
    "Aprovador de Anúncios Meta",
    "2026-08-10T21:27:37Z",
  ],
];

for (const [profileName, contextOptions] of profiles) {
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
  });
  const context = await browser.newContext(contextOptions);
  const page = await context.newPage();
  const pageErrors = [];
  const apiFailures = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (request) => {
    if (request.url().includes("/api/agents")) {
      apiFailures.push(request.url());
    }
  });
  await page.addInitScript(() => {
    Date.now = () => new Date("2026-08-25T12:00:00Z").getTime();
  });

  await page.route("**/api/agents/work-monitor", (route) =>
    route.fulfill({ headers: jsonHeaders, body: "[]" }),
  );
  await page.route("**/api/agents/maturity", (route) =>
    route.fulfill({ headers: jsonHeaders, body: "[]" }),
  );
  await page.route("**/api/agents", (route) => {
    return route.fulfill({
      headers: jsonHeaders,
      body: JSON.stringify(
        agents.map(([id, agentKey, nickname, name, lastContractChangeAt]) => ({
          id,
          agentKey,
          nickname,
          name,
          status: "ACTIVE",
          currentVersion: 2,
          executionMode: "EVENT_DRIVEN",
          lastContractChangeAt,
          inputs: [],
          outputs: [],
          internalFunctions: [],
        })),
      ),
    });
  });

  await page.goto("http://127.0.0.1:15174/agents");
  await expect(page.locator("[data-agent-contract-recency]")).toHaveCount(8);
  await expect(page.locator('[data-agent-contract-recency="1"]')).toBeVisible();
  await expect(page.getByText("20/08/2026")).toBeVisible();
  await expect(page.getByText("5 dias sem alteração")).toBeVisible();
  await expect(page.getByText("1 dia sem alteração")).toBeVisible();
  await expect(page.getByText("Não informada")).toBeVisible();
  await expect(page.getByText("Tempo não informado")).toBeVisible();
  expect(pageErrors).toEqual([]);
  expect(apiFailures).toEqual([]);
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: `/tmp/agent-contract-recency-${profileName}.png`,
    fullPage: true,
  });
  await browser.close();
}

console.log(
  "Recência contratual aprovada em desktop, iPhone 15 Pro e Pixel 7.",
);
