import { JsonRecord, extractObjectCandidates, pickText } from "./parserUtils";

export interface LandingHtmlContent {
  htmlDocument?: string;
  summary?: string;
  canonicalInputHash?: string;
  deterministic?: LandingHtmlVariantContent;
  ai?: LandingHtmlVariantContent;
}

export interface LandingHtmlVariantContent {
  htmlDocument?: string;
  publicUrl?: string;
  validationStatus?: string;
}

function resolveLandingHtmlPayload(
  candidate: JsonRecord,
): JsonRecord | undefined {
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
    candidate.landingPageHtml &&
    typeof candidate.landingPageHtml === "object" &&
    !Array.isArray(candidate.landingPageHtml)
  ) {
    return candidate.landingPageHtml as JsonRecord;
  }
  return candidate;
}

export function hasLandingHtmlContent(
  payload?: LandingHtmlContent | null,
): payload is LandingHtmlContent {
  if (!payload) return false;
  return Boolean(
    pickText(payload.htmlDocument) ||
      pickText(payload.summary) ||
      pickText(payload.deterministic?.htmlDocument) ||
      pickText(payload.ai?.htmlDocument),
  );
}

export function parseLandingHtmlPayload(
  raw?: string | null,
): LandingHtmlContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolveLandingHtmlPayload(candidate);
    if (!scope) continue;
    const parsed: LandingHtmlContent = {
      canonicalInputHash: pickText(scope.canonicalInputHash),
      htmlDocument: pickText(scope.htmlDocument),
      summary: pickText(scope.summary),
      deterministic: extractVariant(scope, "deterministic"),
      ai: extractVariant(scope, "ai"),
    };
    if (!parsed.htmlDocument) {
      parsed.htmlDocument =
        pickText(parsed.deterministic?.htmlDocument) ??
        pickText(parsed.ai?.htmlDocument);
    }
    if (hasLandingHtmlContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

function extractVariant(
  scope: JsonRecord,
  variantKey: "deterministic" | "ai",
): LandingHtmlVariantContent | undefined {
  const htmlVariants =
    scope.htmlVariants &&
    typeof scope.htmlVariants === "object" &&
    !Array.isArray(scope.htmlVariants)
      ? (scope.htmlVariants as JsonRecord)
      : undefined;
  if (!htmlVariants) return undefined;

  const variant =
    htmlVariants[variantKey] &&
    typeof htmlVariants[variantKey] === "object" &&
    !Array.isArray(htmlVariants[variantKey])
      ? (htmlVariants[variantKey] as JsonRecord)
      : undefined;
  if (!variant) return undefined;

  const validationSummary =
    variant.validationSummary &&
    typeof variant.validationSummary === "object" &&
    !Array.isArray(variant.validationSummary)
      ? (variant.validationSummary as JsonRecord)
      : undefined;

  const parsed: LandingHtmlVariantContent = {
    htmlDocument: pickText(variant.htmlDocument),
    publicUrl: pickText(variant.publicUrl),
    validationStatus: pickText(validationSummary?.status),
  };
  return parsed;
}
