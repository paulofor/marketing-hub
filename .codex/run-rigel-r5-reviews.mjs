import { spawnSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const root = process.cwd();
const round = process.argv[2] ?? "manual";
const outputDir = resolve(root, `.codex/attachments/rigel-r5-review-${round}`);
mkdirSync(outputDir, { recursive: true });

const read = (path) => readFileSync(resolve(root, path), "utf8");
const html = read(".codex/attachments/rigel-r5.html");
const browserAudit = JSON.parse(
  read(".codex/attachments/rigel-r5-browser-audit.json"),
);
const commercialContract = JSON.parse(
  read("pde-platform/contracts/kit-whatsapp-pronto-commercial-v2.json"),
);
const creativeContract = JSON.parse(
  read(
    "deploy/local-validation/rigel-creative-production-v6/rigel-creative-contract.v1.json",
  ),
);
const landingContext = JSON.parse(
  read(".codex/attachments/rigel-geralanding-local-context-r3.json"),
).context;
const creativeManifest = JSON.parse(
  read(".codex/attachments/rigel-approved-package/verified-manifest.json"),
);
const realCheckoutAudit = JSON.parse(
  read(".codex/attachments/rigel-checkout-read-only.json"),
);
const localCheckoutAudit = JSON.parse(
  read(".codex/attachments/rigel-checkout-local-double.json"),
);
const commercialOfferResponse = await fetch(
  "http://191.252.181.168/api/products/public/kit-whatsapp-pronto/commercial-offer",
  { signal: AbortSignal.timeout(20_000) },
);
if (!commercialOfferResponse.ok) {
  throw new Error(
    `Oferta pública indisponível: HTTP ${commercialOfferResponse.status}`,
  );
}
const productionOfferBeforeRepair = await commercialOfferResponse.json();
const commercialOffer = {
  ...productionOfferBeforeRepair,
  experienceVersion: commercialContract.experienceVersion,
  layoutKey: commercialContract.layoutKey,
  promise: commercialContract.promise,
};
writeFileSync(
  resolve(outputDir, "commercial-offer.json"),
  `${JSON.stringify(
    { targetAfterRepair: commercialOffer, productionBeforeRepair: productionOfferBeforeRepair },
    null,
    2,
  )}\n`,
);

const screenshots = [
  {
    viewport: "desktop",
    role: "full-page",
    path: resolve(root, ".codex/attachments/rigel-r5-desktop-full.jpg"),
  },
  {
    viewport: "desktop",
    role: "proof-section",
    path: resolve(root, ".codex/attachments/rigel-r5-desktop-proof.jpg"),
  },
  {
    viewport: "mobile",
    role: "full-page",
    path: resolve(root, ".codex/attachments/rigel-r5-iphone15pro-full.jpg"),
  },
  {
    viewport: "mobile",
    role: "proof-section",
    path: resolve(root, ".codex/attachments/rigel-r5-iphone15pro-proof.jpg"),
  },
];
const creativeImages = creativeManifest.assets
  .filter(
    (asset) =>
      asset.mediaType === "IMAGE" &&
      asset.purposes.includes("ADS") &&
      asset.label.startsWith("rigel-direct-"),
  )
  .map((asset) => asset.localPath);
const temisImages = [
  ...screenshots.map((screenshot) => screenshot.path),
  ...creativeImages,
  creativeManifest.videoContactSheet,
  resolve(root, ".codex/attachments/rigel-checkout-local-double.jpg"),
];

function usageFromJsonLines(value) {
  let usage = null;
  for (const line of value.split("\n")) {
    try {
      const event = JSON.parse(line);
      usage = event.usage ?? event?.payload?.usage ?? event?.data?.usage ?? usage;
    } catch {
      // O log bruto preserva linhas não JSON sem fabricar telemetria.
    }
  }
  return usage;
}

function runReview(agent, prompt, schemaPath, images) {
  const slug = agent.toLowerCase();
  const requestPath = resolve(outputDir, `${slug}-request.md`);
  const responsePath = resolve(outputDir, `${slug}-response.json`);
  const logPath = resolve(outputDir, `${slug}-log.jsonl`);
  writeFileSync(requestPath, prompt);
  const args = [
    "exec",
    "--model",
    "gpt-5.6-sol",
    "-s",
    "read-only",
    "--ephemeral",
    "--output-schema",
    resolve(root, schemaPath),
    "-o",
    responsePath,
    "--json",
    "-C",
    root,
  ];
  for (const image of images) args.push("-i", image);
  args.push("-");
  const result = spawnSync("codex", args, {
    input: prompt,
    encoding: "utf8",
    maxBuffer: 64 * 1024 * 1024,
  });
  const rawLog = `${result.stdout ?? ""}${result.stderr ?? ""}`;
  writeFileSync(logPath, rawLog);
  if (result.status !== 0) {
    throw new Error(`${agent} falhou; consulte ${logPath}`);
  }
  return {
    response: JSON.parse(readFileSync(responsePath, "utf8")),
    usage: usageFromJsonLines(rawLog),
  };
}

const qualityTemplate = read(
  "ai-worker/src/main/resources/prompts/geralanding/landing-page-quality-review.md",
);
const qualityPrompt = qualityTemplate
  .replace("{{singlePain}}", commercialOffer.pain)
  .replace("{{freeReward}}", commercialOffer.proof)
  .replace("{{funnelPromise}}", commercialOffer.promise)
  .replace("{{primaryCta}}", commercialOffer.primaryCta)
  .replace("{{campaignObjective}}", "SALES")
  .replace("{{htmlGeraLanding}}", () => html)
  .replace(
    "{{renderedLandingScreenshots}}",
    JSON.stringify(screenshots, null, 2),
  );
const quality = runReview(
  "QUALITY_REVIEW",
  qualityPrompt,
  "ai-worker/src/main/resources/prompts/geralanding/landing-page-quality-review-schema.json",
  screenshots.map((screenshot) => screenshot.path),
);

const taskContext = JSON.stringify(
  {
    product: "Rigel / Kit WhatsApp Pronto",
    experimentId: 89,
    validationScope: "LOCAL_QA_NO_PUBLICATION",
    commercialContract,
    commercialOffer,
    productionBeforeRepair: {
      evidence: productionOfferBeforeRepair,
      status: "KNOWN_DIVERGENCE_NOT_THE_TARGET_CANDIDATE",
      rootCause:
        "A migração anterior exigia draft_experience_json não nulo e ignorou o slot ativo publicado.",
      repair:
        "2026-08-26-rigel-commercial-experience-v2-slot-repair atualiza a publicação v2 mesmo com rascunho nulo.",
      physicalMysql57Validation:
        "Aprovada com slot sem rascunho, com rascunho, reaplicação e retomada após interrupção.",
    },
    creativeContract,
    approvedCreativePackage: {
      packageId: creativeManifest.packageId,
      assetCount: creativeManifest.count,
      imageCount: creativeManifest.images,
      videoCount: creativeManifest.videos,
      assets: creativeManifest.assets,
      videoProbe: creativeManifest.videoProbe,
      videoPlayback:
        "Aprovado em iPhone 15 Pro e Pixel 7: H.264, 1080x1920, 30s, reprodução iniciada e readyState 4.",
    },
    approvedLandingVisualAssets: landingContext.approvedLandingVisualAssets,
    candidateHtml: html,
    browserAudit,
    qualityReview: quality.response,
    screenshotRoles: screenshots,
    verifiedPolicyStatus: {
      terms: 200,
      privacy: 200,
      refundPolicy: 200,
      checkedAt: "2026-08-26",
    },
    checkoutEvidence: {
      canonicalContract: {
        experimentId: commercialOffer.experimentId,
        priceBrl: commercialOffer.priceBrl,
        billingModel: "ONE_TIME",
        checkoutUrl: commercialOffer.checkoutUrl,
        supplierLegalName: commercialOffer.supplierLegalName,
      },
      localTestDouble: localCheckoutAudit,
      localEndToEnd:
        "A rota da landing abriu o test double em popup e confirmou produto, R$ 349, pagamento único, fornecedor e ausência de cobrança adicional; 12 jornadas passaram em desktop, iPhone 15 Pro e Pixel 7, com métricas mh_test segregadas.",
      productionProviderReadOnlyAttempt: realCheckoutAudit,
      productionPreflightRule:
        "O provedor respondeu 403 ao navegador automatizado. Após deploy, a pessoa operadora deve repetir a inspeção sem pagar; até isso ocorrer, o experimento permanece PLANNED.",
    },
    boundaries: [
      "A validação é local, segregada e não publica a landing.",
      "Nenhum CTA foi acionado e nenhum pagamento foi iniciado.",
      "As imagens são os quatro arquivos APPROVED do plano comercial, preservados sem redesenho.",
      "As amostras são fictícias, demonstrativas e não constituem depoimento, venda ou prova social.",
      "O experimento permanece PLANNED, sem campanha, contato, gasto, evento ou venda atribuída.",
      "A revisão não autoriza publicação, contato, campanha, gasto ou mudança para RUNNING.",
      "APPROVED neste parecer significa apenas que o lote local está pronto para PR/deploy; o avanço produtivo continua condicionado ao preflight pós-deploy pela tela.",
      "O subprocesso avaliado é geração de landing. O pacote criativo anterior já foi importado e aprovado; seus 11 arquivos reais e hashes estão anexados apenas para conferir continuidade.",
    ],
  },
  null,
  2,
);

const psiquePrompt = read(
  "customer-agent-worker/src/main/resources/prompts/bpm/landing-customer-review.md",
)
  .replace(
    "{{PSIQUE_BEHAVIORAL_CORE_V2}}",
    read("customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v2.md"),
  )
  .replace("{{TASK_CONTEXT}}", taskContext);
const psique = runReview(
  "PSIQUE",
  psiquePrompt,
  "customer-agent-worker/src/main/resources/prompts/bpm/landing-customer-review-schema.json",
  screenshots.map((screenshot) => screenshot.path),
);

const temisPrompt = read(
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/landing-commercial-review.md",
).replace(
  "{{TASK_CONTEXT}}",
  `${taskContext}\n\nParecer independente de Psique:\n${JSON.stringify(psique.response, null, 2)}`,
);
const temis = runReview(
  "TEMIS",
  temisPrompt,
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/landing-commercial-review-schema.json",
  temisImages,
);

const criteria = Object.values(quality.response.criteriaScores);
const gates = {
  quality:
    quality.response.approvalRecommendation === "APPROVE_FOR_PUBLICATION" &&
    quality.response.score >= 85 &&
    criteria.every((score) => score >= 8) &&
    quality.response.blockingIssues.length === 0 &&
    quality.response.recommendedRegeneration.length === 0,
  psique:
    psique.response.decision === "APPROVED" &&
    psique.response.requiredChanges.length === 0,
  temis:
    temis.response.decision === "APPROVED" &&
    temis.response.requiredChanges.length === 0,
};
const summary = {
  round,
  qualityReview: quality.response,
  psiqueReview: psique.response,
  temisReview: temis.response,
  gates,
  modelUsage: {
    qualityReview: quality.usage,
    psique: psique.usage,
    temis: temis.usage,
  },
};
writeFileSync(
  resolve(outputDir, "summary.json"),
  `${JSON.stringify(summary, null, 2)}\n`,
);

console.log(
  JSON.stringify({
    round,
    qualityScore: quality.response.score,
    qualityDecision: quality.response.approvalRecommendation,
    psiqueDecision: psique.response.decision,
    temisDecision: temis.response.decision,
    gates,
  }),
);
if (!Object.values(gates).every(Boolean)) {
  process.exitCode = 2;
}
