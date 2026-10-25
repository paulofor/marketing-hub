import { test, expect } from "@playwright/test";
import { readFile } from "node:fs/promises";
const version = "musa-pde-entry-v15-estilo-pessoal";
const path = `/experiences/${version}`;
const backend = process.env.PDE_TEST_BACKEND_URL || "http://127.0.0.1:8096";
const contract =
  process.env.PDE_TEST_CONTRACT_SERVER_URL || "http://127.0.0.1:57181";
const slug = "metodo-musa-7-dias";
const headers = (token: string) => ({ "X-PDE-Access-Token": token });

test("preferência muda a orientação; resultado antecede cadastro e recupera sem geração", async ({
  page,
}) => {
  let generations = 0;
  page.on("request", (req) => {
    if (
      req.method() === "POST" &&
      req.url().endsWith("/public/presence-diagnostic")
    )
      generations++;
  });
  await page.goto(`${path}?mh_preview=qa`);
  await expect(
    page.getByRole("heading", {
      name: "Seu guarda-roupa pode ter mais a sua cara.",
    }),
  ).toBeVisible();
  await page.getByLabel("Onde você vai usar?").fill("trabalho e passeio");
  await page
    .getByLabel("Quais roupas você já tem em mente?")
    .fill("camiseta azul, jeans e tênis");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Conforto e praticidade");
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  const result = page.getByRole("article", { name: "Sua orientação pronta" });
  await expect(result).toContainText("caminhe");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Cor e criatividade");
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  await expect(result).toContainText("cor, estampa ou textura");
  await expect(result).not.toContainText("sente, caminhe");
  await page.reload();
  await expect(result).toContainText("cor, estampa ou textura");
  expect(generations).toBe(2);
  const downloading = page.waitForEvent("download");
  await page.getByRole("button", { name: "Guardar minha orientação" }).click();
  expect(await readFile((await (await downloading).path())!, "utf8")).toContain(
    "camiseta azul",
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: test.info().outputPath("estilo-primeiro-valor.png"),
    fullPage: true,
  });
});

test("falha não anuncia resultado; neutro e descoberta são caminhos válidos", async ({
  page,
  request,
}) => {
  for (const answers of [
    { occasion: "trabalho", existingSelection: "camisa" },
    {
      occasion: "trabalho",
      existingSelection: "camisa",
      stylePreference: "inventado",
    },
  ]) {
    expect(
      (
        await request.post(`${backend}/api/pde/public/presence-diagnostic`, {
          data: { experienceVersion: version, answers },
        })
      ).status(),
    ).toBe(400);
  }
  await page.goto(`${path}?mh_preview=qa`);
  await page.getByLabel("Onde você vai usar?").fill("casa");
  await page.getByLabel("Quais roupas você já tem em mente?").fill("vestido");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Quero manter como está");
  await page.route("**/api/pde/public/presence-diagnostic", (route) =>
    route.fulfill({ status: 503, body: "{}" }),
  );
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "formulário foi preservado",
  );
  await expect(
    page.getByRole("article", { name: "Sua orientação pronta" }),
  ).toHaveCount(0);
  await expect(
    page.getByLabel("Quais roupas você já tem em mente?"),
  ).toHaveValue("vestido");
  await page.unroute("**/api/pde/public/presence-diagnostic");
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  await expect(
    page.getByRole("article", { name: "Sua orientação pronta" }),
  ).toContainText("Não há correção obrigatória");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Ainda estou descobrindo");
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  await expect(
    page.getByRole("article", { name: "Sua orientação pronta" }),
  ).toContainText("não precisa escolher um rótulo");
});

test("sete dias reutilizam escolhas, mantêm acesso e refund sem alterar versão anterior", async ({
  page,
  request,
}, info) => {
  const id = `style-${info.project.name}-${Date.now()}`;
  const email = `teste+${id}@sandbox.local`;
  expect(
    (
      await request.post(`${contract}/test/pepper/transactions/${id}`, {
        data: { status: "paid", email, experienceVersion: version },
      })
    ).ok(),
  ).toBeTruthy();
  const paid = await request.post(`${backend}/api/pde/access/pepper/webhook`, {
    data: { productSlug: slug, transactionId: id, status: "paid" },
  });
  expect(paid.status()).toBe(201);
  const access = await paid.json();
  await page.goto(`${path}/access?mh_preview=qa#access=${access.token}`);
  await expect(
    page.getByRole("heading", { name: "Seu estilo em 7 dias", exact: true }),
  ).toBeVisible();
  for (let day = 1; day <= 7; day++) {
    if (day === 1) {
      await page.getByLabel("Onde você vai usar?").fill("SIMULAÇÃO — rotina");
      await page
        .getByLabel("Quais roupas você já tem em mente?")
        .fill("camiseta azul e jeans");
    } else {
      await expect(
        page.getByLabel("Quais roupas você já tem em mente?"),
      ).toHaveValue("camiseta azul e jeans");
    }
    await page
      .getByLabel("O que você quer sentir")
      .selectOption(day % 2 ? "Cor e criatividade" : "Discrição");
    await page.getByRole("button", { name: "Ver minha orientação" }).click();
    await expect(
      page.getByRole("article", { name: "Sua orientação pronta" }),
    ).toBeVisible();
    if (day === 7) {
      await page.getByText("Escolhas que orientaram esta sugestão").click();
      await expect(
        page.getByRole("article", { name: "Sua orientação pronta" }),
      ).toContainText("Guardo a escolha do dia 1");
    }
    await page
      .getByLabel("O que funcionou para você?")
      .fill(`Guardo a escolha do dia ${day}`);
    await page
      .getByRole("button", { name: "Salvar e concluir este dia" })
      .click();
    await expect(page.getByRole("status")).toContainText("ficaram salvos");
  }
  await expect(
    page.getByText("Minha jornada · 100%", { exact: true }),
  ).toBeVisible();
  const downloading = page.waitForEvent("download");
  await page.getByRole("button", { name: "Baixar meus registros" }).click();
  expect(await readFile((await (await downloading).path())!, "utf8")).toContain(
    "Guardo a escolha do dia 7",
  );
  const workspace = await (
    await request.get(`${backend}/api/pde/access/workspace`, {
      headers: headers(access.token),
    })
  ).json();
  for (const m of workspace.product.supportMaterials) {
    expect((await request.get(m.url)).status()).toBe(403);
    expect(
      (await request.get(m.url, { headers: headers(access.token) })).ok(),
    ).toBeTruthy();
  }
  await page.reload();
  await expect(
    page.getByText("Minha jornada · 100%", { exact: true }),
  ).toBeVisible();
  const old = await request.post(`${backend}/api/pde/access/magic-link`, {
    data: {
      productSlug: slug,
      email,
      experienceVersion: "musa-pde-entry-v14-primeiro-ajuste-aplicavel",
    },
  });
  expect((await old.json()).accessUrl).toContain("musa-pde-entry-v14");
  await request.post(`${contract}/test/pepper/transactions/${id}`, {
    data: { status: "refunded", email, experienceVersion: version },
  });
  await request.post(`${backend}/api/pde/access/pepper/webhook`, {
    data: { productSlug: slug, transactionId: id, status: "refunded" },
  });
  expect(
    (
      await request.get(`${backend}/api/pde/access/materials/authorize`, {
        headers: headers(access.token),
      })
    ).status(),
  ).toBe(403);
  const analytics = await (
    await request.get(
      `${backend}/api/pde/access/analytics/${slug}/summary?experienceVersion=${version}`,
    )
  ).json();
  expect(analytics.humanSessions).toBe(0);
});

test("mantém a preferência durante uma resposta em andamento", async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(window.crypto, "randomUUID", {
      value: undefined,
      configurable: true,
    });
    Object.defineProperty(window, "sessionStorage", {
      get() {
        throw new DOMException("Armazenamento indisponível", "SecurityError");
      },
      configurable: true,
    });
  });
  const product = {
    experienceVersion: version,
    publicDiagnosticQuestions: [
      { key: "occasion", question: "Onde você vai usar?", options: [] },
      {
        key: "existingSelection",
        question: "Quais roupas você já tem em mente?",
        options: [],
      },
      {
        key: "stylePreference",
        question: "O que você quer sentir no seu jeito de vestir?",
        options: ["Quero manter como está"],
      },
    ],
  };
  await page.route("**/api/pde/products/**", (route) =>
    route.fulfill({ json: product }),
  );
  await page.route("**/api/pde/access/events", (route) =>
    route.fulfill({ json: {} }),
  );
  let release!: () => void;
  const waiting = new Promise<void>((resolve) => {
    release = resolve;
  });
  await page.route("**/api/pde/public/presence-diagnostic", async (route) => {
    await waiting;
    await route.fulfill({
      json: {
        requestId: "local-delay",
        status: "COMPLETED",
        headline: "Escolha preservada",
        summary: "SIMULAÇÃO LOCAL",
        microActions: ["Mantenha sua escolha."],
        signals: [],
        caution: "",
      },
    });
  });
  await page.goto(`${path}?mh_preview=qa`);
  await page.getByLabel("Onde você vai usar?").fill("casa");
  await page.getByLabel("Quais roupas você já tem em mente?").fill("camiseta");
  await page
    .getByLabel("O que você quer sentir")
    .selectOption("Quero manter como está");
  await page.getByRole("button", { name: "Ver minha orientação" }).click();
  await expect(page.getByLabel("O que você quer sentir")).toBeDisabled();
  await expect(page.getByLabel("Onde você vai usar?")).toBeDisabled();
  release();
  await expect(
    page.getByRole("heading", { name: "Escolha preservada" }),
  ).toBeVisible();
  await expect(page.getByLabel("O que você quer sentir")).toBeEnabled();
});
