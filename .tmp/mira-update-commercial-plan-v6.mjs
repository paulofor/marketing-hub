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
  await page.goto("http://191.252.181.168:5173/planning/8", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  await page.getByRole("button", { name: "Editar plano" }).click();

  const fill = async (id, value) => page.locator(id).fill(String(value));
  await fill("#planning-name", "Mira · piloto pequeno Instagram Ads · R$ 49 · gate financeiro");
  await fill("#planning-operational-revenue-target", 49);
  await page.locator("#planning-status").selectOption("BLOCKED");
  await fill("#planning-deadline", "2026-09-30");
  await fill("#planning-revenue-target", 49);
  await fill("#planning-max-budget", 25);
  await fill("#planning-offerPriceBrl", 49);
  await fill("#planning-variableCostPerSaleBrl", 14);
  await fill("#planning-expectedCacBrl", 25);
  await fill("#planning-expectedRefundRatePercent", 10);
  await fill("#planning-fixedOperationalCostBrl", 0);

  await fill(
    "#planning-objective",
    "Comprovar com risco limitado se a demonstração real da aplicação leva uma compradora elegível do Instagram ao pagamento de R$ 49, à entrega e ao primeiro uso. Controle estático e vídeo curto pertencem à mesma hipótese; o piloto não promete comparação estatística entre peças.",
  );
  await fill(
    "#planning-mainOffer",
    "Compra única de R$ 49 por um pacote fixo de duas organizações individualizadas da rotina: inventário, ordem por momento, sobreposições aparentes, dados ausentes, ressalvas e bloqueios. Cada organização concluída consome uma das duas tentativas. Inclui acesso privado, suporte por 30 dias e reembolso integral em 7 dias; sem assinatura, diagnóstico, prescrição ou promessa clínica.",
  );
  await fill(
    "#planning-success",
    "Primeiro marco exploratório: uma compra líquida atribuída e conciliada de R$ 49, entrega concluída e primeiro uso, com CAC de até R$ 25 e contribuição após mídia positiva. Interromper a veiculação nessa primeira compra para conciliar custo, pagamento, entrega, uso e reembolso antes de qualquer continuação. Excluir QA, agentes, bots e duplicidades; isso não comprova escala.",
  );
  await fill(
    "#planning-stop",
    "Não gastar antes de parecer APPROVE de Plutus, Processo 5 aprovado e autorização final. Durante o piloto, parar ao atingir R$ 25 sem compra, na primeira compra para análise, ou imediatamente diante de falha de segurança, pagamento, entrega, limite de consumo, reembolso ou contribuição não positiva.",
  );
  await fill(
    "#planning-next-action",
    "Gerar a revisão LIVE do plano financeiro com os envelopes desta versão, solicitar o parecer de Plutus e executar o preflight do Processo 5. Somente com todos os gates verdes liberar o piloto de R$ 25 e interrompê-lo na primeira compra.",
  );
  await fill(
    "#planning-blocker",
    "Parecer de Plutus e preflight do Processo 5 ainda pendentes. A mídia permanece desligada até ambos aprovarem a mesma versão comercial e financeira.",
  );
  await fill(
    "#planning-root-cause",
    "O produto e o plano financeiro ainda apontavam para a validação privada, enquanto checkout, entrega, criativos e eventos já pertenciam a mira-commercial-v1. Metas de duas compras também divergiam do teto pequeno e da parada na primeira compra.",
  );

  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "PUT" &&
        candidate.url().endsWith("/api/planning/commercial-plans/8"),
    ),
    page.getByRole("button", { name: "Salvar planejamento" }).click(),
  ]);
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`Plano comercial falhou: HTTP ${response.status()} ${body}`);
  }
  const plan = JSON.parse(body);
  const versionsResponse = await page.request.get(
    "http://191.252.181.168/api/planning/commercial-plans/8/versions",
  );
  if (!versionsResponse.ok()) {
    throw new Error(`Histórico falhou: HTTP ${versionsResponse.status()}`);
  }
  const versions = await versionsResponse.json();
  const latest = [...versions].sort(
    (current, next) => next.versionNumber - current.versionNumber,
  )[0];
  console.log(
    JSON.stringify(
      {
        id: plan.id,
        name: plan.name,
        status: plan.status,
        deadline: plan.deadline,
        targetRevenue: plan.targetRevenue,
        operationalRevenueTarget: plan.operationalRevenueTarget,
        maxBudget: plan.maxBudget,
        offerPriceBrl: plan.offerPriceBrl,
        variableCostPerSaleBrl: plan.variableCostPerSaleBrl,
        expectedCacBrl: plan.expectedCacBrl,
        expectedRefundRatePercent: plan.expectedRefundRatePercent,
        fixedOperationalCostBrl: plan.fixedOperationalCostBrl,
        successCriteria: plan.successCriteria,
        stopCriteria: plan.stopCriteria,
        latestVersion: latest.versionNumber,
        latestChangeReason: latest.changeReason,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
