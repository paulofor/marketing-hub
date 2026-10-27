import { test, expect } from "@playwright/test";
import { readFile } from "node:fs/promises";

const version = "musa-pde-entry-v16-modelagem-internacional";
const base = "/api/pde/vega/style/v1";
const headers = { "X-PDE-Internal-Token": "vega-local-internal-only" };

test("continuidade e conclusão não exigem download nem feedback de uso", async ({
  page,
  request,
  context,
}, info) => {
  const response = await request.post(`${base}/internal/sessions`, {
    headers,
    data: { cycleId: 91002, prototypeVersion: version },
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
    .getByLabel("O que funcionou para você?")
    .fill("Quero experimentar a alça amanhã; ainda não vesti a combinação.");
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
  const workspace = await (
    await request.get(`${base}/access/workspace`, { headers: ownHeaders })
  ).json();
  expect(workspace.completedMissionIds).toEqual(["estilo-dia-1"]);
  expect(workspace.subscriptionStatus).toBe("TRIAL");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBeTruthy();
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
