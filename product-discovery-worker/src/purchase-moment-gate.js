/** Qualifica fontes comerciais e abre a validação privada sem fabricar comportamento do cliente. */
export function buildPurchaseMomentResearchGate(job, marketplaceOffers, options = {}) {
  if (!requiresConsumerInstagramFocus(job)) {
    return {
      required: false,
      status: "NOT_REQUIRED",
      sourceQualityPassed: true,
      finalPrioritizationEligible: true,
      reasons: [],
    };
  }

  const evaluatedAt = parseDate(options.evaluatedAt) || new Date();
  const maxSourceAgeDays = Number(options.maxSourceAgeDays || 30);
  const reasons = [];
  const hotmartOffers = (marketplaceOffers || []).filter(
    (offer) => offer.marketplace === "HOTMART",
  );
  if (hotmartOffers.length === 0) reasons.push("Nenhuma oferta Hotmart nominal foi coletada.");
  for (const offer of hotmartOffers) {
    if (isPlaceholder(offer.title)) {
      reasons.push(`Oferta ${offer.referenceId || "sem identificador"} possui placeholder.`);
    }
    if (!offer.referenceId || !offer.url) {
      reasons.push("Oferta Hotmart possui identidade ou URL incompleta.");
    }
    if (!hasCommercialDetail(offer)) {
      reasons.push(
        `Oferta ${offer.referenceId || "sem identificador"} não possui preço nem sinal de tração.`,
      );
    }
    const collectedAt = parseDate(offer.collectedAt);
    if (!collectedAt) {
      reasons.push(`Oferta ${offer.referenceId || "sem identificador"} não possui data de coleta.`);
      continue;
    }
    const ageDays = (evaluatedAt.getTime() - collectedAt.getTime()) / 86_400_000;
    if (ageDays < 0 || ageDays > maxSourceAgeDays) {
      reasons.push(`Oferta ${offer.referenceId || "sem identificador"} está vencida.`);
    }
  }

  const sourceQualityPassed =
    Number.isInteger(maxSourceAgeDays) &&
    maxSourceAgeDays >= 1 &&
    maxSourceAgeDays <= 90 &&
    reasons.length === 0;
  if (!Number.isInteger(maxSourceAgeDays) || maxSourceAgeDays < 1 || maxSourceAgeDays > 90) {
    reasons.push("Validade das fontes deve ficar entre 1 e 90 dias.");
  }

  return {
    required: true,
    status: sourceQualityPassed
      ? "WAITING_PRIVATE_PROTOTYPE"
      : "WAITING_SOURCE_QUALITY",
    sourceQualityPassed,
    evaluatedAt: evaluatedAt.toISOString(),
    maxSourceAgeDays,
    finalPrioritizationEligible: false,
    requiredObservedSignals: [
      "EXPERIENCE_STARTED",
      "VALUE_MOMENT",
      "READY_RESULT_USED",
      "PREFERRED_OVER_FREE",
      "CHECKOUT_STARTED",
    ],
    humanValueDeliveryRequirements: {
      allowedTerritories: [
        "AFFECTION_AND_BELONGING",
        "RECOGNITION",
        "EFFORT_RELIEF",
      ],
      minimumIndependentEvidencePaths: 2,
      maximumCustomerStepsToValue: 5,
      maximumMinutesToUsableResult: 10,
      requiresPromptEngineering: false,
      requiresManualAssembly: false,
      usableWithoutAiKnowledge: true,
    },
    minimumIndependentReadings: 2,
    reasons,
    interpretation:
      "Pesquisa e intenção não substituem duas leituras privadas com resultado pronto realmente usado e critérios predeclarados; checkout de teste não é venda.",
  };
}

function requiresConsumerInstagramFocus(job) {
  return (
    /instagram/i.test(String(job?.acquisitionChannel || "")) &&
    /\bb2c\b|consumidor|pessoa f[ií]sica/i.test(
      `${job?.commercialConstraints || ""} ${job?.targetAudience || ""}`,
    )
  );
}

function isPlaceholder(value) {
  const normalized = String(value || "").trim();
  return (
    !normalized ||
    /^(produto|oferta|curso|sem t[ií]tulo|n\/a)(\s+\d+)?$/i.test(normalized)
  );
}

function hasCommercialDetail(offer) {
  return [
    offer?.price,
    offer?.tractionSignal,
    offer?.temperature,
    offer?.successScore,
    offer?.rating,
    offer?.reviewCount,
    offer?.rankingPosition,
  ].some((value) => value !== null && value !== undefined && String(value).trim() !== "");
}

function parseDate(value) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
