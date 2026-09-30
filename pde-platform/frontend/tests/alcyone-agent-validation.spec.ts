import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

const apiBase = "/api/pde/agent-validation/v1/products/pde-planejado-46";
const internalToken =
  process.env.PDE_INTERNAL_API_TOKEN || "pde-local-internal-test";

test("ADHERENT entrega três fixtures e conclui sem cobrança", async ({
  page,
  request,
}) => {
  await provision(page, request, "ADHERENT");
  await acceptConsent(page);
  await fillInput(page);
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page
    .getByRole("button", { name: "Gerar três combinações estáticas" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Três combinações para sua ocasião" }),
  ).toBeVisible();
  await expect(page.locator(".alcyone-look-grid article")).toHaveCount(3);
  await expect(page.locator(".alcyone-look-grid img")).toHaveCount(3);
  for (const image of await page.locator(".alcyone-look-grid img").all()) {
    await expect
      .poll(() =>
        image.evaluate((element: HTMLImageElement) => element.naturalWidth),
      )
      .toBe(1024);
  }
  await completeDecision(page, request, false);
  await expect(
    page.getByRole("heading", { name: "Homologação concluída" }),
  ).toBeVisible();
  await expect(page.getByText("0 chamadas pagas")).toBeVisible();
  await expectNoOverflow(page);
});

test("RECOVERY retoma entrada e o mesmo pacote depois de falha transitória", async ({
  page,
  request,
}) => {
  await provision(page, request, "RECOVERY");
  await acceptConsent(page);
  await fillInput(page);
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page.route(`**${apiBase}/generate`, (route) => route.abort("failed"), {
    times: 1,
  });
  await page
    .getByRole("button", { name: "Gerar três combinações estáticas" })
    .click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.reload({ waitUntil: "domcontentloaded" });
  await expect(page.getByLabel("Ocasião", { exact: true })).toHaveValue(
    "Jantar de formatura",
  );
  await page
    .getByRole("button", { name: "Gerar três combinações estáticas" })
    .click();
  await expect(page.locator(".alcyone-look-grid article")).toHaveCount(3);
  await completeDecision(page, request, true);
  await expect(
    page.getByRole("heading", { name: "Homologação concluída" }),
  ).toBeVisible();
  await expectNoOverflow(page);
});

test("SAFETY bloqueia foto corporal e compra antes de produzir resultado", async ({
  page,
  request,
}) => {
  await provision(page, request, "SAFETY");
  await acceptConsent(page);
  await fillInput(page, {
    preferences: "quero enviar foto corporal",
    constraints: "quero comprar uma roupa nova",
  });
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page
    .getByRole("button", { name: "Gerar três combinações estáticas" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Este pedido ficou fora do protótipo" }),
  ).toBeVisible();
  await expect(page.locator(".alcyone-look-grid")).toHaveCount(0);
  await page
    .getByRole("button", { name: "Concluir cenário de segurança" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Homologação concluída" }),
  ).toBeVisible();
  await expectNoOverflow(page);
});

/** Cria uma sessão interna e injeta somente o token opaco no sessionStorage. */
async function provision(
  page: Page,
  request: APIRequestContext,
  scenarioCode: string,
) {
  const response = await request.post(`${apiBase}/internal/sessions`, {
    headers: { "X-PDE-Internal-Token": internalToken },
    data: { sourceReference: "product:11@agent-validation-v1", scenarioCode },
  });
  expect(response.status()).toBe(201);
  const session = (await response.json()) as { sessionToken: string };
  await page.addInitScript(
    (token) =>
      window.sessionStorage.setItem("alcyone-agent-validation-session", token),
    session.sessionToken,
  );
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await expect(page.getByTestId("agent-validation-mode")).toContainText(
    scenarioCode,
  );
}

/** Aceita o contrato sintético antes de expor os campos de entrada. */
async function acceptConsent(page: Page) {
  await expect(page.getByTestId("intake-consent-step")).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Conte o mínimo necessário" }),
  ).toHaveCount(0);
  await page.getByRole("checkbox").check();
  await page
    .getByRole("button", { name: "Autorizar entrada sintética" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Conte o mínimo necessário" }),
  ).toBeVisible();
}

/** Preenche somente ocasião, preferências, restrições e peças sintéticas. */
async function fillInput(
  page: Page,
  overrides: { preferences?: string; constraints?: string } = {},
) {
  await page.getByLabel("Ocasião", { exact: true }).fill("Jantar de formatura");
  await page.getByLabel("Data da ocasião").fill("2026-12-15");
  await page
    .getByLabel("Preferências")
    .fill(overrides.preferences || "linhas simples, tons frios");
  await page
    .getByLabel("Restrições práticas")
    .fill(overrides.constraints || "clima ameno, sem salto alto");
  await page
    .getByLabel("Referências isoladas das peças")
    .fill(
      "piece-fixture-01\npiece-fixture-02\npiece-fixture-03\npiece-fixture-04",
    );
}

/** Comprova interesse, credencial rotativa, retorno autenticado, preferência e checkout. */
async function completeDecision(
  page: Page,
  request: APIRequestContext,
  recovery: boolean,
) {
  await page
    .getByLabel("O que tornou o pacote útil?")
    .fill("As opções reduziram a dúvida e são aplicáveis.");
  await page
    .getByRole("button", { name: "Este pacote resolve minha decisão" })
    .click();
  await page.getByRole("button", { name: "Escolher combinação 1" }).click();
  await page
    .getByLabel("Por que vale preservar esta decisão?")
    .fill("Quero retornar ao mesmo pacote sem refazer a decisão.");
  await page
    .getByRole("button", { name: "Quero preservar esta decisão" })
    .click();
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Criar acesso de retorno" }).click();
  await page.getByTestId("authenticated-return-step").waitFor();

  const before = await page.evaluate(() => ({
    sessionToken: window.sessionStorage.getItem(
      "alcyone-agent-validation-session",
    ),
    continuity: JSON.parse(
      window.localStorage.getItem("alcyone-agent-validation-continuity-v1") ||
        "{}",
    ) as {
      credential?: string;
      resultPackageId?: string;
    },
  }));
  expect(before.sessionToken).toBeTruthy();
  expect(before.continuity.credential).toBeTruthy();
  expect(before.continuity.resultPackageId).toBeTruthy();

  const outsiderResponse = await request.post(`${apiBase}/internal/sessions`, {
    headers: { "X-PDE-Internal-Token": internalToken },
    data: {
      sourceReference: "product:11@agent-validation-v1",
      scenarioCode: "ADHERENT",
    },
  });
  const outsider = (await outsiderResponse.json()) as { sessionToken: string };
  const forbiddenPackage = await request.get(
    `${apiBase}/packages/${before.continuity.resultPackageId}`,
    { headers: { "X-PDE-Agent-Session": outsider.sessionToken } },
  );
  expect(forbiddenPackage.status()).toBe(403);

  if (recovery) {
    const expired = await request.post(
      `${apiBase}/internal/session-expiration`,
      {
        headers: { "X-PDE-Internal-Token": internalToken },
        data: { sessionToken: before.sessionToken },
      },
    );
    expect(expired.ok()).toBeTruthy();
    const rejected = await request.get(`${apiBase}/session`, {
      headers: { "X-PDE-Agent-Session": before.sessionToken || "" },
    });
    expect(rejected.status()).toBe(403);
    await page.route(
      `**${apiBase}/packages/*`,
      (route) => route.abort("failed"),
      { times: 1 },
    );
  }

  await page
    .getByRole("button", { name: "Retomar pacote autenticado" })
    .click();
  if (recovery) {
    await expect(page.getByRole("alert")).toBeVisible();
    await page
      .getByRole("button", { name: "Retomar pacote autenticado" })
      .click();
  }
  await expect(page.getByTestId("return-complete-step")).toBeVisible();
  const after = await page.evaluate(() => ({
    sessionToken: window.sessionStorage.getItem(
      "alcyone-agent-validation-session",
    ),
    credential: (
      JSON.parse(
        window.localStorage.getItem("alcyone-agent-validation-continuity-v1") ||
          "{}",
      ) as {
        credential?: string;
      }
    ).credential,
  }));
  expect(after.sessionToken).not.toBe(before.sessionToken);
  expect(after.credential).not.toBe(before.continuity.credential);
  const oldSession = await request.get(`${apiBase}/session`, {
    headers: { "X-PDE-Agent-Session": before.sessionToken || "" },
  });
  expect(oldSession.status()).toBe(403);

  await page
    .getByLabel("Por que prefere este pacote à pesquisa gratuita?")
    .fill("O pacote é direto, coerente e evita pesquisar referências soltas.");
  await page
    .getByRole("button", { name: "Prefiro isto à pesquisa gratuita" })
    .click();
  await page
    .getByRole("button", { name: "Abrir checkout simulado de R$ 79" })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "Simulação concluída — nenhuma cobrança realizada",
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Concluir cenário interno" }).click();
}

/** Confirma a ausência de rolagem horizontal no dispositivo emulado. */
async function expectNoOverflow(page: Page) {
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth + 1,
      ),
    )
    .toBe(true);
}
