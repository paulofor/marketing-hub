import { JsonRecord, extractObjectCandidates, pickText } from "./parserUtils";

export interface LandingHtmlContent {
  htmlDocument?: string;
  summary?: string;
}

function resolveLandingHtmlPayload(candidate: JsonRecord): JsonRecord | undefined {
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
  return Boolean(pickText(payload.htmlDocument) || pickText(payload.summary));
}

export function parseLandingHtmlPayload(raw?: string | null): LandingHtmlContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolveLandingHtmlPayload(candidate);
    if (!scope) continue;
    const parsed: LandingHtmlContent = {
      htmlDocument: pickText(scope.htmlDocument),
      summary: pickText(scope.summary),
    };
    if (hasLandingHtmlContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}
