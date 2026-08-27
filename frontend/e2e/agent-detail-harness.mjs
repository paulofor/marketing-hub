import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const profiles = [
  ["desktop", { viewport: { width: 1366, height: 900 } }],
  ["iphone-15-pro", devices["iPhone 15 Pro"]],
  ["pixel-7", devices["Pixel 7"]],
];

const agent = {
  id: 7,
  name: "Agente Gerador de Landing",
  nickname: "Dédalo",
  agentKey: "landing-generator",
  status: "TEST",
  currentVersion: 2,
  themeId: 3,
  themeName: "Operações Autônomas",
  ownerName: "Marketing Hub",
  description: "Constrói landings auditáveis.",
  businessObjective: "Elevar conversão sem perder qualidade.",
  successMetrics: "Landing aprovada e vendas reconciliadas.",
  modelName: "gpt-5.6-sol",
  executionMode: "EVENT_DRIVEN",
  automaticExecutionEnabled: false,
  triggerPolicy: "Quality Review reprovou a landing.",
  responsibilityContract: "Corrigir rascunhos pelo pipeline oficial.",
  orchestratorPolicy: "Backend controla o avanço.",
  analysisPolicy: "Oferta, prova, CTA e responsividade.",
  offeringPolicy: "Landing versionada e evidências.",
  authorityPolicy: "Não publica, não gasta e não se autoaprova.",
  promptContractPath:
    "landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/remediation.md",
  schemaContractPath:
    "landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/remediation-schema.json",
  inputs: [],
  outputs: [],
  internalFunctions: [],
  executionResources: [],
  harness: {
    status: "COMPLETE",
    contractVersion: "agent-harness-v1",
    sourceReference: "docs/canonical/premium-ai-agent-architecture-canon.v1.md",
    sensitiveValuesPolicy:
      "Nenhum valor de secret, token, credencial ou conteúdo privado de raciocínio é exibido.",
    sections: [
      {
        code: "executor",
        title: "Executor e implantação",
        description: "Unidade independente do agente.",
        items: [
          {
            key: "module",
            label: "Módulo executor",
            value: "landing-generator-agent-worker",
            description: "Worker exclusivo de Dédalo.",
            sourceReference: "landing-generator-agent-worker",
          },
        ],
      },
      {
        code: "runtime",
        title: "Runtime do modelo",
        description: "Configuração efetiva do Codex.",
        items: [
          {
            key: "reasoning",
            label: "Esforço de raciocínio",
            value: "high",
            description: "Configuração explícita do worker.",
            sourceReference:
              "landing-generator-agent-worker/src/main/resources/application.yml",
          },
        ],
      },
      {
        code: "orchestration",
        title: "Fila e callbacks",
        description: "Contratos oficiais.",
        items: [],
      },
      {
        code: "memory",
        title: "Contexto e memória",
        description: "Memória premium governada.",
        items: [],
      },
      {
        code: "security",
        title: "Segurança e autoridade",
        description: "Sem gasto ou publicação.",
        items: [],
      },
      {
        code: "observability",
        title: "Observabilidade e saúde",
        description: "Telemetria auditável.",
        items: [],
      },
    ],
    artifacts: [
      {
        artifactType: "MCP_SERVER",
        name: "MCP do Gerador de Landing",
        version: "v1",
        path: "landing-generator-agent-worker/src/main/resources/mcp/landing-generator.mjs",
        description: "Ferramentas exclusivas do domínio.",
      },
      {
        artifactType: "PROMPT",
        name: "Remediação da landing",
        version: "v1",
        path: "landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/remediation.md",
        description: "Prompt operacional principal.",
      },
      {
        artifactType: "OUTPUT_SCHEMA",
        name: "Schema da remediação",
        version: "v1",
        path: "landing-generator-agent-worker/src/main/resources/prompts/landing-generator/v1/remediation-schema.json",
        description: "Contrato estruturado da saída.",
      },
    ],
  },
};

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
    if (request.url().includes("/api/agents")) apiFailures.push(request.url());
  });

  await page.route("**/api/agents/7/details", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(agent),
    }),
  );
  await page.route("**/api/agents/work-monitor", (route) =>
    route.fulfill({ contentType: "application/json", body: "[]" }),
  );
  await page.route("**/api/agents/maturity", (route) =>
    route.fulfill({ contentType: "application/json", body: "[]" }),
  );
  await page.route("**/api/agents", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify([
        {
          ...agent,
          harness: undefined,
          executionResources: undefined,
        },
      ]),
    }),
  );

  await page.goto("http://127.0.0.1:15174/agents");
  const detailLink = page.getByRole("link", { name: "Detalhe do agente" });
  await expect(detailLink).toHaveAttribute("href", "/agents/7/details");
  await detailLink.click();

  await expect(
    page.getByRole("heading", { name: "Detalhe do agente — Dédalo" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Harness completo do agente" }),
  ).toBeVisible();
  await expect(page.getByText("Completo", { exact: true })).toBeVisible();
  await expect(page.getByText("6 seções · 3 artefatos")).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Módulo executor" }),
  ).toBeVisible();
  await page
    .locator("summary")
    .filter({ hasText: "Runtime do modelo" })
    .click();
  await expect(page.getByText("Esforço de raciocínio")).toBeVisible();
  await expect(page.getByText("Remediação da landing")).toBeVisible();
  await expect(page.getByText("Schema da remediação")).toBeVisible();
  await expect(page.getByText(/Nenhum valor de secret/)).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Voltar aos agentes" }),
  ).toBeVisible();

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
    path: `/tmp/agent-detail-harness-${profileName}.png`,
    fullPage: true,
  });
  await browser.close();
}

console.log("Detalhe do harness aprovado em desktop, iPhone 15 Pro e Pixel 7.");
