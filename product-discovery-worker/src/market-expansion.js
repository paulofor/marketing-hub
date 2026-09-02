export const MARKET_EXPANSION_STRATEGY_CODE =
  "BOUNDED_ADJACENT_MARKET_EXPANSION_V1";
export const DEFAULT_MARKET_RESEARCH_ATTEMPTS = 3;

/** Limita a configuração operacional a uma, duas ou três rodadas totais. */
export function resolveMarketResearchAttempts(value) {
  const parsed = Number(value ?? DEFAULT_MARKET_RESEARCH_ATTEMPTS);
  if (!Number.isFinite(parsed)) return DEFAULT_MARKET_RESEARCH_ATTEMPTS;
  return Math.min(3, Math.max(1, Math.trunc(parsed)));
}

/**
 * Reavalia uma descoberta ampla com lentes adjacentes, preservando uma única execução e callback.
 */
export async function executeBoundedMarketResearch(job, options) {
  const configuredMaxAttempts = resolveMarketResearchAttempts(
    options.maxAttempts,
  );
  const maxAttempts =
    job.researchMode === "DISCOVER_MARKETS" ? configuredMaxAttempts : 1;
  const directedAttempts = [];
  const analysisAttempts = [];
  const attemptReports = [];
  let publicEvidenceItems = [];
  let marketplaceOfferItems = [];
  let metaAdItems = [];
  let metaCoverage = [];
  let finalReport;
  let stopReason;

  for (let attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber += 1) {
    const expansionContext = buildMarketExpansionContext({
      attemptNumber,
      maxAttempts,
      directedAttempts,
      attemptReports,
      finalReport,
      publicEvidenceItems,
      marketplaceOfferItems,
      metaAdItems,
      metaCoverage,
    });
    const directed = await options.planResearch({
      ...job,
      marketExpansionContext: expansionContext,
    });
    const directedAttempt = { attemptNumber, directed };
    directedAttempts.push(directedAttempt);
    await options.persistPlan(directedAttempts);

    if (
      attemptNumber > 1 &&
      !isNovelExpansionPlan(directed.plan, directedAttempts.slice(0, -1))
    ) {
      attemptReports.push(
        attemptReport(directedAttempt, {
          newPublicEvidenceCount: 0,
          newComparableOfferCount: 0,
          newMetaAdCount: 0,
          candidateCount: finalReport?.opportunities?.length || 0,
          dossierReadyCount: dossierReadyCount(finalReport),
          outcome: "REPEATED_RESEARCH_LENS",
        }),
      );
      stopReason = "REPEATED_RESEARCH_LENS";
      break;
    }

    const before = {
      publicEvidence: publicEvidenceItems.length,
      marketplaceOffers: marketplaceOfferItems.length,
      metaAds: metaAdItems.length,
    };
    const collected = await options.collectEvidence({
      job: { ...job, marketExpansionContext: expansionContext },
      plan: directed.plan,
      attemptNumber,
    });
    publicEvidenceItems = mergeUnique(
      publicEvidenceItems,
      collected.publicEvidence || [],
      publicEvidenceKey,
    );
    marketplaceOfferItems = mergeUnique(
      marketplaceOfferItems,
      collected.marketplaceOffers || [],
      canonicalMarketplaceOfferKey,
    );
    metaAdItems = mergeUnique(
      metaAdItems,
      collected.metaAdEvidence || [],
      metaAdEvidenceKey,
    );
    metaCoverage = mergeLatest(
      metaCoverage,
      collected.metaCoverage || [],
      metaCoverageKey,
    );
    const progress = {
      newPublicEvidenceCount:
        publicEvidenceItems.length - before.publicEvidence,
      newComparableOfferCount:
        marketplaceOfferItems.length - before.marketplaceOffers,
      newMetaAdCount: metaAdItems.length - before.metaAds,
    };

    if (attemptNumber > 1 && !hasNewEvidence(progress)) {
      attemptReports.push(
        attemptReport(directedAttempt, {
          ...progress,
          candidateCount: finalReport?.opportunities?.length || 0,
          dossierReadyCount: dossierReadyCount(finalReport),
          outcome: "NO_NEW_EVIDENCE",
        }),
      );
      stopReason = "NO_NEW_EVIDENCE";
      break;
    }

    const publicEvidence = identifyEvidence(
      publicEvidenceItems,
      "P",
      "PUBLIC_SEARCH",
    );
    const marketplaceOffers = identifyEvidence(
      marketplaceOfferItems,
      "O",
      "COMMERCIAL_OFFER",
    );
    const metaAdEvidence = identifyEvidence(
      metaAdItems,
      "M",
      "META_AD_LIBRARY",
    );
    const analysis = await options.synthesize({
      job,
      plan: directed.plan,
      publicEvidence,
      repositoryEvidence: options.repositoryEvidence || [],
      repositoryCoverage: options.repositoryCoverage || [],
      marketplaceOffers,
      metaAdEvidence,
      metaCoverage,
    });
    analysisAttempts.push({ attemptNumber, analysis });
    finalReport = options.analyze({
      job,
      plan: directed.plan,
      publicEvidence,
      marketplaceOffers,
      metaAdEvidence,
      metaCoverage,
      analysis,
      repositoryEvidence: options.repositoryEvidence || [],
      repositoryCoverage: options.repositoryCoverage || [],
    });
    enforceMarketplaceHandoffGate(
      finalReport,
      marketplaceOffers.length,
      directed.plan.minimumComparableOffers,
    );
    const ready = dossierReadyCount(finalReport);

    if (ready > 0) {
      attemptReports.push(
        attemptReport(directedAttempt, {
          ...progress,
          candidateCount: finalReport.opportunities.length,
          dossierReadyCount: ready,
          outcome: "DOSSIER_READY_FOUND",
        }),
      );
      stopReason = "DOSSIER_READY_FOUND";
      break;
    }
    if (!isExpansionEligible(job, analysis)) {
      attemptReports.push(
        attemptReport(directedAttempt, {
          ...progress,
          candidateCount: finalReport.opportunities.length,
          dossierReadyCount: ready,
          outcome: "EXPANSION_NOT_APPLICABLE",
        }),
      );
      stopReason = "EXPANSION_NOT_APPLICABLE";
      break;
    }
    if (attemptNumber >= maxAttempts) {
      attemptReports.push(
        attemptReport(directedAttempt, {
          ...progress,
          candidateCount: finalReport.opportunities.length,
          dossierReadyCount: ready,
          outcome: "ATTEMPT_LIMIT_REACHED",
        }),
      );
      stopReason = "ATTEMPT_LIMIT_REACHED";
      break;
    }
    attemptReports.push(
      attemptReport(directedAttempt, {
        ...progress,
        candidateCount: finalReport.opportunities.length,
        dossierReadyCount: ready,
        outcome: "ADJUST_AND_CONTINUE",
      }),
    );
  }

  if (!finalReport) {
    throw new Error(
      "Ampliação de mercado terminou sem uma síntese factual preservável",
    );
  }
  synchronizeEvidenceReport(finalReport, {
    publicEvidenceItems,
    marketplaceOfferItems,
    metaAdItems,
    metaCoverage,
  });
  attachMarketExpansionReport(finalReport, {
    maxAttempts,
    attemptReports,
    stopReason: stopReason || "ATTEMPT_LIMIT_REACHED",
  });
  return { report: finalReport, directedAttempts, analysisAttempts };
}

/** Preserva no callback final a cobertura acumulada mesmo quando a última lente não exige síntese. */
function synchronizeEvidenceReport(
  report,
  { publicEvidenceItems, marketplaceOfferItems, metaAdItems, metaCoverage },
) {
  report.evidenceReport ||= {};
  report.evidenceReport.publicEvidence = identifyEvidence(
    publicEvidenceItems,
    "P",
    "PUBLIC_SEARCH",
  );
  report.evidenceReport.marketplaceOffers = identifyEvidence(
    marketplaceOfferItems,
    "O",
    "COMMERCIAL_OFFER",
  );
  report.evidenceReport.metaAdEvidence = identifyEvidence(
    metaAdItems,
    "M",
    "META_AD_LIBRARY",
  );
  report.evidenceReport.metaCoverage = metaCoverage;
}

/** Expõe à próxima rodada somente lacunas e resumos necessários para mudar a lente. */
export function buildMarketExpansionContext({
  attemptNumber,
  maxAttempts,
  directedAttempts,
  attemptReports,
  finalReport,
  publicEvidenceItems,
  marketplaceOfferItems,
  metaAdItems,
  metaCoverage,
}) {
  return {
    strategyCode: MARKET_EXPANSION_STRATEGY_CODE,
    attemptNumber,
    maxAttempts,
    instruction:
      attemptNumber === 1
        ? "Investigue o escopo inicial recebido."
        : "Amplie exatamente uma lente adjacente sem escolher o posicionamento final de Atena.",
    previousResearchLenses: directedAttempts.map((item) => ({
      attemptNumber: item.attemptNumber,
      researchLens: item.directed.plan.researchLens,
      expansionAxis: item.directed.plan.expansionAxis,
    })),
    previousPublicQueries: directedAttempts.flatMap(
      (item) => item.directed.plan.publicQueries || [],
    ),
    previousMarketplaceQueries: directedAttempts.flatMap((item) =>
      (item.directed.plan.marketplaceRequests || []).map(
        (request) => `${request.marketplace}:${request.query}`,
      ),
    ),
    previousMetaQueries: directedAttempts.flatMap((item) =>
      (item.directed.plan.metaAdRequests || []).map((request) => request.query),
    ),
    accumulatedEvidence: {
      publicEvidenceCount: publicEvidenceItems.length,
      comparableOfferCount: marketplaceOfferItems.length,
      metaAdCount: metaAdItems.length,
      metaCoverageStatuses: metaCoverage.map((item) => item.sourceStatus),
    },
    previousCandidates: (finalReport?.opportunities || []).map((item) => ({
      name: item.name,
      primaryAudience: item.primaryAudience,
      rootPain: item.rootPain,
      maturity: item.maturity,
      decision: item.decision,
      commercialRisk: item.commercialRisk,
    })),
    completedAttempts: attemptReports,
  };
}

/** Evita gastar outra coleta quando o modelo repete a lente e quase todas as consultas. */
export function isNovelExpansionPlan(plan, previousAttempts) {
  if (plan.expansionAxis === "INITIAL_SCOPE") return false;
  const lens = normalize(plan.researchLens);
  if (
    previousAttempts.some(
      (item) => normalize(item.directed.plan.researchLens) === lens,
    )
  ) {
    return false;
  }
  const previousQueries = new Set(
    previousAttempts.flatMap((item) =>
      (item.directed.plan.publicQueries || []).map(normalize),
    ),
  );
  const newQueries = new Set(
    (plan.publicQueries || [])
      .map(normalize)
      .filter((query) => !previousQueries.has(query)),
  );
  const previousMarketplaceQueries = new Set(
    previousAttempts.flatMap((item) =>
      (item.directed.plan.marketplaceRequests || []).map((request) =>
        normalize(`${request.marketplace}:${request.query}`),
      ),
    ),
  );
  const hasNewMarketplaceQuery = (plan.marketplaceRequests || []).some(
    (request) =>
      !previousMarketplaceQueries.has(
        normalize(`${request.marketplace}:${request.query}`),
      ),
  );
  const previousMetaQueries = new Set(
    previousAttempts.flatMap((item) =>
      (item.directed.plan.metaAdRequests || []).map((request) =>
        normalize(request.query),
      ),
    ),
  );
  const hasNewMetaQuery = (plan.metaAdRequests || []).some(
    (request) => !previousMetaQueries.has(normalize(request.query)),
  );
  return newQueries.size >= 4 && hasNewMarketplaceQuery && hasNewMetaQuery;
}

/** Mantém a ampliação restrita à descoberta ampla executada por modelo. */
function isExpansionEligible(job, analysis) {
  return job.researchMode === "DISCOVER_MARKETS" && analysis.mode === "CODEX";
}

/** Detecta progresso factual sem usar cobertura vazia como sinal de mercado. */
function hasNewEvidence(progress) {
  return (
    progress.newPublicEvidenceCount > 0 ||
    progress.newComparableOfferCount > 0 ||
    progress.newMetaAdCount > 0
  );
}

/** Conta somente maturidade capaz de liberar o handoff controlado pelo backend. */
function dossierReadyCount(report) {
  return (report?.opportunities || []).filter(
    (item) => item.maturity === "DOSSIER_READY",
  ).length;
}

/** Resume uma rodada sem duplicar o payload factual completo no relatório gerencial. */
function attemptReport(directedAttempt, values) {
  return {
    attemptNumber: directedAttempt.attemptNumber,
    researchLens: directedAttempt.directed.plan.researchLens,
    expansionAxis: directedAttempt.directed.plan.expansionAxis,
    rationale: directedAttempt.directed.plan.expansionRationale,
    ...values,
  };
}

/** Anexa a trilha de decisão à evidência final persistida no mesmo ciclo. */
function attachMarketExpansionReport(
  report,
  { maxAttempts, attemptReports, stopReason },
) {
  const evaluated = attemptReports.filter(
    (item) => item.outcome !== "REPEATED_RESEARCH_LENS",
  );
  report.evidenceReport ||= {};
  report.evidenceReport.marketExpansion = {
    strategyCode: MARKET_EXPANSION_STRATEGY_CODE,
    attemptsCompleted: attemptReports.length,
    maxAttempts,
    stopReason,
    stopSummary: stopSummary(stopReason, maxAttempts),
    finalResearchLens:
      evaluated.at(-1)?.researchLens || attemptReports.at(-1)?.researchLens || null,
    attempts: attemptReports,
  };
}

/** Traduz o motivo técnico em decisão operacional legível sem prometer resultado comercial. */
function stopSummary(reason, maxAttempts) {
  const summaries = {
    DOSSIER_READY_FOUND:
      "Argos encontrou uma candidata factual pronta para o gate de Atena.",
    NO_NEW_EVIDENCE:
      "A lente seguinte não acrescentou evidência pública, oferta comparável nem anúncio Meta.",
    REPEATED_RESEARCH_LENS:
      "O novo plano repetiria a lente ou as consultas já executadas e foi interrompido antes de novo consumo.",
    EXPANSION_NOT_APPLICABLE:
      "O modo atual não autoriza ampliação adaptativa; o resultado da primeira pesquisa foi preservado.",
    ATTEMPT_LIMIT_REACHED: `O limite controlado de ${maxAttempts} tentativas foi alcançado sem fabricar aprovação.`,
  };
  return summaries[reason] || summaries.ATTEMPT_LIMIT_REACHED;
}

/** Mantém a primeira ocorrência de cada fato para preservar ids estáveis entre sínteses. */
function mergeUnique(current, incoming, keyFunction) {
  const merged = new Map(current.map((item) => [keyFunction(item), item]));
  for (const item of incoming) merged.set(keyFunction(item), merged.get(keyFunction(item)) || item);
  return [...merged.values()];
}

/** Mantém a cobertura mais recente para a mesma consulta Meta. */
function mergeLatest(current, incoming, keyFunction) {
  const merged = new Map(current.map((item) => [keyFunction(item), item]));
  for (const item of incoming) merged.set(keyFunction(item), item);
  return [...merged.values()];
}

/** Atribui ids cumulativos estáveis depois da deduplicação. */
function identifyEvidence(items, prefix, sourceType) {
  return items.map((item, index) => ({
    ...item,
    evidenceId: `${prefix}${index + 1}`,
    sourceType,
  }));
}

function publicEvidenceKey(item) {
  return normalize(item.url || `${item.title}:${item.snippet}`);
}

export function canonicalMarketplaceOfferKey(item) {
  const marketplace = canonicalOfferIdentityPart(item.marketplace);
  const title = canonicalOfferIdentityPart(item.title);
  const producer = canonicalOfferIdentityPart(item.producer);
  const referenceId = canonicalOfferIdentityPart(item.referenceId);
  if (title) return `${marketplace}:title:${title}:producer:${producer}`;
  return referenceId ? `${marketplace}:reference:${referenceId}` : "";
}

/** Mantém anúncios distintos por referência sem aplicar a deduplicação comercial de ofertas. */
function metaAdEvidenceKey(item) {
  return normalize(
    `${item.referenceId || item.url || item.title}:${item.producer || ""}`,
  );
}

/** Replica a normalização do backend para o worker não abrir um handoff que será recusado. */
function canonicalOfferIdentityPart(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim();
}

/** Recalcula o gate na fronteira do callback mesmo quando uma análise futura vier permissiva. */
export function enforceMarketplaceHandoffGate(
  report,
  comparableOfferCount,
  minimumComparableOffers = 10,
) {
  const minimum = Math.max(10, Number(minimumComparableOffers || 10));
  if (comparableOfferCount >= minimum) return report;
  const reason = `Foram confirmadas ${comparableOfferCount} de ${minimum} ofertas comparáveis únicas; o handoff permanece bloqueado para enriquecimento.`;
  for (const opportunity of report?.opportunities || []) {
    if (
      opportunity.maturity !== "DOSSIER_READY" &&
      opportunity.decision !== "APPROVE"
    ) {
      continue;
    }
    opportunity.maturity = "RESEARCHABLE";
    opportunity.decision = "RESEARCH_MORE";
    opportunity.commercialRisk = appendUniqueText(
      opportunity.commercialRisk,
      reason,
    );
    opportunity.evidenceJson = demoteEvidenceMaturity(
      opportunity.evidenceJson,
    );
  }
  if (report?.evidenceReport?.gates) {
    report.evidenceReport.gates.marketplaceGatePassed = false;
  }
  if (report && !String(report.decisionSummary || "").includes(reason)) {
    report.decisionSummary = appendUniqueText(report.decisionSummary, reason);
  }
  return report;
}

/** Mantém a maturidade do JSON auditável coerente com a candidata devolvida pelo callback. */
function demoteEvidenceMaturity(value) {
  if (!value) return value;
  try {
    const evidence = JSON.parse(value);
    if (evidence.candidateEvidence) {
      evidence.candidateEvidence.maturity = "RESEARCHABLE";
    }
    return JSON.stringify(evidence);
  } catch {
    return value;
  }
}

/** Acrescenta uma causa funcional sem repetir o mesmo texto em ampliações sucessivas. */
function appendUniqueText(current, addition) {
  const text = String(current || "").trim();
  return text.includes(addition) ? text : `${text} ${addition}`.trim();
}

function metaCoverageKey(item) {
  return normalize(
    `${item.query}:${item.country}:${item.publisherPlatform}`,
  );
}

function normalize(value) {
  return String(value || "").trim().toLocaleLowerCase("pt-BR");
}
