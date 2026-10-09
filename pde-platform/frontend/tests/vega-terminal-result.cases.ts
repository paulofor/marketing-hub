import { expect, test } from "@playwright/test";

const v14 = "musa-pde-entry-v14-primeiro-ajuste-aplicavel";
const savedCard = {
  cardId: "synthetic-card-97101",
  action: "Acomode a camisa com conforto",
  application: "Alinhe o tecido da roupa que já escolheu para o almoço.",
  occasion: "Almoço informal",
  selfAssessmentPrompt: "Está confortável ao se mover?",
};

for (const item of [
  {
    code: "BLOCKED",
    card: null,
    notice: /pedido foi bloqueado e nenhum ajuste foi gerado/i,
  },
  { code: "FAILED", card: null, notice: /Não foi possível gerar um ajuste/i },
  { code: "NOT_STARTED", card: null, notice: /Nenhum ajuste foi gerado/i },
  {
    code: "COMPLETED",
    card: savedCard,
    notice: /Seu ajuste continua disponível/i,
  },
]) {
  test(`v14 encerra ${item.code} com mensagem fiel ao resultado e entradas preservadas`, async ({
    page,
  }, info) => {
    let session = {
      id: `synthetic-terminal-97010-${item.code}`,
      prototypeVersion: v14,
      state: "ACTIVE",
      generationStatus: item.code,
      card: item.card,
      error:
        item.code === "BLOCKED"
          ? "O pedido está fora do escopo: não recomenda compras."
          : "Não foi possível concluir esta geração.",
      input: {
        occasion: "Almoço informal",
        existingSelection: "Camisa branca e calça preta",
        optionalNote: "Usar o que já possuo",
      },
      events: { EXPERIENCE_STARTED: {} },
    };
    const writes: string[] = [];
    await page.addInitScript(() =>
      localStorage.setItem(
        "vega-private-session-v1",
        "synthetic-terminal-only",
      ),
    );
    await page.route("**/api/pde/vega/private/v1/**", async (route) => {
      if (route.request().method() === "POST") {
        expect(new URL(route.request().url()).pathname).toBe(
          "/api/pde/vega/private/v1/finish",
        );
        writes.push("finish");
        session = { ...session, state: "FINISHED" };
      }
      await route.fulfill({ status: 200, json: session });
    });
    await page.goto("/agent-validation", { waitUntil: "domcontentloaded" });
    await page
      .getByRole("button", { name: "Encerrar leitura", exact: true })
      .click();
    const notice = page.locator("footer").getByRole("status");
    await expect(notice).toContainText(item.notice);
    if (!item.card) {
      await expect(notice).not.toContainText(/ajuste continua disponível/i);
      await expect(page.locator(".card-result")).toHaveCount(0);
      await expect(page.getByLabel("Qual roupa")).toHaveValue(
        session.input.existingSelection,
      );
      await expect(page.getByLabel("Algo que gostaria")).toHaveValue(
        session.input.optionalNote,
      );
    } else {
      await expect(
        page.getByRole("heading", { name: savedCard.action }),
      ).toBeVisible();
    }
    expect(writes).toEqual(["finish"]);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    await page.screenshot({
      path: info.outputPath(`${item.code}.png`),
      fullPage: true,
    });
    await page.reload();
    await expect(page.locator("footer").getByRole("status")).toContainText(
      item.notice,
    );
    expect(writes).toEqual(["finish"]);
  });
}

for (const number of [12, 13]) {
  test(`v${number} mantém o encerramento histórico sem reescrever seus pareceres`, async ({
    page,
  }) => {
    await page.addInitScript(() =>
      localStorage.setItem("vega-private-session-v1", "synthetic-frozen-only"),
    );
    await page.route("**/api/pde/vega/private/v1/session", (route) =>
      route.fulfill({
        status: 200,
        json: {
          id: `synthetic-frozen-${number}`,
          state: "FINISHED",
          prototypeVersion: `musa-pde-entry-v${number}-primeiro-ajuste-aplicavel`,
          generationStatus: "BLOCKED",
          card: null,
          events: { EXPERIENCE_STARTED: {} },
        },
      }),
    );
    await page.goto("/agent-validation");
    await expect(page.locator("footer").getByRole("status")).toHaveText(
      "Leitura encerrada. Seu ajuste continua disponível durante a validade do convite.",
    );
  });
}

test("v14 preserva o benefício proposto da continuidade e a simulação sem cobrança", async ({
  page,
}) => {
  await page.addInitScript(() =>
    localStorage.setItem("vega-private-session-v1", "synthetic-continuity-v14"),
  );
  await page.route("**/api/pde/vega/private/v1/session", (route) =>
    route.fulfill({
      status: 200,
      json: {
        id: "synthetic-continuity-97014",
        state: "ACTIVE",
        prototypeVersion: v14,
        card: savedCard,
        events: {
          EXPERIENCE_STARTED: {},
          VALUE_MOMENT: {},
          READY_RESULT_USED: {},
        },
      },
    }),
  );
  await page.goto("/agent-validation");
  const proposal = page
    .locator("section")
    .filter({
      has: page.getByRole("heading", {
        name: "Conhecer uma possível continuidade",
      }),
    });
  await expect(proposal).toContainText(
    /sequência guiada, além do primeiro ajuste gratuito/,
  );
  await expect(proposal).toContainText(/Dias 2 a 7/);
  await expect(proposal).toContainText(/R\$ 67 em pagamento único/);
  await expect(proposal).toContainText(
    /dias seguintes ainda não estão disponíveis/,
  );
  await expect(
    proposal.getByRole("button", { name: "Explorar simulação sem cobrança" }),
  ).toBeVisible();
});
