import { expect, test } from "@playwright/test";

const syntheticPng = Buffer.from([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02, 0x03,
]);

test("homologa conversa, foto, resposta e responsividade do SDK", async ({
  page,
}, testInfo) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));

  await page.goto("/__qa/consultant-sdk-v1");

  await expect(
    page.getByRole("region", { name: "Conversa com Amora" }),
  ).toBeVisible();
  await expect(page.getByText(/Olá, eu sou a Amora/i)).toBeVisible();
  await page
    .getByLabel("Conte o que você precisa agora")
    .fill("Tenho uma reunião hoje. Este look funciona?");
  await page.locator('input[type="file"]').setInputFiles({
    name: "look-qa.png",
    mimeType: "image/png",
    buffer: syntheticPng,
  });
  await expect(page.getByText(/Foto selecionada: look-qa.png/i)).toBeVisible();
  await page
    .getByLabel("Autorizo usar esta foto somente para esta orientação.")
    .check();
  await page.getByRole("button", { name: "Pedir orientação" }).click();
  await expect(
    page.getByRole("button", { name: "Analisando…" }),
  ).toBeDisabled();
  await expect(page.getByText("Minha recomendação")).toBeVisible();
  await expect(page.getByText(/acabamento mais claro/i)).toBeVisible();
  await expect(page.getByAltText("Foto enviada nesta conversa")).toBeVisible();

  const dimensions = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    content: document.documentElement.scrollWidth,
  }));
  expect(dimensions.content).toBeLessThanOrEqual(dimensions.viewport + 1);
  expect(pageErrors).toEqual([]);

  await page.screenshot({
    path: testInfo.outputPath("consultant-sdk-v1.png"),
    fullPage: true,
  });
});

test("bloqueia mídia incompatível sem simular envio", async ({ page }) => {
  await page.goto("/__qa/consultant-sdk-v1");
  await page.locator('input[type="file"]').setInputFiles({
    name: "arquivo-qa.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("não é uma imagem"),
  });

  await expect(page.getByRole("alert")).toHaveText(
    "Envie uma imagem JPEG, PNG ou WebP.",
  );
  await expect(page.getByText(/Foto selecionada:/i)).toHaveCount(0);
});

test("exige entrada e consentimento e preserva o formulário após falha", async ({
  page,
}) => {
  await page.goto("/__qa/consultant-sdk-v1");
  await page.getByRole("button", { name: "Pedir orientação" }).click();
  await expect(page.getByRole("alert")).toHaveText(
    "Escreva uma mensagem ou adicione uma foto.",
  );

  await page
    .getByLabel("Conte o que você precisa agora")
    .fill("Teste de indisponibilidade");
  await page.locator('input[type="file"]').setInputFiles({
    name: "look-qa.png",
    mimeType: "image/png",
    buffer: syntheticPng,
  });
  await page.getByRole("button", { name: "Pedir orientação" }).click();
  await expect(page.getByRole("alert")).toHaveText(
    "Autorize o uso da foto para esta orientação.",
  );

  await page
    .getByLabel("Autorizo usar esta foto somente para esta orientação.")
    .check();
  await page.getByRole("button", { name: "Pedir orientação" }).click();
  await expect(page.getByRole("alert")).toContainText(
    "Sua mensagem e foto continuam aqui",
  );
  await expect(page.getByLabel("Conte o que você precisa agora")).toHaveValue(
    "Teste de indisponibilidade",
  );
  await expect(page.getByText(/Foto selecionada: look-qa.png/i)).toBeVisible();
});

test("explica bloqueio e abre ajuda em nova aba", async ({ page }) => {
  await page.goto("/__qa/consultant-sdk-v1");
  await page
    .getByLabel("Conte o que você precisa agora")
    .fill("Teste de bloqueio");
  await page.getByRole("button", { name: "Pedir orientação" }).click();

  const blocker = page.getByRole("alert");
  await expect(blocker).toContainText("Falta uma informação obrigatória.");
  await expect(blocker).toContainText(
    "Abra a orientação e complete o dado pendente.",
  );
  await expect(
    blocker.getByRole("link", { name: "Abrir orientação" }),
  ).toHaveAttribute("target", "_blank");
});
