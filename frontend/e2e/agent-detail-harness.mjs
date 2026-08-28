import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const profiles = [
  ["desktop", { viewport: { width: 1366, height: 900 } }],
  ["iphone-15-pro", devices["iPhone 15 Pro"]],
  ["pixel-7", devices["Pixel 7"]],
];

const repositoryRoot = fileURLToPath(new URL("../..", import.meta.url));
const harnessManifest = JSON.parse(
  readFileSync(
    new URL(
      "../../backend/ads-service/src/main/resources/agent-harness/agent-harness-v2.json",
      import.meta.url,
    ),
    "utf8",
  ),
);
const irisHarness = harnessManifest.agents.find(
  ({ agentKey }) => agentKey === "communication-director",
);
const behaviorFiles = irisHarness.artifacts
  .filter(({ artifactType }) =>
    ["PROMPT", "OUTPUT_SCHEMA", "BEHAVIOR_LIBRARY"].includes(artifactType),
  )
  .map((artifact) => {
    const content = readFileSync(
      resolve(repositoryRoot, artifact.path),
      "utf8",
    );
    return {
      behaviorType: artifact.artifactType,
      name: artifact.name,
      version: artifact.version,
      path: artifact.path,
      description: artifact.description,
      mediaType: artifact.path.endsWith(".json")
        ? "application/json"
        : "text/markdown",
      sha256: createHash("sha256").update(content).digest("hex"),
      content,
    };
  });

const agent = {
  id: 9,
  name: "Diretora e Materializadora de Comunicação",
  nickname: "Íris",
  agentKey: "communication-director",
  status: "TEST",
  currentVersion: 1,
  themeId: 3,
  themeName: "Operações Autônomas",
  ownerName: "Marketing Hub",
  description: "Materializa comunicação pré-compra fiel ao PDE real.",
  businessObjective: "Aumentar desejo e conversão sem ampliar a promessa.",
  successMetrics: "Aprovação inicial, retrabalho, tempo, custo e correspondência.",
  modelName: "gpt-5.6-sol",
  executionMode: "EVENT_DRIVEN",
  automaticExecutionEnabled: false,
  triggerPolicy: "Contratos e produto aprovados liberaram uma atividade BPM.",
  responsibilityContract: "Mensagem, copy, landing, peças estáticas e e-mails.",
  orchestratorPolicy: "Backend controla o avanço.",
  analysisPolicy: "Compara três alternativas sem redefinir estratégia ou produto.",
  offeringPolicy: "Pacote funcional separado de auditoria e provas.",
  authorityPolicy: "Não publica, não gasta e não se autoaprova.",
  promptContractPath:
    "communication-agent-worker/src/main/resources/prompts/iris/v1/behavioral-core.md",
  schemaContractPath:
    "communication-agent-worker/src/main/resources/prompts/iris/v1/output-schema.json",
  inputs: [],
  outputs: [],
  internalFunctions: [],
  executionResources: [],
  harness: {
    status: "COMPLETE",
    contractVersion: harnessManifest.contractVersion,
    sourceReference: "docs/canonical/premium-ai-agent-architecture-canon.v1.md",
    sensitiveValuesPolicy:
      "Nenhum valor de secret, token, credencial ou conteúdo privado de raciocínio é exibido.",
    sections: irisHarness.sections,
    artifacts: irisHarness.artifacts,
    behaviorFiles,
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

  await page.route("**/api/agents/9/details", (route) =>
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
  await expect(detailLink).toHaveAttribute("href", "/agents/9/details");
  await detailLink.click();

  await expect(
    page.getByRole("heading", { name: "Detalhe do agente — Íris" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Harness completo do agente" }),
  ).toBeVisible();
  await expect(page.getByText("Completo", { exact: true })).toBeVisible();
  await expect(
    page.getByText(
      `${irisHarness.sections.length} seções · ${behaviorFiles.length} arquivos de comportamento · ${irisHarness.artifacts.length} artefatos`,
    ),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", {
      name: "Arquivos que definem o comportamento",
    }),
  ).toBeVisible();
  const constitutionFile = page
    .locator("summary")
    .filter({ hasText: "Constituição de comunicação" });
  await constitutionFile.click();
  await expect(
    page.getByText(/Você é Íris, Diretora e Materializadora de Comunicação/),
  ).toBeVisible();
  await expect(constitutionFile.getByText("Fechar arquivo")).toBeVisible();
  const schemaFile = page
    .locator("summary")
    .filter({ hasText: "Schema de comunicação" });
  await schemaFile.click();
  await expect(schemaFile.getByText("Fechar arquivo")).toBeVisible();
  await expect(
    schemaFile
      .locator("..")
      .getByText(/campo\(s\)/)
      .first(),
  ).toBeVisible();
  await expect(
    page.locator("summary").filter({ hasText: "Responsabilidade exclusiva" }),
  ).toBeVisible();
  await page
    .locator("summary")
    .filter({ hasText: "Executor e implantação" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Módulo executor" }),
  ).toBeVisible();
  await page
    .locator("summary")
    .filter({ hasText: "Runtime do modelo" })
    .click();
  await expect(page.getByText("Esforço de raciocínio")).toBeVisible();
  await expect(page.getByText("Constituição de comunicação").first()).toBeVisible();
  await expect(page.getByText("Schema de comunicação").first()).toBeVisible();
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
    path: `/tmp/iris-agent-detail-harness-${profileName}.png`,
    fullPage: true,
  });
  await browser.close();
}

console.log(
  "Detalhe do harness de Íris aprovado em desktop, iPhone 15 Pro e Pixel 7.",
);
