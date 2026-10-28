import { test, expect } from "@playwright/test";
import { readFile } from "node:fs/promises";

const version = "musa-pde-entry-v17-continuidade-clara";
const base = "/api/pde/vega/style/v1";
const headers = { "X-PDE-Internal-Token": "vega-local-internal-only" };

test("continuidade e conclusão não exigem comentário, download nem feedback de uso", async ({
  page,
  request,
  context,
}, info) => {
  const response = await request.post(`${base}/internal/sessions`, {
    headers,
    data: { cycleId: 91003, prototypeVersion: version },
  });
  expect(response.ok()).toBeTruthy();
  const session = await response.json();
  const ownHeaders = {
    ...headers,
    "X-Vega-Style-Session": session.sessionToken,
  };
  await context.addCookies([
    {
      name: "vega-style-session",
      value: session.sessionToken,
      domain: "127.0.0.1",
      path: base,
      httpOnly: true,
      sameSite: "Strict",
    },
  ]);
  await page.goto(`/experiences/${version}/agent-validation`);
  await page
    .getByRole("button", {
      name: "Experimentar com minhas roupas",
      exact: true,
    })
    .click();
  await page.getByLabel("Onde você vai usar?").fill("almoço");
  await page
    .getByLabel("Quais roupas você já tem em mente?")
    .fill("camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Cor e criatividade");
  await page
    .getByRole("button", { name: "Ver minha orientação", exact: true })
    .click();
  const card = page.getByRole("article", { name: "Sua orientação pronta" });
  await expect(card).toContainText("alça da bolsa");
  await expect(card).toContainText("já está salva");
  await expect(
    page.getByRole("complementary", { name: "Feedback opcional" }),
  ).toContainText("não liberam nem bloqueiam");
  const primary = page.locator("#style-start button.style-button");
  await expect(primary).toHaveCount(1);
  await expect(primary).toHaveText("Salvar e concluir este dia");
  await expect(primary).toBeEnabled();
  await expect(
    page.getByLabel("Quer anotar algo para depois? (opcional)"),
  ).toHaveValue("");
  await expect(
    page.getByRole("complementary", {
      name: "O que a continuidade acrescenta",
    }),
  ).toContainText("mapa de estilo");
  await page.screenshot({
    path: info.outputPath("primeiro-resultado.png"),
    fullPage: true,
  });
  // Conhecer a oferta é possível antes da reflexão, sem declarar uso ou preferência.
  await page
    .getByRole("button", { name: "Conhecer a continuidade", exact: true })
    .click();
  await expect(
    page.getByRole("region", { name: "Condições da continuidade" }),
  ).toContainText("R$ 67, pagamento único. Sem assinatura.");
  await expect(
    page.getByRole("region", { name: "Condições da continuidade" }),
  ).toContainText("não realiza cobranças");
  await page.screenshot({
    path: info.outputPath("continuidade.png"),
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Salvar e concluir este dia", exact: true })
    .click();
  await expect(
    page.getByText("Seu dia e suas escolhas ficaram salvos."),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Sua jornada está guardada" }),
  ).toBeVisible();
  await page.getByRole("button", { name: /Dia 1$/ }).click();
  await expect(card).toContainText("alça da bolsa");
  await page.reload();
  await page.getByRole("button", { name: /Dia 1$/ }).click();
  await expect(card).toContainText("alça da bolsa");
  const report = await (
    await request.get(`${base}/internal/report`, { headers: ownHeaders })
  ).json();
  expect(Object.keys(report.operations)).toHaveLength(1);
  expect(Object.keys(report.events).sort()).toEqual([
    "CHECKOUT_STARTED",
    "EXPERIENCE_STARTED",
  ]);
  expect(report.sideEffects.paymentEnabled).toBe(false);
  expect(report.trafficClass).toBe("AGENT_VALIDATION");
  expect(report.missions["estilo-dia-1"].answers.reflection || "").toBe("");
  const workspace = await (
    await request.get(`${base}/access/workspace`, { headers: ownHeaders })
  ).json();
  expect(workspace.completedMissionIds).toEqual(["estilo-dia-1"]);
  expect(workspace.subscriptionStatus).toBe("TRIAL");
  expect(workspace.reflectionRequiredForCompletion).toBe(false);
  const locked = await request.post(
    `${base}/access/missions/estilo-dia-2/complete`,
    {
      headers: ownHeaders,
      data: {},
    },
  );
  expect(locked.status()).toBe(409);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBeTruthy();
});

test("comentário voluntário permanece e orientação bloqueada não oferece conclusão", async ({
  page,
  request,
  context,
}) => {
  const created = await request.post(`${base}/internal/sessions`, {
    headers,
    data: { cycleId: 91003, prototypeVersion: version },
  });
  expect(created.ok()).toBeTruthy();
  const session = await created.json();
  const ownHeaders = {
    ...headers,
    "X-Vega-Style-Session": session.sessionToken,
  };
  await context.addCookies([
    {
      name: "vega-style-session",
      value: session.sessionToken,
      domain: "127.0.0.1",
      path: base,
      httpOnly: true,
      sameSite: "Strict",
    },
  ]);
  const missing = await request.post(
    `${base}/access/missions/estilo-dia-1/complete`,
    {
      headers: ownHeaders,
      data: {},
    },
  );
  expect(missing.status()).toBe(409);
  await page.goto(`/experiences/${version}/agent-validation`);
  await page
    .getByRole("button", {
      name: "Experimentar com minhas roupas",
      exact: true,
    })
    .click();
  await page.getByLabel("Onde você vai usar?").fill("almoço");
  const selection = page.getByLabel("Quais roupas você já tem em mente?");
  await selection.fill("quero comprar uma roupa nova");
  await page.getByLabel("O que você quer sentir").selectOption("Discrição");
  await page
    .getByRole("button", { name: "Ver minha orientação", exact: true })
    .click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(
    page.getByRole("button", {
      name: "Salvar e concluir este dia",
      exact: true,
    }),
  ).toHaveCount(0);
  await selection.fill(
    "camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça",
  );
  await page
    .getByRole("button", { name: "Ver minha orientação", exact: true })
    .click();
  const note = "Quero repetir esta escolha amanhã; ainda não a usei.";
  await page.getByLabel("Quer anotar algo para depois? (opcional)").fill(note);
  await page
    .getByRole("button", { name: "Salvar e concluir este dia", exact: true })
    .click();
  await expect(
    page.getByText("Seu dia e suas escolhas ficaram salvos."),
  ).toBeVisible();
  await page.getByRole("button", { name: /Dia 1$/ }).click();
  await page.reload();
  await page.getByRole("button", { name: /Dia 1$/ }).click();
  await expect(
    page.getByLabel("Quer anotar algo para depois? (opcional)"),
  ).toHaveValue(note);
  const report = await (
    await request.get(`${base}/internal/report`, { headers: ownHeaders })
  ).json();
  expect(report.missions["estilo-dia-1"].answers.reflection).toBe(note);
  expect(Object.keys(report.events)).toEqual(["EXPERIENCE_STARTED"]);
});

test("apresentação histórica da v15 permanece independente da revisão privada", async ({
  page,
}) => {
  const historical = "musa-pde-entry-v15-estilo-pessoal";
  const contract = JSON.parse(
    await readFile(
      "../backend/src/main/resources/contracts/musa-v15-product-v1.json",
      "utf8",
    ),
  );
  await page.route("**/api/pde/products/metodo-musa-7-dias?*", (route) =>
    route.fulfill({ json: contract }),
  );
  await page.goto(`/experiences/${historical}`);
  await expect(
    page.getByRole("heading", {
      name: "Seu guarda-roupa pode ter mais a sua cara.",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", {
      name: "Guarde o que funciona. Use de novo amanhã.",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("complementary", { name: "Feedback opcional" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Ver minha orientação" }),
  ).toBeVisible();
});

test("v16 preserva a apresentação avaliada no ciclo predecessor", async ({
  page,
  request,
  context,
}) => {
  const priorVersion = "musa-pde-entry-v16-modelagem-internacional";
  const created = await request.post(`${base}/internal/sessions`, {
    headers,
    data: { cycleId: 91002, prototypeVersion: priorVersion },
  });
  expect(created.ok()).toBeTruthy();
  const session = await created.json();
  await context.addCookies([
    {
      name: "vega-style-session",
      value: session.sessionToken,
      domain: "127.0.0.1",
      path: base,
      httpOnly: true,
      sameSite: "Strict",
    },
  ]);
  await page.goto(`/experiences/${priorVersion}/agent-validation`);
  await page
    .getByRole("button", {
      name: "Experimentar com minhas roupas",
      exact: true,
    })
    .click();
  await page.getByLabel("Onde você vai usar?").fill("almoço");
  await page
    .getByLabel("Quais roupas você já tem em mente?")
    .fill("camisa, lenço e bolsa; posso usar o lenço no pescoço ou na alça");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Cor e criatividade");
  await page
    .getByRole("button", { name: "Ver minha orientação", exact: true })
    .click();
  const card = page.getByRole("article", { name: "Sua orientação pronta" });
  await expect(card).toContainText("alça da bolsa");
  await expect(
    card.getByRole("button", { name: "Entendi como aplicar", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Guarde uma escolha para amanhã." }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("complementary", { name: "Feedback opcional" }),
  ).toHaveCount(0);
});
