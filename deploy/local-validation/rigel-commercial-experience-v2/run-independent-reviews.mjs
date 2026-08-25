import { spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const localDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(localDir, "../../..");
const evidenceDir = join(localDir, "evidence");
const requestsDir = join(evidenceDir, "requests");
const responsesDir = join(evidenceDir, "responses");
const logsDir = join(evidenceDir, "logs");
const productUrl = "http://pde-platform-frontend-kit-validation/?mh_test=1";
const model = "gpt-5.6-sol";

await Promise.all(
  [evidenceDir, requestsDir, responsesDir, logsDir].map((directory) =>
    mkdir(directory, { recursive: true }),
  ),
);

const read = (relativePath) => readFile(join(root, relativePath), "utf8");
const contractText = await read(
  "pde-platform/contracts/kit-whatsapp-pronto-commercial-v2.json",
);
const contract = JSON.parse(contractText);
const creativePackageContract = JSON.parse(
  await read(
    "deploy/local-validation/rigel-creative-production-v6/rigel-creative-contract.v1.json",
  ),
);

function usageFromJsonLines(value) {
  let usage = null;
  for (const line of value.split("\n")) {
    try {
      const event = JSON.parse(line);
      usage =
        event.usage ?? event?.payload?.usage ?? event?.data?.usage ?? usage;
    } catch {
      // Linhas não JSON permanecem no log bruto e não geram telemetria estimada.
    }
  }
  return usage;
}

const ledgerFile = join(evidenceDir, "agent-executions.json");
let ledger = [];
try {
  ledger = JSON.parse(await readFile(ledgerFile, "utf8"));
} catch {
  ledger = [];
}

async function review({ agent, prompt, schemaPath, images }) {
  const executionId = randomUUID();
  const slug = `${agent.toLowerCase()}-${executionId}`;
  const requestFile = join(requestsDir, `${slug}.md`);
  const responseFile = join(responsesDir, `${slug}.json`);
  const logFile = join(logsDir, `${slug}.jsonl`);
  await writeFile(requestFile, prompt);
  const args = [
    "exec",
    "--model",
    model,
    "-s",
    "read-only",
    "--ephemeral",
    "--output-schema",
    join(root, schemaPath),
    "-o",
    responseFile,
    "--json",
    "-C",
    root,
  ];
  for (const image of images) args.push("-i", image);
  args.push("-");
  const result = spawnSync("codex", args, {
    input: prompt,
    encoding: "utf8",
    maxBuffer: 50 * 1024 * 1024,
  });
  const rawLog = `${result.stdout ?? ""}${result.stderr ?? ""}`;
  await writeFile(logFile, rawLog);
  ledger.push({
    executionId,
    agent,
    model,
    mode: "LOCAL_READ_ONLY",
    exitCode: result.status,
    requestFile,
    responseFile,
    logFile,
    usage: usageFromJsonLines(rawLog),
    externalMediaProviderCalled: false,
    costUsd: null,
    costStatus: "NOT_EXPOSED_BY_CODEX",
  });
  await writeFile(ledgerFile, `${JSON.stringify(ledger, null, 2)}\n`);
  if (result.status !== 0) {
    throw new Error(`${agent} falhou; consulte ${logFile}`);
  }
  return JSON.parse(await readFile(responseFile, "utf8"));
}

const screenshots = [
  join(evidenceDir, "rigel-desktop.png"),
  join(evidenceDir, "rigel-iphone-15-pro.png"),
  join(evidenceDir, "rigel-pixel-7.png"),
];
const commercialOffer = JSON.parse(
  await readFile(join(evidenceDir, "commercial-offer.json"), "utf8"),
);
const renderedHtml = await readFile(
  join(evidenceDir, "rigel-rendered.html"),
  "utf8",
);
const qualityTemplate = await read(
  "ai-worker/src/main/resources/prompts/geralanding/landing-page-quality-review.md",
);
const qualityPrompt = qualityTemplate
  .replace("{{singlePain}}", commercialOffer.pain)
  .replace("{{freeReward}}", commercialOffer.proof)
  .replace("{{funnelPromise}}", commercialOffer.promise)
  .replace("{{primaryCta}}", commercialOffer.primaryCta)
  .replace("{{campaignObjective}}", "SALES")
  .replace("{{htmlGeraLanding}}", () => renderedHtml)
  .replace("{{renderedLandingScreenshots}}", JSON.stringify(screenshots));
const qualityReview = await review({
  agent: "QUALITY_REVIEW",
  prompt: qualityPrompt,
  schemaPath:
    "ai-worker/src/main/resources/prompts/geralanding/landing-page-quality-review-schema.json",
  images: screenshots,
});

const taskContext = JSON.stringify(
  {
    product: "Rigel / Kit WhatsApp Pronto",
    validationScope: "LOCAL_QA",
    contract,
    commercialOffer,
    creativePackageContract,
    qualityReview,
    renderedUrl: productUrl,
    screenshots,
    implementationPaths: [
      "pde-platform/frontend/src/AssistedServiceApp.tsx",
      "pde-platform/frontend/src/styles.css",
      "pde-platform/frontend/tests/assisted-service-local.spec.ts",
      "docs/homologacao/rigel-commercial-experience-v2-matriz.md",
    ],
    boundaries: [
      "A degustação é determinística e não envia dados a provedor externo.",
      "Os exemplos são fictícios e não são depoimentos nem prova de venda.",
      "Checkout e eventos desta homologação usam mh_test e não contam como vendas.",
      "A revisão não autoriza publicação, contato, campanha ou gasto.",
      "Fornecedor, políticas e URL de checkout da fixture foram conferidos em modo somente leitura na oferta pública produtiva em 2026-08-25; a captura não abriu nem acionou o checkout.",
      "A coerência com a comunicação do canal direto deve ser verificada no creativePackageContract incluído, sem exigir campanha paga neste subprocesso de destino.",
    ],
  },
  null,
  2,
);
const psiqueTemplate = await read(
  "customer-agent-worker/src/main/resources/prompts/bpm/landing-customer-review.md",
);
const behavioralCore = await read(
  "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v2.md",
);
const psiqueReview = await review({
  agent: "PSIQUE",
  prompt: psiqueTemplate
    .replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", behavioralCore)
    .replace("{{TASK_CONTEXT}}", taskContext),
  schemaPath:
    "customer-agent-worker/src/main/resources/prompts/bpm/landing-customer-review-schema.json",
  images: screenshots,
});

const temisTemplate = await read(
  "meta-ad-approver-worker/src/main/resources/prompts/bpm/landing-commercial-review.md",
);
const temisReview = await review({
  agent: "TEMIS_INDEPENDENT",
  prompt: temisTemplate.replace(
    "{{TASK_CONTEXT}}",
    `${taskContext}\n\nParecer independente de Psique:\n${JSON.stringify(psiqueReview, null, 2)}`,
  ),
  schemaPath:
    "meta-ad-approver-worker/src/main/resources/prompts/bpm/landing-commercial-review-schema.json",
  images: screenshots,
});

await writeFile(
  join(evidenceDir, "review-summary.json"),
  `${JSON.stringify({ qualityReview, psiqueReview, temisReview }, null, 2)}\n`,
);

if (
  qualityReview.approvalRecommendation !== "APPROVE_FOR_PUBLICATION" ||
  psiqueReview.decision !== "APPROVED" ||
  temisReview.decision !== "APPROVED"
) {
  throw new Error(
    "A experiência v2 ainda não passou pelos três pareceres independentes.",
  );
}

console.log(
  JSON.stringify({
    qualityReview: qualityReview.approvalRecommendation,
    qualityScore: qualityReview.score,
    psique: psiqueReview.decision,
    temis: temisReview.decision,
  }),
);
