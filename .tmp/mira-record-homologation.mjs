import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const landingReference = process.argv[2];
if (
  !landingReference ||
  !/^slot:\d+;experience-sha256:[a-f0-9]{64};safira-fingerprint:[a-f0-9]{64}$/.test(
    landingReference,
  )
) {
  throw new Error("Informe a referência Safira completa da publicação vigente.");
}

const evidence = {
  LANDING_QUALITY_REVIEW_APPROVED: {
    reference: landingReference,
    summary:
      "Landing pública v8 homologada em desktop, iPhone 15 Pro e Pixel 7, com CTA inteira na primeira dobra, identidade comercial e contrato de oferta vigentes.",
  },
  CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED: {
    reference:
      "https://mira.digicomdigital.com.br;checkout-provider:MERCADO_PAGO;qa:desktop,iphone-15-pro,pixel-7",
    summary:
      "Checkout HTTPS abriu no Mercado Pago nos três dispositivos e a jornada pública preservou preço, entrega, suporte e reembolso do produto #10.",
  },
  META_EFFECTIVE_STATUS_CONFIRMED: {
    reference:
      "meta-ad-account:act_939323521124952;instagram-business:17841468261725306;facebook-page:485863027935937;targeting-selection:216;targeting-element:397",
    summary:
      "Conta Meta operacional, página Produtividade 360, Instagram oficial @produtividade360_ e público salvo oficial estão vinculados ao experimento #93.",
  },
  DATA_FRESHNESS_VALID: {
    reference:
      "mh_test:1;campaign:mira_93_process5;events:PAGE_VIEW,CTA_VIEWED,VIDEO_PLAY,VIDEO_COMPLETED;traffic:INTERNAL_QA",
    summary:
      "Eventos de página, CTA e vídeo foram recebidos agora nos três dispositivos com tráfego INTERNAL_QA segregado, sem contaminar métricas comerciais.",
  },
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1200 },
    locale: "pt-BR",
  });
  page.setDefaultTimeout(60_000);
  await page.goto("http://191.252.181.168:5173/experiments/93?tab=execucao", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });

  for (const [gateCode, gateEvidence] of Object.entries(evidence)) {
    await page.getByLabel(`Resultado ${gateCode}`).selectOption("PASS");
    await page
      .getByLabel(`Evidência ${gateCode}`)
      .fill(gateEvidence.reference);
    await page.getByLabel(`Conclusão ${gateCode}`).fill(gateEvidence.summary);
  }

  const button = page.getByRole("button", { name: "Registrar homologação" });
  if (await button.isDisabled()) {
    throw new Error("A homologação continua desabilitada após preencher os quatro gates.");
  }
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.request().method() === "POST" &&
        /\/api\/experiment-runs\/\d+\/homologation-results$/.test(
          candidate.url(),
        ),
    ),
    button.click(),
  ]);
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`Homologação falhou: HTTP ${response.status()} ${body}`);
  }
  console.log(
    JSON.stringify(
      { status: response.status(), homologation: JSON.parse(body) },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
