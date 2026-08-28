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

const processDefinition = {
  id: 56,
  processCode: "pde-commercial-homologation-activation",
  name: "Homologação e ativação comercial do PDE",
  purpose:
    "Executar experiência humana, integridade comercial, preflight e autorização em decisões separadas.",
  ownerName: "Backend de Experimentos",
  triggerDescription: "Produto e jornada aprovados com versões exatas.",
  outcomeDescription: "Ativação autorizada ou bloqueada com causa.",
  versionNumber: 6,
  status: "PUBLISHED",
  processType: "VALUE_PROCESS",
  technicalReference: "agent-responsibility-matrix-v3",
  createdAt: "2026-08-28T00:00:00Z",
  publishedAt: "2026-08-28T00:00:00Z",
  diagram: {
    nodes: [
      { id: "start", type: "START", label: "Produto e jornada aprovados" },
      {
        id: "humanExperienceReview",
        type: "TASK",
        label: "Validar experiência humana da jornada",
        owner: "Psique",
      },
      {
        id: "commercialIntegrityReview",
        type: "TASK",
        label: "Validar integridade comercial da jornada",
        owner: "Têmis",
      },
      {
        id: "preflight",
        type: "TASK",
        label: "Executar preflight técnico",
        owner: "Backend",
        subprocessCode: "experiment-homologation-activation",
      },
      {
        id: "decision",
        type: "GATEWAY",
        label: "Todos os gates estão aprovados?",
      },
      {
        id: "authorization",
        type: "TASK",
        label: "Autorizar ativação e orçamento",
        owner: "Plutus",
      },
      {
        id: "end",
        type: "END",
        label: "Decisão de ativação registrada",
      },
    ],
    flows: [
      { from: "start", to: "humanExperienceReview" },
      { from: "humanExperienceReview", to: "commercialIntegrityReview" },
      { from: "commercialIntegrityReview", to: "preflight" },
      { from: "preflight", to: "decision" },
      { from: "decision", to: "authorization", label: "sim" },
      { from: "authorization", to: "end" },
    ],
  },
};

const subprocess = {
  id: 58,
  processCode: "experiment-homologation-activation",
  name: "Homologação técnica de experimento",
  purpose: "Comprovar superfícies, transação, eventos e limites.",
  ownerName: "Backend de Experimentos",
  triggerDescription: "Experimento pronto.",
  outcomeDescription: "Homologação técnica comprovada.",
  versionNumber: 5,
  status: "PUBLISHED",
  processType: "SUBPROCESS",
  parentProcessCode: processDefinition.processCode,
  parentProcessDefinitionId: processDefinition.id,
  createdAt: "2026-08-28T00:00:00Z",
  publishedAt: "2026-08-28T00:00:00Z",
  diagram: {
    nodes: [
      { id: "start", type: "START", label: "Experimento pronto" },
      { id: "preflight", type: "TASK", label: "Executar preflight" },
      { id: "end", type: "END", label: "Experimento homologado" },
    ],
    flows: [
      { from: "start", to: "preflight" },
      { from: "preflight", to: "end" },
    ],
  },
};

const composition = {
  process: {
    id: processDefinition.id,
    processCode: processDefinition.processCode,
    name: processDefinition.name,
    purpose: processDefinition.purpose,
    ownerName: processDefinition.ownerName,
    versionNumber: processDefinition.versionNumber,
    status: processDefinition.status,
    processType: processDefinition.processType,
  },
  parentProcess: null,
  subprocessCount: 1,
  subprocesses: [
    {
      id: subprocess.id,
      processCode: subprocess.processCode,
      name: subprocess.name,
      purpose: subprocess.purpose,
      ownerName: subprocess.ownerName,
      versionNumber: subprocess.versionNumber,
      status: subprocess.status,
      processType: subprocess.processType,
    },
  ],
};

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
      if (pathname === "/api/business-processes") {
        await route.fulfill({ json: [processDefinition, subprocess] });
      } else if (pathname === "/api/business-process-execution-resources") {
        await route.fulfill({ json: [] });
      } else if (pathname === "/api/business-processes/56/composition") {
        await route.fulfill({ json: composition });
      } else if (
        pathname === "/api/business-process-chains/by-process/56" ||
        pathname === "/api/business-processes/56/document-activities"
      ) {
        await route.fulfill({ json: [] });
      } else {
        await route.fulfill({ status: 404, json: { message: "not mocked" } });
      }
    });

    await page.goto(`${baseUrl}/business-processes?processId=56`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", {
        name: "Homologação e ativação comercial do PDE · v6",
      }),
    ).toBeVisible();

    const selectedProcess = page.locator(
      ".business-process-detail-title .business-process-entity-name--process",
    );
    await expect(selectedProcess).toHaveCount(1);
    await expect(selectedProcess.locator(".lucide-workflow")).toBeVisible();
    await expect(selectedProcess.locator(".lucide-clipboard-list")).toHaveCount(
      0,
    );

    const catalogProcesses = page.locator(
      ".business-process-list .business-process-entity-name--process",
    );
    await expect(catalogProcesses).toHaveCount(2);
    await expect(catalogProcesses.locator(".lucide-workflow")).toHaveCount(2);

    const activityNames = page.locator(
      ".process-node--task .business-process-entity-name--activity",
    );
    await expect(activityNames).toHaveCount(4);
    await expect(activityNames.locator(".lucide-clipboard-list")).toHaveCount(
      4,
    );
    await expect(activityNames.locator(".lucide-workflow")).toHaveCount(0);
    await expect(
      page.locator(
        ".process-node--start .business-process-entity-name--activity, .process-node--gateway .business-process-entity-name--activity, .process-node--end .business-process-entity-name--activity",
      ),
    ).toHaveCount(0);

    const subprocessLink = page.locator(".business-process-composition__child");
    await expect(subprocessLink).toHaveAttribute(
      "href",
      "/business-processes?processId=58",
    );
    await expect(
      subprocessLink.locator(".business-process-entity-name--process"),
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
      path: `/tmp/business-process-entity-icons-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Ícones de processo e atividade aprovados em desktop, iPhone 15 Pro e Pixel 7.",
);
