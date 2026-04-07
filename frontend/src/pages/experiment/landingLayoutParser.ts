import {
  JsonRecord,
  extractObjectCandidates,
  pickNumber,
  pickText,
  toStringArray,
} from "./parserUtils";

export interface LandingLayoutSection {
  sectionName?: string;
  objective?: string;
  contentType?: string;
  uiNotes?: string;
  mobilePriorityScore?: number;
  dropOffRisk?: string;
  sectionDependsOn?: string;
  supportingElements?: string[];
}

export interface LandingLayoutContent {
  pageGoal?: string;
  variantLayoutId?: string;
  sectionOrder?: LandingLayoutSection[];
  mobilePriorityNotes?: string;
  ctaPlacementNotes?: string;
  formPlacementNotes?: string;
}

function resolveLayoutPayload(candidate: JsonRecord): JsonRecord | undefined {
  if (
    candidate.artifact &&
    typeof candidate.artifact === "object" &&
    !Array.isArray(candidate.artifact)
  ) {
    const artifact = candidate.artifact as JsonRecord;
    if (
      artifact.content &&
      typeof artifact.content === "object" &&
      !Array.isArray(artifact.content)
    ) {
      return artifact.content as JsonRecord;
    }
  }
  if (
    candidate.landingPageWireframe &&
    typeof candidate.landingPageWireframe === "object" &&
    !Array.isArray(candidate.landingPageWireframe)
  ) {
    return candidate.landingPageWireframe as JsonRecord;
  }
  if (
    candidate.wireframe &&
    typeof candidate.wireframe === "object" &&
    !Array.isArray(candidate.wireframe)
  ) {
    return candidate.wireframe as JsonRecord;
  }
  return candidate;
}

function parseSection(value: unknown): LandingLayoutSection | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return undefined;
  }
  const payload = value as JsonRecord;
  const section: LandingLayoutSection = {
    sectionName: pickText(payload.sectionName ?? payload.name),
    objective: pickText(payload.objective),
    contentType: pickText(payload.contentType),
    uiNotes: pickText(payload.uiNotes ?? payload.notes),
    mobilePriorityScore: pickNumber(payload.mobilePriorityScore),
    dropOffRisk: pickText(payload.dropOffRisk),
    sectionDependsOn: pickText(payload.sectionDependsOn),
    supportingElements: toStringArray(payload.supportingElements),
  };
  const hasAny = Object.values(section).some((field) => {
    if (typeof field === "number") return true;
    if (typeof field === "string") return field.trim().length > 0;
    if (Array.isArray(field)) return field.length > 0;
    return false;
  });
  return hasAny ? section : undefined;
}

export function hasLandingLayoutContent(
  payload?: LandingLayoutContent | null,
): payload is LandingLayoutContent {
  if (!payload) return false;
  return Boolean(
    pickText(payload.pageGoal) ||
    pickText(payload.variantLayoutId) ||
    payload.sectionOrder?.length,
  );
}

export function parseLandingLayoutPayload(
  raw?: string | null,
): LandingLayoutContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolveLayoutPayload(candidate);
    if (!scope) continue;
    const sectionsRaw = scope.sectionOrder;
    const sectionOrder = Array.isArray(sectionsRaw)
      ? sectionsRaw
          .map((entry) => parseSection(entry))
          .filter((entry): entry is LandingLayoutSection => Boolean(entry))
      : [];
    const parsed: LandingLayoutContent = {
      pageGoal: pickText(scope.pageGoal),
      variantLayoutId: pickText(scope.variantLayoutId),
      sectionOrder,
      mobilePriorityNotes: pickText(scope.mobilePriorityNotes),
      ctaPlacementNotes: pickText(scope.ctaPlacementNotes),
      formPlacementNotes: pickText(scope.formPlacementNotes),
    };
    if (hasLandingLayoutContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}
