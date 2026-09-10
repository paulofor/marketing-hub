import { expect, test } from "@playwright/test";

/** Exercita o estado terminal verdadeiro sem usar pessoas, tokens ou APIs produtivos. */
test("preserva causa, orientação e saída após concluir e retomar segurança", async ({
  page,
}) => {
  const blocker =
    "O objetivo pede conclusão clínica. Reformule como organização de autocuidado ou procure avaliação profissional.";
  let state = {
    sessionToken: "local-safety-session",
    participantReference: null,
    trafficClass: "AGENT_VALIDATION",
    status: "BLOCKED",
    products: [],
    routine: [],
    blocker,
    events: ["EXPERIENCE_STARTED", "SAFETY_LIMIT_BLOCKED"],
    prototypeVersion: "mira-private-v3",
    checkoutMode: "SIMULATED_NO_CHARGE",
    readingFinished: false,
    agentValidation: true,
    scenarioCode: "SAFETY",
    evidenceId: "local-safety-evidence",
  };
  const mutations: string[] = [];
  await page.addInitScript(() =>
    window.sessionStorage.setItem(
      "mira-private-session",
      "local-safety-session",
    ),
  );
  await page.route("**/api/pde/mira/private/v1/**", async (route) => {
    if (route.request().method() !== "GET") {
      mutations.push(route.request().url());
      expect(route.request().postDataJSON()).toEqual({
        eventType: "AGENT_SCENARIO_COMPLETED",
      });
      state = {
        ...state,
        readingFinished: true,
        events: [...state.events, "AGENT_SCENARIO_COMPLETED"],
      };
    }
    await route.fulfill({ json: state });
  });
  await page.goto("/mira-private");
  await page
    .getByRole("button", { name: "Concluir cenário de segurança" })
    .click();
  for (const reload of [false, true]) {
    if (reload) await page.reload();
    await expect(
      page.getByRole("heading", { name: "Sessão encerrada com segurança" }),
    ).toBeVisible();
    await expect(page.getByRole("alert")).toHaveText(blocker);
    await expect(
      page.getByRole("heading", { name: "Como seguir com segurança" }),
    ).toBeVisible();
    await expect(
      page.getByText("Organizar os produtos que já tenho conforme os rótulos", {
        exact: false,
      }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Encerrar e sair" }),
    ).toBeVisible();
    await expect(page.locator(".mira-private-shell")).not.toContainText(
      /Homologação|Marketing Hub|mira-private-v\d/,
    );
    await expect(
      page.locator('.mira-routine-grid, input[type="password"]'),
    ).toHaveCount(0);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
  }
  await page.getByRole("button", { name: "Encerrar e sair" }).click();
  await expect(
    page.getByRole("heading", { name: "Sua rotina, organizada com calma" }),
  ).toBeVisible();
  expect(
    await page.evaluate(() => sessionStorage.getItem("mira-private-session")),
  ).toBeNull();
  expect(mutations).toHaveLength(1);
});

/** Uma falha de persistência mantém o bloqueio e nunca simula uma conclusão. */
test("não encerra segurança quando o callback falha", async ({ page }) => {
  await page.addInitScript(() =>
    window.sessionStorage.setItem(
      "mira-private-session",
      "local-failure-session",
    ),
  );
  await page.route("**/api/pde/mira/private/v1/**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.fulfill({
        status: 503,
        json: { error: "Registro indisponível. Tente novamente." },
      });
      return;
    }
    await route.fulfill({
      json: {
        sessionToken: "local-failure-session",
        status: "BLOCKED",
        products: [],
        routine: [],
        events: ["EXPERIENCE_STARTED", "SAFETY_LIMIT_BLOCKED"],
        blocker: "O objetivo pede conclusão clínica.",
        readingFinished: false,
        agentValidation: true,
        scenarioCode: "SAFETY",
        trafficClass: "AGENT_VALIDATION",
      },
    });
  });
  await page.goto("/mira-private");
  await page
    .getByRole("button", { name: "Concluir cenário de segurança" })
    .click();
  await expect(
    page.getByRole("alert").filter({ hasText: "Registro indisponível" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Concluir cenário de segurança" }),
  ).toBeEnabled();
  await expect(
    page.getByRole("heading", { name: "Sessão encerrada com segurança" }),
  ).toHaveCount(0);
});
