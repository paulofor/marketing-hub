import http from "node:http";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const canonicalV7ContractPath =
  process.env.PDE_MUSA_V7_CONTRACT_PATH ??
  path.resolve(
    currentDirectory,
    "../../backend/src/main/resources/contracts/musa-v7-product-v1.json",
  );
const canonicalV7Product = JSON.parse(
  readFileSync(canonicalV7ContractPath, "utf8"),
);
const kitWhatsAppV1 = JSON.parse(
  readFileSync(
    process.env.PDE_KIT_WHATSAPP_V1_CONTRACT_PATH ??
      path.resolve(
        currentDirectory,
        "../../contracts/kit-whatsapp-pronto-v1.json",
      ),
    "utf8",
  ),
);
const kitWhatsAppV2 = JSON.parse(
  readFileSync(
    process.env.PDE_KIT_WHATSAPP_V2_CONTRACT_PATH ??
      path.resolve(
        currentDirectory,
        "../../contracts/kit-whatsapp-pronto-commercial-v2.json",
      ),
    "utf8",
  ),
);
const kitWhatsAppProduct = { ...kitWhatsAppV1, ...kitWhatsAppV2 };

const port = Number(process.env.MARKETING_HUB_CONTRACT_SERVER_PORT ?? 57181);
const productSlug = "metodo-musa-7-dias";
const v5ExperienceVersion = "musa-pde-entry-v5-video-explicativo";
const v6ExperienceVersion = "musa-pde-entry-v6-video-motivacional";
const v7ExperienceVersion = "musa-pde-entry-v7-espelho-antes-de-sair";
const pepperTransactions = new Map();
const kitWhatsAppOffer = {
  productSlug: "kit-whatsapp-pronto",
  experienceVersion: "kit-whatsapp-pronto-pde-v2",
  layoutKey: "assisted-service-v2",
  experimentId: 89,
  experimentStatus: "PLANNED",
  acquisitionChannel: "DIRECT_ONE_TO_ONE",
  pain: "Você responde um orçamento no WhatsApp, o cliente some e você fica sem saber qual mensagem mandar depois sem parecer insistente — aí a conversa morre e você perde o timing do ‘fechamos ou não?’.",
  proof:
    "Demonstração real na página: uma sequência completa para retomar um orçamento sem parecer insistente, com três follow-ups respeitosos e perguntas de qualificação. A demonstração prova o método sem prometer conversão nem entregar gratuitamente a implantação completa.",
  promise: kitWhatsAppProduct.promise,
  primaryCta: "Quero meu atendimento sob medida",
  priceBrl: 349,
  checkoutUrl:
    "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=133771061-472e4ef4-5d13-4122-831a-706d12435081",
  salesPageUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br",
  targetAudience: kitWhatsAppProduct.audience,
  productFormat: "IMPLANTACAO_PERSONALIZADA",
  deliveryMode: "ASSISTIDA_MANUAL",
  valueUnit: "Respostas, perguntas e follow-ups prontos para revisar e usar",
  supplierDisplayName: "Digicom Digital",
  supplierRegistrationNumber: "25.215.414/0001-69",
  supportEmail: "contato@digicomdigital.com.br",
  termsUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br/terms",
  privacyUrl: "https://kit-whatsapp-pronto.digicomdigital.com.br/privacy",
  refundPolicyUrl:
    "https://kit-whatsapp-pronto.digicomdigital.com.br/refund-policy",
};

const baseProduct = {
  slug: productSlug,
  funnelVersion: "musa-membership-funnel-v1",
  name: "Método MUSA - Experiência Guiada de 7 Dias",
  promise:
    "Descubra o que sua imagem comunica sem intenção e monte em 7 dias uma presença mais elegante.",
  audience: "Mulheres urbanas",
  priceLabel: "R$67",
  theme: {
    primary: "#7a2444",
    accent: "#d6a75c",
    background: "#fff8f3",
    imageUrl: "/assets/musa-cover.png",
  },
  diagnostic: {
    title: "Mapa de Presença MUSA",
    intro: "Entrada publicada pelo Marketing Hub.",
    questions: ["O que minha imagem comunica hoje?"],
  },
  missions: [
    {
      id: "dia-1-ruido-visual",
      day: 1,
      title: "Ler o sinal que sua imagem comunica",
      principle: "A presença cresce quando você identifica o sinal visual.",
      action: "Escolha uma microação para comunicar mais intenção.",
      evidence: "Frase preenchida.",
      visualCue: "Compare a sensação antes/depois.",
    },
  ],
  supportMaterials: [],
  heroVideos: [
    {
      experienceVersion: v6ExperienceVersion,
      placement: "public_diagnostic_initial_explainer",
      playbackUrl: "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
      hlsPlaybackUrl: "/assets/hls/musa-v6-microexperiencia-visivel/index.m3u8",
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: "MARKETING_HUB_MANAGED_HLS",
      status: "READY",
      reviewStatus: "APPROVED",
    },
  ],
  scientificEvidencePack: {
    version: "musa-evidence-pack-v1",
    principles: [],
    practicalApplications: [],
    allowedLanguage: [],
    forbiddenClaims: [],
    references: [],
  },
  completionOffer: "Continuidade",
};

const slots = {
  v5: {
    experienceVersion: v5ExperienceVersion,
    layoutKey: "video-explicativo",
    publicFirstFold: {
      headline: "Você se arruma, mas ainda sente que falta presença?",
      supportingText:
        "Em poucos minutos, o MUSA identifica o primeiro ajuste de presença.",
      videoCtaLabel: "Ver meu primeiro ajuste MUSA",
    },
  },
  v6: {
    experienceVersion: v6ExperienceVersion,
    layoutKey: "video-motivacional",
    publicFirstFold: {
      headline:
        "Se o look parece certo, por que você ainda sente que falta presença?",
      supportingText:
        "Em poucos minutos, o MUSA identifica o ruído que enfraquece sua imagem hoje e entrega um ajuste simples para você começar.",
      videoKicker: "Comece pelo espelho",
      videoHeadline:
        "Veja o detalhe que pode estar apagando sua elegância antes de pensar em comprar outra peça.",
      videoSupportingText:
        "O vídeo mostra como roupa, acabamento, cor e postura podem mudar a leitura da sua imagem quando pequenos ruídos saem do caminho.",
      videoExtraText:
        "Depois da prévia, responda 4 escolhas rápidas. O MUSA transforma suas respostas em um Mapa de Presença do Dia 1.",
      videoCtaLabel: "Revelar meu ajuste MUSA de hoje",
    },
  },
  v7: {
    ...canonicalV7Product,
  },
};

function resolveSlot(url) {
  const explicitSlot = url.searchParams.get("slotCode");
  if (explicitSlot && slots[explicitSlot]) {
    return explicitSlot;
  }
  const experienceVersion = url.searchParams.get("experienceVersion");
  return (
    Object.entries(slots).find(
      ([, slot]) => slot.experienceVersion === experienceVersion,
    )?.[0] ?? "v5"
  );
}

async function readJsonBody(request) {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  return chunks.length === 0
    ? {}
    : JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

const server = http.createServer(async (request, response) => {
  const url = new URL(request.url ?? "/", `http://${request.headers.host}`);
  if (
    request.method === "GET" &&
    url.pathname === "/api/products/public/kit-whatsapp-pronto/pde-experience"
  ) {
    response.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    });
    response.end(JSON.stringify(kitWhatsAppProduct));
    return;
  }
  if (
    request.method === "GET" &&
    url.pathname === "/api/products/public/kit-whatsapp-pronto/commercial-offer"
  ) {
    response.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    });
    response.end(JSON.stringify(kitWhatsAppOffer));
    return;
  }
  const pepperControlMatch = url.pathname.match(
    /^\/test\/pepper\/transactions\/([^/]+)$/,
  );
  if (request.method === "POST" && pepperControlMatch) {
    const transactionHash = decodeURIComponent(pepperControlMatch[1]);
    const body = await readJsonBody(request);
    const transaction = {
      hash: transactionHash,
      payment_status: body.status ?? "paid",
      amount: 6700,
      currency: "BRL",
      utm_source: "sandbox",
      utm_medium: "qa",
      utm_campaign: "musa-v7-commercial-homologation",
      utm_content: `local-e2e__pde_version__${v7ExperienceVersion}`,
      offer: { hash: "owm6x", title: "Método MUSA em 7 dias" },
      customer: { email: body.email },
    };
    pepperTransactions.set(transactionHash, transaction);
    response.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
    });
    response.end(JSON.stringify(transaction));
    return;
  }

  const pepperTransactionMatch = url.pathname.match(
    /^\/public\/v1\/transactions\/([^/]+)$/,
  );
  if (request.method === "GET" && pepperTransactionMatch) {
    const transaction = pepperTransactions.get(
      decodeURIComponent(pepperTransactionMatch[1]),
    );
    response.writeHead(transaction ? 200 : 404, {
      "Content-Type": "application/json; charset=utf-8",
    });
    response.end(
      JSON.stringify(transaction ?? { error: "transaction not found" }),
    );
    return;
  }

  if (
    request.method !== "GET" ||
    url.pathname !== `/api/products/public/${productSlug}/pde-experience`
  ) {
    response.writeHead(404, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ error: "not found" }));
    return;
  }

  const slot = slots[resolveSlot(url)];
  response.writeHead(200, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
  });
  response.end(JSON.stringify({ ...baseProduct, ...slot }));
});

const host = process.env.PDE_CONTRACT_SERVER_HOST ?? "127.0.0.1";
server.listen(port, host, () => {
  console.log(
    `Marketing Hub contract server listening on http://${host}:${port}`,
  );
});
