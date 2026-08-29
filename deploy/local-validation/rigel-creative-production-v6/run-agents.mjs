import { spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { cp, mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const phase = process.argv[2];
if (!new Set(["planning", "reviews"]).has(phase)) {
  throw new Error("Uso: node run-agents.mjs <planning|reviews>");
}

const localDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(localDir, "../../..");
const evidence = join(localDir, "evidence");
const artifacts = join(evidence, "artifacts");
const proof = join(evidence, "proof");
const logs = join(evidence, "agent-logs");
const requests = join(evidence, "agent-requests");
const responses = join(evidence, "agent-responses");
const agentModel = "gpt-5.6-sol";
const reasoningEffort = "high";
await mkdir(logs, { recursive: true });
await mkdir(requests, { recursive: true });
await mkdir(responses, { recursive: true });

const read = async (file) => readFile(file, "utf8");
const contractText = await read(
  join(localDir, "rigel-creative-contract.v1.json"),
);
const contract = JSON.parse(contractText);
const ledgerFile = join(evidence, "agent-executions.json");
let ledger = [];
try {
  ledger = JSON.parse(await read(ledgerFile));
} catch {
  ledger = [];
}

function usageFromJsonl(jsonl) {
  let found = null;
  for (const line of jsonl.split("\n")) {
    try {
      const event = JSON.parse(line);
      const usage = event.usage ?? event?.payload?.usage ?? event?.data?.usage;
      if (usage) found = usage;
    } catch {
      // Saidas operacionais nao JSON sao preservadas no log, mas nao viram telemetria inventada.
    }
  }
  return found;
}

async function runCodex({
  agent,
  agentPrompt,
  activityPrompt,
  schema,
  output,
  images = [],
}) {
  const executionId = randomUUID();
  const executionSlug = `${agent.toLowerCase()}-${executionId}`;
  const requestFile = join(requests, `${executionSlug}.md`);
  const agentPromptFile = join(requests, `${executionSlug}-agent.md`);
  const activityPromptFile = join(requests, `${executionSlug}-activity.md`);
  const logFile = join(logs, `${executionSlug}.jsonl`);
  const responseFile = join(responses, `${executionSlug}.json`);
  const prompt = `${agentPrompt.trim()}\n\n${activityPrompt.trim()}`;
  await writeFile(requestFile, prompt);
  await writeFile(agentPromptFile, agentPrompt.trim());
  await writeFile(activityPromptFile, activityPrompt.trim());
  const args = [
    "exec",
    "--config",
    `model_reasoning_effort="${reasoningEffort}"`,
    "--model",
    agentModel,
    "-s",
    "read-only",
    "--ephemeral",
    "--output-schema",
    schema,
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
  const log = `${result.stdout ?? ""}${result.stderr ?? ""}`;
  await writeFile(logFile, log);
  ledger.push({
    executionId,
    agent,
    mode: "LOCAL_READ_ONLY",
    agentModelCalled: true,
    model: agentModel,
    reasoningEffort,
    mediaProviderCalled: false,
    externalMediaSpendAuthorized: false,
    externalMediaCostUsd: 0,
    costUsd: null,
    costStatus: "NOT_EXPOSED_BY_CODEX",
    exitCode: result.status,
    requestFile,
    agentPromptFile,
    activityPromptFile,
    responseFile,
    logFile,
    usage: usageFromJsonl(log),
  });
  await writeFile(ledgerFile, `${JSON.stringify(ledger, null, 2)}\n`);
  if (result.status !== 0) {
    throw new Error(
      `${agent} falhou localmente; consulte ${join(logs, `${agent.toLowerCase()}-${executionId}.jsonl`)}`,
    );
  }
  await cp(responseFile, output);
}

if (phase === "planning") {
  const directionTemplate = await read(
    join(localDir, "temis-creative-direction.md"),
  );
  await runCodex({
    agent: "TEMIS_DIRECTION",
    agentPrompt: await read(join(localDir, "temis-agent-core.md")),
    activityPrompt: directionTemplate.replace("{{CONTRACT}}", contractText),
    schema: join(localDir, "temis-creative-direction-schema.json"),
    output: join(evidence, "temis-creative-direction.json"),
    images: contract.sourceProofs.map((item) => join(proof, item.file)),
  });

  const apolloTemplate = await read(
    join(
      root,
      "video-management-service/src/main/resources/prompts/apollo/v2/storyboard-planner.md",
    ),
  );
  const apolloContext = JSON.stringify(
    {
      product: contract.product,
      selectedRoute: contract.routeDecision.selected,
      targetDurationSeconds: 30,
      requiredCutDurationsSeconds: [4, 3, 12, 5, 6],
      requiredCutCount: 5,
      providerCalled: false,
      spendingAuthorized: false,
      existingMaterial: contract.sourceProofs,
      copyForPostProduction: contract.copy,
      instruction:
        "Use exatamente cinco cortes, nas durações [4, 3, 12, 5, 6] segundos, todas as funções comerciais do schema e apenas material existente. O terceiro corte deve preservar os três follow-ups por 12 segundos. Todo texto pertence à pós-produção determinística.",
    },
    null,
    2,
  );
  await runCodex({
    agent: "APOLLO",
    agentPrompt: await read(join(localDir, "apollo-agent-core.md")),
    activityPrompt: apolloTemplate.replace("{{CONTEXT}}", apolloContext),
    schema: join(
      root,
      "video-management-service/src/main/resources/prompts/apollo/v2/storyboard-planner-schema.json",
    ),
    output: join(evidence, "apollo-storyboard.json"),
    images: contract.sourceProofs.map((item) => join(proof, item.file)),
  });
}

if (phase === "reviews") {
  const manifestText = await read(
    join(artifacts, "rigel-creative-manifest.json"),
  );
  const technicalVerificationText = await read(
    join(evidence, "technical-verification.json"),
  );
  const apolloText = await read(join(evidence, "apollo-storyboard.json"));
  const directionText = await read(
    join(evidence, "temis-creative-direction.json"),
  );
  const reviewImages = [
    ...contract.sourceProofs.map((item) => join(proof, item.file)),
    join(proof, "rigel-destination-desktop.png"),
    join(proof, "rigel-destination-mobile.png"),
    ...[
      "rigel-direct-card-preview-360x450.png",
      "rigel-direct-response-preview-360x450.png",
      "rigel-direct-question-preview-360x450.png",
      "rigel-direct-followups-preview-360x450.png",
      "rigel-direct-offer-preview-360x450.png",
      "rigel-direct-conditions-preview-360x450.png",
    ].map((file) => join(artifacts, "channel-previews", file)),
    ...Array.from({ length: 5 }, (_, index) =>
      join(
        artifacts,
        "channel-previews",
        `rigel-video-frame-${index + 1}-preview-360x640.png`,
      ),
    ),
  ];
  const taskContext = JSON.stringify(
    {
      process: "creative-production-approval-v6",
      productContract: contract,
      manifest: JSON.parse(manifestText),
      deterministicTechnicalVerification: JSON.parse(technicalVerificationText),
      direction: JSON.parse(directionText),
      apolloStoryboard: JSON.parse(apolloText),
      localVideo: join(artifacts, "rigel-vertical-demo-1080x1920.mp4"),
      validationScope: "LOCAL_QA",
      destinationContinuity:
        "As capturas rigel-destination-desktop.png e rigel-destination-mobile.png e destinationEvidence do manifesto comprovam localmente produto, oferta, preco, entrega e CTA. Este gate não exige rede pública nem autoriza deploy; o smoke produtivo pertence ao processo posterior.",
      productionSeparation:
        "O produtor é o compositor determinístico versionado identificado no manifesto. Um verificador versionado separado recalculou hashes e executou ffprobe antes dos pareceres. Psique e Têmis usam novas invocações Codex read-only; Têmis independente não executou o compositor nem precisa abrir shell para repetir a prova técnica já auditada.",
      reviewInstruction:
        "Avalie os dois formatos como pacote do canal DIRECT_ONE_TO_ONE. A sequência estática possui seis cards reduzidos a 360x450; a oferta aparece em duas partes sobrepostas, sem redesenho. As cinco imagens verticais a 360x640 são a pré-visualização realista no celular dos cortes do MP4 opcional de 30 segundos, com durações [4, 3, 12, 5, 6]. Use deterministicTechnicalVerification como evidência canônica de hashes, dimensões, codec e duração; bloqueie qualquer divergência entre esse relatório, manifesto, contrato e imagens, mas não tente executar shell dentro da sandbox read-only do parecer.",
    },
    null,
    2,
  );
  const psiqueTemplate = await read(
    join(
      root,
      "customer-agent-worker/src/main/resources/prompts/bpm/creative-customer-review.md",
    ),
  );
  const behavioralCore = await read(
    join(
      root,
      "customer-agent-worker/src/main/resources/prompts/psique/behavioral-core-v2.md",
    ),
  );
  await runCodex({
    agent: "PSIQUE",
    agentPrompt: behavioralCore,
    activityPrompt: psiqueTemplate
      .replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", "")
      .replace("{{TASK_CONTEXT}}", taskContext),
    schema: join(
      root,
      "customer-agent-worker/src/main/resources/prompts/bpm/creative-customer-review-schema.json",
    ),
    output: join(evidence, "psique-review.json"),
    images: reviewImages,
  });

  const psiqueText = await read(join(evidence, "psique-review.json"));
  const temisTemplate = await read(
    join(
      root,
      "meta-ad-approver-worker/src/main/resources/prompts/bpm/creative-commercial-review.md",
    ),
  );
  await runCodex({
    agent: "TEMIS_INDEPENDENT",
    agentPrompt: await read(
      join(
        root,
        "meta-ad-approver-worker/src/main/resources/prompts/temis/v1/agent-core.md",
      ),
    ),
    activityPrompt: temisTemplate.replace(
      "{{TASK_CONTEXT}}",
      `${taskContext}\n\nParecer anterior de Psique:\n${psiqueText}`,
    ),
    schema: join(
      root,
      "meta-ad-approver-worker/src/main/resources/prompts/bpm/creative-commercial-review-schema.json",
    ),
    output: join(evidence, "temis-independent-review.json"),
    images: reviewImages,
  });
}
