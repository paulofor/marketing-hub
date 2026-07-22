export interface PdePersuasiveJourneyStep {
  stageNumber?: number;
  stage: string;
  stageName?: string;
  aidaLabel?: string;
  psychologicalRole?: string;
  trackedSectionId?: string;
  trackedSectionIds?: string[];
  eventNames?: string[];
  commercialFunction?: string;
  userShift?: string;
  primaryMetric?: string;
  optimizationRule?: string;
}

export interface PdePersuasiveJourney {
  version?: string;
  framework?: string;
  psychologicalModel?: string;
  name?: string;
  objective?: string;
  productSlug?: string;
  commercialPromise?: string;
  steps?: PdePersuasiveJourneyStep[];
}

export function parsePdePersuasiveJourney(
  pdeExperienceJson?: string | null,
): PdePersuasiveJourney | null {
  if (!pdeExperienceJson?.trim()) return null;
  try {
    const parsed = JSON.parse(pdeExperienceJson);
    const journey = parsed?.persuasiveJourney;
    if (!journey || typeof journey !== "object") return null;
    return journey as PdePersuasiveJourney;
  } catch {
    return null;
  }
}
