import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1400 },
    locale: "pt-BR",
  });
  page.setDefaultTimeout(120_000);
  await page.goto("http://191.252.181.168:5173/products/10/edit", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });

  const slotsResponse = await page.request.get(
    "http://191.252.181.168/api/products/10/pde-production-slots",
  );
  if (!slotsResponse.ok()) {
    throw new Error(`Não foi possível ler o slot: HTTP ${slotsResponse.status()}`);
  }
  const slots = await slotsResponse.json();
  const slot = slots.find(
    (candidate) =>
      candidate.slotCode === "v1" &&
      candidate.experienceVersion === "mira-commercial-v1",
  );
  if (!slot?.publishedExperienceJson) {
    throw new Error("O contrato publicado de mira-commercial-v1 não foi encontrado.");
  }
  const publishedExperience = JSON.parse(slot.publishedExperienceJson);
  if (publishedExperience.heroVideos?.[0]?.experimentVideoAssetId !== 49) {
    throw new Error("O slot ainda não aponta para o sucessor aprovado #49.");
  }
  const expectedTheme = {
    primary: "#6b3e7d",
    accent: "#7a4e8c",
    background: "#f7f2fa",
    imageUrl: "/media/mira-commercial-demo-v3-poster.jpg",
  };
  if (JSON.stringify(publishedExperience.theme) !== JSON.stringify(expectedTheme)) {
    throw new Error("O slot ainda não possui a identidade canônica roxa do Mira.");
  }

  const validationContract = {
    contractVersion: "MIRA_COMMERCIAL_V1",
    sourceReference: "product:10@mira-commercial-v1-process4-reconciliation",
    lineage: {
      predecessorVersion: "PDE_AGENT_VALIDATED_V1",
      predecessorSourceReference: "product:10@agent-validation-v1",
      predecessorRole:
        "Evidência histórica de validação por agentes, sem venda, receita ou autorização comercial.",
      productionSlotReference: "pde-production-slot:9@v1",
      commercialPlanReference: "commercial-plan:8@v6",
      experimentReference: "experiment:93",
    },
    problem:
      "Esforço e incerteza para transformar os produtos de skincare já disponíveis em uma rotina simples, consultável e limitada às orientações documentadas.",
    promise:
      "Organize os produtos que você já tem em uma rotina individualizada, clara e consultável, por R$ 49, com limites explícitos.",
    mechanism:
      "A aplicação estrutura os produtos informados, ordena o uso a partir das orientações documentadas e bloqueia conclusões quando faltam dados ou existe necessidade de avaliação profissional.",
    format: {
      type: "AI_PERSONALIZED_ROUTINE_PACKAGE",
      valueUnit:
        "pacote fixo de duas organizações individualizadas da rotina por compra única",
    },
    delivery: {
      mode: "AI_PERSONALIZED_PAID_DELIVERY",
      personalization: true,
      startsAfter: "PAYMENT_APPROVED",
      access: "Acesso privado por link enviado após confirmação do pagamento.",
      includedUnits: 2,
      unit:
        "uma organização individualizada da rotina com inventário, ordem de uso, sobreposições aparentes, dados ausentes, ressalvas e bloqueios",
      maximumAttempts: 2,
      attemptRule:
        "Cada organização concluída consome exatamente uma das duas tentativas disponíveis; não existe regeneração aberta.",
      supportDays: 30,
      refundDays: 7,
      safetyBoundaries: [
        "Não diagnosticar, prescrever, tratar ou prometer resultado clínico.",
        "Não recomendar compra de cosmético não informado pela cliente.",
        "Bloquear a geração quando rótulos ou orientações documentadas forem insuficientes.",
        "Revogar o acesso após reembolso confirmado.",
      ],
    },
    economics: {
      nature: "PLANNING_ENVELOPE_NOT_REALIZED_RESULT",
      offerPriceBrl: 49,
      variableCostEnvelopeBrl: 14,
      variableCostCoverage: "ALL_VARIABLE_COSTS_EXCLUDING_CAC",
      fixedOperationalCostEnvelopeBrl: 0,
      fixedCostCondition:
        "A versão já está publicada e o piloto não autoriza nova produção, contratação ou gasto fixo incremental; qualquer novo custo invalida o parecer.",
      maximumCacBrl: 25,
      pilotMediaCapBrl: 25,
      targetSales: 1,
      targetRevenueBrl: 49,
      contributionAfterMaximumCacBrl: 10,
      commercialSpendAuthorized: false,
      approvalRequired: "PLUTUS_AND_PROCESS_5_PREFLIGHT",
    },
    successEvidence: {
      firstMilestoneSales: 1,
      funnelEvents: [
        "PAGE_VIEW",
        "CTA_CLICK",
        "CHECKOUT_STARTED",
        "PURCHASE_COMPLETED",
        "ACCESS_RELEASED",
        "DELIVERY_COMPLETED",
        "FIRST_USE",
        "JOURNEY_COMPLETED",
        "REFUND_CONFIRMED",
      ],
      customerEvidence: [
        "PURCHASE_RECONCILED",
        "DELIVERY_COMPLETED",
        "FIRST_USE",
        "REFUND",
      ],
      exclusions: [
        "QA",
        "AGENT_VALIDATION",
        "BOT",
        "INTERNAL_TEST",
        "DUPLICATE",
      ],
    },
    decisionRules: {
      continue:
        "Reavaliar após a primeira compra líquida atribuída, entregue e usada, somente com contribuição conciliada positiva.",
      adjust:
        "Ajustar se houver clique ou checkout sem compra, CAC acima do limite, atrito corrigível, custo fora do envelope ou reembolso.",
      stop:
        "Parar no teto de R$ 25 sem compra, na primeira compra para análise, ou imediatamente diante de falha de pagamento, entrega, segurança ou contribuição não positiva.",
    },
    evidence: {
      homologationContract:
        "pde-platform/contracts/mira-commercial-homologation-v8.json",
      commercialRuntime: "mira-commercial-v1",
      checkoutProvider: "MERCADO_PAGO",
      approvedVideoAssetId: 49,
      staticControlAssetId: 322,
      productProofAssetId: 321,
      process4RunId: 28,
      independentReviews: ["agent-task:543", "agent-task:544"],
    },
  };

  const checkoutMonetization = {
    offerPriceBrl: 49,
    billing: "ONE_TIME",
    acquisitionChannel: "INSTAGRAM_ADS",
    dailyBudgetBrl: 25,
    maxBudgetBrl: 25,
    maxCacBrl: 25,
    variableCostPerSaleBrl: 14,
    fixedOperationalCostBrl: 0,
    contributionAfterMaximumCacBrl: 10,
    targetSales: 1,
    commercialSpendAuthorized: false,
    financialProjectionStatus: "PENDING_PLUTUS_AND_PROCESS_5_PREFLIGHT",
    refundPolicy:
      "FULL_REFUND_WITHIN_7_DAYS_WITH_AUTOMATED_ACCESS_REVOCATION",
    supportPolicy: "AUTOMATED_ACCESS_AND_DELIVERY_SUPPORT_FOR_30_DAYS",
    truthBoundary:
      "Valores são limites de planejamento para o piloto; não representam venda, CAC, receita, margem ou custo realizado. Qualquer novo custo exige reavaliação.",
  };
  const desireAssociationMap = JSON.parse(
    await page.locator("#product-desireAssociationMapJson").inputValue(),
  );
  desireAssociationMap.evidence ??= {};
  desireAssociationMap.evidence.sources ??= [];
  const historicalPilot = desireAssociationMap.evidence.sources.find(
    (source) => source.reference === "Aprovação do usuário: Aprovo o piloto recomendado",
  );
  if (historicalPilot) {
    historicalPilot.supersededBy =
      "Solicitação do usuário de 29/09/2026 para liberar somente um piloto pequeno após o preflight, materializada no experimento #93 com parada na primeira compra.";
  }
  if (
    !desireAssociationMap.evidence.sources.some(
      (source) => source.reference === "Solicitação do usuário: piloto pequeno após o Processo 5",
    )
  ) {
    desireAssociationMap.evidence.sources.push({
      reference: "Solicitação do usuário: piloto pequeno após o Processo 5",
      date: "2026-09-29",
      scope:
        "Regra vigente: somente após o preflight, teto total de R$ 25 e parada da mídia na primeira compra atribuída e conciliada; entrega e primeiro uso fecham a análise inicial, sem prova de escala.",
    });
  }
  desireAssociationMap.measurementPlan.budgetBrl = 25;
  desireAssociationMap.measurementPlan.success =
    "Parar a mídia na primeira compra atribuída e conciliada de R$ 49; confirmar entrega, primeiro uso, custo integral, reembolso e contribuição antes de decidir repetir. Uma venda é marco exploratório e não prova escala.";
  desireAssociationMap.measurementPlan.stopRule =
    "Parar no teto de R$ 25 sem compra ou imediatamente na primeira compra para análise.";

  await page.locator("#product-name").fill("Mira · sua rotina organizada");
  await page.locator("#product-validationDefinitionVersion").fill("MIRA_COMMERCIAL_V1");
  await page
    .locator("#product-validationDefinitionJson")
    .fill(JSON.stringify(validationContract, null, 2));
  await page
    .locator("#product-pdeExperienceJson")
    .fill(JSON.stringify(publishedExperience, null, 2));
  await page
    .locator("#product-desireAssociationMapJson")
    .fill(JSON.stringify(desireAssociationMap, null, 2));
  await page
    .locator("#product-colorPalette")
    .fill("#6b3e7d, #7a4e8c, #f7f2fa");
  await page
    .locator("#product-riskReversal")
    .fill(
      "Reembolso integral solicitado pelo canal oficial contato@digicomdigital.com.br em até 7 dias corridos após a compra, com o e-mail usado no pagamento. Após a confirmação do estorno pelo Mercado Pago, o acesso é revogado automaticamente e o reembolso é conciliado sem duplicidade. Sem assinatura ou renovação.",
    );
  await page
    .locator("#product-commercialNotes")
    .fill(
      "Histórico preservado: dossiê #36 e validação privada comprovaram somente o funcionamento técnico do protótipo, sem venda. A aquisição vigente é exclusivamente por mídia paga no Instagram. Processo 4 #28 concluído; produto #10, experiência mira-commercial-v1, checkout Mercado Pago de R$ 49, vídeo aprovado #49, prova #321, controle #322, criativos #538 e #539, Instagram oficial #1 e público salvo #216/elemento #397 reconciliados. O Processo 5 deve concluir Psique, Têmis e preflight antes de qualquer liberação. Teto diário e total do piloto: R$ 25; uma venda encerra a mídia para análise. Nenhum gasto é autorizado por estas notas.",
    );
  await page
    .locator("#product-revenueModel")
    .fill("Compra única de R$ 49; sem assinatura ou renovação");
  await page
    .locator("#product-valueUnit")
    .fill(
      "Pacote fixo de duas organizações individualizadas da rotina por compra única",
    );
  await page
    .locator("#product-valueEvidenceMetric")
    .fill(
      "Compra líquida atribuída, entrega concluída, primeiro uso, reembolso, CAC e contribuição conciliados",
    );
  await page
    .locator("#product-checkoutMonetization")
    .fill(JSON.stringify(checkoutMonetization, null, 2));

  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "PUT" &&
        candidate.url().endsWith("/api/products/10"),
    ),
    page.getByRole("button", { name: "Salvar alterações" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`Produto #10 falhou: HTTP ${response.status()} ${body}`);
  }
  const product = JSON.parse(body);
  const savedContract = JSON.parse(product.validationDefinitionJson);
  const savedExperience = JSON.parse(product.pdeExperienceJson);
  console.log(
    JSON.stringify(
      {
        id: product.id,
        name: product.name,
        colorPalette: product.colorPalette,
        validationDefinitionVersion: product.validationDefinitionVersion,
        format: savedContract.format,
        delivery: savedContract.delivery,
        successEvidence: savedContract.successEvidence,
        economics: savedContract.economics,
        pdeExperienceVersion: savedExperience.experienceVersion,
        pdeTheme: savedExperience.theme,
        pdeVideoAssetId: savedExperience.heroVideos?.[0]?.experimentVideoAssetId,
        revenueModel: product.revenueModel,
        riskReversal: product.riskReversal,
        commercialNotes: product.commercialNotes,
        valueUnit: product.valueUnit,
        checkoutMonetization: JSON.parse(product.checkoutMonetization),
        measurementPlan: JSON.parse(product.desireAssociationMapJson).measurementPlan,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
