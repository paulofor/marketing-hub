import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

const apiBase = "/api/pde/agent-validation/v1/products/pde-planejado-46";
const internalToken = process.env.PDE_INTERNAL_API_TOKEN || "pde-local-internal-test";

test("ADHERENT entrega três fixtures e conclui sem cobrança", async ({ page, request }) => {
  await provision(page, request, "ADHERENT");
  await fillInput(page);
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
  await expect(page.getByRole("heading", { name: "Três combinações para sua ocasião" })).toBeVisible();
  await expect(page.locator(".alcyone-look-grid article")).toHaveCount(3);
  await expect(page.locator(".alcyone-look-grid img")).toHaveCount(3);
  for (const image of await page.locator(".alcyone-look-grid img").all()) {
    await expect.poll(() => image.evaluate((element: HTMLImageElement) => element.naturalWidth)).toBe(1024);
  }
  await completeDecision(page, false);
  await expect(page.getByRole("heading", { name: "Homologação concluída" })).toBeVisible();
  await expect(page.getByText("0 chamadas pagas")).toBeVisible();
  await expectNoOverflow(page);
});

test("RECOVERY retoma entrada e o mesmo pacote depois de falha transitória", async ({ page, request }) => {
  await provision(page, request, "RECOVERY");
  await fillInput(page);
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page.route(`**${apiBase}/generate`, (route) => route.abort("failed"), { times: 1 });
  await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.reload({ waitUntil: "domcontentloaded" });
  await expect(page.getByLabel("Ocasião", { exact: true })).toHaveValue("Jantar de formatura");
  await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
  await expect(page.locator(".alcyone-look-grid article")).toHaveCount(3);
  await completeDecision(page, true);
  await expect(page.getByRole("heading", { name: "Homologação concluída" })).toBeVisible();
  await expectNoOverflow(page);
});

test("SAFETY bloqueia foto corporal e compra antes de produzir resultado", async ({ page, request }) => {
  await provision(page, request, "SAFETY");
  await fillInput(page, {
    preferences: "quero enviar foto corporal",
    constraints: "quero comprar uma roupa nova",
  });
  await page.getByRole("button", { name: "Salvar entrada segura" }).click();
  await page.getByRole("button", { name: "Gerar três combinações estáticas" }).click();
  await expect(page.getByRole("heading", { name: "Este pedido ficou fora do protótipo" })).toBeVisible();
  await expect(page.locator(".alcyone-look-grid")).toHaveCount(0);
  await page.getByRole("button", { name: "Registrar bloqueio seguro" }).click();
  await page.getByRole("button", { name: "Concluir cenário de segurança" }).click();
  await expect(page.getByRole("heading", { name: "Homologação concluída" })).toBeVisible();
  await expectNoOverflow(page);
});

/** Cria uma sessão interna e injeta somente o token opaco no sessionStorage. */
async function provision(page: Page, request: APIRequestContext, scenarioCode: string) {
  const response = await request.post(`${apiBase}/internal/sessions`, {
    headers: { "X-PDE-Internal-Token": internalToken },
    data: { sourceReference: "product:11@agent-validation-v1", scenarioCode },
  });
  expect(response.status()).toBe(201);
  const session = (await response.json()) as { sessionToken: string };
  await page.addInitScript(
    (token) => window.sessionStorage.setItem("alcyone-agent-validation-session", token),
    session.sessionToken,
  );
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await expect(page.getByTestId("agent-validation-mode")).toContainText(scenarioCode);
}

/** Preenche somente ocasião, preferências, restrições e peças sintéticas. */
async function fillInput(
  page: Page,
  overrides: { preferences?: string; constraints?: string } = {},
) {
  await page.getByLabel("Ocasião", { exact: true }).fill("Jantar de formatura");
  await page.getByLabel("Data da ocasião").fill("2026-12-15");
  await page.getByLabel("Preferências").fill(overrides.preferences || "linhas simples, tons frios");
  await page.getByLabel("Restrições práticas").fill(overrides.constraints || "clima ameno, sem salto alto");
  await page.getByLabel("Referências isoladas das peças").fill(
    "piece-fixture-01\npiece-fixture-02\npiece-fixture-03\npiece-fixture-04",
  );
}

/** Registra valor, uso, preferência e checkout sem inferir nenhum deles da etapa anterior. */
async function completeDecision(page: Page, recovery: boolean) {
  await page.getByLabel("O que tornou o pacote útil?").fill("As opções reduziram a dúvida e são aplicáveis.");
  await page.getByRole("button", { name: "Este pacote resolve minha decisão" }).click();
  await page.getByRole("button", { name: "Escolher combinação 1" }).click();
  await page.getByLabel("Por que prefere este pacote à pesquisa gratuita?").fill(
    "O pacote é direto, coerente e evita pesquisar referências soltas.",
  );
  await page.getByRole("button", { name: "Prefiro isto à pesquisa gratuita" }).click();
  await page.getByRole("button", { name: "Abrir checkout simulado de R$ 79" }).click();
  await expect(page.getByRole("heading", { name: "Simulação concluída — nenhuma cobrança realizada" })).toBeVisible();
  if (recovery) {
    await page.getByRole("button", { name: "Confirmar retomada do mesmo pacote" }).click();
  }
  await page.getByRole("button", { name: "Concluir cenário interno" }).click();
}

/** Confirma a ausência de rolagem horizontal no dispositivo emulado. */
async function expectNoOverflow(page: Page) {
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBe(true);
}
