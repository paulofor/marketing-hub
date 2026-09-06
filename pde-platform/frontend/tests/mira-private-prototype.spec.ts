import { expect, test, type Page } from "@playwright/test";

const token = process.env.MIRA_PRIVATE_E2E_TOKEN;

/** Protege a experiência contra o retorno de codinomes e instruções operacionais. */
async function expectParticipantLanguage(page: Page) {
  await expect(page).toHaveTitle("Sua rotina, organizada com calma");
  await expect(page.locator(".mira-private-shell")).not.toContainText(
    /\bMira\b|mira-private-v\d|Marketing Hub|QA_INTERNAL|validação privada/i,
  );
}

test.describe("protótipo privado de Mira", () => {
  test.skip(!token, "Exige token interno segregado da rodada local.");

  for (const readingFinished of [undefined, false, true]) {
    test(`retoma simulação preservada com término ${String(readingFinished)}`, async ({
      page,
    }) => {
      const mutations: string[] = [];
      const restored = {
        sessionToken: "local-legacy-session",
        participantReference: "QA-LOCAL",
        trafficClass: "QA_INTERNAL",
        status: "READY",
        products: [],
        routine: [
          {
            productName: "Hidratante",
            order: 2,
            documentedDirection: "Aplicar após a limpeza",
            safetyNote: "Conforme rótulo",
          },
        ],
        events: [
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED",
        ],
        prototypeVersion: "mira-private-v1",
        checkoutMode: "SIMULATED_NO_CHARGE",
        readingFinished,
      };
      await page.addInitScript(() =>
        window.sessionStorage.setItem(
          "mira-private-session",
          "local-legacy-session",
        ),
      );
      await page.route("**/api/pde/mira/private/v1/**", async (route) => {
        if (route.request().method() !== "GET")
          mutations.push(route.request().url());
        await route.fulfill({ json: restored });
      });
      await page.goto("/mira-private");
      const completed = page.getByRole("button", {
        name: "Simulação concluída",
        exact: true,
      });
      await expect(completed).toHaveCount(1);
      await expect(completed).toBeDisabled();
      await expect(
        page.getByText("Nenhuma compra foi realizada", { exact: false }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Não avançaria por esse valor" }),
      ).toHaveCount(0);
      const finish = page.getByRole("button", {
        name: "Encerrar leitura",
        exact: true,
      });
      await expect(finish).toHaveCount(readingFinished ? 0 : 1);
      await expectParticipantLanguage(page);
      await page.reload();
      await expect(completed).toHaveCount(1);
      expect(mutations).toEqual([]);
      if (!readingFinished) {
        await finish.click();
        await expect.poll(() => mutations.length).toBe(1);
        expect(mutations[0]).toMatch(/\/finish$/);
      }
    });
  }

  test("conclui jornada, retoma e não oferece pagamento", async ({
    page,
  }, testInfo) => {
    const errors: string[] = [];
    const requestedUrls: string[] = [];
    page.on("request", (request) => requestedUrls.push(request.url()));
    page.on("console", (message) => {
      if (message.type() === "error") errors.push(message.text());
    });
    page.on("response", (response) => {
      if (response.status() >= 400)
        errors.push(`${response.status()} ${response.url()}`);
    });
    const legacyPath = await page.request.get(
      "/mira-private/legacy-token-must-not-be-accepted",
    );
    expect(legacyPath.status()).toBe(404);
    const navigation = await page.goto(
      `/mira-private#access=${encodeURIComponent(token!)}`,
    );
    expect(navigation).not.toBeNull();
    expect(await navigation!.headerValue("cache-control")).toContain(
      "no-store",
    );
    expect(await navigation!.headerValue("referrer-policy")).toBe(
      "no-referrer",
    );
    expect(await navigation!.headerValue("x-robots-tag")).toContain("noindex");
    expect(new URL(page.url()).pathname).toBe("/mira-private");
    await expect.poll(() => new URL(page.url()).hash).toBe("");
    expect(requestedUrls.every((url) => !url.includes(token!))).toBe(true);
    await expectParticipantLanguage(page);
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "Começar leitura privada" }).click();
    const entry = page.getByRole("heading", {
      name: "Conte o mínimo necessário",
    });
    const result = page.getByRole("heading", {
      name: "Uma ordem simples para consultar",
    });
    await expect(entry.or(result)).toBeVisible();
    await expectParticipantLanguage(page);
    if (await entry.isVisible()) {
      await page.getByLabel("Nome").nth(0).fill("Hidratante Brisa");
      await page
        .getByLabel("Como o rótulo orienta usar")
        .nth(0)
        .fill("Aplicar após a limpeza");
      await page.getByLabel("Nome").nth(1).fill("Limpador Sereno");
      await page
        .getByLabel("Como o rótulo orienta usar")
        .nth(1)
        .fill("Usar para limpar e enxaguar");
      await page.getByRole("button", { name: "Gerar rotina segura" }).click();
    }
    await expect(result).toBeVisible();
    await expectParticipantLanguage(page);
    await expect(page.locator(".mira-routine-grid h2")).toHaveText([
      "Limpador Sereno",
      "Hidratante Brisa",
    ]);
    await page.reload({ waitUntil: "networkidle" });
    await expect(result).toBeVisible();
    await expect(page.locator(".mira-routine-grid h2")).toHaveText([
      "Limpador Sereno",
      "Hidratante Brisa",
    ]);
    const use = page.getByRole("button", {
      name: "Marcar uma parte como consultada",
    });
    if (await use.count()) await use.click();
    await expect(
      page.getByRole("button", { name: "Resultado consultado" }),
    ).toBeDisabled();
    const preference = page.getByRole("button", {
      name: "Sim, prefiro a rotina pronta",
    });
    if (await preference.count()) await preference.click();
    await expect(
      page.getByRole("button", { name: "Preferência registrada" }),
    ).toBeDisabled();
    const checkout = page.getByRole("button", {
      name: "Simular avanço — sem cobrança",
    });
    if (await checkout.count()) await checkout.click();
    await expect(
      page.getByRole("button", { name: "Simulação concluída" }),
    ).toHaveCount(1);
    await expect(
      page.getByRole("button", { name: "Simulação concluída" }),
    ).toBeDisabled();
    await expect(page.getByText("Nenhuma compra foi realizada")).toBeVisible();
    await expectParticipantLanguage(page);
    await expect(
      page.locator('input[type="card"], input[autocomplete="cc-number"]'),
    ).toHaveCount(0);
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute(
      "content",
      /noindex/,
    );
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);
    expect(errors).toEqual([]);
    await page.screenshot({
      path: `/tmp/mira-${testInfo.project.name}.png`,
      fullPage: true,
    });
  });
});
