const REQUIRED_LIVE_COLLECTIONS = [
  "GARTNER",
  "IA_APLICADA",
  "MOMENTOS_COMPRA_B2C",
];

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
  const candidates = (research?.candidates || []).map((candidate) =>
    evaluateCandidate(
      candidate,
      validationsByCandidate.get(candidate.name),
      criteria,
      sourceQuality.passed,
    ),
  );
  const eligibleCandidateNames = candidates
    .filter((candidate) => candidate.eligibleForFinalPrioritization)
    .map((candidate) => candidate.candidateName);
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
  if (!gate.eligibleCandidateNames.includes(candidateName)) {
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
    "minimumPrototypePreferenceRate",
    "minimumCheckoutStartRate",
  ]) {
    if (!Number.isFinite(normalized[key]) || normalized[key] < 0 || normalized[key] > 1) {
      reasons.push(`${key} deve ficar entre 0 e 1.`);
    }
  }
  return { passed: reasons.length === 0, reasons, normalized };
}

function evaluateCandidate(candidate, validation, criteria, sourcesPassed) {
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
    [eligibleParticipants, experienceStarted, valueMoments, prototypePreferredOverFree, checkoutStarted]
      .some((value) => value === null)
  ) {
    reasons.push(`${reading?.readingId || "Leitura"} possui contagem inválida.`);
  } else if (
    experienceStarted > eligibleParticipants ||
    valueMoments > experienceStarted ||
    prototypePreferredOverFree > eligibleParticipants ||
    checkoutStarted > experienceStarted
  ) {
    reasons.push(`${reading.readingId} possui numerador maior que o denominador.`);
  }

  const experienceStartRate = rate(experienceStarted, eligibleParticipants);
  const valueMomentRate = rate(valueMoments, experienceStarted);
  const prototypePreferenceRate = rate(prototypePreferredOverFree, eligibleParticipants);
  const checkoutStartRate = rate(checkoutStarted, experienceStarted);
  const thresholdsPassed =
    eligibleParticipants >= criteria.minimumEligibleParticipantsPerReading &&
    experienceStartRate >= criteria.minimumExperienceStartRate &&
    valueMomentRate >= criteria.minimumValueMomentRate &&
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
    prototypePreferredOverFree,
    checkoutStarted,
    experienceStartRate,
    valueMomentRate,
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
