import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { join, resolve } from "node:path";

const localDir = resolve(process.argv[2] ?? ".");
const evidence = join(localDir, "evidence");
const artifacts = join(evidence, "artifacts");
const readJson = async (file) => JSON.parse(await readFile(file, "utf8"));
const sha256 = async (file) =>
  createHash("sha256")
    .update(await readFile(file))
    .digest("hex");

const contract = await readJson(
  join(localDir, "rigel-creative-contract.v1.json"),
);
const direction = await readJson(
  join(evidence, "temis-creative-direction.json"),
);
const apollo = await readJson(join(evidence, "apollo-storyboard.json"));
const manifest = await readJson(
  join(artifacts, "rigel-creative-manifest.json"),
);
const technicalVerification = await readJson(
  join(evidence, "technical-verification.json"),
);
const psique = await readJson(join(evidence, "psique-review.json"));
const temis = await readJson(join(evidence, "temis-independent-review.json"));
const executions = await readJson(join(evidence, "agent-executions.json"));

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

assert(
  direction.decision === "SELECTED",
  "Temis nao selecionou a rota criativa",
);
assert(
  direction.chosenRoute === contract.routeDecision.selected,
  "Rota de Temis diverge do contrato",
);
assert(
  direction.formatBriefs.length === 2,
  "Temis nao congelou os dois formatos",
);
assert(apollo.cuts.length === 5, "Apolo nao entregou cinco cortes");
assert(
  JSON.stringify(apollo.cuts.map((cut) => cut.durationSeconds)) ===
    JSON.stringify([4, 3, 12, 5, 6]),
  "Storyboard de Apolo nao usa [4, 3, 12, 5, 6] segundos",
);
assert(
  new Set(apollo.cuts.map((cut) => cut.commercialRole)).size === 5,
  "Storyboard de Apolo nao cobre as cinco funcoes comerciais",
);
assert(psique.decision === "APPROVED", "Psique nao aprovou o pacote");
assert(
  psique.requiredChanges.length === 0,
  "Psique deixou mudancas obrigatorias",
);
assert(
  temis.decision === "APPROVED",
  "Temis independente nao aprovou o pacote",
);
assert(
  temis.requiredChanges.length === 0,
  "Temis deixou mudancas obrigatorias",
);
assert(
  technicalVerification.status === "APPROVED" &&
    technicalVerification.verifier ===
      "RIGEL_DETERMINISTIC_TECHNICAL_VERIFIER_V1" &&
    technicalVerification.allHashesMatch === true &&
    technicalVerification.verifiedFileCount === 30,
  "A verificacao tecnica deterministica nao aprovou todos os arquivos",
);
assert(
  technicalVerification.video.width === 1080 &&
    technicalVerification.video.height === 1920 &&
    technicalVerification.video.codec === "h264" &&
    technicalVerification.video.pixelFormat === "yuv420p" &&
    Math.abs(technicalVerification.video.durationSeconds - 30) < 0.15 &&
    JSON.stringify(technicalVerification.video.cutDurationsSeconds) ===
      JSON.stringify([4, 3, 12, 5, 6]),
  "O relatorio tecnico nao comprova o MP4 contratado",
);

assert(
  manifest.contractVersion === contract.contractVersion,
  "Manifesto usa contrato incorreto",
);
assert(
  manifest.formats.length === contract.formats.length &&
    manifest.formats[0].id === "direct-carousel" &&
    manifest.formats[0].assetFiles.length === 6 &&
    manifest.formats[1].id === "vertical-demo" &&
    manifest.formats[1].assetFiles.length === 1,
  "Os formatos produzidos nao correspondem ao contrato v2",
);
assert(
  manifest.externalMediaProviderCalled === false,
  "Um provider externo de midia foi chamado",
);
assert(
  manifest.externalMediaCostUsd === 0,
  "O pacote registrou custo externo de midia",
);
assert(manifest.published === false, "O pacote foi publicado sem autorizacao");
assert(
  JSON.stringify(manifest.mobileReadabilityContract.directCarouselProof) ===
    JSON.stringify([
      "resposta completa",
      "pergunta completa",
      "três follow-ups completos",
      "oferta completa em duas partes sobrepostas",
    ]),
  "A sequencia estatica nao congela a prova visual completa da amostra",
);
assert(
  manifest.mobileReadabilityContract.firstFrame.includes(
    "1 resposta, 1 pergunta e 3 follow-ups",
  ) &&
    manifest.mobileReadabilityContract.firstFrame.includes("exemplo fictício"),
  "O primeiro quadro nao congela a demonstracao e o limite da prova",
);
assert(
  manifest.mobileReadabilityContract.deliveryMode ===
    "Implantação personalizada e assistida",
  "A forma de entrega nao esta congelada para leitura movel",
);
assert(
  manifest.mobileReadabilityContract.manualControl.includes("sem bot") &&
    manifest.mobileReadabilityContract.payment === contract.copy.price &&
    manifest.mobileReadabilityContract.deadline === contract.copy.delivery,
  "Controle, pagamento ou prazo nao estao congelados para leitura movel",
);
assert(
  manifest.sourceProofs.length === 4 &&
    manifest.sourceProofs.every(
      (proof) =>
        proof.purpose === "PRODUCT_PROOF" && proof.sha256.length === 64,
    ),
  "Provas reais nao possuem finalidade e hash completos",
);
for (const proof of manifest.sourceProofs) {
  assert(
    (await sha256(join(evidence, "proof", proof.file))) === proof.sha256,
    `Hash divergente na prova ${proof.file}`,
  );
}
const images = manifest.assets.filter((asset) => asset.mediaType === "IMAGE");
const video = manifest.assets.find((asset) => asset.mediaType === "VIDEO");
assert(
  images.length === 6 &&
    images.every((image) => image.width === 1080 && image.height === 1350),
  "Sequencia estatica possui quantidade ou dimensao incorreta",
);
assert(
  video.width === 1080 && video.height === 1920,
  "Video possui dimensao incorreta",
);
assert(
  Math.abs(video.durationSeconds - 30) < 0.15,
  "Video nao possui 30 segundos",
);
assert(
  video.codec === "h264" && video.pixelFormat === "yuv420p",
  "MP4 nao e compativel",
);
for (const asset of manifest.assets) {
  assert(
    (await sha256(join(artifacts, asset.file))) === asset.sha256,
    `Hash divergente no ativo ${asset.file}`,
  );
}
assert(manifest.reviewFrames.length === 5, "Faltam frames auditaveis do video");
for (const frame of manifest.reviewFrames) {
  assert(
    (await sha256(join(artifacts, "review-frames", frame.file))) ===
      frame.sha256,
    `Hash divergente no frame ${frame.file}`,
  );
}
assert(
  manifest.channelPreviews.length === 11 &&
    manifest.channelPreviews.every((preview) => preview.sha256.length === 64),
  "Faltam pre-visualizacoes auditaveis no tamanho real do canal",
);
for (const preview of manifest.channelPreviews) {
  assert(
    (await sha256(join(artifacts, "channel-previews", preview.file))) ===
      preview.sha256,
    `Hash divergente na pre-visualizacao ${preview.file}`,
  );
}
assert(
  manifest.destinationEvidence.screenshots.length === 2 &&
    manifest.destinationEvidence.screenshots.every(
      (screenshot) => screenshot.sha256.length === 64,
    ),
  "Destino nao possui evidencias completas em desktop e mobile",
);
for (const screenshot of manifest.destinationEvidence.screenshots) {
  assert(
    (await sha256(join(evidence, "proof", screenshot.file))) ===
      screenshot.sha256,
    `Hash divergente na evidencia de destino ${screenshot.file}`,
  );
}

const nonControlCopy = Object.entries(contract.copy)
  .filter(([key]) => key !== "control")
  .map(([, value]) => value.toLowerCase())
  .join(" ");
for (const prohibited of contract.prohibitedClaims) {
  assert(
    !nonControlCopy.includes(prohibited.toLowerCase()),
    `A copy afirmou uma promessa proibida: ${prohibited}`,
  );
}
assert(
  contract.copy.control.startsWith("Você revisa antes de usar. Sem bot"),
  "A ausencia de automacao nao ficou explicita",
);
assert(
  contract.copy.proof.startsWith("Interface real; exemplo fictício"),
  "A prova sintetica nao esta identificada de forma inequivoca",
);
assert(
  contract.copy.mechanism.includes("10 a 20 respostas personalizadas") &&
    contract.copy.mechanism.includes("5 a 10 perguntas") &&
    contract.copy.mechanism.includes("3 a 5 follow-ups manuais"),
  "A entrega principal de Rigel nao esta explicita",
);
assert(
  !contract.copy.offer.includes("7 materiais"),
  "Os modelos-base complementares foram tratados como entrega principal",
);

const lastByAgent = new Map();
for (const execution of executions) lastByAgent.set(execution.agent, execution);
for (const agent of [
  "TEMIS_DIRECTION",
  "APOLLO",
  "PSIQUE",
  "TEMIS_INDEPENDENT",
]) {
  const execution = lastByAgent.get(agent);
  assert(execution?.exitCode === 0, `Execucao ausente ou falha de ${agent}`);
  assert(
    execution.agentModelCalled === true,
    `${agent} nao registrou a chamada ao modelo`,
  );
  assert(
    execution.model === "gpt-5.6-sol",
    `${agent} nao registrou o modelo versionado`,
  );
  assert(
    execution.mediaProviderCalled === false,
    `${agent} chamou provider externo de midia`,
  );
  assert(
    execution.externalMediaSpendAuthorized === false &&
      execution.externalMediaCostUsd === 0,
    `${agent} registrou gasto externo de midia`,
  );
  assert(
    execution.costUsd === null &&
      execution.costStatus === "NOT_EXPOSED_BY_CODEX",
    `${agent} inventou ou omitiu o estado do custo do modelo`,
  );
  assert(
    typeof execution.usage?.input_tokens === "number" &&
      typeof execution.usage?.output_tokens === "number",
    `${agent} nao registrou telemetria de tokens`,
  );
  for (const [kind, file] of [
    ["request", execution.requestFile],
    ["response", execution.responseFile],
    ["log", execution.logFile],
  ]) {
    assert(typeof file === "string", `${agent} nao registrou ${kind}`);
    assert(
      (await readFile(file)).length > 0,
      `${agent} registrou ${kind} vazio`,
    );
  }
  assert(
    execution.requestFile !== execution.responseFile,
    `${agent} misturou request e response na auditoria`,
  );
}
assert(
  new Set([...lastByAgent.values()].map((execution) => execution.executionId))
    .size === lastByAgent.size,
  "Producoes e revisoes nao usam execucoes distintas",
);
assert(
  manifest.producerExecutionId !==
    lastByAgent.get("TEMIS_INDEPENDENT").executionId,
  "O produtor nao pode executar a revisao independente",
);

process.stdout.write(
  `${JSON.stringify(
    {
      status: "APPROVED",
      product: contract.product.internalName,
      formatsApproved: manifest.formats.length,
      formatsRequired: contract.formats.length,
      psique: psique.decision,
      temis: temis.decision,
      agentModelsCalled: true,
      externalMediaProviderCalled: false,
      externalMediaCostUsd: 0,
      published: false,
      salesCreated: 0,
    },
    null,
    2,
  )}\n`,
);
