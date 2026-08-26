const REQUIRED_LIVE_COLLECTIONS = [
  "GARTNER",
  "IA_APLICADA",
  "MOMENTOS_COMPRA_B2C",
];
const HUMAN_VALUE_TERRITORIES = new Set([
  "AFFECTION_AND_BELONGING",
  "RECOGNITION",
  "EFFORT_RELIEF",
]);

/** Consolida fatos observados e impede priorização B2C/Instagram antes do momento de compra. */
export function buildPurchaseMomentGate(research) {
  if (!requiresPurchaseMomentValidation(research)) {
    return {
      required: false,
      status: "NOT_REQUIRED",
      sourceQualityPassed: true,
      finalPrioritizationEligible: true,
      minimumIndependentReadings: 0,
      eligibleCandidateNames: (research?.candidates || []).map((candidate) => candidate.name),
      reasons: [],
      candidates: [],
    };
  }

  const contract = research?.purchaseMomentValidation || {};
  const sourceQuality = evaluateSourceQuality(research, contract);
  const criteria = evaluateCriteria(contract.successCriteria);
  const validationsByCandidate = new Map(
    (contract.candidates || []).map((candidate) => [candidate.candidateName, candidate]),
  );
  const sourceById = new Map(
    (research?.sources || []).map((source) => [source.id, source]),
  );
  const candidates = (research?.candidates || []).map((candidate) =>
    evaluateCandidate(
      candidate,
      validationsByCandidate.get(candidate.name),
      criteria,
      sourceQuality.passed,
      sourceById,
    ),
  );
  const eligibleCandidateNames =
    sourceQuality.passed && criteria.passed
      ? candidates
          .filter((candidate) => candidate.eligibleForFinalPrioritization)
          .map((candidate) => candidate.candidateName)
      : [];
  const reasons = [...sourceQuality.reasons, ...criteria.reasons];
  if (eligibleCandidateNames.length === 0) {
    reasons.push("Nenhuma candidata possui duas leituras válidas para priorização final.");
  }

  let status = "WAITING_VALIDATION";
  if (!sourceQuality.passed) status = "WAITING_SOURCE_QUALITY";
  else if (sourceQuality.passed && criteria.passed && eligibleCandidateNames.length > 0) {
    status = "PASS";
  } else if (candidates.length > 0 && candidates.every((candidate) => candidate.status === "STOP")) {
    status = "STOP";
  } else if (candidates.some((candidate) => candidate.status === "ADJUST")) {
    status = "ADJUST";
  }

  return {
    required: true,
    status,
    sourceQualityPassed: sourceQuality.passed,
    finalPrioritizationEligible: status === "PASS",
    minimumIndependentReadings: 2,
    sourceQuality,
    successCriteria: criteria.normalized,
    eligibleCandidateNames,
    reasons,
    candidates,
  };
}

/** Confirma que a vencedora passou pelo gate antes de qualquer comparação com benchmark. */
export function assertPurchaseMomentEligible(gate, candidateName) {
  if (!gate.required) return;
  if (
    gate.status !== "PASS" ||
    gate.finalPrioritizationEligible !== true ||
    !gate.eligibleCandidateNames.includes(candidateName)
  ) {
    throw new Error(
      `${candidateName} não possui Validação do Momento de Compra aprovada antes da priorização.`,
    );
  }
}

/** Identifica exclusivamente o recorte que exige prova comportamental privada. */
export function requiresPurchaseMomentValidation(research) {
  return (
    research?.processVersion === 5 &&
    research?.commercialFocus?.audienceModel === "B2C" &&
    research?.commercialFocus?.acquisitionChannel === "INSTAGRAM"
  );
}

function evaluateSourceQuality(research, contract) {
  const reasons = [];
  const evaluatedAt = parseInstant(contract?.sourceQuality?.evaluatedAt);
  const maxAgeDays = Number(contract?.sourceQuality?.maxAgeDays);
  if (!evaluatedAt) reasons.push("Data de avaliação das fontes ausente ou inválida.");
  if (!Number.isInteger(maxAgeDays) || maxAgeDays < 1 || maxAgeDays > 90) {
    reasons.push("Validade das fontes deve ser declarada entre 1 e 90 dias.");
  }

  const liveCollections = new Map(
    (research?.inspirations?.collections || []).map((collection) => [collection.code, collection]),
  );
  for (const collectionCode of REQUIRED_LIVE_COLLECTIONS) {
    const collection = liveCollections.get(collectionCode);
    if (!collection) {
      reasons.push(`Coleção ${collectionCode} não foi consultada nesta execução.`);
    } else if (collection.status !== "CURRENT") {
      reasons.push(`Coleção ${collectionCode} está ${collection.status}.`);
    }
  }

  const hotmart = research?.inspirations?.hotmartContract || {};
  if (!/^(CURRENT|OBSERVED|VALID)$/.test(String(hotmart.status || ""))) {
    reasons.push(`Snapshot Hotmart está ${hotmart.status || "UNAVAILABLE"}.`);
  }
  const products = research?.inspirations?.hotmartProducts || [];
  if (products.length === 0) reasons.push("Snapshot Hotmart não possui produto nominal.");
  for (const product of products) {
    if (isPlaceholder(product.title)) {
      reasons.push(`Produto Hotmart ${product.id || "sem identificador"} possui placeholder.`);
    }
    if (!product.id?.trim() || !product.url?.trim()) {
      reasons.push("Produto Hotmart possui identidade ou URL incompleta.");
    }
    if (!hasCommercialDetail(product)) {
      reasons.push(
        `Produto Hotmart ${product.id || "sem identificador"} não possui preço nem sinal de tração.`,
      );
    }
    const collectedAt = parseInstant(product.collectedAt);
    if (!collectedAt || !evaluatedAt || !Number.isInteger(maxAgeDays)) {
      if (!collectedAt) reasons.push(`Produto Hotmart ${product.id || "sem identificador"} não possui coleta válida.`);
      continue;
    }
    const ageDays = (evaluatedAt.getTime() - collectedAt.getTime()) / 86_400_000;
    if (ageDays < 0 || ageDays > maxAgeDays) {
      reasons.push(`Produto Hotmart ${product.id || "sem identificador"} está fora da validade declarada.`);
    }
  }

  return {
    passed: reasons.length === 0,
    evaluatedAt: evaluatedAt?.toISOString() || null,
    maxAgeDays: Number.isInteger(maxAgeDays) ? maxAgeDays : null,
    reasons,
  };
}

function evaluateCriteria(criteria) {
  const reasons = [];
  const normalized = {
    declaredAt: parseInstant(criteria?.declaredAt)?.toISOString() || null,
    minimumEligibleParticipantsPerReading: Number(
      criteria?.minimumEligibleParticipantsPerReading,
    ),
    minimumExperienceStartRate: Number(criteria?.minimumExperienceStartRate),
    minimumValueMomentRate: Number(criteria?.minimumValueMomentRate),
    minimumReadyResultUseRate: Number(criteria?.minimumReadyResultUseRate),
    minimumPrototypePreferenceRate: Number(criteria?.minimumPrototypePreferenceRate),
    minimumCheckoutStartRate: Number(criteria?.minimumCheckoutStartRate),
  };
  if (!normalized.declaredAt) reasons.push("Critérios não possuem data de declaração válida.");
  if (
    !Number.isInteger(normalized.minimumEligibleParticipantsPerReading) ||
    normalized.minimumEligibleParticipantsPerReading < 1
  ) {
    reasons.push("Amostra mínima por leitura deve ser um inteiro positivo.");
  }
  for (const key of [
    "minimumExperienceStartRate",
    "minimumValueMomentRate",
    "minimumReadyResultUseRate",
    "minimumPrototypePreferenceRate",
    "minimumCheckoutStartRate",
  ]) {
    if (!Number.isFinite(normalized[key]) || normalized[key] < 0 || normalized[key] > 1) {
      reasons.push(`${key} deve ficar entre 0 e 1.`);
    }
  }
  if (normalized.minimumReadyResultUseRate <= 0) {
    reasons.push(
      "minimumReadyResultUseRate deve ser maior que zero para exigir uso observado.",
    );
  }
  return { passed: reasons.length === 0, reasons, normalized };
}

function evaluateCandidate(candidate, validation, criteria, sourcesPassed, sourceById) {
  const reasons = [];
  if (!validation) {
    return candidateResult(candidate.name, "WAITING_VALIDATION", reasons.concat(
      "Contrato do momento de compra ainda não foi registrado.",
    ));
  }
  if (validation.candidateName !== candidate.name) {
    reasons.push("Contrato foi vinculado à candidata incorreta.");
  }
  for (const field of [
    "trigger",
    "deadline",
    "costOfError",
    "budgetEvidence",
    "failedAttempt",
    "currentPaidBehavior",
  ]) {
    if (!validation.scene?.[field]?.trim()) reasons.push(`Cena de compra sem ${field}.`);
  }
  if (!validation.freeAlternative?.name?.trim()) reasons.push("Alternativa gratuita ausente.");
  if (!validation.freeAlternative?.prototypeAdvantage?.trim()) {
    reasons.push("Vantagem pretendida sobre o gratuito ausente.");
  }
  const humanValueDelivery = evaluateHumanValueDelivery(
    candidate,
    validation.humanValueDelivery,
    sourceById,
  );
  reasons.push(...humanValueDelivery.reasons);
  if (
    !validation.prototype?.prototypeId?.trim() ||
    validation.prototype.private !== true ||
    validation.prototype.published !== false ||
    validation.prototype.paymentEnabled !== false ||
    Number(validation.prototype.mediaSpend) !== 0 ||
    !["PRIVATE_PROTOTYPE", "LOCAL_QA"].includes(validation.prototype.testMarker)
  ) {
    reasons.push("Protótipo não está privado, segregado e sem pagamento ou mídia.");
  }

  const declaredAt = parseInstant(criteria.normalized.declaredAt);
  const readings = (validation.readings || []).map((reading) =>
    evaluateReading(reading, criteria.normalized, declaredAt),
  );
  if (new Set(readings.map((reading) => reading.readingId)).size !== readings.length) {
    reasons.push("As leituras possuem identificadores duplicados.");
  }
  if (readings.length < 2) reasons.push("São necessárias duas leituras independentes.");
  reasons.push(...readings.flatMap((reading) => reading.reasons));

  const safetyBlocked = readings.some(
    (reading) => reading.psiqueDecision === "BLOCK" || reading.temisDecision === "BLOCK",
  );
  const freeAlternativeWon =
    readings.length >= 2 &&
    readings.every((reading) => reading.prototypePreferenceRate <= 0.5);
  const readingsPassed = readings.length >= 2 && readings.every((reading) => reading.passed);

  let status = "ADJUST";
  if (!sourcesPassed) status = "WAITING_SOURCE_QUALITY";
  else if (safetyBlocked || freeAlternativeWon) status = "STOP";
  else if (readings.length < 2) status = "WAITING_VALIDATION";
  else if (readingsPassed && reasons.length === 0) status = "PASS";

  return {
    candidateName: candidate.name,
    status,
    eligibleForFinalPrioritization: status === "PASS",
    scene: validation.scene,
    freeAlternative: validation.freeAlternative,
    humanValueDelivery: humanValueDelivery.normalized,
    prototype: validation.prototype,
    readings,
    reasons,
  };
}

function evaluateReading(reading, criteria, declaredAt) {
  const reasons = [];
  const observedAt = parseInstant(reading?.observedAt);
  const eligibleParticipants = integer(reading?.eligibleParticipants);
  const experienceStarted = integer(reading?.experienceStarted);
  const valueMoments = integer(reading?.valueMoments);
  const readyResultsUsedWithoutAssembly = integer(
    reading?.readyResultsUsedWithoutAssembly,
  );
  const prototypePreferredOverFree = integer(reading?.prototypePreferredOverFree);
  const checkoutStarted = integer(reading?.checkoutStarted);
  if (!reading?.readingId?.trim()) reasons.push("Leitura sem identificador.");
  if (!observedAt) reasons.push(`${reading?.readingId || "Leitura"} sem data válida.`);
  if (declaredAt && observedAt && declaredAt > observedAt) {
    reasons.push(`${reading.readingId} ocorreu antes da declaração dos critérios.`);
  }
  if (reading?.eventSource !== "FIRST_PARTY_EVENTS") {
    reasons.push(`${reading?.readingId || "Leitura"} não usa eventos próprios.`);
  }
  if (!["PRIVATE_PROTOTYPE", "LOCAL_QA"].includes(reading?.testMarker)) {
    reasons.push(`${reading?.readingId || "Leitura"} não está segregada como teste privado.`);
  }
  if (!["APPROVE", "BLOCK"].includes(reading?.psiqueDecision)) {
    reasons.push(`${reading?.readingId || "Leitura"} não possui decisão de Psique.`);
  }
  if (!["APPROVE", "BLOCK"].includes(reading?.temisDecision)) {
    reasons.push(`${reading?.readingId || "Leitura"} não possui decisão de Têmis.`);
  }
  if (
    [
      eligibleParticipants,
      experienceStarted,
      valueMoments,
      readyResultsUsedWithoutAssembly,
      prototypePreferredOverFree,
      checkoutStarted,
    ]
      .some((value) => value === null)
  ) {
    reasons.push(`${reading?.readingId || "Leitura"} possui contagem inválida.`);
  } else if (
    experienceStarted > eligibleParticipants ||
    valueMoments > experienceStarted ||
    readyResultsUsedWithoutAssembly > valueMoments ||
    prototypePreferredOverFree > eligibleParticipants ||
    checkoutStarted > experienceStarted
  ) {
    reasons.push(`${reading.readingId} possui numerador maior que o denominador.`);
  }

  const experienceStartRate = rate(experienceStarted, eligibleParticipants);
  const valueMomentRate = rate(valueMoments, experienceStarted);
  const readyResultUseRate = rate(readyResultsUsedWithoutAssembly, experienceStarted);
  const prototypePreferenceRate = rate(prototypePreferredOverFree, eligibleParticipants);
  const checkoutStartRate = rate(checkoutStarted, experienceStarted);
  const thresholdsPassed =
    eligibleParticipants >= criteria.minimumEligibleParticipantsPerReading &&
    experienceStartRate >= criteria.minimumExperienceStartRate &&
    valueMomentRate >= criteria.minimumValueMomentRate &&
    readyResultUseRate >= criteria.minimumReadyResultUseRate &&
    prototypePreferenceRate >= criteria.minimumPrototypePreferenceRate &&
    checkoutStartRate >= criteria.minimumCheckoutStartRate;
  const safetyPassed =
    reading?.psiqueDecision === "APPROVE" && reading?.temisDecision === "APPROVE";

  if (
    eligibleParticipants !== null &&
    Number.isInteger(criteria.minimumEligibleParticipantsPerReading) &&
    eligibleParticipants < criteria.minimumEligibleParticipantsPerReading
  ) {
    reasons.push(
      `${reading?.readingId || "Leitura"} não atingiu a amostra mínima predeclarada.`,
    );
  }
  appendRateFailure(
    reasons,
    reading?.readingId,
    "início da experiência",
    experienceStartRate,
    criteria.minimumExperienceStartRate,
  );
  appendRateFailure(
    reasons,
    reading?.readingId,
    "microvalor",
    valueMomentRate,
    criteria.minimumValueMomentRate,
  );
  appendRateFailure(
    reasons,
    reading?.readingId,
    "uso do resultado pronto sem montagem",
    readyResultUseRate,
    criteria.minimumReadyResultUseRate,
  );
  appendRateFailure(
    reasons,
    reading?.readingId,
    "preferência sobre o gratuito",
    prototypePreferenceRate,
    criteria.minimumPrototypePreferenceRate,
  );
  appendRateFailure(
    reasons,
    reading?.readingId,
    "checkout iniciado",
    checkoutStartRate,
    criteria.minimumCheckoutStartRate,
  );
  if (reading?.psiqueDecision === "BLOCK") {
    reasons.push(`${reading?.readingId || "Leitura"} foi bloqueada por Psique.`);
  }
  if (reading?.temisDecision === "BLOCK") {
    reasons.push(`${reading?.readingId || "Leitura"} foi bloqueada por Têmis.`);
  }

  return {
    readingId: reading?.readingId || null,
    observedAt: observedAt?.toISOString() || null,
    eligibleParticipants,
    experienceStarted,
    valueMoments,
    readyResultsUsedWithoutAssembly,
    prototypePreferredOverFree,
    checkoutStarted,
    experienceStartRate,
    valueMomentRate,
    readyResultUseRate,
    prototypePreferenceRate,
    checkoutStartRate,
    psiqueDecision: reading?.psiqueDecision || null,
    temisDecision: reading?.temisDecision || null,
    eventSource: reading?.eventSource || null,
    testMarker: reading?.testMarker || null,
    passed: reasons.length === 0 && thresholdsPassed && safetyPassed,
    reasons,
  };
}

function evaluateHumanValueDelivery(candidate, contract, sourceById) {
  const reasons = [];
  const territories = Array.isArray(contract?.territories)
    ? [...new Set(contract.territories)]
    : [];
  if (
    territories.length === 0 ||
    territories.some((territory) => !HUMAN_VALUE_TERRITORIES.has(territory))
  ) {
    reasons.push("Território humano ausente ou fora do contrato canônico.");
  }
  const evidenceSourceIds = Array.isArray(contract?.evidenceSourceIds)
    ? [...new Set(contract.evidenceSourceIds)]
    : [];
  const evidenceSources = evidenceSourceIds.map((sourceId) => sourceById.get(sourceId));
  if (
    evidenceSourceIds.length < 2 ||
    evidenceSources.some(
      (source) => !source || source.candidateName !== candidate.name,
    ) ||
    new Set(evidenceSources.filter(Boolean).map((source) => source.pathway)).size < 2
  ) {
    reasons.push("Território humano não possui duas evidências independentes da candidata.");
  }
  for (const field of [
    "desiredTransformation",
    "readyMadeOutcome",
    "minimumCustomerInput",
    "automationBoundary",
  ]) {
    if (!contract?.[field]?.trim()) {
      reasons.push(`Entrega de valor humano sem ${field}.`);
    }
  }
  if (
    contract?.requiresPromptEngineering !== false ||
    contract?.requiresManualAssembly !== false ||
    contract?.usableWithoutAiKnowledge !== true
  ) {
    reasons.push("A entrega transfere prompting, montagem ou conhecimento de IA ao cliente.");
  }
  const customerStepsToValue = integer(contract?.customerStepsToValue);
  const timeToUsableResultMinutes = integer(contract?.timeToUsableResultMinutes);
  if (
    customerStepsToValue === null ||
    customerStepsToValue < 1 ||
    customerStepsToValue > 5
  ) {
    reasons.push("A entrega pronta deve chegar ao valor em um a cinco passos.");
  }
  if (
    timeToUsableResultMinutes === null ||
    timeToUsableResultMinutes < 1 ||
    timeToUsableResultMinutes > 10
  ) {
    reasons.push("O resultado pronto deve ficar utilizável em até dez minutos.");
  }
  if (
    Array.isArray(candidate?.humanValueTerritories) &&
    !sameStringSet(candidate.humanValueTerritories, territories)
  ) {
    reasons.push("A validação alterou os territórios humanos declarados na pesquisa.");
  }
  if (
    candidate?.readyMadeDeliverable?.trim() &&
    candidate.readyMadeDeliverable.trim() !== contract?.readyMadeOutcome?.trim()
  ) {
    reasons.push("A validação alterou o resultado pronto declarado na pesquisa.");
  }

  return {
    normalized: {
      territories,
      desiredTransformation: contract?.desiredTransformation || null,
      evidenceSourceIds,
      evidencePathways: [
        ...new Set(evidenceSources.filter(Boolean).map((source) => source.pathway)),
      ],
      readyMadeOutcome: contract?.readyMadeOutcome || null,
      minimumCustomerInput: contract?.minimumCustomerInput || null,
      requiresPromptEngineering: contract?.requiresPromptEngineering,
      requiresManualAssembly: contract?.requiresManualAssembly,
      usableWithoutAiKnowledge: contract?.usableWithoutAiKnowledge,
      customerStepsToValue,
      timeToUsableResultMinutes,
      automationBoundary: contract?.automationBoundary || null,
    },
    reasons,
  };
}

function sameStringSet(left, right) {
  const normalizedLeft = [...new Set(left)].sort();
  const normalizedRight = [...new Set(right)].sort();
  return JSON.stringify(normalizedLeft) === JSON.stringify(normalizedRight);
}

function appendRateFailure(reasons, readingId, metric, observed, minimum) {
  if (
    observed !== null &&
    Number.isFinite(minimum) &&
    observed < minimum
  ) {
    reasons.push(
      `${readingId || "Leitura"} não atingiu o mínimo predeclarado de ${metric}.`,
    );
  }
}

function candidateResult(candidateName, status, reasons) {
  return {
    candidateName,
    status,
    eligibleForFinalPrioritization: false,
    readings: [],
    reasons,
  };
}

function isPlaceholder(value) {
  return !String(value || "").trim() || /^(produto|oferta|curso|sem t[ií]tulo|n\/a)(\s+\d+)?$/i.test(String(value).trim());
}

function hasCommercialDetail(product) {
  return [
    product?.price,
    product?.tractionSignal,
    product?.temperature,
    product?.successScore,
    product?.rating,
    product?.reviewCount,
    product?.rankingPosition,
  ].some((value) => value !== null && value !== undefined && String(value).trim() !== "");
}

function parseInstant(value) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function integer(value) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : null;
}

function rate(numerator, denominator) {
  if (!Number.isInteger(numerator) || !Number.isInteger(denominator) || denominator === 0) return 0;
  return Number((numerator / denominator).toFixed(4));
}
