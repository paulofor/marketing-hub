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
    candidate.text &&
    typeof candidate.text === "string" &&
    candidate.type === "output_text"
  ) {
    const nested = extractObjectCandidates(candidate.text).find((entry) =>
      hasLandingPayloadShape(entry),
    );
    if (nested) return resolveLandingPayload(nested);
  }

  if (
    candidate.landingPageCopy &&
    typeof candidate.landingPageCopy === "object" &&
    !Array.isArray(candidate.landingPageCopy)
  ) {
    return candidate.landingPageCopy as JsonRecord;
  }
  if (typeof candidate.landingPageCopy === "string") {
    const nested = extractObjectCandidates(candidate.landingPageCopy).find((entry) =>
      hasLandingPayloadShape(entry),
    );
    if (nested) return nested;
  }
  if (
    candidate.copy &&
    typeof candidate.copy === "object" &&
    !Array.isArray(candidate.copy)
  ) {
    return candidate.copy as JsonRecord;
  }
  if (
    candidate.treatment &&
    typeof candidate.treatment === "object" &&
    !Array.isArray(candidate.treatment)
  ) {
    const nested = resolveLandingPayload(candidate.treatment as JsonRecord);
    if (nested) return nested;
  }
  if (
    candidate.control &&
    typeof candidate.control === "object" &&
    !Array.isArray(candidate.control)
  ) {
    const nested = resolveLandingPayload(candidate.control as JsonRecord);
    if (nested) return nested;
  }
  if (hasLandingPayloadShape(candidate)) {
    return candidate;
  }
  return candidate;
}

function hasLandingPayloadShape(candidate: JsonRecord): boolean {
  return Boolean(
    candidate.landingCurta ||
      candidate.landingCompleta ||
      candidate.landingLonga ||
      candidate.shortVersion ||
      candidate.longVersion,
  );
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
    heroPromise: pickText(payload.heroPromise ?? payload.promise),
    offerPromise: pickText(payload.offerPromise ?? payload.offer),
    heroTitle: pickText(payload.heroTitle ?? payload.headline),
    heroSubtitle: pickText(payload.heroSubtitle ?? payload.subheadline),
    heroBullets: toStringArray(payload.heroBullets ?? payload.bullets),
    primaryCTA: pickText(payload.primaryCTA ?? payload.cta ?? payload.ctaPrincipal),
    formMicrocopy: parseFormMicrocopy(payload.formMicrocopy ?? payload.form),
    formFields: parseFormFields(payload.formFields ?? payload.fields),
    benefitsSection: parseBlock(payload.benefitsSection ?? payload.benefits),
    howItWorksSection: parseBlock(payload.howItWorksSection ?? payload.howItWorks),
    proofSection: parseBlock(payload.proofSection ?? payload.proof),
    offerSection: parseBlock(payload.offerSection ?? payload.offerDetails),
    objectionHandlingSection: parseBlock(
      payload.objectionHandlingSection ?? payload.objections,
    ),
    faqSection: parseFaq(payload.faqSection ?? payload.faq),
    closingCTA: pickText(payload.closingCTA ?? payload.finalCTA),
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
      messageMatchSource: pickText(scope.messageMatchSource ?? scope.messageMatch ?? scope.headlineSource),
      landingCurta: parseVersion(scope.landingCurta ?? scope.shortVersion),
      landingCompleta: parseVersion(scope.landingCompleta ?? scope.landingLonga ?? scope.longVersion),
    };
    if (hasLandingCopyContent(parsed)) {
      return parsed;
    }
  }
  return undefined;
}
