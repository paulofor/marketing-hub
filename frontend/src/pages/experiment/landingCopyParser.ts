import {
  JsonRecord,
  extractObjectCandidates,
  pickText,
  toStringArray,
} from "./parserUtils";

export interface LandingCopyFormMicrocopy {
  headline?: string;
  support?: string;
  instructions?: string;
}

export interface LandingCopyFormField {
  label?: string;
  placeholder?: string;
  helper?: string;
  type?: string;
  required?: boolean;
}

export interface LandingCopyBlock {
  title?: string;
  subtitle?: string;
  description?: string;
  bullets?: string[];
  highlights?: string[];
  steps?: string[];
  rawText?: string;
}

export interface LandingCopyFaqItem {
  question?: string;
  answer?: string;
}

export interface LandingCopyVersion {
  heroPromise?: string;
  offerPromise?: string;
  heroTitle?: string;
  heroSubtitle?: string;
  heroBullets?: string[];
  primaryCTA?: string;
  formMicrocopy?: LandingCopyFormMicrocopy;
  formFields?: LandingCopyFormField[];
  benefitsSection?: LandingCopyBlock;
  howItWorksSection?: LandingCopyBlock;
  proofSection?: LandingCopyBlock;
  offerSection?: LandingCopyBlock;
  objectionHandlingSection?: LandingCopyBlock;
  faqSection?: LandingCopyFaqItem[];
  closingCTA?: string;
}

export interface LandingCopyContent {
  messageMatchSource?: string;
  landingCurta?: LandingCopyVersion;
  landingCompleta?: LandingCopyVersion;
}

function resolveLandingPayload(candidate: JsonRecord): JsonRecord | undefined {
  if (
    candidate.landingPageCopy &&
    typeof candidate.landingPageCopy === "object" &&
    !Array.isArray(candidate.landingPageCopy)
  ) {
    return candidate.landingPageCopy as JsonRecord;
  }
  if (
    candidate.copy &&
    typeof candidate.copy === "object" &&
    !Array.isArray(candidate.copy)
  ) {
    return candidate.copy as JsonRecord;
  }
  return candidate;
}

function hasVersionContent(version?: LandingCopyVersion): boolean {
  if (!version) return false;
  return Object.values(version).some((field) => {
    if (typeof field === "string") return field.trim().length > 0;
    if (Array.isArray(field)) return field.length > 0;
    if (typeof field === "object" && field) return true;
    return false;
  });
}

function parseFormMicrocopy(value: unknown): LandingCopyFormMicrocopy | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) return undefined;
  const payload = value as JsonRecord;
  const microcopy: LandingCopyFormMicrocopy = {
    headline: pickText(payload.headline),
    support: pickText(payload.support),
    instructions: pickText(payload.instructions),
  };
  return Object.values(microcopy).some(Boolean) ? microcopy : undefined;
}

function parseFormFields(value: unknown): LandingCopyFormField[] | undefined {
  if (!Array.isArray(value)) return undefined;
  const fields = value
    .map((entry) => {
      if (!entry || typeof entry !== "object" || Array.isArray(entry)) return undefined;
      const field = entry as JsonRecord;
      const parsed: LandingCopyFormField = {
        label: pickText(field.label ?? field.name),
        placeholder: pickText(field.placeholder),
        helper: pickText(field.helper ?? field.helpText),
        type: pickText(field.type),
        required: field.required === true,
      };
      const hasAny = Object.values(parsed).some((val) =>
        typeof val === "boolean" ? val : typeof val === "string" ? val.trim().length > 0 : false,
      );
      return hasAny ? parsed : undefined;
    })
    .filter((entry): entry is LandingCopyFormField => Boolean(entry));
  return fields.length > 0 ? fields : undefined;
}

function parseBlock(value: unknown): LandingCopyBlock | undefined {
  if (!value) return undefined;
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed ? { rawText: trimmed } : undefined;
  }
  if (typeof value !== "object" || Array.isArray(value)) return undefined;
  const payload = value as JsonRecord;
  const block: LandingCopyBlock = {
    title: pickText(payload.title),
    subtitle: pickText(payload.subtitle),
    description: pickText(payload.description ?? payload.copy ?? payload.body),
    bullets: toStringArray(payload.bullets) ?? toStringArray(payload.items),
    highlights: toStringArray(payload.highlights),
    steps: toStringArray(payload.steps),
    rawText: pickText(payload.rawText),
  };
  return Object.values(block).some((field) => {
    if (typeof field === "string") return field.trim().length > 0;
    if (Array.isArray(field)) return field.length > 0;
    return false;
  })
    ? block
    : undefined;
}

function parseFaq(value: unknown): LandingCopyFaqItem[] | undefined {
  if (!value) return undefined;
  const items = Array.isArray(value) ? value : (value as JsonRecord).items;
  if (!Array.isArray(items)) return undefined;
  const parsed = items
    .map((entry) => {
      if (!entry || typeof entry !== "object" || Array.isArray(entry)) return undefined;
      const payload = entry as JsonRecord;
      const item: LandingCopyFaqItem = {
        question: pickText(payload.question),
        answer: pickText(payload.answer ?? payload.response),
      };
      return item.question || item.answer ? item : undefined;
    })
    .filter((entry): entry is LandingCopyFaqItem => Boolean(entry));
  return parsed.length > 0 ? parsed : undefined;
}

function parseVersion(value: unknown): LandingCopyVersion | undefined {
  if (!value || typeof value !== "object" || Array.isArray(value)) return undefined;
  const payload = value as JsonRecord;
  const version: LandingCopyVersion = {
    heroPromise: pickText(payload.heroPromise),
    offerPromise: pickText(payload.offerPromise),
    heroTitle: pickText(payload.heroTitle),
    heroSubtitle: pickText(payload.heroSubtitle),
    heroBullets: toStringArray(payload.heroBullets),
    primaryCTA: pickText(payload.primaryCTA ?? payload.cta),
    formMicrocopy: parseFormMicrocopy(payload.formMicrocopy),
    formFields: parseFormFields(payload.formFields),
    benefitsSection: parseBlock(payload.benefitsSection),
    howItWorksSection: parseBlock(payload.howItWorksSection),
    proofSection: parseBlock(payload.proofSection),
    offerSection: parseBlock(payload.offerSection),
    objectionHandlingSection: parseBlock(payload.objectionHandlingSection),
    faqSection: parseFaq(payload.faqSection),
    closingCTA: pickText(payload.closingCTA),
  };
  return hasVersionContent(version) ? version : undefined;
}

export function hasLandingCopyContent(
  payload?: LandingCopyContent | null,
): payload is LandingCopyContent {
  if (!payload) return false;
  return Boolean(hasVersionContent(payload.landingCurta) || hasVersionContent(payload.landingCompleta));
}

export function parseLandingCopyPayload(raw?: string | null): LandingCopyContent | undefined {
  const candidates = extractObjectCandidates(raw);
  for (const candidate of candidates) {
    const scope = resolveLandingPayload(candidate);
    if (!scope) continue;
    const parsed: LandingCopyContent = {
      messageMatchSource: pickText(scope.messageMatchSource),
      landingCurta: parseVersion(scope.landingCurta),
      landingCompleta: parseVersion(scope.landingCompleta ?? scope.landingLonga),
    };
    if (hasLandingCopyContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}
