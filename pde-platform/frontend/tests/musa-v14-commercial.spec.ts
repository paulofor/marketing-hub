import { expect, test } from "@playwright/test";
import { readFile } from "node:fs/promises";
import {
  MUSA_POINTED_DOMAINS,
  resolveMusaVersionedHostConfig,
} from "../src/musaExperiences";

const version = "musa-pde-entry-v14-primeiro-ajuste-aplicavel";
const slug = "metodo-musa-7-dias";
const backend = process.env.PDE_TEST_BACKEND_URL ?? "http://127.0.0.1:8096";
const frontend = process.env.PDE_TEST_FRONTEND_URL ?? "http://127.0.0.1:57180";
const contract =
  process.env.PDE_TEST_CONTRACT_SERVER_URL ?? "http://127.0.0.1:57181";
const path = `/experiences/${version}`;
const headers = (token: string) => ({ "X-PDE-Access-Token": token });

test("domínios preservam a versão declarada, incluindo reservas e legado", () => {
  for (const domain of MUSA_POINTED_DOMAINS) {
    expect(
      resolveMusaVersionedHostConfig(domain.host)?.experienceVersion,
      domain.host,
    ).toBe(domain.experienceVersion);
  }
  expect(
    resolveMusaVersionedHostConfig("alcyone.digicomdigital.com.br"),
  ).toBeUndefined();
});

test("raiz do domínio da candidata envia a mesma versão e entrega o primeiro valor", async ({
  page,
}) => {
  await page.route("http://v9.clubemusa.com.br/**", async (route) => {
    const url = new URL(route.request().url());
    const response = await route.fetch({
      url: `${frontend}${url.pathname}${url.search}`,
    });
    await route.fulfill({ response });
  });
  await page.goto(
    "http://v9.clubemusa.com.br/?mh_preview=qa&pde_analytics=off",
  );
  await expect(
    page.getByRole("heading", {
      name: "A roupa está escolhida. Qual ajuste fazer primeiro?",
    }),
  ).toBeVisible();
  const form = page.getByRole("region", {
    name: "Primeiro ajuste MUSA",
    exact: true,
  });
  await form
    .locator("textarea")
    .first()
    .fill("SIMULAÇÃO LOCAL — reunião pela manhã");
  await form.getByRole("button", { name: "Continuar", exact: true }).click();
  await form.locator("textarea").first().fill("camisa azul e calça preta");
  const receiving = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/api/pde/public/presence-diagnostic"),
  );
  await form
    .getByRole("button", { name: "Descobrir meu primeiro ajuste", exact: true })
    .click();
  const response = await receiving;
  expect(response.request().postDataJSON().experienceVersion).toBe(version);
  expect(response.status()).toBe(201);
  const result = await response.json();
  expect(result.status).toBe("COMPLETED");
  expect(result.serviceTier).toBe("LOCAL");
  expect(result.costUsd).toBe(0);
  await expect(page.locator(".public-diagnostic-result")).toContainText(
    "camisa azul e calça preta",
  );
});

test("v14 entrega valor com dois campos antes de cadastro e preserva a raiz v12", async ({
  page,
}) => {
  await page.goto(
    `${frontend}/?experienceVersion=musa-pde-entry-v12-primeiro-ajuste-aplicavel`,
  );
  await expect(
    page.getByRole("heading", {
      name: "Você já escolheu a roupa. Falta saber qual ajuste fazer primeiro.",
    }),
  ).toBeVisible();
  await page.goto(`${frontend}${path}?mh_preview=qa&pde_analytics=off`);
  await expect(
    page.getByRole("heading", {
      name: "A roupa está escolhida. Qual ajuste fazer primeiro?",
    }),
  ).toBeVisible();
  const form = page.getByRole("region", {
    name: "Primeiro ajuste MUSA",
    exact: true,
  });
  await form.locator("textarea").first().fill("reunião pela manhã");
  await form.getByRole("button", { name: "Continuar", exact: true }).click();
  await expect(form.locator("textarea").first()).toHaveValue("");
  await form.locator("textarea").first().fill("camisa azul e calça preta");
  await form
    .getByRole("button", { name: "Descobrir meu primeiro ajuste", exact: true })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "Sua mensagem visual e o primeiro ajuste estão organizados",
    }),
  ).toBeVisible();
  await expect(
    page.getByText(/Para reunião pela manhã, ajuste o caimento/),
  ).toBeVisible();
  await expect(page.locator(".public-diagnostic-result")).toContainText(
    "camisa azul e calça preta",
  );
  await expect(page.locator(".public-diagnostic-result")).not.toContainText(
    "o ruído visual",
  );
  const downloading = page.waitForEvent("download");
  await page
    .getByRole("button", { name: "Baixar meu primeiro ajuste", exact: true })
    .click();
  const downloaded = await downloading;
  const content = await readFile((await downloaded.path())!, "utf8");
  expect(content).toContain("camisa azul e calça preta");
  expect(content).toContain("Para reunião pela manhã, ajuste o caimento");
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: test.info().outputPath("v14-primeiro-valor.png"),
    fullPage: true,
  });
});

test("v14 recusa entrada inválida e recupera falha sem anunciar resultado inexistente", async ({
  page,
  request,
}) => {
  for (const answers of [
    { occasion: "trabalho" },
    { occasion: "x".repeat(161), existingSelection: "camisa" },
    {
      occasion: "trabalho",
      existingSelection: "camisa",
      extra: "não contratado",
    },
  ]) {
    const response = await request.post(
      `${backend}/api/pde/public/presence-diagnostic`,
      { data: { experienceVersion: version, answers } },
    );
    expect(response.status()).toBe(400);
  }
  await page.goto(`${frontend}${path}?mh_preview=qa&pde_analytics=off`);
  const form = page.getByRole("region", {
    name: "Primeiro ajuste MUSA",
    exact: true,
  });
  await form.locator("textarea").first().fill("encontro à tarde");
  await form.getByRole("button", { name: "Continuar", exact: true }).click();
  await form.locator("textarea").first().fill("vestido e sandálias baixas");
  await page.route("**/api/pde/public/presence-diagnostic", (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: '{"message":"Indisponibilidade local simulada"}',
    }),
  );
  await form
    .getByRole("button", { name: "Descobrir meu primeiro ajuste", exact: true })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "Sua mensagem visual e o primeiro ajuste estão organizados",
    }),
  ).toHaveCount(0);
  await expect(form.locator("textarea").first()).toHaveValue(
    "vestido e sandálias baixas",
  );
  await page.unroute("**/api/pde/public/presence-diagnostic");
  await form
    .getByRole("button", { name: "Descobrir meu primeiro ajuste", exact: true })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "Sua mensagem visual e o primeiro ajuste estão organizados",
    }),
  ).toBeVisible();
  await page.goto(`${frontend}/experiences/versao-inexistente`);
  await expect(
    page.getByRole("heading", { name: "Experiência indisponível" }),
  ).toBeVisible();
});

test("v14 reconcilia compra simulada, sete dias, recuperação e reembolso na mesma versão", async ({
  page,
  request,
}, info) => {
  const id = `v14-${info.project.name}-${Date.now()}`;
  const email = `teste+${id}@sandbox.local`;
  const configured = await request.post(
    `${contract}/test/pepper/transactions/${id}`,
    { data: { status: "paid", email, experienceVersion: version } },
  );
  expect(configured.ok()).toBeTruthy();
  const paid = await request.post(`${backend}/api/pde/access/pepper/webhook`, {
    data: { productSlug: slug, transactionId: id, status: "paid" },
  });
  expect(paid.status()).toBe(201);
  const access = await paid.json();
  expect(access.accessUrl).toMatch(new RegExp(`^${path}/access#access=`));
  const workspaceResponse = await request.get(
    `${backend}/api/pde/access/workspace`,
    { headers: headers(access.token) },
  );
  const workspace = await workspaceResponse.json();
  expect(workspace.experienceVersion).toBe(version);
  expect(workspace.product.missions).toHaveLength(7);
  expect(workspace.product.supportMaterials).toHaveLength(3);
  expect(
    (Date.parse(workspace.accessExpiresAt) - Date.now()) / 86400000,
  ).toBeGreaterThan(89);
  for (const material of workspace.product.supportMaterials) {
    expect((await request.get(`${frontend}${material.url}`)).status()).toBe(
      403,
    );
    expect(
      (
        await request.get(`${frontend}${material.url}`, {
          headers: headers(access.token),
        })
      ).ok(),
    ).toBeTruthy();
  }
  for (const mission of workspace.product.missions) {
    const answers = Object.fromEntries(
      mission.interaction.fields.map(
        (field: { key: string; options: string[] }) => [
          field.key,
          field.options[0],
        ],
      ),
    );
    const response = await request.post(
      `${backend}/api/pde/access/missions/${mission.id}/ai-guidance`,
      {
        headers: headers(access.token),
        data: {
          guidanceType: mission.interaction.guidanceType,
          answers,
          experienceVersion: version,
        },
      },
    );
    expect(response.ok()).toBeTruthy();
    expect(await response.json()).toMatchObject({
      status: "COMPLETED",
      costUsd: 0,
      inputTokens: 0,
      outputTokens: 0,
    });
    expect(
      (
        await request.post(
          `${backend}/api/pde/access/missions/${mission.id}/complete`,
          { headers: headers(access.token) },
        )
      ).ok(),
    ).toBeTruthy();
  }
  await page.goto(`${frontend}${access.accessUrl}`);
  await expect(page.getByText("Produto ativo", { exact: true })).toBeVisible();
  const restored = await request.get(`${backend}/api/pde/access/workspace`, {
    headers: headers(access.token),
  });
  expect(await restored.json()).toMatchObject({
    experienceVersion: version,
    completedMissions: 7,
    progressPercent: 100,
  });
  const recovery = await request.post(`${backend}/api/pde/access/login-link`, {
    data: { productSlug: slug, email, experienceVersion: version },
  });
  expect(recovery.ok()).toBeTruthy();
  const recoveryData = await recovery.json();
  if (process.env.PDE_TEST_EXPECT_MAIL_SENT === "true")
    expect(recoveryData.deliveryStatus).toBe("SENT");
  expect(recoveryData.accessUrl).toMatch(new RegExp(`^${path}/access#access=`));
  await request.post(`${contract}/test/pepper/transactions/${id}`, {
    data: { status: "refunded", email, experienceVersion: version },
  });
  const refund = await request.post(
    `${backend}/api/pde/access/pepper/webhook`,
    { data: { productSlug: slug, transactionId: id, status: "refunded" } },
  );
  expect(refund.status()).toBe(201);
  expect(
    (
      await request.get(`${backend}/api/pde/access/materials/authorize`, {
        headers: headers(access.token),
      })
    ).status(),
  ).toBe(403);
  const summary = await request.get(
    `${backend}/api/pde/access/analytics/${slug}/summary?experienceVersion=${version}`,
  );
  expect((await summary.json()).humanSessions).toBe(0);
});

test("v14 abre checkout somente com liberação e vínculo exatos, sem navegar no pagamento real", async ({
  page,
  request,
}, info) => {
  const email = `teste+checkout-${info.project.name}-${Date.now()}@sandbox.local`;
  const accessResponse = await request.post(
    `${backend}/api/pde/access/magic-link`,
    { data: { productSlug: slug, email, experienceVersion: version } },
  );
  expect(accessResponse.ok()).toBeTruthy();
  const access = await accessResponse.json();
  if (process.env.PDE_TEST_EXPECT_MAIL_SENT === "true")
    expect(access.deliveryStatus).toBe("SENT");
  let status = "PLANNED";
  let experimentId = 106;
  await page.route(
    "**/api/pde/products/metodo-musa-7-dias/commercial-offer?*",
    async (route) => {
      const response = await route.fetch();
      const offer = await response.json();
      await route.fulfill({
        response,
        json: { ...offer, experimentStatus: status, experimentId },
      });
    },
  );
  await page.addInitScript(() => {
    (window as any).openedCheckout = [];
    window.open = (url) => {
      (window as any).openedCheckout.push(String(url));
      return null;
    };
  });
  for (const state of ["PLANNED", "WRONG_EXPERIMENT", "RUNNING"]) {
    status = state === "PLANNED" ? state : "RUNNING";
    experimentId = state === "WRONG_EXPERIMENT" ? 999 : 106;
    const offered = page.waitForResponse((response) =>
      response.url().includes("/commercial-offer?experienceVersion="),
    );
    await page.goto(
      `${frontend}${access.accessUrl.replace("#", `?qaCase=${state}#`)}`,
    );
    await offered;
    await page
      .getByRole("button", { name: "Liberar por R$67", exact: true })
      .click();
    if (state === "RUNNING") {
      await expect
        .poll(() => page.evaluate(() => (window as any).openedCheckout.length))
        .toBe(1);
      const target = await page.evaluate(
        () => (window as any).openedCheckout[0],
      );
      expect(target).toContain("https://go.pepper.com.br/owm6x");
      expect(target).toContain(encodeURIComponent(version));
    } else {
      await expect(
        page.getByText(
          "A liberação do acesso completo ainda não está disponível. Tente novamente em alguns instantes.",
        ),
      ).toBeVisible();
      expect(await page.evaluate(() => (window as any).openedCheckout)).toEqual(
        [],
      );
    }
  }
});
