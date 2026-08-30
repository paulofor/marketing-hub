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

const catalog = [
  {
    processDefinitionId: 52,
    processCode: "pde-opportunity-discovery",
    name: "Descoberta e priorização da oportunidade PDE",
    purpose: "Reunir evidências factuais antes de criar qualquer produto.",
    ownerName: "Argos",
    triggerDescription: "Uma pergunta real de mercado.",
    outcomeDescription: "Dossiê factual auditável.",
    versionNumber: 6,
    executionAvailable: true,
    executionAvailabilityReason: "Pronto para iniciar sem produto.",
    inputFields: [
      {
        key: "theme",
        label: "Tema ou pergunta de mercado",
        controlType: "TEXT",
        required: true,
        maxLength: 191,
        helpText: "Descreva a dor pesquisada.",
      },
      {
        key: "objective",
        label: "Objetivo comercial da pesquisa",
        controlType: "TEXTAREA",
        required: false,
        maxLength: 5000,
      },
      {
        key: "country",
        label: "País",
        controlType: "TEXT",
        required: true,
        maxLength: 16,
        defaultValue: "BR",
      },
      {
        key: "language",
        label: "Idioma",
        controlType: "TEXT",
        required: true,
        maxLength: 16,
        defaultValue: "pt-BR",
      },
    ],
  },
];

const blocked = {
  id: 91,
  requestKey: "b82df168-e383-4acd-8ca4-ab858b39fd3e",
  processDefinitionId: 52,
  processCode: "pde-opportunity-discovery",
  processName: "Descoberta e priorização da oportunidade PDE",
  processVersionNumber: 6,
  sourceReference: "product-discovery-cycle:76",
  displayName: "dor anterior",
  requestedByName: "Marketing Hub",
  input: { theme: "dor anterior", country: "BR", language: "pt-BR" },
  status: "BLOCKED",
  activityCount: 1,
  completedActivityCount: 0,
  costCoverage: "NOT_REPORTED",
  latestError: "Fonte pública recusou a consulta.",
  createdAt: "2026-08-30T13:00:00Z",
  startedAt: "2026-08-30T13:00:05Z",
  finishedAt: "2026-08-30T13:01:00Z",
};

function detail(execution) {
  return {
    execution,
    activities: [
      {
        activityId: "marketEvidence",
        activityName: "Reunir evidências factuais de mercado",
        status: execution.status,
        tasks: [
          {
            taskId: execution.id === 91 ? 271 : 272,
            status: execution.status,
            assignedAgentKey: "market-radar",
            assignedAgentNickname: "Argos",
            title: "Reunir evidências factuais",
            executionError: execution.latestError,
            costEstimationStatus: "NOT_REPORTED",
            createdAt: execution.createdAt,
          },
        ],
      },
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
    const posts = [];
    let createdDetail;
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
    await page.route("**/api/**", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (pathname === "/api/independent-business-process-executions/catalog") {
        await route.fulfill({ json: catalog });
        return;
      }
      if (
        pathname === "/api/independent-business-process-executions" &&
        request.method() === "GET"
      ) {
        await route.fulfill({ json: [blocked] });
        return;
      }
      if (pathname === "/api/independent-business-process-executions/91") {
        await route.fulfill({ json: detail(blocked) });
        return;
      }
      if (pathname === "/api/independent-business-process-executions/92") {
        await route.fulfill(
          createdDetail
            ? { json: createdDetail }
            : { status: 404, json: { message: "Execução ainda não criada" } },
        );
        return;
      }
      if (
        pathname === "/api/independent-business-process-executions" &&
        request.method() === "POST"
      ) {
        posts.push(request.postDataJSON());
        await new Promise((resolve) => setTimeout(resolve, 250));
        const pending = {
          ...blocked,
          id: 92,
          requestKey: posts[0].requestKey,
          sourceReference: "product-discovery-cycle:77",
          displayName: posts[0].input.theme,
          input: posts[0].input,
          status: "PENDING",
          latestError: undefined,
          createdAt: "2026-08-30T14:00:00Z",
          startedAt: undefined,
          finishedAt: undefined,
        };
        createdDetail = detail(pending);
        await route.fulfill({ status: 201, json: createdDetail });
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });

    await page.goto(`${baseUrl}/business-process-executions`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "Executar processos independentes" }),
    ).toBeVisible();
    await expect(
      page.getByText("Fonte pública recusou a consulta.").first(),
    ).toBeVisible();
    await expect(page.getByLabel("País *")).toHaveValue("BR");

    const submit = page.getByRole("button", { name: "Iniciar processo" });
    await submit.click();
    assert.equal(
      posts.length,
      0,
      `${profileName}: validação obrigatória não bloqueou envio`,
    );
    await page
      .getByLabel("Tema ou pergunta de mercado *")
      .fill("agenda vazia para manicures");
    await page
      .getByLabel("Objetivo comercial da pesquisa")
      .fill("Comprovar uma dor urgente e monetizável sem inventar produto.");
    await submit.click({ noWaitAfter: true });
    await expect(
      page.getByRole("button", { name: "Iniciando..." }),
    ).toBeDisabled();
    if (process.env.DEBUG_E2E === "1") {
      await page.waitForTimeout(600);
      console.log(
        JSON.stringify({
          profileName,
          posts,
          pageErrors,
          consoleErrors,
          body: await page.locator("body").innerText(),
        }),
      );
    }
    await expect(
      page.getByRole("heading", {
        name: "Execução #92 · agenda vazia para manicures",
      }),
    ).toBeVisible();

    assert.equal(posts.length, 1, `${profileName}: duplo envio da execução`);
    assert.equal(
      posts[0].processDefinitionId,
      52,
      `${profileName}: processo incorreto`,
    );
    assert.equal(
      posts[0].input.country,
      "BR",
      `${profileName}: default de país ausente`,
    );
    assert.equal(
      posts[0].input.language,
      "pt-BR",
      `${profileName}: default de idioma ausente`,
    );
    assert.equal(
      "productId" in posts[0],
      false,
      `${profileName}: productId artificial`,
    );
    assert.equal(
      "experimentId" in posts[0],
      false,
      `${profileName}: experimentId artificial`,
    );
    assert.match(
      posts[0].requestKey,
      /^[0-9a-f-]{36}$/i,
      `${profileName}: requestKey inválida`,
    );
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    assert.deepEqual(consoleErrors, [], `${profileName}: erros no console`);
    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${profileName}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/independent-process-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Processos independentes aprovados em desktop, iPhone 15 Pro e Pixel 7.",
);
