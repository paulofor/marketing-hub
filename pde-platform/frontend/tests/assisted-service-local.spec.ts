import { expect, test } from "@playwright/test";

const productSlug = "kit-whatsapp-pronto";
const backendBaseUrl =
  process.env.PDE_ASSISTED_BACKEND_URL ?? "http://127.0.0.1:8096";
const mailBaseUrl =
  process.env.PDE_ASSISTED_MAIL_URL ?? "http://sandbox-mail:8025";

test.beforeEach(async ({ page, request }) => {
  await request.post(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/reset-campaign-start`,
  );
  await page.goto("/?mh_test=1");
});

test("conclui a jornada assistida com marcos operacionais, preserva progresso e abre materiais", async ({
  page,
  request,
}, testInfo) => {
  await expect(
    page.getByRole("heading", { name: "Kit WhatsApp Pronto" }),
  ).toBeVisible();
  await expect(page.getByText("Microvalor em até 12 horas")).toBeVisible();
  await expect(page.getByText("Revisão humana antes do uso")).toBeVisible();

  const email = `teste+kit-whatsapp-${testInfo.project.name}-${Date.now()}@sandbox.local`;
  await page.getByLabel("E-mail").fill(email);
  await page.getByRole("button", { name: "Entrar na homologação" }).click();

  await expect(page.getByTestId("assisted-workspace")).toBeVisible();
  await expect(page.getByText(email)).toBeVisible();
  await expect(page.getByText("0 de 6 etapas concluídas")).toBeVisible();
  await expect(
    page.getByText(
      /prazos começam após pagamento aprovado e entrada completa/i,
    ),
  ).toBeVisible();
  await expect(
    page.getByText(/não informe nomes, telefones, endereços/i),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "Abrir material" })).toHaveCount(
    0,
  );
  await expect(
    page.getByText(
      /materiais liberados após a equipe concluir a entrega completa/i,
    ),
  ).toBeVisible();
  await expect(
    page.getByText(/aguardando a equipe concluir esta etapa/i),
  ).toHaveCount(4);

  await page
    .getByLabel("Serviços principais")
    .fill("Manicure, alongamento e manutenção de unhas.");
  await page
    .getByLabel("Dúvidas e objeções recorrentes")
    .fill("Preço, duração e agenda disponível.");
  await page
    .getByLabel("Regras de preço, agenda e área atendida")
    .fill("Atendimento com hora marcada na região central.");
  await page
    .getByLabel("Tom de voz desejado")
    .fill("Acolhedor, profissional e direto.");
  await page
    .getByLabel("Cinco situações equivalentes ou exemplos anonimizados")
    .fill(
      "1. Pedido de preço; 2. Horário; 3. Durabilidade; 4. Reagendamento; 5. Primeira visita.",
    );
  await page.getByRole("button", { name: "Concluir etapa" }).first().click();
  await expect(page.getByText("1 de 6 etapas concluídas")).toBeVisible();
  await expect(
    page.getByText("Etapa salva. Você pode sair e retomar com o mesmo acesso."),
  ).toBeVisible();

  await page.reload();
  await expect(page.getByTestId("assisted-workspace")).toBeVisible();
  await expect(page.getByText("1 de 6 etapas concluídas")).toBeVisible();

  const accessToken = await page.evaluate(
    (slug) => localStorage.getItem(`pde-access:${slug}`),
    productSlug,
  );
  expect(accessToken).toBeTruthy();

  const deliveryBeforeOperation = await request.get(
    `${backendBaseUrl}/api/pde/access/${accessToken}/deliveries/microvalor-12h/download`,
  );
  expect(deliveryBeforeOperation.ok()).toBeFalsy();

  const unauthorizedCustomerAdvance = await request.post(
    `${backendBaseUrl}/api/pde/access/${accessToken}/missions/conferencia-de-completude/complete`,
  );
  expect(unauthorizedCustomerAdvance.ok()).toBeFalsy();

  const unauthorizedOperationAdvance = await request.post(
    `${backendBaseUrl}/api/internal/pde/assisted-operation/access/${accessToken}/missions/conferencia-de-completude/complete`,
    { headers: { "X-PDE-Operation-Token": "token-incorreto" } },
  );
  expect(unauthorizedOperationAdvance.status()).toBe(403);

  const operationalStages = [
    { missionId: "conferencia-de-completude" },
    { missionId: "diagnostico-humano" },
    {
      missionId: "microvalor-12h",
      deliveryTitle: "Microentrega personalizada — Studio Aurora",
      deliveryVersion: "micro-v1",
      deliveryContent:
        "# Microentrega\n\nCenários: preço, agenda e durabilidade.\n\nPerguntas: qual serviço deseja e para quando precisa?\n\nResposta inicial: Olá! Para manicure com hora marcada, diga o serviço e o melhor período para eu confirmar a agenda. Revisar antes de enviar.",
    },
    {
      missionId: "entrega-completa-48h",
      deliveryTitle: "Kit completo personalizado — Studio Aurora",
      deliveryVersion: "kit-v1",
      deliverySections: [
        {
          sectionId: "responses",
          items: Array.from(
            { length: 15 },
            (_, index) =>
              `Resposta ${index + 1}: Olá! Para manicure, alongamento ou manutenção com hora marcada, confirme o serviço e o melhor período; responderei com preço e disponibilidade em tom acolhedor.`,
          ),
        },
        {
          sectionId: "qualificationQuestions",
          items: [
            "Qual serviço você deseja: manicure, alongamento ou manutenção?",
            "Para qual dia e período você precisa do atendimento?",
            "É sua primeira visita ao Studio Aurora?",
            "Você precisa remover ou manter algum alongamento anterior?",
            "Qual região central facilita seu atendimento?",
            "Existe alguma preferência de acabamento?",
            "Você já conhece nossas regras de reagendamento?",
            "Posso confirmar a duração prevista antes de reservar?",
          ],
        },
        {
          sectionId: "followUps",
          items: [
            "Follow-up 1: Posso reservar o melhor período para você hoje?",
            "Follow-up 2: Ainda quer que eu confirme preço e duração deste serviço?",
            "Follow-up 3: Se o horário mudou, diga outro período e eu verifico.",
            "Follow-up 4: Posso encerrar por agora e retomar quando você preferir.",
          ],
        },
        {
          sectionId: "escalationRules",
          items: [
            "Interromper o modelo e revisar pessoalmente qualquer reclamação.",
            "Não inventar preço, desconto, duração ou disponibilidade.",
            "Confirmar com uma pessoa qualquer exceção de reagendamento.",
            "Não reutilizar dados pessoais de conversas anteriores.",
          ],
        },
        {
          sectionId: "usageGuide",
          items: [
            "Escolha a resposta correspondente à intenção recebida.",
            "Substitua somente os campos confirmados pelo negócio.",
            "Leia integralmente antes de copiar para o WhatsApp.",
            "Faça as perguntas de qualificação uma por vez.",
            "Registre ajustes pequenos para a revisão do kit.",
          ],
        },
        {
          sectionId: "checklist",
          items: [
            "Serviço confirmado.",
            "Preço confirmado.",
            "Agenda confirmada.",
            "Área atendida confirmada.",
            "Tom acolhedor preservado.",
            "Nenhum dado pessoal reaproveitado.",
            "Exceções revisadas por pessoa.",
            "Mensagem lida antes do envio.",
          ],
        },
      ],
    },
  ];
  const incompleteDelivery = await request.post(
    `${backendBaseUrl}/api/internal/pde/assisted-operation/access/${accessToken}/missions/microvalor-12h/complete`,
    { headers: { "X-PDE-Operation-Token": "pde-local-operation-test" } },
  );
  expect(incompleteDelivery.ok()).toBeFalsy();

  for (const stage of operationalStages) {
    const operationAdvance = await request.post(
      `${backendBaseUrl}/api/internal/pde/assisted-operation/access/${accessToken}/missions/${stage.missionId}/complete`,
      {
        headers: { "X-PDE-Operation-Token": "pde-local-operation-test" },
        data:
          stage.deliveryContent || stage.deliverySections
            ? {
                deliveryTitle: stage.deliveryTitle,
                deliveryVersion: stage.deliveryVersion,
                deliveryContent: stage.deliveryContent,
                deliverySections: stage.deliverySections,
              }
            : undefined,
      },
    );
    expect(
      operationAdvance.ok(),
      `etapa operacional não concluída: ${stage.missionId}`,
    ).toBeTruthy();
  }

  await page.reload();
  await expect(page.getByText("5 de 6 etapas concluídas")).toBeVisible();
  await expect(page.getByTestId("delivery-microvalor-12h")).toContainText(
    "preço, agenda e durabilidade",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Resposta 15",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Perguntas de qualificação",
  );
  await expect(page.getByTestId("delivery-entrega-completa-48h")).toContainText(
    "Checklist de revisão",
  );
  await expect(
    page.getByRole("link", { name: "Baixar entrega personalizada" }),
  ).toHaveCount(2);
  await expect(page.getByRole("link", { name: "Abrir material" })).toHaveCount(
    7,
  );

  for (const link of await page
    .getByRole("link", { name: "Baixar entrega personalizada" })
    .evaluateAll((nodes) =>
      nodes.map((node) => (node as HTMLAnchorElement).href),
    )) {
    const response = await request.get(link);
    expect(
      response.ok(),
      `entrega personalizada indisponível: ${link}`,
    ).toBeTruthy();
    expect(response.headers()["content-disposition"]).toContain("attachment");
    expect((await response.text()).length).toBeGreaterThan(100);
  }

  for (const link of await page
    .getByRole("link", { name: "Abrir material" })
    .evaluateAll((nodes) =>
      nodes.map((node) => (node as HTMLAnchorElement).href),
    )) {
    const response = await request.get(link);
    expect(response.ok(), `material indisponível: ${link}`).toBeTruthy();
    expect((await response.text()).length).toBeGreaterThan(150);
  }

  await page
    .getByLabel("Três respostas escolhidas")
    .fill("Preço, agenda e durabilidade.");
  await page
    .getByLabel("Bloco de qualificação escolhido")
    .fill("Serviço desejado e melhor período.");
  await page
    .getByLabel("Regra de escalonamento escolhida")
    .fill("Interromper o modelo em reclamações ou exceções de preço.");
  await page
    .getByLabel("Situação da primeira aplicação")
    .selectOption("PLANNED");
  await page
    .getByLabel("Retorno observado na primeira aplicação")
    .fill("Ainda não aplicado; apenas planejado para homologar o bloqueio.");
  await page
    .getByLabel("Revisão da primeira aplicação")
    .fill("Planejamento revisado, sem afirmar que houve uso real.");
  await page.getByRole("button", { name: "Concluir etapa" }).click();
  await expect(page.getByRole("alert")).toContainText(
    /primeira aplicação manual real/i,
  );
  await page
    .getByLabel("Situação da primeira aplicação")
    .selectOption("APPLIED");
  await page
    .getByLabel("Retorno observado na primeira aplicação")
    .fill(
      "Uso manual de homologação concluído: a pergunta de serviço produziu resposta clara, sem envio a cliente real.",
    );
  await page
    .getByLabel("Revisão da primeira aplicação")
    .fill(
      "O fluxo exigiu leitura humana e não publicou, enviou ou tratou a homologação como venda.",
    );
  await page.getByRole("button", { name: "Concluir etapa" }).click();
  await expect(page.getByText("6 de 6 etapas concluídas")).toBeVisible();

  await page
    .getByLabel("Como podemos ajudar?")
    .fill("Preciso revisar uma resposta antes da primeira aplicação.");
  await page
    .getByRole("button", { name: "Solicitar suporte ou revisão" })
    .click();
  await expect(page.getByText(/pedido de suporte registrado/i)).toBeVisible();
  await page.reload();
  await expect(
    page.getByText(/existe um pedido de suporte aberto/i),
  ).toBeVisible();

  const magicLinkResponse = await request.post(
    `${backendBaseUrl}/api/pde/access/login-link`,
    {
      data: { productSlug, email },
    },
  );
  expect(magicLinkResponse.ok()).toBeTruthy();
  expect((await magicLinkResponse.json()).deliveryStatus).toBe("SENT");
  await expect
    .poll(async () => {
      const mailResponse = await request.get(
        `${mailBaseUrl}/api/v1/messages`,
      );
      const mailbox = await mailResponse.json();
      return mailbox.messages.some((message: { To?: { Address?: string }[] }) =>
        message.To?.some((recipient) => recipient.Address === email),
      );
    })
    .toBeTruthy();

  await page.evaluate(
    (slug) => localStorage.removeItem(`pde-access:${slug}`),
    productSlug,
  );
  const secondPage = await page.context().newPage();
  await secondPage.goto(`/access/${accessToken}?mh_test=1`);
  await expect(secondPage.getByTestId("assisted-workspace")).toBeVisible();
  await expect(secondPage.getByText(email)).toBeVisible();
  await expect(secondPage.getByText("6 de 6 etapas concluídas")).toBeVisible();
  await expect
    .poll(() =>
      secondPage.evaluate(
        (slug) => localStorage.getItem(`pde-access:${slug}`),
        productSlug,
      ),
    )
    .toBe(accessToken);
  await secondPage.close();

  await expect
    .poll(async () => {
      const response = await request.get(
        `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary`,
      );
      const summary = await response.json();
      return {
        commercialEvents: summary.totalEvents,
        rawEvents: summary.rawTotalEvents,
        humanSessions: summary.humanSessions,
        hasInternalQaSession: summary.internalQaSessions >= 1,
      };
    })
    .toEqual({
      commercialEvents: 0,
      rawEvents: expect.any(Number),
      humanSessions: 0,
      hasInternalQaSession: true,
    });

  const summaryResponse = await request.get(
    `${backendBaseUrl}/api/pde/access/analytics/${productSlug}/summary`,
  );
  expect((await summaryResponse.json()).rawTotalEvents).toBeGreaterThanOrEqual(
    12,
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth + 1,
    ),
  ).toBeTruthy();
});

test("bloqueia e orienta acesso inválido sem perder o contrato público", async ({
  page,
}) => {
  await page.goto("/?access=token-invalido&mh_test=1");
  await expect(
    page.getByRole("heading", { name: "Kit WhatsApp Pronto" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toContainText(
    /acesso|encontrado|solicitação/i,
  );
  await expect(page.getByTestId("assisted-workspace")).toHaveCount(0);
});

test("exige e-mail válido antes de criar acesso segregado", async ({
  page,
}) => {
  await page.getByLabel("E-mail").fill("email-invalido");
  await page.getByRole("button", { name: "Entrar na homologação" }).click();
  await expect(page.getByTestId("assisted-workspace")).toHaveCount(0);
  expect(
    await page
      .getByLabel("E-mail")
      .evaluate((input: HTMLInputElement) => input.validity.typeMismatch),
  ).toBeTruthy();
});
