const HTML_HINT = /<\s*!doctype|<\s*html|<\s*body|<\s*(?:main|section|div|header|footer)/i;
const FENCED_JSON = /```(?:json)?\s*([\s\S]*?)```/i;

export interface CustomTemplateFormFieldSpec {
  name: string;
  type: "text" | "email" | "tel";
  label: string;
  required: boolean;
  placeholder?: string;
}

export interface CustomTemplateFormSpec {
  formId: string;
  title?: string;
  submitLabel: string;
  submitTarget?: string;
  fields: CustomTemplateFormFieldSpec[];
  consent?: {
    enabled?: boolean;
    required?: boolean;
    label?: string;
  };
  successState?: {
    title?: string;
    message?: string;
  };
  cta?: {
    label?: string;
    target?: string;
    variant?: string;
  };
}

export interface CustomTemplatePayload {
  html: string;
  formSpec?: CustomTemplateFormSpec;
}

export function normalizeCustomTemplateHtml(raw?: string | null): string | undefined {
  return normalizeCustomTemplatePayload(raw)?.html;
}

export function normalizeCustomTemplatePayload(raw?: string | null): CustomTemplatePayload | undefined {
  if (typeof raw !== "string") {
    return undefined;
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return undefined;
  }
  if (looksLikeHtml(trimmed)) {
    return { html: trimmed };
  }
  return extractFromJson(trimmed);
}

function extractFromJson(text: string): CustomTemplatePayload | undefined {
  const parsed = tryParseJson(text) ?? tryParseJson(extractEmbeddedJson(text) ?? "");
  if (!parsed) {
    return undefined;
  }
  return findPayloadInValue(parsed);
}

function findPayloadInValue(value: unknown): CustomTemplatePayload | undefined {
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) {
      return undefined;
    }
    if (looksLikeHtml(trimmed)) {
      return { html: trimmed };
    }
    if (looksLikeJsonCandidate(trimmed)) {
      return extractFromJson(trimmed);
    }
    return undefined;
  }
  if (Array.isArray(value)) {
    for (const entry of value) {
      const payload = findPayloadInValue(entry);
      if (payload) {
        return payload;
      }
    }
    return undefined;
  }
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    if (record.htmlDocument) {
      const htmlPayload = findPayloadInValue(record.htmlDocument);
      if (htmlPayload) {
        return {
          html: htmlPayload.html,
          formSpec: normalizeFormSpec(record.formSpec),
        };
      }
    }
    for (const entry of Object.values(record)) {
      const payload = findPayloadInValue(entry);
      if (payload) {
        return payload;
      }
    }
  }
  return undefined;
}

function normalizeFormSpec(raw: unknown): CustomTemplateFormSpec | undefined {
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const record = raw as Record<string, unknown>;
  const formId = asNonEmptyString(record.formId);
  const submitLabel = asNonEmptyString(record.submitLabel);
  const fields = normalizeFields(record.fields);
  if (!formId || !submitLabel || fields.length === 0) {
    return undefined;
  }
  return {
    formId,
    title: asNonEmptyString(record.title),
    submitLabel,
    submitTarget: asNonEmptyString(record.submitTarget),
    fields,
    consent: normalizeConsent(record.consent),
    successState: normalizeSuccessState(record.successState),
    cta: normalizeCta(record.cta),
  };
}

function normalizeFields(raw: unknown): CustomTemplateFormFieldSpec[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .map((entry) => {
      if (!entry || typeof entry !== "object") {
        return null;
      }
      const field = entry as Record<string, unknown>;
      const name = asNonEmptyString(field.name);
      const type = asFieldType(field.type);
      const label = asNonEmptyString(field.label);
      const required = typeof field.required === "boolean" ? field.required : false;
      if (!name || !type || !label) {
        return null;
      }
      return {
        name,
        type,
        label,
        required,
        placeholder: asNonEmptyString(field.placeholder),
      };
    })
    .filter((entry): entry is CustomTemplateFormFieldSpec => Boolean(entry));
}

function normalizeConsent(raw: unknown) {
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const consent = raw as Record<string, unknown>;
  return {
    enabled: typeof consent.enabled === "boolean" ? consent.enabled : undefined,
    required: typeof consent.required === "boolean" ? consent.required : undefined,
    label: asNonEmptyString(consent.label),
  };
}

function normalizeSuccessState(raw: unknown) {
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const successState = raw as Record<string, unknown>;
  return {
    title: asNonEmptyString(successState.title),
    message: asNonEmptyString(successState.message),
  };
}

function normalizeCta(raw: unknown) {
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const cta = raw as Record<string, unknown>;
  return {
    label: asNonEmptyString(cta.label),
    target: asNonEmptyString(cta.target),
    variant: asNonEmptyString(cta.variant),
  };
}

function asNonEmptyString(raw: unknown): string | undefined {
  if (typeof raw !== "string") {
    return undefined;
  }
  const trimmed = raw.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function asFieldType(raw: unknown): "text" | "email" | "tel" | undefined {
  const normalized = asNonEmptyString(raw)?.toLowerCase();
  if (normalized === "text" || normalized === "email" || normalized === "tel") {
    return normalized;
  }
  return undefined;
}

function looksLikeHtml(value: string): boolean {
  return HTML_HINT.test(value);
}

function looksLikeJsonCandidate(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.startsWith("{") || trimmed.startsWith("[");
}

function tryParseJson(value?: string): Record<string, unknown> | unknown[] | undefined {
  const trimmed = value?.trim();
  if (!trimmed || !looksLikeJsonCandidate(trimmed)) {
    return undefined;
  }
  try {
    return JSON.parse(trimmed) as Record<string, unknown> | unknown[];
  } catch {
    return undefined;
  }
}

function extractEmbeddedJson(text: string): string | undefined {
  if (!text) {
    return undefined;
  }
  const fenced = text.match(FENCED_JSON)?.[1]?.trim();
  if (fenced) {
    return fenced;
  }
  const firstBrace = text.indexOf("{");
  const lastBrace = text.lastIndexOf("}");
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    return text.slice(firstBrace, lastBrace + 1);
  }
  return undefined;
}
