import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  executeCodexWithInput,
  parseCodexUsage,
} from "./argos-codex.js";

/** Sintetiza candidatas factuais usando somente os identificadores coletados pelo executor. */
export async function synthesizeMarketCandidates(context, options = {}) {
  const enabled =
    String(options.enabled ?? process.env.ARGOS_CODEX_ENABLED) === "true";
  if (!enabled) return deterministicSynthesis(context);
  const prompt = await buildResearchPrompt(context);
  const schemaContract = await readFile(
    new URL(
      "../prompts/productdiscovery.v1/research/response-schema.json",
      import.meta.url,
    ),
    "utf8",
  );
  JSON.parse(schemaContract);
  const directory = await mkdtemp(join(tmpdir(), "argos-research-"));
  const output = join(directory, "output.json");
  const schema = join(directory, "schema.json");
  const model = options.model || process.env.ARGOS_CODEX_MODEL;
  const reasoningEffort =
    options.reasoningEffort ||
    process.env.ARGOS_CODEX_SYNTHESIS_REASONING_EFFORT ||
    process.env.ARGOS_CODEX_REASONING_EFFORT ||
    "medium";
  let execution;
  try {
    await writeFile(schema, schemaContract);
    const command = options.command || process.env.ARGOS_CODEX_COMMAND || "codex";
    const args = [
      "exec",
      "-",
      "--skip-git-repo-check",
      "--sandbox",
      "read-only",
      "--output-schema",
      schema,
      "--output-last-message",
      output,
      "--json",
      "--color",
      "never",
      "--config",
      `model_reasoning_effort="${reasoningEffort}"`,
    ];
    if (model) args.push("--model", model);
    const execute = options.execute || executeCodexWithInput;
    execution = await execute(command, args, prompt.fullPrompt, {
      timeoutMs: Number(
        options.timeoutMs ||
          process.env.ARGOS_CODEX_SYNTHESIS_TIMEOUT_MS ||
          process.env.ARGOS_CODEX_TIMEOUT_MS ||
          600000,
      ),
      maxBuffer: 10 * 1024 * 1024,
      phaseName: "síntese factual",
    });
    let rawResponse;
    try {
      rawResponse = await readFile(output, "utf8");
    } catch (error) {
      if (error?.code === "ENOENT") {
        throw new Error(
          "Codex terminou sem produzir a síntese estruturada de Argos",
          { cause: error },
        );
      }
      throw error;
    }
    const synthesis = JSON.parse(rawResponse);
    validateSynthesis(synthesis, context);
    return {
      synthesis,
      rawResponse,
      model: model || "codex-default",
      mode: "CODEX",
      prompt: prompt.fullPrompt,
      agentPromptPart: prompt.agentPromptPart,
      activityPromptPart: prompt.activityPromptPart,
      reasoningEffort,
      usage: parseCodexUsage(execution?.stdout),
      accessedUrls: accessedUrls(context),
    };
  } catch (error) {
    const failure = error instanceof Error ? error : new Error(String(error));
    failure.executionAudit = {
      executionMode: "MODEL",
      modelCode: model || "codex-default",
      reasoningEffort,
      promptSent: prompt.fullPrompt,
      agentPromptPart: prompt.agentPromptPart,
      activityPromptPart: prompt.activityPromptPart,
      accessedUrls: accessedUrls(context),
    };
    throw failure;
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

/** Expõe degradação honesta sem repetir as três sugestões genéricas do fluxo antigo. */
export function deterministicSynthesis(context) {
  const synthesis = {
    decisionSummary:
      "A coleta foi preservada, mas a síntese de candidatas não foi executada porque o modelo de Argos está desabilitado.",
    candidates: [],
  };
  return {
    synthesis,
    rawResponse: JSON.stringify(synthesis),
    model: "deterministic-research-fallback-v1",
    mode: "DETERMINISTIC",
    prompt: JSON.stringify({
      operation: "PRODUCT_DISCOVERY_MARKET_SYNTHESIS_V1",
      cycleId: context.job?.cycleId,
      evidenceIds: allEvidence(context).map((item) => item.evidenceId),
    }),
    reasoningEffort: "NOT_APPLICABLE",
    usage: null,
    accessedUrls: accessedUrls(context),
  };
}

/** Bloqueia fontes inventadas, duplicidade e retorno genérico antes do callback ao backend. */
export function validateSynthesis(synthesis, context) {
  if (
    !synthesis ||
    typeof synthesis.decisionSummary !== "string" ||
    !synthesis.decisionSummary.trim() ||
    !Array.isArray(synthesis.candidates) ||
    synthesis.candidates.length > 3
  ) {
    throw new Error("Síntese factual de Argos fora do contrato v1");
  }
  const knownIds = new Set(
    allEvidence(context).map((evidence) => evidence.evidenceId),
  );
  const names = new Set();
  for (const candidate of synthesis.candidates) {
    const name = String(candidate?.name || "").trim();
    if (!name || names.has(name.toLowerCase())) {
      throw new Error("Síntese de Argos contém candidata sem identidade distinta");
    }
    names.add(name.toLowerCase());
    if (/^(diagnóstico|plano de primeira ação|simulador prático)\b/i.test(name)) {
      throw new Error("Síntese de Argos repetiu um molde genérico de produto");
    }
    if (
      !Array.isArray(candidate.evidenceIds) ||
      candidate.evidenceIds.length < 2 ||
      new Set(candidate.evidenceIds).size !== candidate.evidenceIds.length ||
      candidate.evidenceIds.some((id) => !knownIds.has(id))
    ) {
      throw new Error(
        `Candidata ${name} referencia evidência ausente ou insuficiente`,
      );
    }
    if (!candidate.evidenceIds.some((id) => /^[POM]/.test(id))) {
      throw new Error(
        `Candidata ${name} usa inspiração interna sem confirmação pública`,
      );
    }
    const pdeFit = candidate.pdeDeliveryFit;
    if (
      !pdeFit ||
      pdeFit.deliveryMode !== "AI_DIGITAL_EXPERIENCE" ||
      pdeFit.physicalDependency !== "NONE" ||
      !String(pdeFit.minimumInput || "").trim() ||
      !String(pdeFit.aiBackstageWork || "").trim() ||
      !String(pdeFit.readyDigitalOutcome || "").trim()
    ) {
      throw new Error(
        `Candidata ${name} não comprova entrega como experiência digital com IA`,
      );
    }
    const candidateDelivery = `${name} ${pdeFit.readyDigitalOutcome}`
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase();
    if (
      /\b(assinatura|envio|enviad[ao]|receber|entrega)\b.{0,50}\b(caixa|cosmetico|suplemento|roupa|produto fisico)\b/.test(
        candidateDelivery,
      ) ||
      /\bcaixas? de (beleza|autocuidado|cosmeticos)\b/.test(candidateDelivery)
    ) {
      throw new Error(
        `Candidata ${name} descreve entrega física em vez de experiência digital com IA`,
      );
    }
  }
}

/** Monta o prompt versionado com fatos estruturados e sem acesso autônomo a novas fontes. */
async function buildResearchPrompt(context) {
  const [systemPrompt, userPrompt] = await Promise.all([
    readFile(
      new URL(
        "../prompts/productdiscovery.v1/research/system.md",
        import.meta.url,
      ),
      "utf8",
    ),
    readFile(
      new URL(
        "../prompts/productdiscovery.v1/research/user.md",
        import.meta.url,
      ),
      "utf8",
    ),
  ]);
  const agentPromptPart = systemPrompt.trim();
  const activityPromptPart = userPrompt
    .replace(
      "{{researchContextJson}}",
      JSON.stringify(sanitizedContext(context), null, 2),
    )
    .trim();
  if (/{{[^}]+}}/.test(activityPromptPart)) {
    throw new Error("Prompt de síntese de Argos possui placeholder não resolvido");
  }
  return {
    fullPrompt: `${agentPromptPart}\n\n${activityPromptPart}`,
    agentPromptPart,
    activityPromptPart,
  };
}

/** Limita o contexto ao briefing, plano e fatos que podem ser citados pelo schema. */
function sanitizedContext(context) {
  return {
    job: compactJob(context.job),
    plan: compactPlan(context.plan),
    publicEvidence: (context.publicEvidence || []).map(compactPublicEvidence),
    repositoryEvidence: (context.repositoryEvidence || []).map(
      compactRepositoryEvidence,
    ),
    repositoryCoverage: (context.repositoryCoverage || []).map((item) => ({
      collection: item.collection,
      status: item.status,
      documentCount: Number(item.documentCount || 0),
    })),
    marketplaceOffers: (context.marketplaceOffers || []).map(
      compactMarketplaceOffer,
    ),
    metaAdEvidence: (context.metaAdEvidence || []).map(compactMetaAd),
    metaCoverage: (context.metaCoverage || []).map(compactMetaCoverage),
  };
}

/** Evita duplicar no prompt o acervo e a auditoria que já possuem blocos próprios. */
function compactJob(job = {}) {
  return {
    cycleId: job.cycleId,
    theme: job.theme,
    targetAudience: job.targetAudience,
    country: job.country,
    language: job.language,
    acquisitionChannel: job.acquisitionChannel,
    commercialConstraints: job.commercialConstraints,
    forbiddenCategories: job.forbiddenCategories,
    objective: job.objective,
    researchMode: job.researchMode,
    marketType: job.marketType,
    referenceSources: job.referenceSources,
    marketExpansionContext: job.marketExpansionContext,
  };
}

/** Mantém na síntese somente a intenção do plano e os gates necessários à classificação. */
function compactPlan(plan = {}) {
  return {
    researchLens: plan.researchLens,
    expansionAxis: plan.expansionAxis,
    expansionRationale: plan.expansionRationale,
    questions: (plan.questions || []).slice(0, 12).map((item) =>
      truncateForPrompt(item, 350),
    ),
    metaAdRequests: plan.metaAdRequests || [],
    minimumComparableOffers: plan.minimumComparableOffers,
  };
}

/** Preserva identidade, URL e linguagem pública dentro de um orçamento previsível. */
function compactPublicEvidence(item = {}) {
  return {
    evidenceId: item.evidenceId,
    sourceType: item.sourceType,
    title: truncateForPrompt(item.title, 300),
    url: item.url,
    snippet: truncateForPrompt(item.snippet, 700),
  };
}

/** Resume a inspiração versionada sem remover a referência auditável do arquivo. */
function compactRepositoryEvidence(item = {}) {
  return {
    evidenceId: item.evidenceId,
    sourceType: item.sourceType,
    path: item.path,
    collection: item.collection,
    title: truncateForPrompt(item.title, 300),
    date: item.date,
    excerpt: truncateForPrompt(item.excerpt, 900),
  };
}

/** Resume a alternativa paga mantendo os sinais factuais usados na decisão. */
function compactMarketplaceOffer(item = {}) {
  return {
    evidenceId: item.evidenceId,
    sourceType: item.sourceType,
    marketplace: item.marketplace,
    referenceId: item.referenceId,
    title: truncateForPrompt(item.title, 300),
    url: item.url,
    description: truncateForPrompt(item.description, 700),
    producer: truncateForPrompt(item.producer, 200),
    price: item.price,
    tractionSignal: item.tractionSignal,
    rating: item.rating,
    reviewCount: item.reviewCount,
    rankingPosition: item.rankingPosition,
    collectedAt: item.collectedAt,
    evidenceConfidence: item.evidenceConfidence,
  };
}

/** Resume o anúncio observado sem transportar o payload bruto da interface da Meta. */
function compactMetaAd(item = {}) {
  return {
    evidenceId: item.evidenceId,
    sourceType: item.sourceType,
    referenceId: item.referenceId,
    title: truncateForPrompt(item.title, 300),
    url: item.url,
    description: truncateForPrompt(item.description, 700),
    producer: truncateForPrompt(item.producer, 200),
    publisherPlatforms: item.publisherPlatforms,
    active: item.active,
    commercialSignal: item.commercialSignal,
    longevityDays: item.longevityDays,
    collectedAt: item.collectedAt,
  };
}

/** Mantém a cobertura necessária para o modelo distinguir observado, vazio e indisponível. */
function compactMetaCoverage(item = {}) {
  return {
    attemptNumber: item.attemptNumber,
    query: item.query,
    country: item.country,
    publisherPlatform: item.publisherPlatform,
    sourceStatus: item.sourceStatus,
    collectionMode: item.collectionMode,
    adsObserved: item.adsObserved,
    activeAds: item.activeAds,
    advertisersObserved: item.advertisersObserved,
    latestObservationAt: item.latestObservationAt,
    interpretation: truncateForPrompt(item.interpretation, 500),
  };
}

/** Aplica teto por campo no prompt sem alterar os fatos completos persistidos no relatório. */
function truncateForPrompt(value, maxChars) {
  return Array.from(String(value || "").trim()).slice(0, maxChars).join("");
}

/** Consolida as famílias de evidência em uma única identidade verificável. */
function allEvidence(context) {
  return [
    ...(context.publicEvidence || []),
    ...(context.repositoryEvidence || []),
    ...(context.marketplaceOffers || []),
    ...(context.metaAdEvidence || []),
  ];
}

/** Registra somente URLs realmente recebidas, sem transformar caminho local em navegação web. */
function accessedUrls(context) {
  const accessedAt = new Date().toISOString();
  return [
    ...(context.publicEvidence || []),
    ...(context.marketplaceOffers || []),
    ...(context.metaAdEvidence || []),
  ]
    .filter((item) => /^https?:\/\//i.test(String(item.url || "")))
    .filter(
      (item, index, items) =>
        items.findIndex((candidate) => candidate.url === item.url) === index,
    )
    .slice(0, 50)
    .map((item) => ({
      url: item.url,
      label: String(item.title || item.evidenceId || "Fonte pública").slice(
        0,
        200,
      ),
      accessMethod: "WEB_SEARCH",
      accessedAt,
    }));
}
