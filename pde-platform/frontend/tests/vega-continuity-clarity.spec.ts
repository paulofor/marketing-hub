import { expect, test } from "@playwright/test";

// Sessão sintética interceptada: a revisão da proposta não cria dados, chamadas de IA ou cobrança.
test("cartão salvo explica o ganho da proposta antes da simulação e mantém limites claros", async ({
  page,
}, testInfo) => {
  const writes: string[] = [];
  let session = {
    id: "local-clarity-91008",
    state: "ACTIVE",
    prototypeVersion: "musa-pde-entry-v13-primeiro-ajuste-aplicavel",
    generationStatus: "COMPLETED",
    preference: "CARD",
    card: {
      cardId: "local-card-91101",
      action: "Ajuste o caimento com conforto.",
      application: "Use a combinação já disponível para a ocasião escolhida.",
      occasion: "Dia de trabalho",
      selfAssessmentPrompt: "Ficou confortável ao se mover?",
    },
    events: {
      EXPERIENCE_STARTED: {},
      VALUE_MOMENT: {},
      READY_RESULT_USED: {},
    } as Record<string, unknown>,
  };
  await page.addInitScript(() =>
    localStorage.setItem("vega-private-session-v1", "sandbox-clarity-only"),
  );
  await page.route("**/api/pde/vega/private/v1/**", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    if (request.method() === "GET" && pathname.endsWith("/session")) {
      await route.fulfill({ status: 200, json: session });
      return;
    }
    writes.push(request.method() + " " + pathname);
    if (pathname.endsWith("/events")) {
      expect(request.postDataJSON()).toEqual({
        eventType: "CHECKOUT_STARTED",
        answer: "Abrir voluntariamente a simulação de R$ 67",
      });
      session = {
        ...session,
        events: { ...session.events, CHECKOUT_STARTED: {} },
      };
      await route.fulfill({ status: 200, json: session });
      return;
    }
    await route.abort();
  });
  await page.goto("/agent-validation", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: session.card.action }),
  ).toBeVisible();
  const proposal = page.locator("section").filter({
    has: page.getByRole("heading", {
      name: "Conhecer uma possível continuidade",
    }),
  });
  await expect(proposal).toContainText(/ajuste gratuito.*ocasião/i);
  await expect(proposal).toContainText(/Dias 2 a 7.*outras ocasiões/i);
  await expect(proposal).toContainText(
    /orientações curtas.*roupas que você já tem/i,
  );
  await expect(proposal).toContainText(
    /sequência guiada.*R\$ 67 em pagamento único/i,
  );
  await expect(proposal).toContainText(
    /dias seguintes ainda não estão disponíveis/i,
  );
  await expect(proposal).toContainText(/simulação sem cobrança/i);
  await proposal
    .getByRole("button", { name: "Explorar simulação sem cobrança" })
    .click();
  await expect(
    proposal.getByRole("heading", { name: "Simulação concluída" }),
  ).toBeVisible();
  await expect(proposal).toContainText(
    "Nenhuma compra foi feita. Não há cobrança, pedido ou acesso pago.",
  );
  await expect(
    page.getByRole("heading", { name: session.card.action }),
  ).toBeVisible();
  expect(writes).toEqual(["POST /api/pde/vega/private/v1/events"]);
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: testInfo.outputPath("continuity-private.png"),
    fullPage: true,
  });
});

// A mensagem do predecessor não recebe a variante corrigida por atualização da imagem compartilhada.
test("sessão v12 preserva a mensagem homologada anteriormente", async ({
  page,
}) => {
  await page.addInitScript(() =>
    localStorage.setItem("vega-private-session-v1", "sandbox-predecessor-only"),
  );
  await page.route("**/api/pde/vega/private/v1/session", (route) =>
    route.fulfill({
      status: 200,
      json: {
        id: "local-predecessor-91009",
        state: "ACTIVE",
        prototypeVersion: "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
        generationStatus: "COMPLETED",
        preference: "CARD",
        card: {
          cardId: "local-card-91102",
          action: "Acomode a camisa",
          application: "Alinhe com conforto",
          occasion: "Almoço",
          selfAssessmentPrompt: "Está confortável?",
        },
        events: {
          EXPERIENCE_STARTED: {},
          VALUE_MOMENT: {},
          READY_RESULT_USED: {},
        },
      },
    }),
  );
  await page.goto("/agent-validation");
  await expect(
    page.getByText(
      "A proposta de continuidade é uma jornada dos Dias 2 a 7, por R$ 67 em pagamento único. Nesta experiência você só pode explorar uma simulação.",
    ),
  ).toBeVisible();
  await expect(
    page.getByText(/sequência guiada, além do primeiro ajuste gratuito/),
  ).toHaveCount(0);
});
