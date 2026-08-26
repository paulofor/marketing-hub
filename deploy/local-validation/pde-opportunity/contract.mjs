const REQUIRED_WINNER_SUPPORTS = ["RECURRENCE", "UNMETNESS", "PURCHASE_INTENT"];

/** Mantém no ciclo somente fontes das três oportunidades ativas, preservando o histórico no arquivo. */
export function selectActiveResearch(research) {
  const candidateNames = new Set((research?.candidates || []).map((candidate) => candidate.name));
  const sources = (research?.sources || []).filter((source) =>
    candidateNames.has(source.candidateName),
  );
  const paidOffers = sources.filter(
    (source) => source.sourceType === "COMMERCIAL_OFFER" && source.paid,
  );
  return {
    ...research,
    sources,
    auditFacts: {
      activeCandidateCount: candidateNames.size,
      activeSourceCount: sources.length,
      paidOfferCount: new Set(paidOffers.map((source) => source.offerKey)).size,
      paidOffersByCandidate: Object.fromEntries(
        [...candidateNames].map((candidateName) => [
          candidateName,
          new Set(
            paidOffers
              .filter((source) => source.candidateName === candidateName)
              .map((source) => source.offerKey),
          ).size,
        ]),
      ),
    },
  };
}

/** Valida as evidências antes de qualquer consumo de modelo. */
export function validateResearchInput(research) {
  if (research?.processVersion === 5) validateInspirationContract(research);
  validateCommercialFocus(research);
  const candidates = research?.candidates || [];
  if (candidates.length !== 3) {
    throw new Error("A pesquisa deve comparar exatamente três oportunidades.");
  }

  const candidateNames = candidates.map((item) => item.name);
  if (new Set(candidateNames).size !== 3) {
    throw new Error("As oportunidades da pesquisa devem ter nomes distintos.");
  }

  const sources = research?.sources || [];
  const sourceById = new Map(sources.map((source) => [source.id, source]));
  if (sourceById.size !== sources.length) {
    throw new Error("A pesquisa possui fontes duplicadas.");
  }
  if (sources.some((source) => !candidateNames.includes(source.candidateName))) {
    throw new Error("A pesquisa possui fonte órfã ou atribuída a oportunidade fora do ciclo.");
  }

  const paidOffers = sources.filter(
    (source) => source.sourceType === "COMMERCIAL_OFFER" && source.paid,
  );
  const uniqueOffers = new Set(paidOffers.map((source) => source.offerKey));
  if (uniqueOffers.has("") || uniqueOffers.size < 10) {
    throw new Error("O ciclo precisa de ao menos dez ofertas pagas e deduplicadas.");
  }
  if (
    research.auditFacts &&
    research.auditFacts.paidOfferCount !== uniqueOffers.size
  ) {
    throw new Error("A contagem determinística de ofertas pagas foi alterada.");
  }

  for (const candidate of candidates) {
    if (!Array.isArray(candidate.sourceIds) || candidate.sourceIds.length < 6) {
      throw new Error(`${candidate.name} precisa de ao menos seis fontes.`);
    }
    for (const sourceId of candidate.sourceIds) {
      const source = sourceById.get(sourceId);
      if (!source) throw new Error(`Fonte inexistente em ${candidate.name}: ${sourceId}.`);
      if (source.candidateName !== candidate.name) {
        throw new Error(`Fonte ${sourceId} foi atribuída à oportunidade incorreta.`);
      }
    }
    const candidateOffers = paidOffers.filter(
      (source) => source.candidateName === candidate.name,
    );
    if (new Set(candidateOffers.map((source) => source.offerKey)).size < 3) {
      throw new Error(`${candidate.name} precisa de ao menos três ofertas pagas comparáveis.`);
    }
  }
}

/** Protege o recorte comercial sem transformar qualquer tema popular em oportunidade B2C. */
function validateCommercialFocus(research) {
  const focus = research?.commercialFocus;
  if (!focus) return;
  if (
    focus.audienceModel !== "B2C" ||
    focus.acquisitionChannel !== "INSTAGRAM" ||
    focus.benchmarkRule !== "STRICTLY_EXCEEDS" ||
    focus.maxMinutesToValue !== 10
  ) {
    throw new Error("O recorte comercial deve ser B2C/Instagram, com valor em até dez minutos e benchmark estrito.");
  }
  for (const candidate of research.candidates || []) {
    if (
      candidate.audienceModel !== "B2C" ||
      candidate.acquisitionChannel !== "INSTAGRAM" ||
      candidate.requiresBusinessOperation !== false ||
      !candidate.consumerMoment?.trim() ||
      !candidate.instagramHook?.trim() ||
      !candidate.mobileValueMoment?.trim()
    ) {
      throw new Error(`${candidate.name} não cumpre o contrato B2C/Instagram.`);
    }
  }
}

/** Protege a consulta dinâmica e separa inspiração de evidência independente. */
function validateInspirationContract(research) {
  const inspirations = research?.inspirations || {};
  const articles = inspirations.articles || [];
  for (const origin of ["GARTNER", "IA_APLICADA"]) {
    if (!articles.some((article) => article.origin === origin)) {
      throw new Error(`A Descoberta v5 precisa consultar a coleção viva ${origin}.`);
    }
  }
  if ((inspirations.hotmartProducts || []).length === 0) {
    throw new Error("A Descoberta v5 precisa registrar a consulta aos produtos Hotmart.");
  }
  for (const product of inspirations.hotmartProducts || []) {
    if (product.tractionIsNotSale !== true) {
      throw new Error("Produto Hotmart não pode transformar score ou temperatura em venda.");
    }
  }

  const inspirationIds = new Set([
    ...articles.map((article) => article.id),
    ...(inspirations.hotmartProducts || []).map((product) => product.id),
  ]);
  const sourceById = new Map((research.sources || []).map((source) => [source.id, source]));
  for (const candidate of research.candidates || []) {
    const usages = (inspirations.usages || []).filter(
      (usage) => usage.candidateName === candidate.name,
    );
    if (usages.length < 2) {
      throw new Error(`${candidate.name} precisa registrar ao menos duas inspirações rastreáveis.`);
    }
    for (const usage of usages) {
      if (!inspirationIds.has(usage.inspirationId)) {
        throw new Error(`Inspiração inexistente em ${candidate.name}: ${usage.inspirationId}.`);
      }
      if (!usage.originalValueHypothesis?.trim() || !usage.copyBoundary?.trim()) {
        throw new Error(`${candidate.name} possui inspiração sem hipótese original ou limite de cópia.`);
      }
      const confirmations = (usage.confirmingSourceIds || []).map((sourceId) =>
        sourceById.get(sourceId),
      );
      if (
        confirmations.length < 2 ||
        confirmations.some((source) => !source || source.candidateName !== candidate.name) ||
        new Set(confirmations.map((source) => source.pathway)).size < 2
      ) {
        throw new Error(
          `${candidate.name} precisa confirmar ou descartar cada inspiração por duas vias independentes.`,
        );
      }
    }
  }
}

/** Impede que uma nova amostragem do modelo altere as regras objetivas do processo. */
export function validateFunctionalResult(agentRole, context, result) {
  if (agentRole === "argos") validateArgos(context, result);
  if (agentRole === "hermes") validateHermes(context, result);
  if (agentRole === "dedalo") validateDedalo(context, result);
  if (agentRole === "psique") validatePsique(context, result);
}

/** Consolida o gate sem delegar a regra de avanço ao modelo. */
export function buildFinalDecision(context) {
  const decisions = {
    argos: context.argos.decision,
    hermes: context.hermes.decision,
    dedalo: context.dedalo.decision,
    psique: context.psique.decision,
  };
  const chosen = context.dedalo.chosenOpportunity.sourceAlternativeName;
  const scored = context.dedalo.comparison.find((item) => item.name === chosen);
  const benchmarkScore = context.research.benchmark.score;
  const reasons = [];

  for (const [agent, decision] of Object.entries(decisions)) {
    if (decision !== "APPROVE") reasons.push(`${agent} decidiu ${decision}.`);
  }
  if (scored.totalScore <= benchmarkScore) {
    reasons.push(`Score ${scored.totalScore} não supera o benchmark ${benchmarkScore}.`);
  }
  if (context.psique.valueScore < 75) {
    reasons.push(`Valor percebido ${context.psique.valueScore} abaixo do mínimo 75.`);
  }

  const finalDecision = Object.values(decisions).includes("REJECT")
    ? "REJECT"
    : reasons.length === 0
      ? "APPROVE"
      : "RESEARCH_MORE";

  return {
    decision: finalDecision,
    chosenOpportunity: chosen,
    workingProductName: context.dedalo.chosenOpportunity.workingProductName,
    totalScore: scored.totalScore,
    benchmarkName: context.research.benchmark.name,
    benchmarkScore,
    benchmarkResult:
      scored.totalScore > benchmarkScore
        ? "EXCEEDS"
        : scored.totalScore === benchmarkScore
          ? "MEETS"
          : "BELOW",
    agentDecisions: decisions,
    reasons,
  };
}

function validateArgos(research, result) {
  validateResearchInput(research);
  const expectedNames = research.candidates.map((item) => item.name).sort();
  assertSameNames(expectedNames, result.alternatives, "Argos alterou as três oportunidades.");

  const sourceById = new Map(research.sources.map((source) => [source.id, source]));
  const offerCount = new Set(
    research.sources
      .filter((source) => source.sourceType === "COMMERCIAL_OFFER" && source.paid)
      .map((source) => source.offerKey),
  ).size;
  if (result.evidenceSummary.cycleOfferCount !== offerCount) {
    throw new Error("Argos alterou a contagem auditável de ofertas pagas.");
  }
  for (const alternative of result.alternatives) {
    if (alternative.evidenceSourceIds.length < 6) {
      throw new Error(`${alternative.name} saiu de Argos com menos de seis fontes.`);
    }
    for (const sourceId of alternative.evidenceSourceIds) {
      const source = sourceById.get(sourceId);
      if (!source) throw new Error(`Argos citou fonte inexistente: ${sourceId}.`);
      if (source.candidateName !== alternative.name) {
        throw new Error(`Argos cruzou fonte de outra oportunidade em ${alternative.name}.`);
      }
    }
    const paidIds = new Set(alternative.paidOfferSourceIds);
    if (paidIds.size < 3) {
      throw new Error(`${alternative.name} saiu de Argos com menos de três ofertas pagas.`);
    }
    for (const sourceId of paidIds) {
      const source = sourceById.get(sourceId);
      if (
        !source ||
        source.candidateName !== alternative.name ||
        source.sourceType !== "COMMERCIAL_OFFER" ||
        !source.paid
      ) {
        throw new Error(`Argos classificou como oferta paga uma fonte inválida: ${sourceId}.`);
      }
    }
    if (research.commercialFocus) {
      if (
        alternative.audienceModel !== "B2C" ||
        alternative.acquisitionChannel !== "INSTAGRAM" ||
        alternative.mobileValueMomentMinutes > research.commercialFocus.maxMinutesToValue ||
        !alternative.consumerMoment?.trim() ||
        !alternative.instagramHook?.trim()
      ) {
        throw new Error(`${alternative.name} saiu de Argos sem recorte B2C/Instagram executável.`);
      }
    }
  }

  const recommended = result.alternatives.find(
    (item) => item.name === result.recommendedOpportunity,
  );
  if (!recommended) throw new Error("Argos recomendou oportunidade fora da comparação.");

  if (result.decision === "APPROVE") {
    const recommendedSourceIds = [
      ...new Set([
        ...recommended.evidenceSourceIds,
        ...recommended.paidOfferSourceIds,
      ]),
    ];
    for (const support of REQUIRED_WINNER_SUPPORTS) {
      const pathways = new Set(
        recommendedSourceIds
          .map((sourceId) => sourceById.get(sourceId))
          .filter((source) => source.supports.includes(support))
          .map((source) => source.pathway),
      );
      if (pathways.size < 2) {
        throw new Error(
          `Argos aprovou ${recommended.name} sem duas vias independentes para ${support}.`,
        );
      }
    }
  }
}

function validateHermes(context, result) {
  const expectedNames = context.argos.alternatives.map((item) => item.name).sort();
  assertSameNames(
    expectedNames,
    result.journeyComparison,
    "Hermes não preservou as três oportunidades de Argos.",
    "candidateName",
  );
  for (const item of result.journeyComparison) {
    if (item.distributionRoutes.length !== 3) {
      throw new Error(`Hermes deve comparar três rotas em ${item.candidateName}.`);
    }
    if (item.chosenInitialRouteIndex < 0 || item.chosenInitialRouteIndex > 2) {
      throw new Error(`Hermes escolheu índice de rota inexistente em ${item.candidateName}.`);
    }
    if (item.distributionRoutes.some((route) => route.externalSpend !== "NONE")) {
      throw new Error("Hermes não pode autorizar gasto externo na Descoberta.");
    }
    if (context.research.commercialFocus) {
      const chosenRoute = item.distributionRoutes[item.chosenInitialRouteIndex];
      if (chosenRoute.channel !== "INSTAGRAM") {
        throw new Error(`Hermes não escolheu Instagram como rota inicial de ${item.candidateName}.`);
      }
      const eventPath = new Set(chosenRoute.eventPath || []);
      for (const event of ["IMPRESSION", "CLICK", "EXPERIENCE_STARTED", "VALUE_MOMENT", "CHECKOUT_STARTED"]) {
        if (!eventPath.has(event)) {
          throw new Error(`Hermes não tornou a rota de ${item.candidateName} atribuível até checkout.`);
        }
      }
    }
  }
  if (result.decision === "APPROVE" && context.argos.decision !== "APPROVE") {
    throw new Error("Hermes aprovou uma pesquisa não aprovada por Argos.");
  }
}

function validateDedalo(context, result) {
  const expectedNames = context.argos.alternatives.map((item) => item.name).sort();
  assertSameNames(
    expectedNames,
    result.comparison,
    "Dédalo não preservou as três oportunidades de Argos.",
  );
  for (const item of result.comparison) {
    if (item.formats.length !== 3) {
      throw new Error(`Dédalo deve comparar três formatos em ${item.name}.`);
    }
    if (!item.formats.map((format) => format.name).includes(item.chosenFormat)) {
      throw new Error(`Dédalo escolheu formato inexistente em ${item.name}.`);
    }
    const sum =
      item.evidenceScore +
      item.purchaseIntentScore +
      item.painValueScore +
      item.pdeFitScore +
      item.differentiationScore +
      item.distributionScore +
      item.economicsScalabilityScore +
      item.riskSafetyScore;
    if (sum !== item.totalScore) {
      throw new Error(`Score total inconsistente em ${item.name}.`);
    }
    if (context.research.commercialFocus) {
      if (
        item.audienceModel !== "B2C" ||
        item.mobileValueMomentMinutes > context.research.commercialFocus.maxMinutesToValue ||
        !item.consumerMoment?.trim() ||
        !item.instagramHook?.trim()
      ) {
        throw new Error(`${item.name} não atingiu o gate B2C/Instagram de Dédalo.`);
      }
    }
  }

  const winner = [...result.comparison].sort(
    (left, right) =>
      right.totalScore - left.totalScore ||
      right.riskSafetyScore - left.riskSafetyScore ||
      left.name.localeCompare(right.name),
  )[0];
  if (result.chosenOpportunity.sourceAlternativeName !== winner.name) {
    throw new Error("Dédalo escolheu alternativa diferente do maior score auditável.");
  }
  if (result.chosenOpportunity.chosenFormat !== winner.chosenFormat) {
    throw new Error("Dédalo divergiu sobre o formato da oportunidade vencedora.");
  }
  if (
    context.research.commercialFocus &&
    result.decision === "APPROVE" &&
    winner.distributionScore < 8
  ) {
    throw new Error("Dédalo aprovou vencedora sem distribuição mínima para Instagram.");
  }

  const benchmark = result.chosenOpportunity.benchmark;
  if (
    benchmark.name !== context.research.benchmark.name ||
    benchmark.score !== context.research.benchmark.score ||
    benchmark.candidateScore !== winner.totalScore
  ) {
    throw new Error("Dédalo alterou o benchmark ou o score da vencedora.");
  }
  const expectedResult =
    winner.totalScore > benchmark.score
      ? "EXCEEDS"
      : winner.totalScore === benchmark.score
        ? "MEETS"
        : "BELOW";
  if (benchmark.result !== expectedResult) {
    throw new Error("Dédalo classificou incorretamente a comparação com Rigel.");
  }
  if (
    result.decision === "APPROVE" &&
    (winner.totalScore <= benchmark.score ||
      context.argos.decision !== "APPROVE" ||
      context.hermes.decision !== "APPROVE")
  ) {
    throw new Error("Dédalo aprovou sem cumprir pesquisa, jornada e benchmark.");
  }
}

function validatePsique(context, result) {
  const expected = context.dedalo.chosenOpportunity.sourceAlternativeName;
  if (result.sourceAlternativeName !== expected) {
    throw new Error("Psique avaliou oportunidade diferente da escolhida por Dédalo.");
  }
  if (result.workingProductName !== context.dedalo.chosenOpportunity.workingProductName) {
    throw new Error("Psique alterou o nome de trabalho definido por Dédalo.");
  }
  if (result.decision === "APPROVE" && result.valueScore < 75) {
    throw new Error("Psique aprovou valor percebido inferior ao mínimo de 75.");
  }
  if (
    context.research?.commercialFocus &&
    result.decision === "APPROVE" &&
    (!result.canReachValueAlone || result.manipulationRisk !== "LOW")
  ) {
    throw new Error("Psique aprovou B2C sem valor autônomo ou com risco de manipulação.");
  }
}

function assertSameNames(expectedNames, items, message, property = "name") {
  const actualNames = items.map((item) => item[property]).sort();
  if (JSON.stringify(expectedNames) !== JSON.stringify(actualNames)) throw new Error(message);
}
