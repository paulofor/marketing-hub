import {
  JsonRecord,
  extractObjectCandidates,
  pickNumber,
  pickText,
  toStringArray,
} from "./parserUtils";

export interface ImagePromptBriefing {
  mustMatchAdVariant?: string;
  visualAngle?: string;
  assetType?: string;
  imageTextMaxWords?: number;
  visualBriefing?: string;
  hierarchy?: string;
  formatByPlacement?: string;
  safeMargins?: string;
  complianceNotes?: string;
  messageMatchNotes?: string;
  supportingKeywords?: string[];
}

export interface ImagePromptContent {
  briefings: ImagePromptBriefing[];
}

function resolveScope(candidate: JsonRecord): JsonRecord | undefined {
  if (
    candidate.adImageBriefing &&
    typeof candidate.adImageBriefing === "object" &&
    !Array.isArray(candidate.adImageBriefing)
  ) {
    return candidate.adImageBriefing as JsonRecord;
  }
  if (
    candidate.briefings &&
    typeof candidate.briefings === "object" &&
    !Array.isArray(candidate.briefings)
  ) {
    return candidate as JsonRecord;
  }
  return candidate;
}

function parseBriefing(value: unknown): ImagePromptBriefing | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return undefined;
  }

  const payload = value as JsonRecord;
  const briefing: ImagePromptBriefing = {
    mustMatchAdVariant: pickText(payload.mustMatchAdVariant),
    visualAngle: pickText(payload.visualAngle),
    assetType: pickText(payload.assetType),
    imageTextMaxWords: pickNumber(payload.imageTextMaxWords),
    visualBriefing: pickText(payload.visualBriefing),
    hierarchy: pickText(payload.hierarchy),
    formatByPlacement: pickText(payload.formatByPlacement),
    safeMargins: pickText(payload.safeMargins),
    complianceNotes: pickText(payload.complianceNotes),
    messageMatchNotes: pickText(payload.messageMatchNotes),
    supportingKeywords: toStringArray(payload.supportingKeywords),
  };

  const hasAny = Object.values(briefing).some((field) => {
    if (typeof field === "number") return true;
    if (typeof field === "string") return field.trim().length > 0;
    if (Array.isArray(field)) return field.length > 0;
    return false;
  });
  return hasAny ? briefing : undefined;
}

export function hasImagePromptContent(
  payload?: ImagePromptContent | null,
): payload is ImagePromptContent {
  return Boolean(payload?.briefings && payload.briefings.length > 0);
}

export function parseImagePromptPayload(raw?: string | null): ImagePromptContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolveScope(candidate);
    if (!scope) continue;
    const briefingsRaw = scope.briefings;
    if (!Array.isArray(briefingsRaw)) {
      continue;
    }
    const briefings = briefingsRaw
      .map((entry) => parseBriefing(entry))
      .filter((entry): entry is ImagePromptBriefing => Boolean(entry));
    if (briefings.length > 0) {
      return { briefings };
    }
  }
  return undefined;
}
